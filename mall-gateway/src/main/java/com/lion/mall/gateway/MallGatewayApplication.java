package com.lion.mall.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * 网关服务启动类
 * <p>
 * 说明：网关基于 WebFlux，不依赖 mall-common（mall-common 带 spring-boot-starter-web，
 * 引入会把网关从响应式切换成 Servlet 模式），因此这里内联了一份精简版环境摘要打印，
 * 业务服务统一使用 mall-common 的 {@code StartupEnvPrinter}。
 *
 * @author lion
 */
@Slf4j
@EnableDiscoveryClient
@SpringBootApplication
public class MallGatewayApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MallGatewayApplication.class, args);
        printEnv(ctx.getEnvironment());
    }

    /** 打印环境摘要：端口 / 注册中心 / 缓存 / 链路 / 日志 */
    private static void printEnv(Environment env) {
        String app = env.getProperty("spring.application.name", "app");
        String port = env.getProperty("server.port", "8080");
        double seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
        String[] profiles = env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles();

        log.info("========== {} 启动完成 | 耗时 {}s | 端口 {} ==========",
                app, String.format("%.1f", seconds), port);
        log.info("运行环境 : Java {} | PID {} | profiles={}", System.getProperty("java.version"), pid(), Arrays.toString(profiles));
        log.info("注册中心 : Nacos {}", env.getProperty("spring.cloud.nacos.discovery.server-addr", "-"));
        log.info("缓存     : Redis {}:{}", env.getProperty("spring.data.redis.host", "-"),
                env.getProperty("spring.data.redis.port", "6379"));
        log.info("链路追踪 : SkyWalking {}（agent {}）", skywalkingBackend(), agentMounted() ? "已挂载" : "未挂载");
        log.info("日志目录 : {}/{}.log", logHome(), app);
        log.info("====================================================");
    }

    private static String pid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }

    private static String skywalkingBackend() {
        String v = System.getProperty("skywalking.collector.backend_service");
        if (v == null) {
            v = System.getenv("SW_AGENT_COLLECTOR_BACKEND_SERVICES");
        }
        return v == null ? "-" : v;
    }

    /** Dockerfile 中只有挂载了 agent 才会传 -Dskywalking.agent.service_name */
    private static boolean agentMounted() {
        return System.getProperty("skywalking.agent.service_name") != null;
    }

    private static String logHome() {
        String v = System.getenv("LOG_PATH");
        return (v == null || v.isBlank()) ? "logs" : v;
    }
}
