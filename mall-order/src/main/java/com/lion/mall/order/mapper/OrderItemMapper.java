package com.lion.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lion.mall.order.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper
 *
 * @author lion
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
