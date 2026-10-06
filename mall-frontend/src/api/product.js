import request from './request'

/** 商品分页列表 */
export function getProductPage(params) {
  return request.get('/product/page', { params })
}

/** 商品详情 */
export function getProductDetail(id) {
  return request.get(`/product/${id}`)
}

/** 新增/修改商品（需登录） */
export function saveProduct(data) {
  return data.id ? request.put('/product', data) : request.post('/product', data)
}

/** 删除商品（需登录） */
export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}
