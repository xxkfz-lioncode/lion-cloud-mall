package com.lion.mall.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(Swagger) 文档配置
 * <p>
 * 访问地址：http://localhost:8102/swagger-ui.html
 *
 * @author lion
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mallProductOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("商城-商品服务 API")
                .description("商品查询、维护与库存扣减")
                .version("v1.0.0"));
    }
}
