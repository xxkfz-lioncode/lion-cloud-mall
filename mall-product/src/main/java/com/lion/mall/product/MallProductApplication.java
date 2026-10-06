package com.lion.mall.product;

import com.lion.mall.common.env.StartupEnvPrinter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 商品服务启动类
 *
 * @author lion
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.lion.mall")
public class MallProductApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MallProductApplication.class, args);
        // 打印当前环境摘要：端口 / 注册中心 / 数据库 / 缓存 / 事务 / 链路 / 日志 / 文档
        StartupEnvPrinter.print(ctx.getEnvironment());
    }
}
