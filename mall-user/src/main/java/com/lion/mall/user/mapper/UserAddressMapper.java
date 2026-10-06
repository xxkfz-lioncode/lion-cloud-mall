package com.lion.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lion.mall.user.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收货地址 Mapper
 *
 * @author lion
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
