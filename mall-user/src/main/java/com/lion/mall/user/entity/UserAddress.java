package com.lion.mall.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lion.mall.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户收货地址实体（t_user_address）
 *
 * @author lion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_address")
public class UserAddress extends BaseEntity {

    /** 所属用户ID */
    private Long userId;
    /** 收货人 */
    private String receiverName;
    /** 收货人手机号 */
    private String receiverPhone;
    /** 详细收货地址 */
    private String address;
    /** 是否默认：0-否 1-是 */
    @TableField("is_default")
    private Integer isDefault;
}
