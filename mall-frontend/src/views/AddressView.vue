<template>
  <div class="page-container">
    <h2 class="lm-title">收货地址管理</h2>

    <!-- 新增 / 编辑表单 -->
    <div class="lm-card form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" class="address-form">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入收货人手机号" />
        </el-form-item>
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
        <el-form-item>
          <el-button class="lm-gradient-btn" type="primary" :loading="submitting" @click="handleSave">
            {{ form.id ? '保存修改' : '新增地址' }}
          </el-button>
          <el-button v-if="form.id" @click="resetForm">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 地址列表 -->
    <div class="lm-card list-card">
      <el-table v-loading="loading" :data="addresses" style="width: 100%">
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column prop="receiverPhone" label="手机号" width="150" />
        <el-table-column prop="address" label="收货地址" min-width="240" show-overflow-tooltip />
        <el-table-column label="默认" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" effect="light">默认</el-tag>
            <el-button v-else link type="primary" @click="handleSetDefault(row)">设为默认</el-button>
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
import {
  deleteAddress,
  getAddressList,
  saveAddress,
  setDefaultAddress,
  updateAddress
} from '@/api/address'

const addresses = ref([])
const loading = ref(false)
const submitting = ref(false)
const formRef = ref()

const defaultForm = () => ({
  id: null,
  receiverName: '',
  receiverPhone: '',
  address: '',
  isDefault: false
})

const form = reactive(defaultForm())

const rules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

/** 加载地址列表 */
const loadAddresses = async () => {
  loading.value = true
  try {
    addresses.value = await getAddressList()
  } finally {
    loading.value = false
  }
}

/** 新增 / 修改 */
const handleSave = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateAddress({ ...form })
      ElMessage.success('修改成功')
    } else {
      await saveAddress({ ...form })
      ElMessage.success('新增成功')
    }
    resetForm()
    loadAddresses()
  } finally {
    submitting.value = false
  }
}

/** 编辑：回填表单 */
const handleEdit = row => {
  Object.assign(form, {
    id: row.id,
    receiverName: row.receiverName,
    receiverPhone: row.receiverPhone,
    address: row.address,
    isDefault: row.isDefault === 1
  })
}

/** 设为默认 */
const handleSetDefault = async row => {
  await setDefaultAddress(row.id)
  ElMessage.success('已设为默认地址')
  loadAddresses()
}

/** 删除 */
const handleDelete = async row => {
  await ElMessageBox.confirm(`确定删除「${row.receiverName}」的收货地址吗？`, '提示', { type: 'warning' })
  await deleteAddress(row.id)
  ElMessage.success('删除成功')
  loadAddresses()
}

const resetForm = () => {
  Object.assign(form, defaultForm())
}

onMounted(loadAddresses)
</script>

<style scoped>
.form-card {
  padding: 24px 24px 4px;
  margin-bottom: 20px;
}

.address-form {
  max-width: 760px;
}

.list-card {
  padding: 8px;
}
</style>
