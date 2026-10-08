package com.lion.mall.order.mq;

import com.lion.mall.order.config.RabbitConfig;
import com.lion.mall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 超时关单消费者：消费延迟消息，检查订单是否仍未支付，是则关单并回滚库存。
 * <p>
 * <b>幂等由 {@link OrderService#closeTimeoutOrder(Long)} 内部保证</b>：
 * 里面用的是 CAS 条件更新（`where id = ? and status = 0`），
 * 所以即使消息重投、或与用户支付/取消并发，库存也只会被回滚一次。
 *
 * @author lion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitConfig.ORDER_CLOSE_QUEUE)
    public void handleClose(String orderIdStr, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Long orderId = Long.parseLong(orderIdStr.trim());
            // 内部已做状态校验 + CAS：已支付 / 已取消的订单会被直接跳过
            orderService.closeTimeoutOrder(orderId);
            channel.basicAck(deliveryTag, false);
            log.info("延迟消息关单处理完成：orderId={}", orderId);
        } catch (NumberFormatException e) {
            // 消息体本身就是坏的，重试也没用 → 直接丢弃，避免毒消息循环
            log.error("消息体不是合法订单号，丢弃：body={}", orderIdStr);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("处理超时关单失败，消息重新入队：orderId={}", orderIdStr, e);
            // requeue=true 重新入队重试。
            // 生产建议：加「重试次数上限 + 超限后进死信队列」，避免坏消息无限循环。
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
