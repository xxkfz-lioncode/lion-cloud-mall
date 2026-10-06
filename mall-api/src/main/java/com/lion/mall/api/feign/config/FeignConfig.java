package com.lion.mall.api.feign.config;

import com.lion.mall.common.constant.Constants;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * OpenFeign 全局配置
 * <p>
 * 作用：微服务内部调用时，把当前请求的 token 与用户ID 透传到下游服务，
 * 解决「Feign 调用丢失登录态」的经典问题。
 *
 * @author lion
 */
@Slf4j
@Configuration
public class FeignConfig {

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
