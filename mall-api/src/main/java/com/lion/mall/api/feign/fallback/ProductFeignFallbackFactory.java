package com.lion.mall.api.feign.fallback;

import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.api.feign.ProductFeignClient;
import com.lion.mall.common.result.R;
import com.lion.mall.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 商品服务调用的熔断降级处理
 *
 * @author lion
 */
@Slf4j
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("调用 mall-product 失败，进入降级逻辑", cause);
        return new ProductFeignClient() {

            @Override
            public R<ProductDTO> getProductById(Long id) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "商品服务暂不可用");
            }

            @Override
            public R<Void> deductStock(List<StockDeductDTO> items) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "商品服务暂不可用，扣减库存失败");
            }

            @Override
            public R<Void> restoreStock(List<StockDeductDTO> items) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "商品服务暂不可用，回滚库存失败");
            }
        };
    }
}
