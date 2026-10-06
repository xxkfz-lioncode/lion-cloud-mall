<template>
  <div class="lm-card product-card" @click="$router.push(`/product/${product.id}`)">
    <div class="img-box">
      <el-image :src="product.image" fit="cover" lazy>
        <template #error>
          <div class="img-fallback">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
    </div>
    <div class="info">
      <div class="name">{{ product.name }}</div>
      <div class="subtitle">{{ product.subtitle }}</div>
      <div class="bottom">
        <span class="lm-price">¥ {{ product.price }}</span>
        <span class="stock">库存 {{ product.stock }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Picture } from '@element-plus/icons-vue'

defineProps({
  product: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.product-card {
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  margin-bottom: 20px;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.18);
}

.img-box {
  height: 180px;
  background: #f3f4f6;
}

.img-box :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.img-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #c0c4cc;
}

.info {
  padding: 12px 14px 16px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #9ca3af;
  height: 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bottom {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stock {
  font-size: 12px;
  color: #9ca3af;
}
</style>
