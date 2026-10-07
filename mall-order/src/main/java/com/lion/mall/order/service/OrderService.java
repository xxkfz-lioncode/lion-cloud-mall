package com.lion.mall.order.service;

import com.lion.mall.common.result.PageResult;
import com.lion.mall.order.model.req.CreateOrderReq;
import com.lion.mall.order.model.vo.OrderVO;

import java.util.List;

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

    // ---------------- 定时任务（XXL-Job）----------------

    /**
     * 查询超时未支付的订单ID（供定时任务扫描）
     *
     * @param timeoutMinutes 超时分钟数：创建时间早于「当前时间 - 该值」的待支付订单
     * @param limit          单次最多返回条数，防止一次扫描过多拖垮服务
     * @param shardTotal     分片总数（admin 用分片广播策略时 = 执行器实例数；单实例为 1，非分片为 0）
     * @param shardIndex     当前分片序号（0 基）
     */
    List<Long> findTimeoutOrderIds(int timeoutMinutes, int limit, int shardTotal, int shardIndex);

    /**
     * 关闭单个超时订单：状态置为已取消并回滚库存
     * <p>
     * 与 {@link #cancel(Long)} 的区别：<b>不做登录用户校验</b> —— 定时任务由调度中心触发，
     * 请求里没有 token，拿不到 UserContext，因此不能复用带用户校验的 cancel。
     * <p>
     * 幂等设计：非「待支付」状态的订单直接跳过；每单独立事务，单条失败不影响其余订单。
     */
    void closeTimeoutOrder(Long orderId);
}
