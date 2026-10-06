package com.lion.mall.api.feign.fallback;

import com.lion.mall.api.dto.UserDTO;
import com.lion.mall.api.feign.UserFeignClient;
import com.lion.mall.common.result.R;
import com.lion.mall.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户服务调用的熔断降级处理
 *
 * @author lion
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("调用 mall-user 失败，进入降级逻辑", cause);
        return new UserFeignClient() {
            @Override
            public R<UserDTO> getUserById(Long id) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "用户服务暂不可用");
            }
        };
    }
}
