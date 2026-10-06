package com.lion.mall.order.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lion.mall.common.result.PageResult;
import com.lion.mall.common.result.R;
import com.lion.mall.order.model.req.CreateOrderReq;
import com.lion.mall.order.model.vo.OrderVO;
import com.lion.mall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口
 *
 * @author lion
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @SaCheckLogin
    @Operation(summary = "创建订单（下单）")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody CreateOrderReq req) {
        return R.ok(orderService.create(req));
    }

    @SaCheckLogin
    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public R<OrderVO> detail(@PathVariable("id") Long id) {
        return R.ok(orderService.detail(id));
    }

    @SaCheckLogin
    @Operation(summary = "我的订单（分页）")
    @GetMapping("/page")
    public R<PageResult<OrderVO>> page(@RequestParam(name = "pageNo", defaultValue = "1") Long pageNo,
                                       @RequestParam(name = "pageSize", defaultValue = "10") Long pageSize) {
        return R.ok(orderService.page(pageNo, pageSize));
    }

    @SaCheckLogin
    @Operation(summary = "支付订单（模拟支付）")
    @PostMapping("/{id}/pay")
    public R<Void> pay(@PathVariable("id") Long id) {
        orderService.pay(id);
        return R.ok();
    }

    @SaCheckLogin
    @Operation(summary = "取消订单（回滚库存）")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable("id") Long id) {
        orderService.cancel(id);
        return R.ok();
    }
}
