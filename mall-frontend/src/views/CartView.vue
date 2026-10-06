<template>
  <div class="page-container">
    <h2 class="lm-title">我的购物车</h2>

    <el-empty v-if="!cartStore.items.length" description="购物车是空的，去逛逛吧">
      <el-button class="lm-gradient-btn" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>

    <template v-else>
      <div class="lm-card cart-card">
        <el-table :data="cartStore.items" style="width: 100%">
          <el-table-column label="商品" min-width="240">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image :src="row.image" fit="cover" class="cell-img">
                  <template #error>
                    <div class="img-fallback"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <span class="cell-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120">
            <template #default="{ row }">
              <span class="lm-price">¥ {{ row.price }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="1"
                :max="row.stock"
                size="small"
                @change="value => cartStore.updateQuantity(row.productId, value)"
              />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="lm-price">¥ {{ (row.price * row.quantity).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ row }">
              <el-button link type="danger" @click="cartStore.remove(row.productId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 结算栏 -->
      <div class="lm-card settle-bar">
        <div class="total">
          共 <b>{{ cartStore.totalCount }}</b> 件，合计：
          <span class="lm-price total-price">¥ {{ cartStore.totalAmount }}</span>
        </div>
        <div>
          <el-button @click="cartStore.clear()">清空购物车</el-button>
          <el-button class="lm-gradient-btn" type="primary" @click="dialogVisible = true">
            去结算
          </el-button>
        </div>
      </div>
    </template>

    <!-- 下单弹窗 -->
    <el-dialog v-model="dialogVisible" title="确认订单" width="560px" @open="handleDialogOpen">
      <div v-loading="addressLoading">
        <!-- 已保存的收货地址：直接选择即可 -->
        <template v-if="addresses.length">
          <div class="addr-label">选择收货地址</div>
          <div class="addr-list">
            <div
              v-for="item in addresses"
              :key="item.id"
              class="addr-item"
              :class="{ active: item.id === selectedAddressId }"
              @click="selectedAddressId = item.id"
            >
              <el-radio v-model="selectedAddressId" :value="item.id" class="addr-radio" />
              <div class="addr-body">
                <div class="addr-head">
                  <span class="addr-name">{{ item.receiverName }}</span>
                  <span class="addr-phone">{{ item.receiverPhone }}</span>
                  <el-tag v-if="item.isDefault === 1" size="small" type="success" effect="plain">默认</el-tag>
                </div>
                <div class="addr-text">{{ item.address }}</div>
              </div>
            </div>
          </div>
          <el-button link type="primary" class="toggle-new" @click="showNewForm = !showNewForm">
            {{ showNewForm ? '收起' : '+ 使用新地址' }}
          </el-button>
        </template>

        <!-- 地址簿为空 -->
        <el-empty v-else-if="!showNewForm" :image-size="70" description="还没有收货地址，先添加一个吧">
          <el-button class="lm-gradient-btn" size="small" @click="showNewForm = true">
            新增收货地址
          </el-button>
        </el-empty>

        <!-- 新增地址表单 -->
        <el-form
          v-if="showNewForm"
          ref="addrFormRef"
          :model="addrForm"
          :rules="addrRules"
          label-width="80px"
          class="new-addr-form"
        >
          <el-form-item label="收货人" prop="receiverName">
            <el-input v-model="addrForm.receiverName" placeholder="请输入收货人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="receiverPhone">
            <el-input v-model="addrForm.receiverPhone" placeholder="请输入收货人手机号" />
          </el-form-item>
          <el-form-item label="收货地址" prop="address">
            <el-input v-model="addrForm.address" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="addrForm.isDefault" />
          </el-form-item>
          <el-form-item>
            <el-button class="lm-gradient-btn" type="primary" :loading="savingAddress" @click="handleSaveAddress">
              保存地址
            </el-button>
            <el-button v-if="addresses.length" @click="showNewForm = false">取消</el-button>
          </el-form-item>
        </el-form>

        <el-form label-width="80px" class="remark-form">
          <el-form-item label="备注">
            <el-input v-model="form.remark" placeholder="选填" />
          </el-form-item>
        </el-form>
      </div>

      <div class="dialog-total">
        应付金额：<span class="lm-price">¥ {{ cartStore.totalAmount }}</span>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button class="lm-gradient-btn" type="primary" :loading="submitting" @click="handleSubmit">
          提交订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { createOrder } from '@/api/order'
import { getAddressList, saveAddress } from '@/api/address'

const router = useRouter()
const cartStore = useCartStore()

const dialogVisible = ref(false)
const submitting = ref(false)

// ---------- 收货地址 ----------
const addresses = ref([])
const addressLoading = ref(false)
const selectedAddressId = ref(null)
const showNewForm = ref(false)
const savingAddress = ref(false)
const addrFormRef = ref()

const form = reactive({ remark: '' })

const defaultAddrForm = () => ({
  receiverName: '',
  receiverPhone: '',
  address: '',
  isDefault: true
})

const addrForm = reactive(defaultAddrForm())

const addrRules = {
  receiverName: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

/** 打开弹窗：加载地址簿并自动选中默认地址 */
const handleDialogOpen = () => {
  showNewForm.value = false
  loadAddresses()
}

/** 加载地址簿 */
const loadAddresses = async () => {
  addressLoading.value = true
  try {
    addresses.value = await getAddressList()
    // 已选地址仍存在则不改变选择
    if (addresses.value.some(item => item.id === selectedAddressId.value)) return
    const preferred = addresses.value.find(item => item.isDefault === 1) || addresses.value[0]
    selectedAddressId.value = preferred ? preferred.id : null
    if (!addresses.value.length) showNewForm.value = true
  } finally {
    addressLoading.value = false
  }
}

/** 保存新地址并自动选中 */
const handleSaveAddress = async () => {
  await addrFormRef.value.validate()
  savingAddress.value = true
  try {
    const id = await saveAddress({ ...addrForm })
    ElMessage.success('地址已保存')
    Object.assign(addrForm, defaultAddrForm())
    showNewForm.value = false
    selectedAddressId.value = id
    await loadAddresses()
  } finally {
    savingAddress.value = false
  }
}

/** 提交订单 */
const handleSubmit = async () => {
  const address = addresses.value.find(item => item.id === selectedAddressId.value)
  if (!address) {
    ElMessage.warning('请先选择收货地址')
    return
  }
  submitting.value = true
  try {
    await createOrder({
      receiverName: address.receiverName,
      receiverPhone: address.receiverPhone,
      address: address.address,
      remark: form.remark,
      items: cartStore.items.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    })
    ElMessage.success('下单成功')
    cartStore.clear()
    dialogVisible.value = false
    router.push('/orders')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.cart-card {
  padding: 8px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cell-img {
  width: 60px;
  height: 60px;
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

.cell-name {
  font-size: 14px;
}

.settle-bar {
  margin-top: 20px;
  padding: 18px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.total-price {
  font-size: 22px;
}

.dialog-total {
  text-align: right;
  font-size: 15px;
}

/* ---------- 收货地址选择 ---------- */
.addr-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 10px;
}

.addr-list {
  max-height: 240px;
  overflow-y: auto;
}

.addr-item {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.addr-item:hover {
  border-color: var(--lm-primary-light);
}

.addr-item.active {
  border-color: var(--lm-primary);
  background: rgba(79, 70, 229, 0.06);
}

.addr-radio {
  height: 22px;
  margin-right: 0;
}

.addr-body {
  flex: 1;
  min-width: 0;
}

.addr-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.addr-name {
  font-size: 14px;
  font-weight: 600;
}

.addr-phone {
  font-size: 13px;
  color: #6b7280;
}

.addr-text {
  font-size: 13px;
  line-height: 1.5;
  color: #4b5563;
}

.toggle-new {
  margin-bottom: 4px;
}

.new-addr-form {
  margin-top: 6px;
}

.remark-form {
  margin-top: 4px;
}
</style>
