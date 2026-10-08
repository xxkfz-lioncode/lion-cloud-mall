package com.lion.mall.order.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑配置：超时关单（延迟消息）+ 下单短信通知。
 * <p>
 * <b>延迟消息的实现方式：TTL + 死信交换机（DLX），不需要任何插件。</b>
 *
 * <pre>
 * 下单成功
 *   └─► order.delay.exchange ─► order.delay.queue   （TTL = 30 分钟，没有消费者）
 *                                     │ 消息到期
 *                                     ▼  RabbitMQ 自动投递到死信交换机
 *                               order.close.exchange ─► order.close.queue ─► 关单消费者
 * </pre>
 *
 * <b>为什么不用定时轮询</b>：轮询要每分钟扫一次库，且最多只能做到分钟级精度；
 * 延迟消息由 broker 计时，精确到毫秒，且不产生任何数据库扫描压力。
 * <p>
 * <b>注意</b>：队列的参数（TTL、死信配置）只在<b>首次创建</b>时生效。
 * 改了 `order.timeout-minutes` 后，需要在控制台删掉 `order.delay.queue` 或直接删除
 * RabbitMQ 数据卷，否则新 TTL 不会应用。
 *
 * @author lion
 */
@Configuration
public class RabbitConfig {

    // ===== 超时关单：延迟队列 + 死信 =====
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    public static final String ORDER_CLOSE_EXCHANGE = "order.close.exchange";
    public static final String ORDER_CLOSE_QUEUE = "order.close.queue";
    public static final String ORDER_CLOSE_ROUTING_KEY = "order.close";

    // ===== 下单成功通知 =====
    public static final String ORDER_NOTIFY_EXCHANGE = "order.notify.exchange";
    public static final String SMS_QUEUE = "order.sms.queue";
    public static final String SMS_ROUTING_KEY = "order.sms";

    /** 订单超时分钟数 → 决定延迟队列的 TTL */
    @Value("${order.timeout-minutes:30}")
    private int timeoutMinutes;

    // ---------- 延迟侧 ----------

    @Bean
    public TopicExchange orderDelayExchange() {
        return new TopicExchange(ORDER_DELAY_EXCHANGE, true, false);
    }

    /**
     * 延迟队列：<b>不挂任何消费者</b>，消息在这里静静等到 TTL 过期，
     * 过期后由 broker 自动转发到死信交换机。
     */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                // 队列级 TTL：本项目所有订单超时时间一致，用队列级比消息级更高效
                .ttl(timeoutMinutes * 60 * 1000)
                .deadLetterExchange(ORDER_CLOSE_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CLOSE_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(orderDelayExchange())
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    // ---------- 死信侧（真正被消费） ----------

    @Bean
    public TopicExchange orderCloseExchange() {
        return new TopicExchange(ORDER_CLOSE_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCloseQueue() {
        return QueueBuilder.durable(ORDER_CLOSE_QUEUE).build();
    }

    @Bean
    public Binding orderCloseBinding() {
        return BindingBuilder.bind(orderCloseQueue())
                .to(orderCloseExchange())
                .with(ORDER_CLOSE_ROUTING_KEY);
    }

    // ===== WebSocket 推送（多实例广播） =====
    public static final String WS_FANOUT_EXCHANGE = "order.ws.exchange";

    // ---------- 下单通知 ----------

    @Bean
    public TopicExchange orderNotifyExchange() {
        return new TopicExchange(ORDER_NOTIFY_EXCHANGE, true, false);
    }

    /**
     * fanout 交换机：广播给所有绑定队列，用于把通知推送到每个服务实例。
     */
    @Bean
    public FanoutExchange wsExchange() {
        return new FanoutExchange(WS_FANOUT_EXCHANGE, true, false);
    }

    /**
     * 匿名队列：<b>每个服务实例启动时生成一个名字唯一的队列</b>，再绑定到上面的 fanout 交换机。
     * <p>
     * 为什么不能用固定队列名：多实例共用同名队列时，它们会「争抢」同一队列里的消息，
     * 每条消息只会被其中一个实例取走 —— 结果就是连在另一个实例上的用户收不到推送。
     * 用匿名队列，每个实例各收一份，由持有该用户 WebSocket 连接的实例负责推送。
     */
    @Bean
    public Queue wsQueue() {
        return new AnonymousQueue();
    }

    /**
     * 绑定用「方法调用」而不是「参数注入」：
     * 容器里有 4 个 Queue bean（delay / close / ws / sms），按类型注入无法区分，
     * 且项目编译时未开启 {@code -parameters}，参数名丢失后也无法按名字匹配，
     * 会直接启动失败。方法调用则由 {@code @Configuration} 的 CGLIB 代理拦截，返回的都是同一个单例。
     */
    @Bean
    public Binding wsBinding() {
        return BindingBuilder.bind(wsQueue()).to(wsExchange());
    }

    /**
     * 短信通知队列。下单主流程只管往这里丢消息，发送短信由消费者异步执行 ——
     * 短信服务的耗时与失败都不会拖慢下单响应。
     */
    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE).build();
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder.bind(smsQueue())
                .to(orderNotifyExchange())
                .with(SMS_ROUTING_KEY);
    }
}
