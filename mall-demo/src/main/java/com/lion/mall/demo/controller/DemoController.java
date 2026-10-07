package com.lion.mall.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * K8s 演示专用接口
 *
 * <p>设计目的：把 K8s 里"看不见"的东西通过 HTTP 响应暴露出来，方便教学观察：
 * <ul>
 *   <li>{@code /demo/hello} —— 每次请求返回处理它的 Pod 名，
 *       多副本时连续请求会看到不同的 Pod，直观感受 Service 的负载均衡。</li>
 *   <li>{@code /demo/info} —— 返回 Pod 元数据与配置来源，
 *       验证 Downward API 环境变量、ConfigMap 是否注入成功。</li>
 *   <li>{@code /demo/echo} —— 回显请求头，观察 Service / Ingress 转发后的变化。</li>
 *   <li>{@code /demo/slow} —— 人为拖慢接口，用于演示探针超时与优雅停机。</li>
 * </ul>
 *
 * @author lion
 */
@Slf4j
@Tag(name = "K8s 演示接口")
@RestController
@RequestMapping("/demo")
public class DemoController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 进程级计数器：同一个 Pod 内累加，可用来确认请求确实落到了不同副本 */
    private final AtomicLong counter = new AtomicLong();

    /** 自定义演示文案：K8s 里可由 ConfigMap 注入 DEMO_MESSAGE 覆盖 */
    @Value("${demo.message:Hello from mall-demo}")
    private String message;

    @Value("${server.port:8100}")
    private String port;

    /** Downward API 注入的 Pod 元数据（只有在 K8s 里运行才有值） */
    @Value("${POD_NAME:local}")
    private String podName;

    @Value("${POD_IP:127.0.0.1}")
    private String podIp;

    @Operation(summary = "打招呼：返回处理本次请求的 Pod 信息")
    @GetMapping("/hello")
    public Map<String, Object> hello(
            @RequestParam(name = "name", required = false, defaultValue = "K8s") String name) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", message + ", " + name);
        data.put("hostname", hostname());
        data.put("port", port);
        data.put("requestCountInThisPod", counter.incrementAndGet());
        data.put("time", LocalDateTime.now().format(FMT));
        log.info("/demo/hello name={} servedBy={}", name, hostname());
        return data;
    }

    @Operation(summary = "运行环境详情：Pod 元数据、端口、ConfigMap 配置")
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("hostname", hostname());
        data.put("osName", System.getProperty("os.name"));
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        data.put("maxMemoryMB", Runtime.getRuntime().maxMemory() / 1024 / 1024);

        Map<String, Object> k8s = new LinkedHashMap<>();
        k8s.put("podName", podName);
        k8s.put("podIp", podIp);
        k8s.put("serverPort", port);
        data.put("k8s", k8s);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("demoMessage", message);
        data.put("config", config);
        return data;
    }

    @Operation(summary = "回显请求内容：观察 Service 转发后的 Header 变化")
    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("receivedBody", body);
        data.put("clientAddr", request.getRemoteAddr());
        data.put("userAgent", request.getHeader("User-Agent"));
        data.put("hostHeader", request.getHeader("Host"));
        data.put("servedBy", hostname());
        data.put("requestCountInThisPod", counter.incrementAndGet());
        return data;
    }

    @Operation(summary = "模拟慢接口：按指定毫秒 sleep，用于演示探针超时 / 优雅停机")
    @GetMapping("/slow")
    public Map<String, Object> slow(@RequestParam(name = "ms", defaultValue = "3000") long ms)
            throws InterruptedException {
        Thread.sleep(ms);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sleptMs", ms);
        data.put("servedBy", hostname());
        return data;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
