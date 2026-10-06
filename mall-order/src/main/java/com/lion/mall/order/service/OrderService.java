package com.lion.mall.order.service;

import com.lion.mall.common.result.PageResult;
import com.lion.mall.order.model.req.CreateOrderReq;
import com.lion.mall.order.model.vo.OrderVO;

/**
 * 订单服务
 *
 * @author lion
 */
public interface OrderService {

    /** 创建订单（下单） */
    Long create(CreateOrderReq req);

    /** 查询订单详情 */
    OrderVO detail(Long id);

    /** 分页查询我的订单 */
    PageResult<OrderVO> page(Long pageNo, Long pageSize);

    /** 支付订单 */
    void pay(Long id);

    /** 取消订单（回滚库存） */
    void cancel(Long id);
}
