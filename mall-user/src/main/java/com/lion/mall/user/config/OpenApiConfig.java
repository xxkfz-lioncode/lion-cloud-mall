package com.lion.mall.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 文档配置
 * <p>
 * 访问地址：http://localhost:8101/swagger-ui.html
 *
 * @author lion
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mallUserOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("商城-用户服务 API")
                .description("用户注册、登录、信息查询")
                .version("v1.0.0"));
    }
}
