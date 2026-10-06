package com.lion.mall.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码
 *
 * @author lion
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),
    /** 未登录 / 登录已失效 */
    UNAUTHORIZED(401, "未登录或登录已失效"),
    /** 无权限 */
    FORBIDDEN(403, "没有访问权限"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /** 请求过于频繁 */
    TOO_MANY_REQUEST(429, "请求过于频繁，请稍后再试"),

    /** 服务器内部错误 */
    ERROR(500, "服务器繁忙，请稍后再试"),

    /* ---------------- 业务错误码 ---------------- */
    /** 通用业务异常 */
    BIZ_ERROR(1000, "业务处理失败"),
    /** 用户相关 */
    USER_NOT_FOUND(1101, "用户不存在"),
    USER_EXISTS(1102, "用户名已被注册"),
    PASSWORD_ERROR(1103, "用户名或密码错误"),
    /** 商品相关 */
    PRODUCT_NOT_FOUND(1201, "商品不存在"),
    PRODUCT_OFF_SHELF(1202, "商品已下架"),
    STOCK_NOT_ENOUGH(1203, "商品库存不足"),
    /** 订单相关 */
    ORDER_NOT_FOUND(1301, "订单不存在"),
    ORDER_STATUS_ERROR(1302, "订单状态不正确"),
    /** 远程调用失败 */
    REMOTE_CALL_ERROR(1401, "远程服务调用失败");

    /** 状态码 */
    private final Integer code;
    /** 提示信息 */
    private final String msg;
}
