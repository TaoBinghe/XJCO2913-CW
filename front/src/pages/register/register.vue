<template>
  <view class="register-page">
    <view class="register-header">
      <view class="logo-circle">
        <text class="logo-icon">⚡</text>
      </view>
      <text class="page-title">Create Account</text>
      <text class="page-subtitle">Join the green ride community</text>
    </view>

    <view class="register-form">
      <view class="input-group">
        <text class="input-label">Username</text>
        <input
          class="input-field"
          v-model="username"
          placeholder="5-16 characters"
          maxlength="16"
        />
      </view>

      <view class="input-group">
        <text class="input-label">Password</text>
        <input
          class="input-field"
          v-model="password"
          type="password"
          placeholder="5-16 characters"
          maxlength="16"
        />
      </view>

      <view class="input-group">
        <text class="input-label">Confirm Password</text>
        <input
          class="input-field"
          v-model="confirmPassword"
          type="password"
          placeholder="Enter password again"
          maxlength="16"
        />
      </view>

      <button class="btn-primary" :loading="loading" @click="handleRegister">
        Register
      </button>

      <view class="register-footer">
        <text class="link-text" @click="goLogin">
          Already have an account? Login
        </text>
      </view>
    </view>
  </view>
</template>

<script>
import { register } from '@/api/user'

export default {
  data() {
    return {
      username: '',
      password: '',
      confirmPassword: '',
      loading: false
    }
  },
  methods: {
    async handleRegister() {
      if (this.username.length < 5 || this.username.length > 16) {
        uni.showToast({ title: 'Username must be 5-16 characters', icon: 'none' })
        return
      }
      if (this.password.length < 5 || this.password.length > 16) {
        uni.showToast({ title: 'Password must be 5-16 characters', icon: 'none' })
        return
      }
      if (this.password !== this.confirmPassword) {
        uni.showToast({ title: 'Passwords do not match', icon: 'none' })
        return
      }

      this.loading = true
      try {
        await register(this.username, this.password)
        uni.showToast({ title: 'Registration successful', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1000)
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.loading = false
      }
    },
    goLogin() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #07c160 0%, #f5f7f5 40%);
  padding: 0 40rpx;
}

.register-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 100rpx;
  padding-bottom: 50rpx;
}

.logo-circle {
  width: 120rpx;
  height: 120rpx;
  background-color: #ffffff;
  border-radius: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24rpx;
  box-shadow: 0 8rpx 32rpx rgba(0, 0, 0, 0.1);
}

.logo-icon {
  font-size: 56rpx;
}

.register-header .page-title {
  color: #ffffff;
  font-size: 40rpx;
}

.register-header .page-subtitle {
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 0;
}

.register-form {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
  box-shadow: 0 4rpx 24rpx rgba(0, 0, 0, 0.06);
}

.register-footer {
  margin-top: 36rpx;
  display: flex;
  justify-content: center;
}
</style>
