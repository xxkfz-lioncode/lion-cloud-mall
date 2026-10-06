<template>
  <header class="app-header">
    <div class="header-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <span class="logo-mark">L</span>
        <span class="logo-text">Lion 商城</span>
      </router-link>

      <!-- 导航 -->
      <nav class="nav">
        <router-link to="/" class="nav-item">首页</router-link>
        <router-link to="/orders" class="nav-item">我的订单</router-link>
        <router-link to="/admin/product" class="nav-item">商品管理</router-link>
      </nav>

      <!-- 右侧操作区 -->
      <div class="actions">
        <router-link to="/cart" class="cart-link">
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0" :max="99">
            <el-icon :size="20"><ShoppingCart /></el-icon>
          </el-badge>
          <span>购物车</span>
        </router-link>

        <el-dropdown v-if="userStore.isLogin" @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="30" :src="userStore.userInfo?.avatar" />
            <span class="nickname">{{ userStore.nickname }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="orders">我的订单</el-dropdown-item>
              <el-dropdown-item command="address">收货地址</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-button v-else type="primary" round @click="$router.push('/login')">
          登录 / 注册
        </el-button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

// 已登录但用户信息为空时（刷新页面）自动拉取
onMounted(() => {
  if (userStore.isLogin && !userStore.userInfo) {
    userStore.loadUserInfo().catch(() => userStore.logout())
  }
})

const handleCommand = async command => {
  if (command === 'orders') {
    router.push('/orders')
  } else if (command === 'address') {
    router.push('/address')
  } else if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(31, 41, 55, 0.06);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  font-size: 18px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav {
  display: flex;
  gap: 20px;
  margin-right: auto;
}

.nav-item {
  color: #4b5563;
  font-size: 15px;
  padding: 6px 0;
}

.nav-item.router-link-exact-active {
  color: #4f46e5;
  font-weight: 600;
  border-bottom: 2px solid #4f46e5;
}

.actions {
  display: flex;
  align-items: center;
  gap: 18px;
}

.cart-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #4b5563;
}

.cart-link:hover {
  color: #4f46e5;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.nickname {
  font-size: 14px;
  color: #374151;
}

@media (max-width: 640px) {
  .nav {
    display: none;
  }
}
</style>
