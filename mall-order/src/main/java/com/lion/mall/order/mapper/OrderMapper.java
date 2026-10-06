package com.lion.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lion.mall.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper
 *
 * @author lion
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
