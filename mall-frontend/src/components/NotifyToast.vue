<template>
  <div class="notify-layer">
    <div
      v-for="item in list"
      :key="item.id"
      class="notify-card"
      :class="item.type === 'ORDER_PAID' ? 'is-paid' : 'is-created'"
      @click="dismiss(item.id)"
    >
      <div class="notify-icon">{{ item.type === 'ORDER_PAID' ? '✓' : '⏰' }}</div>

      <div class="notify-main">
        <div class="notify-title">{{ item.title }}</div>
        <div class="notify-content">{{ item.content }}</div>
        <div v-if="item.orderNo" class="notify-meta">
          订单号 {{ item.orderNo }}
          <template v-if="item.amount"> · ¥{{ item.amount }}</template>
        </div>
      </div>

      <div class="notify-hint">点击关闭</div>
    </div>
  </div>
</template>

<script setup>
import { onUnmounted, ref, watch } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 后端实时推送的通知弹窗
 *
 * 两种消息会通过 WebSocket 推过来，用不同颜色区分：
 *   ORDER_PAID    支付成功  → 绿色
 *   ORDER_CREATED 下单待支付 → 橙色
 *
 * 点击卡片即关闭；不点也会在 8 秒后自动消失。
 */

const userStore = useUserStore()

const list = ref([])

const AUTO_DISMISS_MS = 8000
const HEARTBEAT_MS = 30000
const RECONNECT_MS = 3000

let socket = null
let heartbeatTimer = null
let reconnectTimer = null
let seq = 0

const nextId = () => `${Date.now()}-${seq++}`

/** 弹出一条通知 */
function push(raw) {
  let payload
  try {
    payload = typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch (e) {
    console.warn('[ws] 收到非 JSON 消息，已忽略', raw)
    return
  }
  const item = { ...payload, id: nextId() }
  list.value.push(item)
  setTimeout(() => dismiss(item.id), AUTO_DISMISS_MS)
}

/** 关闭一条通知 */
function dismiss(id) {
  list.value = list.value.filter(i => i.id !== id)
}

function connect() {
  const token = localStorage.getItem('lm_token')
  if (!token) return // 未登录不建立连接

  // 和 axios 一样走相对地址：由 vite proxy（开发）或 nginx（容器）转发到网关，
  // 这样不用在代码里写死网关地址。
  const scheme = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const url = `${scheme}//${location.host}/ws/notify?token=${encodeURIComponent(token)}`

  socket = new WebSocket(url)

  socket.onopen = () => {
    console.log('[ws] 订单通知已连接')
    startHeartbeat()
  }

  socket.onmessage = event => push(event.data)

  // 出错后必然会触发 onclose，重连统一交给 onclose 处理，避免重复重连
  socket.onerror = err => console.warn('[ws] 连接异常', err)

  socket.onclose = () => {
    stopHeartbeat()
    scheduleReconnect()
  }
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send('ping')
    }
  }, HEARTBEAT_MS)
}

function stopHeartbeat() {
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  heartbeatTimer = null
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, RECONNECT_MS)
}

/** 主动关闭连接（退出登录 / 组件卸载） */
function closeSocket() {
  if (socket) {
    // 置空回调，避免主动关闭时被当成掉线而自动重连
    socket.onclose = null
    socket.close()
    socket = null
  }
  list.value = []
}

// 登录后才建立连接；退出登录后主动断开。
// immediate: true 让「刷新页面时已是登录态」的场景也能马上连上。
watch(
  () => userStore.isLogin,
  val => (val ? connect() : closeSocket()),
  { immediate: true }
)

onUnmounted(() => {
  stopHeartbeat()
  if (reconnectTimer) clearTimeout(reconnectTimer)
  closeSocket()
})
</script>

<style scoped>
.notify-layer {
  position: fixed;
  top: 68px;
  right: 20px;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notify-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-width: 320px;
  max-width: 380px;
  padding: 14px 16px;
  border-radius: 10px;
  background: #fff;
  border-left: 4px solid #909399;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  cursor: pointer;
  animation: slide-in 0.28s ease;
  transition: transform 0.15s ease;
}

.notify-card:hover {
  transform: translateX(-4px);
}

/* 支付成功 */
.is-paid {
  border-left-color: #67c23a;
}
.is-paid .notify-icon {
  background: #f0f9eb;
  color: #67c23a;
}

/* 下单待支付 */
.is-created {
  border-left-color: #e6a23c;
}
.is-created .notify-icon {
  background: #fdf6ec;
  color: #e6a23c;
}

.notify-icon {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 700;
}

.notify-main {
  flex: 1;
  min-width: 0;
}

.notify-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 3px;
}

.notify-content {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.notify-meta {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

.notify-hint {
  flex-shrink: 0;
  align-self: center;
  font-size: 11px;
  color: #c0c4cc;
}

@keyframes slide-in {
  from {
    opacity: 0;
    transform: translateX(40px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
