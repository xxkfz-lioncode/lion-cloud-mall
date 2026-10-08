package com.lion.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lion.mall.api.dto.ProductDTO;
import com.lion.mall.api.dto.StockDeductDTO;
import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.api.feign.ProductFeignClient;
import com.lion.mall.api.feign.UserFeignClient;
import com.lion.mall.common.constant.Constants;
import com.lion.mall.common.context.UserContext;
import com.lion.mall.common.exception.BizException;
import com.lion.mall.common.result.PageResult;
import com.lion.mall.common.result.R;
import com.lion.mall.common.result.ResultCode;
import com.lion.mall.order.constant.OrderStatus;
import com.lion.mall.order.entity.Order;
import com.lion.mall.order.entity.OrderItem;
import com.lion.mall.order.mapper.OrderItemMapper;
import com.lion.mall.order.mapper.OrderMapper;
import com.lion.mall.order.mq.OrderMqProducer;
import com.lion.mall.order.model.req.CreateOrderReq;
import com.lion.mall.order.model.vo.OrderItemVO;
import com.lion.mall.order.model.vo.OrderVO;
import com.lion.mall.order.service.OrderService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务实现
 * <p>
 * 下单流程：校验用户 -> 查询商品计算金额 -> 远程扣减库存 -> 落库订单与明细。
 *
 * @author lion
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductFeignClient productFeignClient;
    private final UserFeignClient userFeignClient;
    private final OrderMqProducer mqProducer;

    /**
     * 下单：跨服务写库（本服务写订单库 + 商品服务扣库存），用 Seata 保证一致性。
     * <p>
     * {@code @GlobalTransactional} 开启全局事务（本服务是 TM），
     * {@code @Transactional} 保留作为本地分支事务（RM）；XID 由 FeignConfig 透传给下游。
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateOrderReq req) {
        // 0. 当前登录用户（网关透传的 X-User-Id 请求头）
        Long userId = UserContext.getRequiredUserId();

        // 1. 远程调用用户服务，校验用户是否存在
        R<UserDTO> userResult = userFeignClient.getUserById(userId);
        if (!userResult.isSuccess() || userResult.getData() == null) {
            throw new BizException(userResult.getCode(), userResult.getMsg());
        }

        // 2. 远程调用商品服务，查询商品并计算订单金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<StockDeductDTO> deductList = new ArrayList<>();

        for (CreateOrderReq.OrderItemReq item : req.getItems()) {
            R<ProductDTO> productResult = productFeignClient.getProductById(item.getProductId());
            if (!productResult.isSuccess() || productResult.getData() == null) {
                throw new BizException(productResult.getCode(), productResult.getMsg());
            }
            ProductDTO product = productResult.getData();
            if (Integer.valueOf(0).equals(product.getStatus())) {
                throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
            }
            if (product.getStock() < item.getQuantity()) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH.getCode(),
                        "商品【" + product.getName() + "】库存不足");
            }

            BigDecimal amount = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(amount);

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getImage());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalAmount(amount);
            orderItems.add(orderItem);

            StockDeductDTO deduct = new StockDeductDTO();
            deduct.setProductId(product.getId());
            deduct.setQuantity(item.getQuantity());
            deductList.add(deduct);
        }

        // 3. 远程调用商品服务扣减库存（商品服务内部使用 Redisson 分布式锁防超卖）
        R<Void> deductResult = productFeignClient.deductStock(deductList);
        if (!deductResult.isSuccess()) {
            throw new BizException(deductResult.getCode(), deductResult.getMsg());
        }

        // 4. 保存订单与明细
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.WAIT_PAY.getCode());
        order.setAddress(req.getAddress());
        order.setReceiverName(req.getReceiverName());
        order.setReceiverPhone(req.getReceiverPhone());
        order.setRemark(req.getRemark());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(order);

        orderItems.forEach(item -> {
            item.setOrderId(order.getId());
            item.setCreateTime(LocalDateTime.now());
            orderItemMapper.insert(item);
        });

        log.info("下单成功：orderNo={}, userId={}, amount={}", order.getOrderNo(), userId, totalAmount);

        // 事务提交后再发 MQ 消息，避免「事务回滚了、消息却已经发出去」
        sendMqAfterCommit(order.getId(), userId, order.getOrderNo(), totalAmount);

        return order.getId();
    }

    /**
     * 事务提交后发送三条消息：
     * <ol>
     *   <li>超时关单延迟消息 → 30 分钟后触发关单检查</li>
     *   <li>下单短信通知消息 → 异步发短信，不拖慢下单响应</li>
     *   <li>WebSocket 推送消息 → 前端实时弹出「待支付」提示</li>
     * </ol>
     * <p>
     * 为什么不直接发：消息一旦发出不可撤回。若下单事务随后回滚，
     * 消费者就会拿到一个不存在的订单。挂在 afterCommit 上可消除这个时间窗口。
     * 消费者端还会再校验一次订单状态，双保险。
     */
    private void sendMqAfterCommit(Long orderId, Long userId, String orderNo, BigDecimal amount) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 没有事务（如单元测试）时直接发送
            mqProducer.sendTimeoutCheck(orderId);
            mqProducer.sendSmsNotify(orderId);
            mqProducer.pushOrderCreated(userId, orderNo, amount);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mqProducer.sendTimeoutCheck(orderId);
                mqProducer.sendSmsNotify(orderId);
                mqProducer.pushOrderCreated(userId, orderNo, amount);
            }
        });
    }

    @Override
    public OrderVO detail(Long id) {
        Order order = getOrder(id);
        return convert(order, orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId())));
    }

    @Override
    public PageResult<OrderVO> page(Long pageNo, Long pageSize) {
        Long userId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreateTime);
        Page<Order> page = orderMapper.selectPage(Page.of(pageNo, pageSize), wrapper);
        List<OrderVO> list = page.getRecords().stream()
                .map(order -> convert(order, orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()))))
                .toList();
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long id) {
        Order order = getOrder(id);
        if (!OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR.getCode(), "只有待支付订单可以支付");
        }
        // CAS 更新：只有「待支付」才能变成「已支付」。
        // 与超时关单任务并发时，靠数据库条件更新互斥，防止订单既被支付又被关单。
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .set(Order::getStatus, OrderStatus.PAID.getCode())
                .set(Order::getPayTime, LocalDateTime.now())
                .set(Order::getUpdateTime, LocalDateTime.now());
        if (orderMapper.update(null, uw) == 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR.getCode(), "订单状态已变更，请刷新后重试");
        }
        // 支付成功 → 实时推送通知给该用户的所有在线页面（区别于「待支付」的通知）
        mqProducer.pushOrderPaid(order.getUserId(), order.getOrderNo(), order.getTotalAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Order order = getOrder(id);
        if (!OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR.getCode(), "只有待支付订单可以取消");
        }
        doCancel(order, "用户主动取消");
    }

    @Override
    public List<Long> findTimeoutOrderIds(int timeoutMinutes, int limit, int shardTotal, int shardIndex) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .select(Order::getId)
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .lt(Order::getCreateTime, deadline)
                .orderByAsc(Order::getCreateTime);
        // 分片广播：多实例部署时每个实例只扫自己那一片（id % shardTotal = shardIndex），
        // 天然不重复；单实例时 shardTotal=1，条件不生效，无副作用。
        if (shardTotal > 1) {
            // {0}/{1} 是 MyBatis-Plus 的参数占位符，值会被预编译绑定，无注入风险
            wrapper.apply("id % {0} = {1}", shardTotal, shardIndex);
        }
        // limit 为外部传入的 int，Math.max 保证为正整数，不存在注入风险
        wrapper.last("LIMIT " + Math.max(limit, 1));
        return orderMapper.selectList(wrapper).stream().map(Order::getId).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        // 幂等 + 防并发：扫描后可能已被支付或取消，这里再次校验状态，非待支付直接跳过
        if (order == null || !OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            return;
        }
        doCancel(order, "超时未支付自动关闭");
    }

    /**
     * 关单核心逻辑：状态置为「已取消」+ 远程回滚库存。
     * <p>
     * 被「用户主动取消」与「定时任务超时关单」共用，<b>不含用户校验</b>；
     * 事务由调用方（cancel / closeTimeoutOrder）控制。
     */
    private void doCancel(Order order, String reason) {
        // 1. CAS 更新状态：只有「待支付」才能被关单。
        //    并发/多实例下由数据库条件更新互斥，抢不到的直接返回，
        //    绝不执行下面的库存回滚 —— 否则同一订单的库存会被回滚两次。
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, OrderStatus.WAIT_PAY.getCode())
                .set(Order::getStatus, OrderStatus.CANCELED.getCode())
                .set(Order::getUpdateTime, LocalDateTime.now());
        if (orderMapper.update(null, uw) == 0) {
            // 期间已被支付或已取消：跳过，不回滚库存
            log.warn("订单已非待支付状态，跳过关单：orderNo={}, reason={}", order.getOrderNo(), reason);
            return;
        }

        // 2. 远程调用商品服务回滚库存
        List<StockDeductDTO> restoreList = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()))
                .stream()
                .map(item -> {
                    StockDeductDTO dto = new StockDeductDTO();
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();
        productFeignClient.restoreStock(restoreList);

        log.info("订单已关闭：orderNo={}, reason={}", order.getOrderNo(), reason);
    }

    /** 查询订单，不存在或不属于当前用户则抛异常 */
    private Order getOrder(Long id) {
        Long userId = UserContext.getRequiredUserId();
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getId, id)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /** 订单号：LM + 时间戳 + 4 位随机数 */
    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return Constants.ORDER_NO_PREFIX + time + random;
    }

    /** 实体转 VO */
    private OrderVO convert(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusDesc(OrderStatus.of(order.getStatus()) == null
                ? "未知" : OrderStatus.of(order.getStatus()).getDesc());
        List<OrderItemVO> itemVos = items.stream().map(item -> {
            OrderItemVO itemVo = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVo);
            return itemVo;
        }).toList();
        vo.setItems(itemVos);
        return vo;
    }
}
