package com.lion.mall.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户传输对象（跨服务传输用）
 *
 * @author lion
 */
@Data
public class UserDTO implements Serializable {

    /** 用户ID */
    private Long id;
    /** 用户名 */
    private String username;
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 头像 */
    private String avatar;
    /** 创建时间 */
    private LocalDateTime createTime;
}
