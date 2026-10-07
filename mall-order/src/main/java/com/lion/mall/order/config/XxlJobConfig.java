package com.lion.mall.order.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-Job 执行器配置
 * <p>
 * 执行器启动后会向 admin 注册（以 appname 为标识），admin 按 appname 找到本机地址并下发调度。
 * 注意三点：
 * 1. appname 必须与 admin 后台「执行器管理」里的 AppName 完全一致（本项目已在 SQL 中预置为 mall-order-executor）；
 * 2. accessToken 必须与 admin 的一致，否则调度会被拒绝；
 * 3. port 是执行器自己监听的端口（admin 主动连过来），容器内无需映射到宿主机。
 *
 * @author lion
 */
@Slf4j
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.access-token:}")
    private String accessToken;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    /** 执行器 IP，留空则自动探测（容器里通常就是容器 IP，与 admin 同网络可互通） */
    @Value("${xxl.job.executor.ip:}")
    private String ip;

    /** 执行器端口，0 表示随机；建议固定（便于排查） */
    @Value("${xxl.job.executor.port:9999}")
    private int port;

    /** 任务日志落盘目录（容器内 /app/logs 已挂到宿主机 ./logs） */
    @Value("${xxl.job.executor.logpath:/app/logs/xxl-job}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays:7}")
    private int logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> xxl-job 执行器初始化：appname={}, admin={}, port={}", appname, adminAddresses, port);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
