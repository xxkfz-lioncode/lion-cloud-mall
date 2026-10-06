package com.lion.mall.api.feign;

import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.api.feign.fallback.UserFeignFallbackFactory;
import com.lion.mall.common.result.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 用户服务远程调用接口（服务名即 Nacos 注册名 mall-user）
 *
 * @author lion
 */
@FeignClient(name = "mall-user", path = "/user", fallbackFactory = UserFeignFallbackFactory.class)
public interface UserFeignClient {

    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    R<UserDTO> getUserById(@PathVariable("id") Long id);
}
