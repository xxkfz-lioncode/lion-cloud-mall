package com.lion.mall.order.ws;

import cn.dev33.satoken.stp.StpUtil;
import com.rabbitmq.client.Channel;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单通知 WebSocket 端点：后端主动推送消息给前端。
 * <p>
 * 连接地址：{@code ws://网关地址/ws/notify?token=<sa-token>}
 * <p>
 * <b>为什么要用 static Map 存 Session</b>：
 * {@code @ServerEndpoint} 修饰的类由 WebSocket 容器（Tomcat）管理，
 * <b>每个连接都会 new 一个新实例</b>，并且不走 Spring 的依赖注入。
 * 所以会话表必须声明成 static 才能被所有实例共享。
 * <p>
 * <b>为什么鉴权要传 token 而不是直接读 Header</b>：
 * 浏览器原生 WebSocket API 不允许自定义请求头，只能在 URL 的 query 上带参数，
 * 因此这里从 {@code ?token=} 取值，再用 Sa-Token 校验。
 *
 * @author lion
 */
@Slf4j
@Component
@ServerEndpoint("/ws/notify")
public class OrderNotifyWebSocket {

    /** userId → 该用户的所有连接（同一个账号可能开多个标签页） */
    private static final Map<Long, Set<Session>> SESSIONS = new ConcurrentHashMap<>();
    private static final String USER_ID_KEY = "userId";

    @OnOpen
    public void onOpen(Session session) throws IOException {
        String token = getToken(session);
        Long userId = resolveUserId(token);
        if (userId == null) {
            log.warn("WebSocket 连接被拒绝：token 无效或未传");
            session.close();
            return;
        }
        session.getUserProperties().put(USER_ID_KEY, userId);
        SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("WebSocket 连接建立：userId={}，当前在线 {} 个用户", userId, SESSIONS.size());
    }

    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get(USER_ID_KEY);
        if (userId == null) {
            return;
        }
        Set<Session> set = SESSIONS.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                SESSIONS.remove(userId);
            }
        }
        log.info("WebSocket 连接关闭：userId={}，剩余在线 {} 个用户", userId, SESSIONS.size());
    }

    @OnError
    public void onError(Session session, Throwable e) {
        log.error("WebSocket 异常", e);
    }

    /**
     * 接收前端消息。
     * <p>
     * <b>这个方法必须存在</b>：Tomcat 的 WebSocket 端点如果没有 {@code @OnMessage}，
     * 收到任何消息都会因找不到消息处理器而报错并关闭连接。
     * 所以即使我们只需要单向推送，也要提供一个入口来吃掉前端的心跳包。
     */
    @jakarta.websocket.OnMessage
    public void onMessage(Session session, String msg) {
        // 心跳保活包，忽略即可（TCP 之上的应用层保活，防止代理/NAT 超时断开）
        if ("ping".equalsIgnoreCase(msg)) {
            return;
        }
        log.debug("收到 WebSocket 消息（暂不处理）：{}", msg);
    }

    /**
     * 向指定用户推送消息（只推本实例持有的连接）。
     * <p>
     * 多实例场景下，别的实例上也可能有该用户的连接，
     * 靠 {@link #onNotify(String, Message, Channel)} 接收 MQ 广播来覆盖 —— 每个实例都会收到一份。
     *
     * @return true = 本实例确实推送出去了
     */
    public static boolean sendToUser(Long userId, String payload) {
        Set<Session> sessions = SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        sessions.removeIf(s -> s == null || !s.isOpen());
        boolean pushed = false;
        for (Session session : sessions) {
            try {
                session.getAsyncRemote().sendText(payload);
                pushed = true;
            } catch (Exception e) {
                log.error("WebSocket 推送失败：userId={}", userId, e);
            }
        }
        return pushed;
    }

    /**
     * 接收 MQ 广播的通知，并推送给本机连接的用户。
     * <p>
     * 队列是<b>每个实例专属的匿名队列</b>（见 {@code RabbitConfig#wsQueue}），
     * 配合 fanout 交换机，所有实例都会各收到一份消息。
     */
    @RabbitListener(queues = "#{@wsQueue.name}")
    public void onNotify(String json, Message message, Channel channel) throws Exception {
        long tag = message.getMessageProperties().getDeliveryTag();
        try {
            NotifyMessage msg = NotifyMessageCodec.decode(json);
            if (msg != null && msg.getUserId() != null) {
                boolean pushed = sendToUser(msg.getUserId(), json);
                log.info("收到推送广播：userId={}，type={}，本实例是否有该用户连接={}",
                        msg.getUserId(), msg.getType(), pushed);
            }
            channel.basicAck(tag, false);
        } catch (Exception e) {
            log.error("处理推送通知失败", e);
            channel.basicNack(tag, false, false);
        }
    }

    /** 从 URL query 里取出 token */
    private String getToken(Session session) {
        String query = session.getQueryString();
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int i = pair.indexOf('=');
            if (i > 0 && "token".equals(pair.substring(0, i))) {
                return URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    /** 用 Sa-Token 把 token 换成 userId；无效返回 null */
    private Long resolveUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Object loginId = StpUtil.getLoginIdByToken(token);
            return Long.parseLong(String.valueOf(loginId));
        } catch (Exception e) {
            return null;
        }
    }
}
