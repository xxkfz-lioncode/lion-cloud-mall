package com.lion.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lion.mall.common.exception.BizException;
import com.lion.mall.common.result.ResultCode;
import com.lion.mall.user.entity.UserAddress;
import com.lion.mall.user.mapper.UserAddressMapper;
import com.lion.mall.user.model.req.AddressSaveReq;
import com.lion.mall.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务实现
 *
 * @author lion
 */
@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    /** 默认地址标记 */
    private static final int DEFAULT_FLAG = 1;
    /** 非默认地址标记 */
    private static final int NOT_DEFAULT_FLAG = 0;

    private final UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> listByUser(Long userId) {
        // 默认地址排在最前，其余按新→旧
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getId));
    }

    @Override
    public UserAddress getDefault(Long userId) {
        List<UserAddress> list = listByUser(userId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(Long userId, AddressSaveReq req) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setReceiverName(req.getReceiverName());
        address.setReceiverPhone(req.getReceiverPhone());
        address.setAddress(req.getAddress());

        // 用户的第一条地址自动成为默认地址
        boolean first = userAddressMapper.selectCount(new LambdaQueryWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)) == 0;
        boolean asDefault = first || Boolean.TRUE.equals(req.getIsDefault());
        if (asDefault) {
            clearDefault(userId);
        }
        address.setIsDefault(asDefault ? DEFAULT_FLAG : NOT_DEFAULT_FLAG);
        userAddressMapper.insert(address);
        return address.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, AddressSaveReq req) {
        if (req.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "地址ID不能为空");
        }
        UserAddress exists = getOwned(userId, req.getId());
        exists.setReceiverName(req.getReceiverName());
        exists.setReceiverPhone(req.getReceiverPhone());
        exists.setAddress(req.getAddress());
        if (Boolean.TRUE.equals(req.getIsDefault())) {
            clearDefault(userId);
            exists.setIsDefault(DEFAULT_FLAG);
        }
        userAddressMapper.updateById(exists);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long id) {
        UserAddress exists = getOwned(userId, id);
        userAddressMapper.deleteById(id);
        // 删除的是默认地址时，把剩余的最新一条提升为默认，保证总有可用地址
        if (DEFAULT_FLAG == exists.getIsDefault()) {
            List<UserAddress> remain = listByUser(userId);
            if (!remain.isEmpty()) {
                UserAddress next = remain.get(0);
                next.setIsDefault(DEFAULT_FLAG);
                userAddressMapper.updateById(next);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long userId, Long id) {
        UserAddress exists = getOwned(userId, id);
        clearDefault(userId);
        exists.setIsDefault(DEFAULT_FLAG);
        userAddressMapper.updateById(exists);
    }

    /** 查询地址并校验归属，非本人地址视为不存在 */
    private UserAddress getOwned(Long userId, Long id) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "收货地址不存在");
        }
        return address;
    }

    /** 清除该用户的全部默认标记 */
    private void clearDefault(Long userId) {
        userAddressMapper.update(null, new LambdaUpdateWrapper<UserAddress>()
                .eq(UserAddress::getUserId, userId)
                .set(UserAddress::getIsDefault, NOT_DEFAULT_FLAG));
    }
}
