package com.lion.mall.common.constant;

/**
 * 通用常量
 *
 * @author lion
 */
public interface Constants {

    /** 网关解析 token 后向下游传递的用户 ID 请求头 */
    String HEADER_USER_ID = "X-User-Id";

    /** Sa-Token 的 token 名称（请求头） */
    String TOKEN_NAME = "satoken";

    /**
     * 链路追踪 ID 请求头：一次请求经过 网关 → order → product 时用它串起来。
     * 由最外层（网关）生成，Feign 调用时自动透传给下游。
     */
    String HEADER_TRACE_ID = "X-Trace-Id";

    /** 链路追踪 ID 在 Logback MDC 中的 key（日志 JSON 里会带上该字段） */
    String MDC_TRACE_ID = "traceId";

    /** 订单号前缀 */
    String ORDER_NO_PREFIX = "LM";
}
