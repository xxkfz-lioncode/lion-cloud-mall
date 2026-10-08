package com.lion.mall.order.ws;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知消息的 JSON 编解码（统一用 Hutool 的 {@link JSONUtil}，不额外引入 Jackson/Gson）。
 *
 * @author lion
 */
@Slf4j
public final class NotifyMessageCodec {

    private NotifyMessageCodec() {
    }

    /**
     * 解析消息体，失败返回 null（避免坏消息导致消费者崩溃）。
     * <p>
     * 注意：Hutool 的 toBean 依赖无参构造器 + getter/setter，
     * {@link NotifyMessage} 用 Lombok 的 {@code @NoArgsConstructor + @Data} 已满足。
     */
    public static NotifyMessage decode(String json) {
        try {
            return JSONUtil.toBean(json, NotifyMessage.class);
        } catch (Exception e) {
            log.error("通知消息 JSON 解析失败：{}", json, e);
            return null;
        }
    }

    public static String encode(NotifyMessage message) {
        try {
            return JSONUtil.toJsonStr(message);
        } catch (Exception e) {
            log.error("通知消息序列化失败", e);
            return null;
        }
    }
}
