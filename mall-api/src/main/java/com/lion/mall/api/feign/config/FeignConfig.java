package com.lion.mall.api.feign.config;

import com.lion.mall.common.constant.Constants;
import com.lion.mall.common.context.TraceIdContext;
import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 全局配置
 * <p>
 * 作用：微服务内部调用时把上下文透传给下游服务，解决两类经典问题：
 * 1. token 与用户ID：Feign 调用丢失登录态；
 * 2. Seata XID：下游服务无法加入当前全局事务（见 {@link #seataXidInterceptor()}）。
 *
 * @author lion
 */
@Slf4j
@Configuration
public class FeignConfig {

    /**
     * 透传 Seata 全局事务 XID（请求头 TX_XID）
     * <p>
     * 没有这一步，下游服务拿不到 XID，就不会注册到当前全局事务里，
     * 表现为「订单回滚了，但商品库存照样被扣」。这是集成 Seata 最常踩的坑。
     */
    @Bean
    public RequestInterceptor seataXidInterceptor() {
        return template -> {
            String xid = RootContext.getXID();
            if (xid != null && !xid.isBlank()) {
                template.header(RootContext.KEY_XID, xid);
            }
        };
    }

    /**
     * 透传链路追踪 ID（请求头 X-Trace-Id）
     * <p>
     * Feign 调用是全新的 HTTP 请求，下游服务的 ThreadLocal / MDC 不会自动带过去。
     * 没有这一步的话，一次「下单」请求在 order 服务和 product 服务里会是两个不同的
     * traceId，Kibana 里就没法把整条链路的日志串起来看了。
     * <p>
     * 取值顺序：优先 ThreadLocal（{@code TraceIdFilter} 写入），
     * 再退化到 MDC —— 因为定时任务、MQ 消费者等场景没有经过 Filter，
     * 但可能别处手动往 MDC 塞过 traceId。
     */
    @Bean
    public RequestInterceptor traceIdInterceptor() {
        return template -> {
            String traceId = TraceIdContext.get();
            if (traceId == null || traceId.isBlank()) {
                traceId = MDC.get(Constants.MDC_TRACE_ID);
            }
            if (traceId != null && !traceId.isBlank()) {
                template.header(Constants.HEADER_TRACE_ID, traceId);
            }
        };
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            // 透传登录 token
            String token = attributes.getRequest().getHeader(Constants.TOKEN_NAME);
            if (token != null && !token.isBlank()) {
                template.header(Constants.TOKEN_NAME, token);
            }
            // 透传用户ID
            String userId = attributes.getRequest().getHeader(Constants.HEADER_USER_ID);
            if (userId != null && !userId.isBlank()) {
                template.header(Constants.HEADER_USER_ID, userId);
            }
        };
    }
}
