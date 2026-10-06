<template>
  <div class="login-page">
    <div class="lm-card login-card">
      <div class="card-left">
        <h2>欢迎来到 Lion 商城</h2>
        <p>一个用于学习 Spring Cloud 微服务全家桶的演示项目</p>
        <ul>
          <li>Nacos 注册中心与配置中心</li>
          <li>Gateway 网关统一鉴权与路由</li>
          <li>OpenFeign 服务间调用</li>
          <li>Sa-Token + Redis 共享登录态</li>
        </ul>
      </div>

      <div class="card-right">
        <el-tabs v-model="activeTab" stretch>
          <!-- 登录 -->
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  :prefix-icon="Lock"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-button class="lm-gradient-btn submit-btn" size="large" :loading="loading" @click="handleLogin">
                登 录
              </el-button>
            </el-form>
          </el-tab-pane>

          <!-- 注册 -->
          <el-tab-pane label="注册" name="register">
            <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" size="large">
              <el-form-item prop="username">
                <el-input v-model="registerForm.username" placeholder="用户名（3~20 位）" :prefix-icon="User" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="registerForm.password"
                  type="password"
                  show-password
                  placeholder="密码（6~20 位）"
                  :prefix-icon="Lock"
                />
              </el-form-item>
              <el-form-item prop="phone">
                <el-input v-model="registerForm.phone" placeholder="手机号（选填）" :prefix-icon="Phone" />
              </el-form-item>
              <el-button class="lm-gradient-btn submit-btn" size="large" :loading="loading" @click="handleRegister">
                注 册
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, Phone, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = ref({ username: '', password: '' })
const registerForm = ref({ username: '', password: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3~20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

/** 登录：成功后跳转原访问页或首页 */
const handleLogin = async () => {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    await userStore.login(loginForm.value)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

/** 注册：成功后自动登录 */
const handleRegister = async () => {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await userStore.register(registerForm.value)
    ElMessage.success('注册成功，已自动登录')
    await userStore.login({
      username: registerForm.value.username,
      password: registerForm.value.password
    })
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 64px - 70px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.login-card {
  width: 860px;
  max-width: 100%;
  display: flex;
  overflow: hidden;
}

.card-left {
  flex: 1;
  padding: 48px 36px;
  background: linear-gradient(135deg, #4f46e5, #8b5cf6);
  color: #fff;
}

.card-left h2 {
  margin: 0 0 12px;
  font-size: 24px;
}

.card-left p {
  opacity: 0.9;
  font-size: 14px;
}

.card-left ul {
  margin-top: 24px;
  padding-left: 18px;
  line-height: 2;
  font-size: 14px;
  opacity: 0.95;
}

.card-right {
  width: 380px;
  padding: 40px 32px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}

@media (max-width: 768px) {
  .card-left {
    display: none;
  }

  .card-right {
    width: 100%;
  }
}
</style>
