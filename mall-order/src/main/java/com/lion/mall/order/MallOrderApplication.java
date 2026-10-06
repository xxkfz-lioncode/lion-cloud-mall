package com.lion.mall.order;

import com.lion.mall.common.env.StartupEnvPrinter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 订单服务启动类
 *
 * @author lion
 */
@EnableDiscoveryClient
// 扫描 mall-api 模块中的 @FeignClient 接口
@EnableFeignClients(basePackages = "com.lion.mall.api")
@SpringBootApplication(scanBasePackages = "com.lion.mall")
public class MallOrderApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MallOrderApplication.class, args);
        // 打印当前环境摘要：端口 / 注册中心 / 数据库 / 缓存 / 事务 / 链路 / 日志 / 文档
        StartupEnvPrinter.print(ctx.getEnvironment());
    }
}
