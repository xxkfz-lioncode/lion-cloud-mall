<template>
  <div class="page-container">
    <h2 class="lm-title">商品管理（发布 / 下架）</h2>

    <div class="lm-card form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="product-form">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="一句话描述商品" />
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="form.image" placeholder="https://picsum.photos/seed/demo/500/500" />
        </el-form-item>
        <el-form-item label="售价" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="商品详情">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品详细介绍" />
        </el-form-item>
        <el-form-item>
          <el-button class="lm-gradient-btn" type="primary" :loading="submitting" @click="handleSave">
            {{ form.id ? '保存修改' : '发布商品' }}
          </el-button>
          <el-button v-if="form.id" @click="resetForm">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="lm-card list-card">
      <el-table :data="products" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="商品" min-width="220">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image :src="row.image" fit="cover" class="cell-img">
                <template #error>
                  <div class="img-fallback"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="售价" width="100">
          <template #default="{ row }"><span class="lm-price">¥ {{ row.price }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '在售' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { deleteProduct, getProductPage, saveProduct } from '@/api/product'

const products = ref([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: null,
  name: '',
  subtitle: '',
  image: '',
  price: 9.9,
  stock: 100,
  description: '',
  status: 1
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

/** 加载商品列表 */
const loadProducts = async () => {
  loading.value = true
  try {
    const data = await getProductPage({ pageNo: 1, pageSize: 50 })
    products.value = data.list
  } finally {
    loading.value = false
  }
}

/** 新增/修改商品 */
const handleSave = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    await saveProduct({ ...form })
    ElMessage.success(form.id ? '修改成功' : '发布成功')
    resetForm()
    loadProducts()
  } finally {
    submitting.value = false
  }
}

/** 编辑 */
const handleEdit = row => {
  Object.assign(form, row)
}

/** 删除 */
const handleDelete = async row => {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  loadProducts()
}

const resetForm = () => {
  Object.assign(form, defaultForm())
}

onMounted(loadProducts)
</script>

<style scoped>
.form-card {
  padding: 24px 24px 4px;
  margin-bottom: 20px;
}

.product-form {
  max-width: 760px;
}

.list-card {
  padding: 8px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cell-img {
  width: 48px;
  height: 48px;
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
</style>
