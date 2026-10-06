package com.lion.mall.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lion.mall.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author lion
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
