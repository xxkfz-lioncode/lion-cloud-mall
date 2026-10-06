package com.lion.mall.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单响应对象
 *
 * @author lion
 */
@Data
public class OrderVO {

    /** 订单ID */
    private Long id;
    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 订单总金额 */
    private BigDecimal totalAmount;
    /** 订单状态编码 */
    private Integer status;
    /** 订单状态描述 */
    private String statusDesc;
    /** 收货地址 */
    private String address;
    /** 收货人 */
    private String receiverName;
    /** 收货人手机号 */
    private String receiverPhone;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 支付时间 */
    private LocalDateTime payTime;
    /** 订单明细 */
    private List<OrderItemVO> items;
}
