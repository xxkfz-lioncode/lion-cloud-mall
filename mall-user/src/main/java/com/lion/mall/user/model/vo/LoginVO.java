package com.lion.mall.user.model.vo;

import com.lion.mall.api.dto.UserDTO;
import lombok.Data;

/**
 * 登录响应：token + 用户信息
 *
 * @author lion
 */
@Data
public class LoginVO {

    /** token 值 */
    private String token;
    /** token 名称（前端需要以此名称作为请求头） */
    private String tokenName;
    /** 用户信息 */
    private UserDTO user;
}
