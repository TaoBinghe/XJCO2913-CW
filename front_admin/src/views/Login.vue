<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon :size="48" color="#07c160"><Van /></el-icon>
        <h1>E-Scooter Admin</h1>
        <p>Management Console</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="Username" prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="User"
            placeholder="Enter admin username"
          />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            prefix-icon="Lock"
            placeholder="Enter password"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            Login
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { adminLogin } from '@/api/admin'
import { setToken, setUsername } from '@/utils/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await adminLogin(form.username, form.password)
      setToken(res.data)
      setUsername(form.username)
      ElMessage.success('Login successful')
      router.push('/dashboard')
    } catch {
      // error handled by request interceptor
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1d1e1f 0%, #2c3e50 100%);
}

.login-card {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-header h1 {
  margin: 12px 0 4px;
  font-size: 24px;
  color: #1d1e1f;
}

.login-header p {
  font-size: 14px;
  color: #999;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  background-color: #07c160;
  border-color: #07c160;
}

.login-btn:hover {
  background-color: #06ad56;
  border-color: #06ad56;
}
</style>
