package com.lion.mall.common.exception;

import com.lion.mall.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：用于主动抛出可预期的业务错误（如库存不足、订单不存在）
 *
 * @author lion
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码 */
    private final Integer code;

    public BizException(String msg) {
        super(msg);
        this.code = ResultCode.BIZ_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }
}
