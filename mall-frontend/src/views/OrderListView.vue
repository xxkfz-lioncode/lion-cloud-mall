<template>
  <div class="page-container" v-loading="loading">
    <h2 class="lm-title">我的订单</h2>

    <el-empty v-if="!orders.length" description="还没有订单，去下单吧" />

    <template v-else>
      <div v-for="order in orders" :key="order.id" class="lm-card order-card">
        <div class="order-header">
          <div>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <span class="order-time">{{ order.createTime }}</span>
          </div>
          <el-tag :type="statusTag(order.status)" effect="light">{{ order.statusDesc }}</el-tag>
        </div>

        <div v-for="item in order.items" :key="item.id" class="order-item">
          <el-image :src="item.productImage" fit="cover" class="item-img">
            <template #error>
              <div class="img-fallback"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
          <div class="item-name">{{ item.productName }}</div>
          <div class="item-price">¥ {{ item.productPrice }}</div>
          <div class="item-qty">x{{ item.quantity }}</div>
        </div>

        <div class="order-footer">
          <div class="order-info">
            <span>{{ order.receiverName }} {{ order.receiverPhone }}</span>
            <span>{{ order.address }}</span>
          </div>
          <div class="order-actions">
            <span class="amount">合计：<span class="lm-price">¥ {{ order.totalAmount }}</span></span>
            <el-button
              v-if="order.status === 0"
              class="lm-gradient-btn"
              type="primary"
              size="small"
              @click="handlePay(order)"
            >
              立即支付
            </el-button>
            <el-button
              v-if="order.status === 0"
              type="danger"
              plain
              size="small"
              @click="handleCancel(order)"
            >
              取消订单
            </el-button>
          </div>
        </div>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="pageNo"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          background
          @current-change="loadOrders"
        />
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { cancelOrder, getOrderPage, payOrder } from '@/api/order'

const orders = ref([])
const loading = ref(false)
const pageNo = ref(1)
const pageSize = ref(10)
const total = ref(0)

/** 状态标签样式 */
const statusTag = status => ({ 0: 'warning', 1: 'success', 2: 'info' })[status] || 'info'

/** 加载订单列表 */
const loadOrders = async () => {
  loading.value = true
  try {
    const data = await getOrderPage({ pageNo: pageNo.value, pageSize: pageSize.value })
    orders.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 支付 */
const handlePay = async order => {
  await payOrder(order.id)
  // 支付结果同样由后端 WebSocket 推送，这里不重复提示
  loadOrders()
}

/** 取消订单（会回滚库存） */
const handleCancel = async order => {
  await ElMessageBox.confirm('取消后库存会回滚，确定取消该订单吗？', '提示', { type: 'warning' })
  await cancelOrder(order.id)
  ElMessage.success('订单已取消')
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.order-card {
  padding: 20px 24px;
  margin-bottom: 18px;
}

.order-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

.order-no {
  font-weight: 600;
}

.order-time {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 13px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px dashed #f3f4f6;
}

.item-img {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  background: #f3f4f6;
  flex-shrink: 0;
}

.img-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.item-name {
  flex: 1;
  font-size: 14px;
}

.item-price {
  width: 90px;
  color: #6b7280;
}

.item-qty {
  width: 50px;
  color: #9ca3af;
}

.order-footer {
  padding-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
}

.order-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.amount {
  font-size: 14px;
}

.pagination {
  display: flex;
  justify-content: center;
}
</style>
