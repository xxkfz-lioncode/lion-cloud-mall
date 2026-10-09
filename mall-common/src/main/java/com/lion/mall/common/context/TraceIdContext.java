package com.lion.mall.common.context;

/**
 * 链路追踪 ID 上下文（ThreadLocal）
 * <p>
 * 作用：让同一次请求在「网关 → 订单服务 → 商品/用户服务」的日志里带上同一个 traceId，
 * 这样在 Kibana 里搜一个 traceId 就能把整条链路的日志捞出来。
 * <p>
 * <b>为什么需要它</b>：Feign 调用是全新的 HTTP 请求，下游服务的线程不同，
 * 上游的 ThreadLocal / MDC 不会自动带过去，必须靠请求头传递 + 下游重新写入。
 * <p>
 * <b>为什么不用 SkyWalking 的 {@code TraceContext.traceId()}</b>：
 * 那样需要额外引入 {@code apm-toolkit-trace}，且版本必须与 Agent 严格一致；
 * 更重要的是未挂 Agent 时（本地 IDEA 裸跑）它返回空，日志就没有 traceId 了。
 * 自己生成则可以<b>无论有没有 Agent 都能工作</b>。
 * 代价是这个 traceId 与 SkyWalking 的 TID 不是同一个值 —— 但文本日志里
 * SkyWalking 的 {@code %tid} 仍然保留，两边各查各的即可。
 *
 * @author lion
 */
public final class TraceIdContext {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private TraceIdContext() {
    }

    public static void set(String traceId) {
        CONTEXT.set(traceId);
    }

    public static String get() {
        return CONTEXT.get();
    }

    /**
     * 必须清理：Web 容器线程是复用的，
     * 不 remove 的话下一个请求可能读到上一个请求的 traceId（串号）。
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
