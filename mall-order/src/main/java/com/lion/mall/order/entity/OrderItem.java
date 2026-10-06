package com.lion.mall.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lion.mall.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单明细实体（t_order_item）
 *
 * @author lion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order_item")
public class OrderItem extends BaseEntity {

    /** 订单ID */
    private Long orderId;
    /** 商品ID */
    private Long productId;
    /** 商品名称（下单时冗余存储，避免商品改名影响历史订单） */
    private String productName;
    /** 商品图片 */
    private String productImage;
    /** 下单时单价 */
    private BigDecimal productPrice;
    /** 购买数量 */
    private Integer quantity;
    /** 小计金额 */
    private BigDecimal totalAmount;
}
