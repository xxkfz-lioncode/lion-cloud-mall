package com.lion.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lion.mall.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体（t_order）
 *
 * @author lion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {

    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 订单状态：0-待支付 1-已支付 2-已取消 */
    private Integer status;
    /** 收货地址 */
    private String address;
    /** 收货人 */
    private String receiverName;
    /** 收货人手机号 */
    private String receiverPhone;
    /** 备注 */
    private String remark;
    /** 支付时间 */
    private LocalDateTime payTime;
}
