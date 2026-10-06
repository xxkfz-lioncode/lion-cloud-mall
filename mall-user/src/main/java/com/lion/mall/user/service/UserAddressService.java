package com.lion.mall.user.service;

import com.lion.mall.user.entity.UserAddress;
import com.lion.mall.user.model.req.AddressSaveReq;

import java.util.List;

/**
 * 收货地址服务
 *
 * @author lion
 */
public interface UserAddressService {

    /** 查询指定用户的收货地址列表（默认地址排在最前） */
    List<UserAddress> listByUser(Long userId);

    /** 查询指定用户的默认收货地址（无地址时返回 null） */
    UserAddress getDefault(Long userId);

    /** 新增收货地址，返回新地址ID */
    Long save(Long userId, AddressSaveReq req);

    /** 修改收货地址 */
    void update(Long userId, AddressSaveReq req);

    /** 删除收货地址 */
    void remove(Long userId, Long id);

    /** 设为默认收货地址 */
    void setDefault(Long userId, Long id);
}
