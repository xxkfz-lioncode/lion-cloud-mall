package com.lion.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.common.exception.BizException;
import com.lion.mall.common.result.PageResult;
import com.lion.mall.common.result.ResultCode;
import com.lion.mall.product.entity.Product;
import com.lion.mall.product.mapper.ProductMapper;
import com.lion.mall.product.model.req.ProductSaveReq;
import com.lion.mall.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务实现
 * <p>
 * 库存扣减使用 Redisson 分布式锁 + 带条件的 UPDATE 语句双重保障，防止超卖。
 *
 * @author lion
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** 库存锁前缀 */
    private static final String STOCK_LOCK_KEY = "lock:stock:product:";

    private final ProductMapper productMapper;
    private final RedissonClient redissonClient;

    @Override
    public PageResult<ProductDTO> page(Long pageNo, Long pageSize, String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .like(StringUtils.hasText(keyword), Product::getName, keyword)
                .orderByDesc(Product::getCreateTime);
        Page<Product> page = productMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::convert).toList());
    }

    @Override
    public ProductDTO getById(Long id) {
        return convert(getProduct(id));
    }

    @Override
    public Long save(ProductSaveReq req) {
        Product product = new Product();
        BeanUtils.copyProperties(req, product);
        product.setId(null);
        product.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());
        productMapper.insert(product);
        return product.getId();
    }

    @Override
    public void update(ProductSaveReq req) {
        Product product = getProduct(req.getId());
        BeanUtils.copyProperties(req, product);
        product.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(product);
    }

    @Override
    public void remove(Long id) {
        getProduct(id);
        productMapper.deleteById(id);
    }

    /**
     * 扣减库存（Seata 全局事务中的一个分支 RM）
     * <p>
     * 双重保障：Redisson 分布式锁保证并发不超卖，Seata AT 保证订单回滚时库存也能回滚。
     * 注意：AT 模式默认全局隔离级别是「读未提交」，这里的 Redis 锁会在分支事务提交前释放，
     * 学习演示没问题；生产严谨场景建议把锁上移到订单侧，或使用 Seata 的 {@code @GlobalLock}。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(List<StockDeductDTO> items) {
        for (StockDeductDTO item : items) {
            // 每个商品一把锁，锁粒度尽量小，避免所有下单请求串行
            RLock lock = redissonClient.getLock(STOCK_LOCK_KEY + item.getProductId());
            lock.lock(10, TimeUnit.SECONDS);
            try {
                Product product = getProduct(item.getProductId());
                if (Integer.valueOf(0).equals(product.getStatus())) {
                    throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
                }
                int rows = productMapper.deductStock(product.getId(), item.getQuantity());
                if (rows == 0) {
                    throw new BizException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                            "商品【" + product.getName() + "】库存不足");
                }
                log.info("扣减库存成功：productId={}, quantity={}", item.getProductId(), item.getQuantity());
            } finally {
                // 只释放自己持有的锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(List<StockDeductDTO> items) {
        for (StockDeductDTO item : items) {
            RLock lock = redissonClient.getLock(STOCK_LOCK_KEY + item.getProductId());
            lock.lock(10, TimeUnit.SECONDS);
            try {
                productMapper.restoreStock(item.getProductId(), item.getQuantity());
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /** 查询商品，不存在则抛业务异常 */
    private Product getProduct(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    /** 实体转 DTO */
    private ProductDTO convert(Product product) {
        ProductDTO dto = new ProductDTO();
        BeanUtils.copyProperties(product, dto);
        return dto;
    }
}
