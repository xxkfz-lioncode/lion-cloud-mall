package com.lion.mall.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lion.mall.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体（t_user）
 *
 * @author lion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {

    /** 用户名（唯一） */
    private String username;
    /** 密码（MD5 加盐存储） */
    private String password;
    /** 昵称 */
    private String nickname;
    /** 手机号 */
    private String phone;
    /** 头像 */
    private String avatar;
    /** 状态：0-禁用 1-正常 */
    private Integer status;
}
