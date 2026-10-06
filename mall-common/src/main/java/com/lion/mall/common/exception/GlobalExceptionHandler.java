package com.lion.mall.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.lion.mall.common.result.R;
import com.lion.mall.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 各业务服务启动时扫描 com.lion.mall 包即可自动生效，无需重复编写。
 *
 * @author lion
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 未登录异常（Sa-Token 抛出） */
    @ExceptionHandler(NotLoginException.class)
    public R<?> handleNotLoginException(NotLoginException e) {
        return R.fail(ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMsg());
    }

    /** 参数校验异常（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ":" + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.ERROR);
    }
}
