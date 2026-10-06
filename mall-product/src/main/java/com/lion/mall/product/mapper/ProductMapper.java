package com.lion.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lion.mall.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper
 * <p>
 * 库存变更使用带条件的 SQL，保证并发下不会出现超卖。
 *
 * @author lion
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 扣减库存（stock >= quantity 时才扣减成功）
     *
     * @return 影响行数，0 表示库存不足
     */
    @Update("update t_product set stock = stock - #{quantity} where id = #{id} and stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 回滚库存
     */
    @Update("update t_product set stock = stock + #{quantity} where id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
