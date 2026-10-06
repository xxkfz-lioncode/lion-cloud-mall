package com.lion.mall.user.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.common.context.UserContext;
import com.lion.mall.common.result.R;
import com.lion.mall.user.model.req.LoginReq;
import com.lion.mall.user.model.req.RegisterReq;
import com.lion.mall.user.model.vo.LoginVO;
import com.lion.mall.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口
 *
 * @author lion
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public R<Long> register(@Valid @RequestBody RegisterReq req) {
        return R.ok(userService.register(req));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginReq req) {
        return R.ok(userService.login(req));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout() {
        userService.logout();
        return R.ok();
    }

    @SaCheckLogin
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public R<UserDTO> info() {
        // 网关鉴权通过后会将用户ID放入请求头，此处从上下文中获取
        return R.ok(userService.getUserById(UserContext.getRequiredUserId()));
    }

    @Operation(summary = "根据ID查询用户（供其它微服务 Feign 调用）")
    @GetMapping("/{id}")
    public R<UserDTO> getUserById(@PathVariable("id") Long id) {
        return R.ok(userService.getUserById(id));
    }
}
