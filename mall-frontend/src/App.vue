<template>
  <div class="app-wrapper">
    <AppHeader />
    <main>
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
    <footer class="app-footer">
      Lion 商城 · Spring Cloud + Spring Cloud Alibaba 微服务学习演示
    </footer>

    <!-- 后端 WebSocket 实时推送的订单通知（全局挂载，登录后才连接） -->
    <NotifyToast />
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
import NotifyToast from '@/components/NotifyToast.vue'
</script>

<style scoped>
.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

main {
  flex: 1;
}

.app-footer {
  text-align: center;
  padding: 24px 0;
  color: #9ca3af;
  font-size: 13px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
