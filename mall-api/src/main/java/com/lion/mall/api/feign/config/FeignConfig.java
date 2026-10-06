package com.lion.mall.api.feign.config;

import com.lion.mall.common.constant.Constants;
import feign.RequestInterceptor;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
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
