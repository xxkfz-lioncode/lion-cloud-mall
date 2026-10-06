package com.lion.mall.product.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lion.mall.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品实体（t_product）
 *
 * @author lion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_product")
public class Product extends BaseEntity {

    /** 商品名称 */
    private String name;
    /** 副标题 */
    private String subtitle;
    /** 主图地址 */
    private String image;
    /** 售价 */
    private BigDecimal price;
    /** 库存 */
    private Integer stock;
    /** 商品详情 */
    private String description;
    /** 状态：0-下架 1-上架 */
    private Integer status;
}
