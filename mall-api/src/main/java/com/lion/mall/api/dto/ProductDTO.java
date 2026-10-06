package com.lion.mall.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品传输对象（跨服务传输用）
 *
 * @author lion
 */
@Data
public class ProductDTO implements Serializable {

    /** 商品ID */
    private Long id;
    /** 商品名称 */
    private String name;
    /** 商品副标题 */
    private String subtitle;
    /** 主图 */
    private String image;
    /** 售价 */
    private BigDecimal price;
    /** 库存 */
    private Integer stock;
    /** 商品详情 */
    private String description;
    /** 状态：0-下架 1-上架 */
    private Integer status;
    /** 创建时间 */
    private LocalDateTime createTime;
}
