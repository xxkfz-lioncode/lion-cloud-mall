package com.lion.mall.user.model.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 收货地址新增/修改请求
 *
 * @author lion
 */
@Data
public class AddressSaveReq {

    /** 地址ID（修改时必填） */
    private Long id;

    /** 收货人 */
    @NotBlank(message = "收货人不能为空")
    @Size(max = 50, message = "收货人姓名不能超过 50 个字符")
    private String receiverName;

    /** 收货人手机号 */
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    /** 详细收货地址 */
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 255, message = "收货地址不能超过 255 个字符")
    private String address;

    /** 是否设为默认地址 */
    private Boolean isDefault;
}
