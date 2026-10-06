import request from './request'

/** 我的收货地址列表 */
export function getAddressList() {
  return request.get('/user/address/list')
}

/** 默认收货地址 */
export function getDefaultAddress() {
  return request.get('/user/address/default')
}

/** 新增收货地址 */
export function saveAddress(data) {
  return request.post('/user/address', data)
}

/** 修改收货地址 */
export function updateAddress(data) {
  return request.put('/user/address', data)
}

/** 删除收货地址 */
export function deleteAddress(id) {
  return request.delete(`/user/address/${id}`)
}

/** 设为默认收货地址 */
export function setDefaultAddress(id) {
  return request.post(`/user/address/${id}/default`)
}
