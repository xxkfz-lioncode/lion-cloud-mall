package com.lion.mall.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪 ID 过滤器（网关，WebFlux 版）
 * <p>
 * 网关是请求的<b>第一站</b>，由它生成 traceId 并放进请求头，
 * 后面经过的每个微服务（order / product / user）都会沿用同一个值，
 * 于是 Kibana 里搜一个 traceId 就能捞出整条链路的日志。
 * <p>
 * <b>为什么这里没有用 MDC</b>：WebFlux 跑在 Netty 的 event-loop 线程上，
 * 而 MDC 是 ThreadLocal。这类线程是<b>被所有请求共享复用</b>的，
 * 在这里 {@code MDC.put()} 不仅下游的日志往往读不到（响应式链路会切线程），
 * 还会导致 traceId 串到别的请求上。
 * 所以网关只做<b>请求头注入</b>：下游 Servlet 服务的 {@code TraceIdFilter}
 * 会从请求头取出并写进自己的 MDC —— 那才是安全的作用域。
 * <p>
 * 代价：网关自身打印的日志里没有 traceId 字段（下游服务的日志都有）。
 * 网关侧的链路追踪仍可借助 SkyWalking 的 {@code %tid}。
 *
 * @author lion
 */
@Slf4j
@Order(-200)
@Component
public class TraceIdWebFilter implements WebFilter {

    /**
     * 必须与 mall-common 的 {@code Constants.HEADER_TRACE_ID} 保持一致。
     * 网关不依赖 mall-common（会引入 Servlet 栈导致 WebFlux 失效），所以这里单独声明。
     */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 调用方可能自己带了（例如服务间直调），优先沿用
        String traceId = exchange.getRequest().getHeaders().getFirst(HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            // 用 JDK 原生 UUID：网关不依赖 mall-common，拿不到 Hutool 的 IdUtil
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // mutate 出的新请求会带上该头，网关转发时自动透传给下游微服务
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(HEADER_TRACE_ID, traceId)
                .build();
        // 回写响应头：浏览器 Network 面板可直接看到本次请求的 traceId
        exchange.getResponse().getHeaders().set(HEADER_TRACE_ID, traceId);

        return chain.filter(exchange.mutate().request(mutated).build());
    }
}
