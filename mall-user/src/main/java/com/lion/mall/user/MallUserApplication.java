package com.lion.mall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

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
        SpringApplication.run(MallUserApplication.class, args);
    }
}
