package com.lion.mall.user.service;

import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.user.model.req.LoginReq;
import com.lion.mall.user.model.req.RegisterReq;
import com.lion.mall.user.model.vo.LoginVO;

/**
 * 用户服务
 *
 * @author lion
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param req 注册参数
     * @return 新用户ID
     */
    Long register(RegisterReq req);

    /**
     * 用户登录
     *
     * @param req 登录参数
     * @return token 与用户信息
     */
    LoginVO login(LoginReq req);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long id);
}
