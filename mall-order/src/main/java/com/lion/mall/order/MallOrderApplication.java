package com.lion.mall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

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
        SpringApplication.run(MallOrderApplication.class, args);
    }
}
