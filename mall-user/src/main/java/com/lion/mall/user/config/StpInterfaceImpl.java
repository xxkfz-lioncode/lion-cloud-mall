package com.lion.mall.user.config;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 权限/角色数据源
 * <p>
 * 本演示项目只做登录校验，不做细粒度权限，返回空集合即可。
 *
 * @author lion
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
