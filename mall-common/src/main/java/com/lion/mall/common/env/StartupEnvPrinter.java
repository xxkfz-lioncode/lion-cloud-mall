package com.lion.mall.common.env;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * 启动环境信息打印
 * <p>
 * 服务启动完成后打印一份「当前连的是哪套环境」的摘要：端口、注册中心、数据库、缓存、
 * 分布式事务、链路追踪、日志目录、接口文档。避免连错环境排查半天。
 * <p>
 * 用法：在 {@code SpringApplication.run(...)} 之后调用
 * <pre>
 *     ConfigurableApplicationContext ctx = SpringApplication.run(XxxApplication.class, args);
 *     StartupEnvPrinter.print(ctx.getEnvironment());
 * </pre>
 *
 * @author lion
 */
@Slf4j
public final class StartupEnvPrinter {

    private StartupEnvPrinter() {
    }

    /**
     * 打印环境摘要（不存在的组件自动跳过，保证输出简洁）
     *
     * @param env Spring 环境（{@code ctx.getEnvironment()}）
     */
    public static void print(Environment env) {
        String app = env.getProperty("spring.application.name", "app");
        String port = env.getProperty("server.port", "8080");
        double seconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;

        log.info("========== {} 启动完成 | 耗时 {}s | 端口 {} ==========",
                app, String.format("%.1f", seconds), port);
        log.info("运行环境 : Java {} | PID {} | host {} | profiles={}",
                System.getProperty("java.version"), pid(), host(), profiles(env));
        log.info("注册中心 : Nacos {}", env.getProperty("spring.cloud.nacos.discovery.server-addr", "-"));

        String url = env.getProperty("spring.datasource.url");
        if (url != null) {
            log.info("数据库   : {}", simplifyJdbcUrl(url));
        }
        String redisHost = env.getProperty("spring.data.redis.host");
        if (redisHost != null) {
            log.info("缓存     : Redis {}:{} (db{})", redisHost,
                    env.getProperty("spring.data.redis.port", "6379"),
                    env.getProperty("spring.data.redis.database", "0"));
        }
        String txGroup = env.getProperty("seata.tx-service-group");
        if (txGroup != null) {
            log.info("事务     : Seata tx-group={} | TC {}", txGroup,
                    env.getProperty("seata.registry.nacos.application", "-"));
        }
        log.info("链路追踪 : SkyWalking {}（agent {}）", skywalkingBackend(), agentMounted() ? "已挂载" : "未挂载");
        log.info("日志目录 : {}/{}.log", logHome(), app);

        String docPath = env.getProperty("springdoc.swagger-ui.path");
        if (docPath != null) {
            log.info("接口文档 : http://127.0.0.1:{}{}", port, docPath);
        }
        log.info("====================================================");
    }

    private static String profiles(Environment env) {
        String[] active = env.getActiveProfiles();
        return Arrays.toString(active.length == 0 ? env.getDefaultProfiles() : active);
    }

    private static String pid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }

    private static String host() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /** 去掉 jdbc: 前缀与 ? 之后的参数，只保留 host:port/db */
    private static String simplifyJdbcUrl(String url) {
        String s = url.replaceFirst("^jdbc:", "");
        int idx = s.indexOf('?');
        return idx > 0 ? s.substring(0, idx) : s;
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
