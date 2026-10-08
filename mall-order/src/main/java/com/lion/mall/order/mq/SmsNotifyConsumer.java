package com.lion.mall.order.mq;

import com.lion.mall.order.config.RabbitConfig;
import com.lion.mall.order.entity.Order;
import com.lion.mall.order.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 下单成功短信通知消费者。
 * <p>
 * 下单主流程（{@code OrderServiceImpl#create}）只负责往队列里丢一条消息就返回，
 * 短信的耗时、失败重试都不影响下单接口响应时间 —— 这就是 MQ 的<b>异步解耦</b>。
 * <p>
 * 本项目没有真实短信服务，用日志模拟；真实项目把 {@code log.info} 换成
 * 短信服务商 SDK（阿里云 / 腾讯云）的调用即可。
 *
 * @author lion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotifyConsumer {

    private final OrderMapper orderMapper;

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void handleSms(String orderIdStr, Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Long orderId = Long.parseLong(orderIdStr.trim());
            // 回查订单拿最新数据（不信任消息里的快照）
            Order order = orderMapper.selectById(orderId);
            if (order == null) {
                // 订单不存在：通常是下单的全局事务回滚了，不该通知用户
                log.warn("订单不存在，跳过短信通知：orderId={}", orderId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // TODO 真实项目：调用短信服务商 SDK 发送
            log.info("【短信通知】发送至 {}：您已成功下单，订单号 {}，金额 {} 元，请在 30 分钟内完成支付",
                    order.getReceiverPhone(), order.getOrderNo(), order.getTotalAmount());

            channel.basicAck(deliveryTag, false);
        } catch (NumberFormatException e) {
            log.error("消息体不是合法订单号，丢弃：body={}", orderIdStr);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("处理短信通知失败，消息重新入队：orderId={}", orderIdStr, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
