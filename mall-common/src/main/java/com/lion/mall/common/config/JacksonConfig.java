package com.lion.mall.common.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

/**
 * 全局 Jackson 时间序列化配置。
 * <p>
 * <b>要解决什么问题</b>：{@link LocalDateTime} 默认被 Jackson 序列化成 ISO-8601 格式
 * （{@code 2026-10-08T12:25:12}），前端直接展示会带上难看的 {@code T}。
 * <p>
 * <b>为什么不用 {@code application.yml} 的 {@code spring.jackson.date-format}</b>：
 * 那个配置只对老的 {@code java.util.Date} 生效，对 Java 8 时间类型<b>无效</b>，
 * 必须显式注册序列化器，这是最容易踩的坑。
 * <p>
 * <b>为什么放在 mall-common</b>：各业务服务启动类都是
 * {@code @SpringBootApplication(scanBasePackages = "com.lion.mall")}，
 * 放在这个包下 user / product / order 等<b>所有服务自动生效</b>，格式天然统一。
 * <p>
 * <b>⚠ 影响范围不止「返回给前端」</b>：Feign 的解码器（{@code SpringDecoder}）
 * 用的也是 Spring 容器里这个 ObjectMapper，所以<b>服务之间的 JSON 调用同样会被影响</b>。
 * 这意味着如果只重建了部分服务，新旧服务之间时间格式不一致，Feign 就会解码失败：
 * <pre>
 *   feign.codec.DecodeException: Error while extracting response for type [R&lt;UserDTO&gt;]
 * </pre>
 * 为此，下面的<b>解析</b>格式做成了宽松模式：同时接受本配置输出的空格格式
 * 和 Jackson 默认的 ISO 格式（带 {@code T}）。
 * 这样即使某台服务还没升级，也只会格式不统一，而<b>不会直接报错</b>，
 * 部署可以平滑过渡。
 *
 * @author lion
 */
@Configuration
public class JacksonConfig {

    /** 统一的时间输出格式：{@code 2026-10-08 12:25:12} */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 序列化用：严格按 {@link #DATE_TIME_PATTERN} 输出 */
    private static final DateTimeFormatter OUTPUT_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    /**
     * 反序列化用：宽松解析，下面两种都认。
     * <ul>
     *   <li>{@code 2026-10-08 12:25:12} —— 本配置的输出格式</li>
     *   <li>{@code 2026-10-08T12:25:12} —— Jackson 默认的 ISO-8601（未升级服务仍在用）</li>
     * </ul>
     * 两种只差「日期与时间之间那个字符」：{@code T} 或空格。
     * 所以日期部分用 ISO 规则解析，然后<b>把 {@code T} 和空格都设为可选分隔符</b>，
     * 最后接 ISO 时间部分 —— 无论遇上哪种都能吃掉。
     */
    private static final DateTimeFormatter INPUT_FORMATTER = new DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart().appendLiteral('T').optionalEnd()
            .optionalStart().appendLiteral(' ').optionalEnd()
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder
                .serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(OUTPUT_FORMATTER))
                .deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(INPUT_FORMATTER));
    }
}
