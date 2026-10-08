package com.lion.mall.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关全局鉴权过滤器
 * <p>
 * 职责：
 * 1. 判断请求是否为公开接口（登录、注册、商品浏览等），公开接口直接放行；
 * 2. 非公开接口校验 Sa-Token 的 token（token 存于 Redis，网关与各服务共享登录态）；
 * 3. 校验通过后，把用户ID 放入 X-User-Id 请求头透传给下游服务，下游无需再解析 token。
 *
 * @author lion
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 网关向服务透传的用户ID请求头 */
    private static final String HEADER_USER_ID = "X-User-Id";
    /** token 名称（与 sa-token.token-name 保持一致） */
    private static final String TOKEN_NAME = "satoken";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        // 1. 公开接口直接放行
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        // 2. 读取 token（优先请求头，其次 Cookie）
        String token = request.getHeaders().getFirst(TOKEN_NAME);
        if (token == null && request.getCookies().getFirst(TOKEN_NAME) != null) {
            token = request.getCookies().getFirst(TOKEN_NAME).getValue();
        }

        // 3. 校验 token：未登录返回 null
        Object loginId = token == null ? null : StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            log.warn("未登录请求被拦截：{} {}", method, path);
            return unauthorized(exchange);
        }

        // 4. 透传用户ID给下游服务
        ServerHttpRequest newRequest = request.mutate()
                .header(HEADER_USER_ID, String.valueOf(loginId))
                .build();
        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    @Override
    public int getOrder() {
        // 需要早于路由转发，晚于 CORS 处理
        return -100;
    }

    /** 判断是否为无需登录即可访问的路径 */
    private boolean isPublicPath(String path, String method) {
        // 跨域预检请求
        if (HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        // WebSocket 握手：浏览器的 WebSocket API 不允许自定义请求头，
        // 所以 token 只能拼在 URL 参数上（?token=xxx），本网关的 header 鉴权拿不到它。
        // 鉴权下沉到 mall-order 的 WebSocket 端点里用 Sa-Token 完成，这里先放行握手。
        if (path.startsWith("/ws/")) {
            return true;
        }
        // 登录、注册
        if (path.startsWith("/api/user/login") || path.startsWith("/api/user/register")) {
            return true;
        }
        // 商品浏览（GET）公开，新增/修改/删除商品仍需登录
        if (path.startsWith("/api/product") && HttpMethod.GET.matches(method)) {
            return true;
        }
        // 接口文档与静态资源
        return path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/doc.html")
                || path.startsWith("/favicon.ico")
                || path.startsWith("/error");
    }

    /** 返回 401 未登录的 JSON 响应 */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":401,\"msg\":\"未登录或登录已失效\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
