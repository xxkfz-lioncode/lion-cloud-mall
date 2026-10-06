import request from './request'

/** 创建订单（下单） */
export function createOrder(data) {
  return request.post('/order/create', data)
}

/** 订单详情 */
export function getOrderDetail(id) {
  return request.get(`/order/${id}`)
}

/** 我的订单（分页） */
export function getOrderPage(params) {
  return request.get('/order/page', { params })
}

/** 支付订单 */
export function payOrder(id) {
  return request.post(`/order/${id}/pay`)
}

/** 取消订单 */
export function cancelOrder(id) {
  return request.post(`/order/${id}/cancel`)
}
