package com.lion.mall.order.model.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 创建订单请求
 *
 * @author lion
 */
@Data
public class CreateOrderReq {

    /** 收货地址 */
    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 收货人 */
    @NotBlank(message = "收货人不能为空")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    /** 备注 */
    private String remark;

    /** 订单明细 */
    @NotEmpty(message = "订单商品不能为空")
    @Valid
    private List<OrderItemReq> items;

    /**
     * 订单明细项
     */
    @Data
    public static class OrderItemReq {

        /** 商品ID */
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        /** 购买数量 */
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于0")
        private Integer quantity;
    }
}
