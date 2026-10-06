package com.lion.mall.api.feign;

import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.api.feign.fallback.ProductFeignFallbackFactory;
import com.lion.mall.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商品服务远程调用接口（服务名即 Nacos 注册名 mall-product）
 *
 * @author lion
 */
@FeignClient(name = "mall-product", path = "/product", fallbackFactory = ProductFeignFallbackFactory.class)
public interface ProductFeignClient {

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    R<ProductDTO> getProductById(@PathVariable("id") Long id);

    /**
     * 扣减库存（下单时使用，内部接口）
     */
    @PostMapping("/stock/deduct")
    R<Void> deductStock(@RequestBody List<StockDeductDTO> items);

    /**
     * 回滚库存（取消订单时使用，内部接口）
     */
    @PostMapping("/stock/restore")
    R<Void> restoreStock(@RequestBody List<StockDeductDTO> items);
}
