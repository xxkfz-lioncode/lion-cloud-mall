import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例：统一处理 token 携带、响应拆包、错误提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截：携带 Sa-Token 的 token（token 名称需与后端 sa-token.token-name 一致）
request.interceptors.request.use(config => {
  const token = localStorage.getItem('lm_token')
  if (token) {
    config.headers['satoken'] = token
  }
  return config
})

// 响应拦截：后端统一返回 { code, msg, data }
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('lm_token')
      localStorage.removeItem('lm_user')
      ElMessage.warning('登录已失效，请重新登录')
      setTimeout(() => (location.href = '/login'), 500)
    } else {
      ElMessage.error(error.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
