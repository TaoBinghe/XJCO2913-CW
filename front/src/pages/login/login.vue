<template>
  <view class="login-page">
    <view class="login-header">
      <view class="logo-circle">
        <text class="logo-icon">⚡</text>
      </view>
      <text class="page-title">E-Scooter Rental</text>
      <text class="page-subtitle">Login to start your ride</text>
    </view>

    <view class="login-form">
      <view class="input-group">
        <text class="input-label">Username</text>
        <input
          class="input-field"
          v-model="username"
          placeholder="Enter username (5-16 characters)"
          maxlength="16"
        />
      </view>

      <view class="input-group">
        <text class="input-label">Password</text>
        <input
          class="input-field"
          v-model="password"
          type="password"
          placeholder="Enter password (5-16 characters)"
          maxlength="16"
        />
      </view>

      <button class="btn-primary" :loading="loading" @click="handleLogin">
        Login
      </button>

      <view class="login-footer">
        <text class="link-text" @click="goRegister">
          Don't have an account? Register
        </text>
        <text class="link-text admin-link" @click="goAdminLogin">
          Admin Login
        </text>
      </view>
    </view>
  </view>
</template>

<script>
import { login } from '@/api/user'
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
      if (this.username.length < 5 || this.username.length > 16) {
        uni.showToast({ title: 'Username must be 5-16 characters', icon: 'none' })
        return
      }
      if (this.password.length < 5 || this.password.length > 16) {
        uni.showToast({ title: 'Password must be 5-16 characters', icon: 'none' })
        return
      }

      this.loading = true
      try {
        const res = await login(this.username, this.password)
        setToken(res.data)
        setUsername(this.username)
        setUserRole('CUSTOMER')
        uni.showToast({ title: 'Login successful', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/index/index' })
        }, 500)
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.loading = false
      }
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/register/register' })
    },
    goAdminLogin() {
      uni.navigateTo({ url: '/pages/admin-login/admin-login' })
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #07c160 0%, #f5f7f5 40%);
  padding: 0 40rpx;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 120rpx;
  padding-bottom: 60rpx;
}

.logo-circle {
  width: 140rpx;
  height: 140rpx;
  background-color: #ffffff;
  border-radius: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 30rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.logo-icon {
  font-size: 64rpx;
}

.login-header .page-title {
  color: #ffffff;
  font-size: 44rpx;
}

.login-header .page-subtitle {
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 0;
}

.login-form {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.06);
}

.login-footer {
  margin-top: 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20rpx;
}

.admin-link {
  color: #999999 !important;
  font-size: 26rpx !important;
}
</style>
