package com.lion.mall.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应体 R(Result)
 * <p>
 * 所有 Controller 均返回该对象，前端根据 code 是否为 200 判断成功与否。
 *
 * @param <T> 响应数据类型
 * @author lion
 */
@Data
public class R<T> implements Serializable {

    /** 状态码 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 响应数据 */
    private T data;
    /** 时间戳 */
    private Long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMsg(ResultCode.SUCCESS.getMsg());
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(ResultCode.BIZ_ERROR.getCode(), msg);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return fail(resultCode.getCode(), resultCode.getMsg());
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    /** 是否成功 */
    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
