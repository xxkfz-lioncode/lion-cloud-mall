package com.lion.mall.product.model.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增/修改请求
 *
 * @author lion
 */
@Data
public class ProductSaveReq {

    /** 商品ID（修改时必填） */
    private Long id;

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    private String name;

    /** 副标题 */
    private String subtitle;

    /** 主图地址 */
    private String image;

    /** 售价 */
    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于0")
    private BigDecimal price;

    /** 库存 */
    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能为负数")
    private Integer stock;

    /** 商品详情 */
    private String description;

    /** 状态：0-下架 1-上架 */
    private Integer status;
}
