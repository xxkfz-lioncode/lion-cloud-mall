package com.lion.mall.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lion.mall.common.context.UserContext;
import com.lion.mall.common.result.R;
import com.lion.mall.user.entity.UserAddress;
import com.lion.mall.user.model.req.AddressSaveReq;
import com.lion.mall.user.service.UserAddressService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 收货地址接口（均为当前登录用户自己的地址）
 *
 * @author lion
 */
@SaCheckLogin
@Tag(name = "收货地址")
@RestController
@RequestMapping("/user/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService userAddressService;

    @Operation(summary = "我的收货地址列表")
    @GetMapping("/list")
    public R<List<UserAddress>> list() {
        return R.ok(userAddressService.listByUser(UserContext.getRequiredUserId()));
    }

    @Operation(summary = "默认收货地址")
    @GetMapping("/default")
    public R<UserAddress> defaultAddress() {
        return R.ok(userAddressService.getDefault(UserContext.getRequiredUserId()));
    }

    @Operation(summary = "新增收货地址")
    @PostMapping
    public R<Long> save(@Valid @RequestBody AddressSaveReq req) {
        return R.ok(userAddressService.save(UserContext.getRequiredUserId(), req));
    }

    @Operation(summary = "修改收货地址")
    @PutMapping
    public R<Void> update(@Valid @RequestBody AddressSaveReq req) {
        userAddressService.update(UserContext.getRequiredUserId(), req);
        return R.ok();
    }

    @Operation(summary = "删除收货地址")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable("id") Long id) {
        userAddressService.remove(UserContext.getRequiredUserId(), id);
        return R.ok();
    }

    @Operation(summary = "设为默认收货地址")
    @PostMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable("id") Long id) {
        userAddressService.setDefault(UserContext.getRequiredUserId(), id);
        return R.ok();
    }
}
