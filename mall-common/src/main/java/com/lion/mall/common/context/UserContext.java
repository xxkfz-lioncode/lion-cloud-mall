package com.lion.mall.common.context;

import cn.dev33.satoken.stp.StpUtil;
import com.lion.mall.common.constant.Constants;

/**
 * 用户上下文：基于 ThreadLocal 存储当前登录用户 ID
 * <p>
 * 网关鉴权通过后会把用户 ID 放入 X-User-Id 请求头，下游服务通过过滤器写入本上下文；
 * 若请求头缺失（如服务间直连调试），则降级从 Sa-Token 的登录态中读取。
 *
 * @author lion
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /** 设置当前登录用户 ID */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /** 获取当前登录用户 ID */
    public static Long getUserId() {
        Long userId = USER_ID_HOLDER.get();
        if (userId == null && StpUtil.isLogin()) {
            userId = StpUtil.getLoginIdAsLong();
        }
        return userId;
    }

    /** 获取当前登录用户 ID，不存在则抛出业务异常 */
    public static Long getRequiredUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new com.lion.mall.common.exception.BizException(
                    com.lion.mall.common.result.ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    /** 清理 ThreadLocal，防止内存泄漏 */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }

    /** 请求头名称 */
    public static String headerName() {
        return Constants.HEADER_USER_ID;
    }
}
