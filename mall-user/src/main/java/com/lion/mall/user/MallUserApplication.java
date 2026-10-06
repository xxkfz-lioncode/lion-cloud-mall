package com.lion.mall.user;

import com.lion.mall.common.env.StartupEnvPrinter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 用户服务启动类
 *
 * @author lion
 */
// scanBasePackages 扫描到 com.lion.mall，使 mall-common 中的全局异常处理、过滤器等自动生效
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.lion.mall")
public class MallUserApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MallUserApplication.class, args);
        // 打印当前环境摘要：端口 / 注册中心 / 数据库 / 缓存 / 事务 / 链路 / 日志 / 文档
        StartupEnvPrinter.print(ctx.getEnvironment());
    }
}
