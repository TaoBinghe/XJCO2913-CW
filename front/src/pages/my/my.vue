<template>
  <view class="my-page">
    <!-- Header -->
    <view class="my-header">
      <view class="avatar-circle">
        <text class="avatar-text">{{ avatarLetter }}</text>
      </view>
      <text class="username">{{ username || 'Not logged in' }}</text>
      <text v-if="!isLoggedIn" class="login-tip" @click="goLogin">Tap to login</text>
    </view>

    <!-- Menu List -->
    <view class="container">
      <view class="menu-card card">
        <view class="menu-item" @click="goOrders">
          <text class="menu-icon">📋</text>
          <text class="menu-text">My Orders</text>
          <text class="menu-arrow">→</text>
        </view>

        <view class="menu-divider"></view>

        <view class="menu-item" @click="goAdminLogin">
          <text class="menu-icon">🔧</text>
          <text class="menu-text">Admin Portal</text>
          <text class="menu-arrow">→</text>
        </view>
      </view>

      <!-- Logout -->
      <view v-if="isLoggedIn" class="logout-section">
        <button class="btn-danger" @click="handleLogout">Logout</button>
      </view>

      <!-- App Info -->
      <view class="app-info">
        <text class="app-version">E-Scooter Rental v1.0.0</text>
        <text class="app-copyright">XJCO2913 Coursework</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getToken, getUsername, clearAll } from '@/utils/auth'

export default {
  data() {
    return {
      username: '',
      isLoggedIn: false
    }
  },
  computed: {
    avatarLetter() {
      return this.username ? this.username.charAt(0).toUpperCase() : '?'
    }
  },
  onShow() {
    this.isLoggedIn = !!getToken()
    this.username = getUsername()
  },
  methods: {
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    goOrders() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.switchTab({ url: '/pages/orders/orders' })
    },
    goAdminLogin() {
      uni.navigateTo({ url: '/pages/admin-login/admin-login' })
    },
    handleLogout() {
      uni.showModal({
        title: 'Logout',
        content: 'Are you sure you want to logout?',
        success: (res) => {
          if (res.confirm) {
            clearAll()
            this.isLoggedIn = false
            this.username = ''
            uni.showToast({ title: 'Logged out', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.my-page {
  min-height: 100vh;
  background-color: #f5f7f5;
}

.my-header {
  background: linear-gradient(135deg, #07c160, #10b981);
  padding: 80rpx 40rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-circle {
  width: 140rpx;
  height: 140rpx;
  background-color: rgba(255, 255, 255, 0.25);
  border-radius: 70rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
}

.avatar-text {
  font-size: 56rpx;
  font-weight: 700;
  color: #ffffff;
}

.username {
  font-size: 36rpx;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 8rpx;
}

.login-tip {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.75);
}

.menu-card {
  padding: 0;
  overflow: hidden;
  margin-top: -20rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 30rpx;
}

.menu-icon {
  font-size: 40rpx;
  margin-right: 24rpx;
}

.menu-text {
  flex: 1;
  font-size: 30rpx;
  color: #333333;
}

.menu-arrow {
  font-size: 28rpx;
  color: #cccccc;
}

.menu-divider {
  height: 1rpx;
  background-color: #f0f0f0;
  margin: 0 30rpx;
}

.logout-section {
  margin-top: 48rpx;
}

.app-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 60rpx;
  padding-bottom: 40rpx;
}

.app-version {
  font-size: 24rpx;
  color: #cccccc;
  margin-bottom: 8rpx;
}

.app-copyright {
  font-size: 22rpx;
  color: #dddddd;
}
</style>
