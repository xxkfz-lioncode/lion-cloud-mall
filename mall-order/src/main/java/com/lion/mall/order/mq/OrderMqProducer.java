package com.lion.mall.order.mq;

import com.lion.mall.order.config.RabbitConfig;
import com.lion.mall.order.ws.NotifyMessage;
import com.lion.mall.order.ws.NotifyMessageCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 订单相关消息的生产者。
 * <p>
 * 消息体统一只传 <b>orderId 字符串</b>：
 * 消费者收到后再回查订单拿最新数据。好处是消息小、不含快照，
 * 而且天然避免了「消息发出后订单又被修改」导致的数据不一致。
 *
 * @author lion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMqProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 下单成功 → 发延迟消息。
     * 消息先进入 {@link RabbitConfig#ORDER_DELAY_QUEUE} 等待，
     * TTL 到期后自动流转到关单队列，由 {@link OrderCloseConsumer} 处理。
     */
    public void sendTimeoutCheck(Long orderId) {
        send(RabbitConfig.ORDER_DELAY_EXCHANGE, RabbitConfig.ORDER_DELAY_ROUTING_KEY, orderId, "超时关单延迟消息");
    }

    /**
     * 下单成功 → 发短信通知消息（异步解耦，不阻塞下单主流程）。
     */
    public void sendSmsNotify(Long orderId) {
        send(RabbitConfig.ORDER_NOTIFY_EXCHANGE, RabbitConfig.SMS_ROUTING_KEY, orderId, "下单短信通知消息");
    }

    /**
     * 下单成功（待支付）→ 向前端推送通知。
     * 走 fanout 交换机广播，所有实例都会收到，由持有该用户 WebSocket 连接的实例真正推送。
     */
    public void pushOrderCreated(Long userId, String orderNo, BigDecimal amount) {
        broadcast(NotifyMessage.builder()
                .userId(userId)
                .type(NotifyMessage.TYPE_CREATED)
                .title("下单成功，待支付")
                .content("订单已提交，请在 30 分钟内完成支付，超时将自动取消")
                .orderNo(orderNo)
                .amount(amount == null ? null : amount.toPlainString())
                .build());
    }

    /**
     * 支付成功 → 向前端推送通知（与「待支付」用不同的 type，前端区分样式）。
     */
    public void pushOrderPaid(Long userId, String orderNo, BigDecimal amount) {
        broadcast(NotifyMessage.builder()
                .userId(userId)
                .type(NotifyMessage.TYPE_PAID)
                .title("支付成功")
                .content("订单已完成支付，我们会尽快为您发货")
                .orderNo(orderNo)
                .amount(amount == null ? null : amount.toPlainString())
                .build());
    }

    /** 广播到 fanout 交换机（fanout 忽略 routingKey，传空串即可） */
    private void broadcast(NotifyMessage message) {
        String json = NotifyMessageCodec.encode(message);
        if (json == null) {
            return;
        }
        rabbitTemplate.convertAndSend(RabbitConfig.WS_FANOUT_EXCHANGE, "", json);
        log.info("已广播 WebSocket 通知：userId={}, type={}, orderNo={}",
                message.getUserId(), message.getType(), message.getOrderNo());
    }

    private void send(String exchange, String routingKey, Long orderId, String desc) {
        // CorrelationData 用于生产者确认回调（publisher-confirm），便于排查发送失败
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, String.valueOf(orderId), correlationData);
        log.info("已发送{}：orderId={}", desc, orderId);
    }
}
