package com.lion.mall.product.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.common.result.PageResult;
import com.lion.mall.common.result.R;
import com.lion.mall.product.model.req.ProductSaveReq;
import com.lion.mall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品接口
 *
 * @author lion
 */
@Tag(name = "商品管理")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "商品分页列表")
    @GetMapping("/page")
    public R<PageResult<ProductDTO>> page(
            @RequestParam(name = "pageNo", defaultValue = "1") Long pageNo,
            @RequestParam(name = "pageSize", defaultValue = "8") Long pageSize,
            @RequestParam(name = "keyword", required = false) String keyword) {
        return R.ok(productService.page(pageNo, pageSize, keyword));
    }

    @Operation(summary = "商品详情")
    @GetMapping("/{id}")
    public R<ProductDTO> detail(@PathVariable("id") Long id) {
        return R.ok(productService.getById(id));
    }

    @SaCheckLogin
    @Operation(summary = "新增商品")
    @PostMapping
    public R<Long> save(@Valid @RequestBody ProductSaveReq req) {
        return R.ok(productService.save(req));
    }

    @SaCheckLogin
    @Operation(summary = "修改商品")
    @PutMapping
    public R<Void> update(@Valid @RequestBody ProductSaveReq req) {
        productService.update(req);
        return R.ok();
    }

    @SaCheckLogin
    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable("id") Long id) {
        productService.remove(id);
        return R.ok();
    }

    @Operation(summary = "扣减库存（内部接口，供订单服务 Feign 调用）")
    @PostMapping("/stock/deduct")
    public R<Void> deductStock(@Valid @RequestBody List<StockDeductDTO> items) {
        productService.deductStock(items);
        return R.ok();
    }

    @Operation(summary = "回滚库存（内部接口，供订单服务 Feign 调用）")
    @PostMapping("/stock/restore")
    public R<Void> restoreStock(@Valid @RequestBody List<StockDeductDTO> items) {
        productService.restoreStock(items);
        return R.ok();
    }
}
