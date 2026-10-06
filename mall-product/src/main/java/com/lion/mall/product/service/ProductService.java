package com.lion.mall.product.service;

import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.common.result.PageResult;
import com.lion.mall.product.model.req.ProductSaveReq;

import java.util.List;

/**
 * 商品服务
 *
 * @author lion
 */
public interface ProductService {

    /** 分页查询商品 */
    PageResult<ProductDTO> page(Long pageNo, Long pageSize, String keyword);

    /** 查询商品详情 */
    ProductDTO getById(Long id);

    /** 新增商品 */
    Long save(ProductSaveReq req);

    /** 修改商品 */
    void update(ProductSaveReq req);

    /** 删除商品 */
    void remove(Long id);

    /** 扣减库存（下单时由订单服务 Feign 调用） */
    void deductStock(List<StockDeductDTO> items);

    /** 回滚库存（取消订单时由订单服务 Feign 调用） */
    void restoreStock(List<StockDeductDTO> items);
}
