import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import CartView from '@/views/CartView.vue'
import OrderListView from '@/views/OrderListView.vue'
import ProductAdminView from '@/views/ProductAdminView.vue'
import AddressView from '@/views/AddressView.vue'

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/product/:id', name: 'product-detail', component: ProductDetailView },
  { path: '/cart', name: 'cart', component: CartView, meta: { requiresAuth: true } },
  { path: '/orders', name: 'orders', component: OrderListView, meta: { requiresAuth: true } },
  { path: '/address', name: 'address', component: AddressView, meta: { requiresAuth: true } },
  {
    path: '/admin/product',
    name: 'admin-product',
    component: ProductAdminView,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 路由守卫：需要登录的页面未登录则跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('lm_token')
  if (to.meta.requiresAuth && !token) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
