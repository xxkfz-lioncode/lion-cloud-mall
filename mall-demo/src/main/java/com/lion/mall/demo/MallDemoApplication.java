package com.lion.mall.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 演示服务启动类
 *
 * <p>与其他模块的区别：不加 @EnableDiscoveryClient、不读 Nacos 配置、不连数据库，
 * 完全独立运行 —— 这样打包成镜像后在 K8s 里几秒就能 Ready，
 * 非常适合反复练习扩缩容、滚动更新、回滚等操作。
 *
 * @author lion
 */
@SpringBootApplication(scanBasePackages = "com.lion.mall")
public class MallDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallDemoApplication.class, args);
    }
}
