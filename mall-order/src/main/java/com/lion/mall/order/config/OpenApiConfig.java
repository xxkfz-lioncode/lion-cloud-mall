package com.lion.mall.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 文档配置
 * <p>
 * 访问地址：http://localhost:8103/swagger-ui.html
 *
 * @author lion
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mallOrderOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("商城-订单服务 API")
                .description("下单、支付、取消订单")
                .version("v1.0.0"));
    }
}
