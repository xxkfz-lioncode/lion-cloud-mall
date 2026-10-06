package com.lion.mall.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.common.exception.BizException;
import com.lion.mall.common.result.ResultCode;
import com.lion.mall.user.entity.User;
import com.lion.mall.user.mapper.UserMapper;
import com.lion.mall.user.model.req.LoginReq;
import com.lion.mall.user.model.req.RegisterReq;
import com.lion.mall.user.model.vo.LoginVO;
import com.lion.mall.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户服务实现
 *
 * @author lion
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Long register(RegisterReq req) {
        // 1. 校验用户名是否被占用
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.USER_EXISTS);
        }

        // 2. 保存用户（密码 MD5 加盐后入库）
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(encryptPassword(req.getUsername(), req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname());
        user.setPhone(req.getPhone());
        user.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        log.info("用户注册成功：userId={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginReq req) {
        // 1. 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        // 2. 校验密码
        if (!user.getPassword().equals(encryptPassword(req.getUsername(), req.getPassword()))) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        // 3. 状态校验
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "账号已被禁用");
        }

        // 4. Sa-Token 登录，token 自动写入 Redis（各微服务共享）
        StpUtil.login(user.getId());

        log.info("用户登录成功：userId={}, username={}", user.getId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setTokenName(StpUtil.getTokenName());
        vo.setUser(convert(user));
        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return convert(user);
    }

    /** 实体转 DTO */
    private UserDTO convert(User user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    /** 密码加密：MD5(密码 + 用户名盐) */
    private String encryptPassword(String username, String password) {
        String raw = password + "{" + username + "}";
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
