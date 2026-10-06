package com.lion.mall.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 库存扣减请求对象
 *
 * @author lion
 */
@Data
public class StockDeductDTO implements Serializable {

    /** 商品ID */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 扣减数量 */
    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于0")
    private Integer quantity;
}
