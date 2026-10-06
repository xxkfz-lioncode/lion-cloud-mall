import request from './request'

/** 用户登录 */
export function login(data) {
  return request.post('/user/login', data)
}

/** 用户注册 */
export function register(data) {
  return request.post('/user/register', data)
}

/** 获取当前登录用户信息 */
export function getInfo() {
  return request.get('/user/info')
}
