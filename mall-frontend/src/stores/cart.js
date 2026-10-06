import { defineStore } from 'pinia'

/**
 * 购物车状态：演示项目使用 localStorage 存储，刷新不丢失
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: JSON.parse(localStorage.getItem('lm_cart') || '[]')
  }),

  getters: {
    /** 商品总件数 */
    totalCount: state => state.items.reduce((sum, item) => sum + item.quantity, 0),
    /** 商品总金额 */
    totalAmount: state =>
      state.items.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2)
  },

  actions: {
    /** 加入购物车（已存在则累加数量） */
    add(product, quantity = 1) {
      const exist = this.items.find(item => item.productId === product.id)
      if (exist) {
        exist.quantity += quantity
      } else {
        this.items.push({
          productId: product.id,
          name: product.name,
          image: product.image,
          price: product.price,
          stock: product.stock,
          quantity
        })
      }
      this.persist()
    },

    /** 修改数量 */
    updateQuantity(productId, quantity) {
      const item = this.items.find(i => i.productId === productId)
      if (item) {
        item.quantity = quantity
        this.persist()
      }
    },

    /** 移除商品 */
    remove(productId) {
      this.items = this.items.filter(item => item.productId !== productId)
      this.persist()
    },

    /** 清空购物车 */
    clear() {
      this.items = []
      this.persist()
    },

    persist() {
      localStorage.setItem('lm_cart', JSON.stringify(this.items))
    }
  }
})
