<template>
  <div class="page-container" v-loading="loading">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>商品详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="product" class="lm-card detail-card">
      <div class="detail-left">
        <el-image :src="product.image" fit="cover" class="detail-img">
          <template #error>
            <div class="img-fallback"><el-icon><Picture /></el-icon></div>
          </template>
        </el-image>
      </div>

      <div class="detail-right">
        <h1 class="title">{{ product.name }}</h1>
        <p class="subtitle">{{ product.subtitle }}</p>

        <div class="price-box">
          <span class="lm-price price">¥ {{ product.price }}</span>
          <span class="stock">剩余库存 {{ product.stock }} 件</span>
        </div>

        <div class="buy-row">
          <span class="label">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="product.stock" size="large" />
        </div>

        <div class="btn-row">
          <el-button class="lm-gradient-btn" size="large" :icon="ShoppingCart" @click="handleAddCart">
            加入购物车
          </el-button>
          <el-button size="large" type="danger" plain @click="handleBuyNow">立即购买</el-button>
        </div>

        <el-divider content-position="left">商品详情</el-divider>
        <p class="description">{{ product.description || '暂无详情描述' }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, ShoppingCart } from '@element-plus/icons-vue'
import { getProductDetail } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const product = ref(null)
const loading = ref(false)
const quantity = ref(1)

onMounted(async () => {
  loading.value = true
  try {
    product.value = await getProductDetail(route.params.id)
  } finally {
    loading.value = false
  }
})

/** 加入购物车 */
const handleAddCart = () => {
  cartStore.add(product.value, quantity.value)
  ElMessage.success('已加入购物车')
}

/** 立即购买：加入购物车后跳转结算 */
const handleBuyNow = () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return router.push({ path: '/login', query: { redirect: '/cart' } })
  }
  cartStore.add(product.value, quantity.value)
  router.push('/cart')
}
</script>

<style scoped>
.breadcrumb {
  margin-bottom: 16px;
}

.detail-card {
  display: flex;
  padding: 24px;
  gap: 32px;
}

.detail-left {
  width: 380px;
  flex-shrink: 0;
}

.detail-img {
  width: 100%;
  height: 380px;
  border-radius: 12px;
  background: #f3f4f6;
}

.img-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #c0c4cc;
}

.detail-right {
  flex: 1;
}

.title {
  margin: 0 0 8px;
  font-size: 24px;
}

.subtitle {
  margin: 0 0 20px;
  color: #6b7280;
}

.price-box {
  background: #f9fafb;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.price {
  font-size: 28px;
}

.stock {
  color: #9ca3af;
  font-size: 13px;
}

.buy-row {
  margin: 24px 0;
  display: flex;
  align-items: center;
  gap: 16px;
}

.label {
  color: #6b7280;
}

.btn-row {
  display: flex;
  gap: 12px;
}

.description {
  color: #4b5563;
  line-height: 1.8;
}

@media (max-width: 768px) {
  .detail-card {
    flex-direction: column;
  }

  .detail-left {
    width: 100%;
  }
}
</style>
