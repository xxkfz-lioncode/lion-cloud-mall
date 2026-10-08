package com.lion.mall.order.ws;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送给前端的通知消息体（也是 WebSocket 的 JSON 载荷）。
 *
 * @author lion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage {

    /**
     * 消息类型，前端据此区分样式：
     * <ul>
     *   <li>{@link #TYPE_CREATED}：下单成功待支付（提醒去付钱）</li>
     *   <li>{@link #TYPE_PAID}：支付成功</li>
     *   <li>{@link #TYPE_CLOSED}：超时未支付，订单已关闭</li>
     * </ul>
     */
    public static final String TYPE_CREATED = "ORDER_CREATED";
    public static final String TYPE_PAID = "ORDER_PAID";
    public static final String TYPE_CLOSED = "ORDER_CLOSED";

    /** 接收用户的 ID（用于找到对应的 WebSocket 连接） */
    private Long userId;

    /** 类型，见上面的常量 */
    private String type;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 订单号 */
    private String orderNo;

    /** 订单金额 */
    private String amount;
}
