package com.lion.mall.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置中心测试用的配置项（{@code mall.demo.*}）
 * <p>
 * 作用：验证 Nacos 配置中心是否真的能「改了就生效」。
 * 标注 {@code @ConfigurationProperties} 的 bean 在配置变更时会被重新绑定，
 * 所以不用 {@code @RefreshScope}、也不用重启就能拿到新值。
 * <p>
 * 验证完可以连同 {@code ConfigCenterTestController} 一起删掉。
 *
 * @author lion
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = DemoConfigProperties.PREFIX)
public class DemoConfigProperties {

    public static final String PREFIX = "mall.demo";

    /** 随便一个字符串配置 */
    private String tip = "(未配置，用默认值)";
    /** 随便一个数字配置 */
    private int count = 0;
}
