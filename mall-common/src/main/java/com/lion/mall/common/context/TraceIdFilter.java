package com.lion.mall.common.context;

import cn.hutool.core.util.IdUtil;
import com.lion.mall.common.constant.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪 ID 过滤器（Servlet 服务：user / product / order）
 * <p>
 * 职责：
 * <ol>
 *   <li>优先沿用上游传来的 {@code X-Trace-Id}（网关或别的微服务已在链路里）；</li>
 *   <li>没有就自己生成一个 —— 这样即使请求不经过网关（比如 XXL-Job 直接调服务）也有 traceId；</li>
 *   <li>写入 ThreadLocal + MDC，让本线程打印的日志自动带上 traceId；</li>
 *   <li>回写响应头，浏览器 Network 面板可直接拿到，便于反馈问题。</li>
 * </ol>
 * <p>
 * <b>order 为什么是 -200</b>：必须早于 {@link UserContextFilter}（{@code -100}），
 * 越小越先执行，这样后续所有日志（包括用户上下文处理里的）都能带上 traceId。
 * <p>
 * <b>只适用于 Servlet 栈</b>：网关是 WebFlux，用 {@code OncePerRequestFilter} 无效，
 * 那边对应的是 {@code TraceIdWebFilter}。网关启动类没有配置
 * {@code scanBasePackages}，只扫 {@code com.lion.mall.gateway}，不会加载本类。
 *
 * @author lion
 */
@Order(-200)
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String traceId = request.getHeader(Constants.HEADER_TRACE_ID);
            if (!StringUtils.hasText(traceId)) {
                traceId = IdUtil.fastSimpleUUID();
            }
            TraceIdContext.set(traceId);
            // MDC：Logback 会从这里取值，JSON 日志里就会出现 traceId 字段
            MDC.put(Constants.MDC_TRACE_ID, traceId);
            response.setHeader(Constants.HEADER_TRACE_ID, traceId);
            chain.doFilter(request, response);
        } finally {
            // 容器线程复用，必须清理，否则下一个请求会读到上一个的 traceId
            TraceIdContext.clear();
            MDC.remove(Constants.MDC_TRACE_ID);
        }
    }
}
