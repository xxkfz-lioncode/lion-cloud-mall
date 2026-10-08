package com.lion.mall.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket 配置。
 * <p>
 * {@link ServerEndpointExporter} 负责扫描并注册 {@code @ServerEndpoint} 标注的端点。
 * <b>注意</b>：打 war 包部署到外部 Tomcat 时不需要这个 Bean（ Tomcat 自己会扫描），
 * 本项目是 Spring Boot 内嵌容器（jar），必须有它，否则 WebSocket 连不上。
 *
 * @author lion
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
