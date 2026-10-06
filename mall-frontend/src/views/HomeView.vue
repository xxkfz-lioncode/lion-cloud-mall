<template>
  <div>
    <!-- 顶部 Banner -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">Lion 商城</h1>
        <p class="hero-desc">
          基于 Spring Cloud Gateway + Nacos + OpenFeign + Sa-Token 的微服务商城演示
        </p>
        <el-input
          v-model="keyword"
          size="large"
          class="hero-search"
          placeholder="搜索你感兴趣的商品"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch" />
          </template>
        </el-input>
      </div>
    </section>

    <div class="page-container">
      <div class="section-header">
        <h2 class="lm-title">全部商品</h2>
        <span class="section-tip">共 {{ total }} 件商品</span>
      </div>

      <el-skeleton :loading="loading" animated :count="4">
        <template #template>
          <div class="skeleton-box"><el-skeleton-item variant="image" /></div>
        </template>
        <template #default>
          <el-empty v-if="!products.length" description="暂无商品，去「商品管理」发布一个吧" />
          <el-row :gutter="20">
            <el-col v-for="item in products" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6">
              <ProductCard :product="item" />
            </el-col>
          </el-row>
          <div class="pagination">
            <el-pagination
              v-model:current-page="pageNo"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="loadProducts"
            />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import ProductCard from '@/components/ProductCard.vue'
import { getProductPage } from '@/api/product'

const products = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(8)
const total = ref(0)

/** 加载商品列表 */
const loadProducts = async () => {
  loading.value = true
  try {
    const data = await getProductPage({
      pageNo: pageNo.value,
      pageSize: pageSize.value,
      keyword: keyword.value
    })
    products.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索（重置到第一页） */
const handleSearch = () => {
  pageNo.value = 1
  loadProducts()
}

onMounted(loadProducts)
</script>

<style scoped>
.hero {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 45%, #8b5cf6 100%);
  color: #fff;
  padding: 64px 16px 72px;
}

.hero-inner {
  max-width: 720px;
  margin: 0 auto;
  text-align: center;
}

.hero-title {
  font-size: 40px;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.hero-desc {
  margin: 0 0 28px;
  opacity: 0.9;
  font-size: 15px;
}

.hero-search {
  max-width: 520px;
  margin: 0 auto;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.section-tip {
  color: #9ca3af;
  font-size: 13px;
}

.skeleton-box {
  height: 180px;
  margin-bottom: 20px;
}

.pagination {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}
</style>
