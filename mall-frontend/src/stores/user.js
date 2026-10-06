import { defineStore } from 'pinia'
import { getInfo, login, register } from '@/api/user'

/**
 * 用户状态：token + 用户信息（持久化到 localStorage）
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('lm_token') || '',
    userInfo: JSON.parse(localStorage.getItem('lm_user') || 'null')
  }),

  getters: {
    isLogin: state => !!state.token,
    nickname: state => (state.userInfo ? state.userInfo.nickname : '')
  },

  actions: {
    /** 登录并拉取用户信息 */
    async login(data) {
      const res = await login(data)
      this.setToken(res.token)
      await this.loadUserInfo()
    },

    /** 注册（注册成功后需自行登录） */
    async register(data) {
      await register(data)
    },

    /** 加载当前用户信息 */
    async loadUserInfo() {
      this.userInfo = await getInfo()
      localStorage.setItem('lm_user', JSON.stringify(this.userInfo))
    },

    setToken(token) {
      this.token = token
      localStorage.setItem('lm_token', token)
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('lm_token')
      localStorage.removeItem('lm_user')
    }
  }
})
