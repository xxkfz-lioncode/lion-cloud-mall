package com.lion.mall.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.lion.mall.order.model.req.CreateOrderReq;
import com.lion.mall.order.model.vo.OrderItemVO;
import com.lion.mall.order.model.vo.OrderVO;
import com.lion.mall.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
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
        return order.getId();
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
        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatus.PAID.getCode());
        update.setPayTime(LocalDateTime.now());
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        Order order = getOrder(id);
        if (!OrderStatus.WAIT_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR.getCode(), "只有待支付订单可以取消");
        }

        // 1. 修改订单状态
        Order update = new Order();
        update.setId(order.getId());
        update.setStatus(OrderStatus.CANCELED.getCode());
        update.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(update);

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
