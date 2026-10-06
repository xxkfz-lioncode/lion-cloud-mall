package com.lion.mall.order.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态
 *
 * @author lion
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    /** 待支付 */
    WAIT_PAY(0, "待支付"),
    /** 已支付 */
    PAID(1, "已支付"),
    /** 已取消 */
    CANCELED(2, "已取消");

    private final Integer code;
    private final String desc;

    /** 根据 code 获取枚举 */
    public static OrderStatus of(Integer code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
