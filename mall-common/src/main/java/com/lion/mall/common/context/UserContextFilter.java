package com.lion.mall.common.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 用户上下文过滤器：从请求头 X-User-Id 中提取用户 ID，写入 UserContext
 *
 * @author lion
 */
@Slf4j
@Component
@Order(-100)
public class UserContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(UserContext.headerName());
            if (userId != null && !userId.isBlank()) {
                UserContext.setUserId(Long.valueOf(userId));
            }
            filterChain.doFilter(request, response);
        } catch (NumberFormatException e) {
            log.warn("非法的用户ID请求头：{}", request.getHeader(UserContext.headerName()));
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
