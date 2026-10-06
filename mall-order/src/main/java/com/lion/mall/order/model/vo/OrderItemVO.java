package com.lion.mall.order.model.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细响应对象
 *
 * @author lion
 */
@Data
public class OrderItemVO {

    /** 明细ID */
    private Long id;
    /** 商品ID */
    private Long productId;
    /** 商品名称 */
    private String productName;
    /** 商品图片 */
    private String productImage;
    /** 下单单价 */
    private BigDecimal productPrice;
    /** 购买数量 */
    private Integer quantity;
    /** 小计金额 */
    private BigDecimal totalAmount;
}
