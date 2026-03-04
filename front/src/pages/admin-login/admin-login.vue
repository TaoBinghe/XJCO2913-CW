<template>
  <view class="admin-login-page">
    <view class="admin-header">
      <view class="logo-circle">
        <text class="logo-icon">🔧</text>
      </view>
      <text class="page-title">Admin Portal</text>
      <text class="page-subtitle">Login with manager credentials</text>
    </view>

    <view class="admin-form">
      <view class="input-group">
        <text class="input-label">Username</text>
        <input
          class="input-field"
          v-model="username"
          placeholder="Enter admin username"
        />
      </view>

      <view class="input-group">
        <text class="input-label">Password</text>
        <input
          class="input-field"
          v-model="password"
          type="password"
          placeholder="Enter admin password"
        />
      </view>

      <button class="btn-primary" :loading="loading" @click="handleLogin">
        Admin Login
      </button>

      <view class="admin-footer">
        <text class="link-text" @click="goBack">← Back to User Login</text>
      </view>
    </view>
  </view>
</template>

<script>
import { adminLogin } from '@/api/admin'
import { setToken, setUsername, setUserRole } from '@/utils/auth'

export default {
  data() {
    return {
      username: '',
      password: '',
      loading: false
    }
  },
  methods: {
    async handleLogin() {
      if (!this.username || !this.password) {
        uni.showToast({ title: 'Please fill in all fields', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const res = await adminLogin(this.username, this.password)
        setToken(res.data)
        setUsername(this.username)
        setUserRole('MANAGER')
        uni.showToast({ title: 'Login successful', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({ url: '/pages/admin/admin' })
        }, 500)
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.loading = false
      }
    },
    goBack() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 40%, #f5f7f5 40%);
  padding: 0 40rpx;
}

.admin-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  padding-bottom: 60rpx;
}

.logo-circle {
  width: 140rpx;
  height: 140rpx;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.2);
}

.logo-icon {
  font-size: 64rpx;
}

.admin-header .page-title {
  color: #ffffff;
  font-size: 44rpx;
}

.admin-header .page-subtitle {
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 0;
}

.admin-form {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.08);
}

.admin-footer {
  margin-top: 36rpx;
  display: flex;
  justify-content: center;
}
</style>
