<template>
  <view class="theme-page my-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">PROFILE SPACE</text>
        <text class="theme-headline">My Page</text>
      </view>

      <view class="card profile-card">
        <view class="profile-head">
          <view class="avatar-circle">
            <text class="avatar-text">{{ avatarLetter }}</text>
          </view>

          <view class="profile-copy-block">
            <text class="profile-name">{{ profileName }}</text>
            <text class="profile-note">{{ profileNote }}</text>
          </view>
        </view>

        <button v-if="!isLoggedIn" class="btn-primary profile-action" @click="goLogin">
          Login
        </button>
        <view v-else class="profile-status">Customer account active</view>
      </view>

      <view class="menu-list">
        <view class="card menu-card" @click="goOrders">
          <view class="menu-copy">
            <text class="menu-title">My Orders</text>
            <text class="menu-desc">Review store reservations, scan rides, and payment receipts.</text>
          </view>
          <text class="menu-pill">Open</text>
        </view>

        <view class="card menu-card" @click="goStoreBooking">
          <view class="menu-copy">
            <text class="menu-title">Reserve at a Store</text>
            <text class="menu-desc">Book a pickup window and compare store inventory before you arrive.</text>
          </view>
          <text class="menu-pill">Reserve</text>
        </view>

        <view class="card menu-card" @click="goScanRide">
          <view class="menu-copy">
            <text class="menu-title">Scan to Ride</text>
            <text class="menu-desc">Open the live map, preview the route, and start a scan ride on demand.</text>
          </view>
          <text class="menu-pill">Ride</text>
        </view>
      </view>

      <view v-if="isLoggedIn" class="logout-section">
        <button class="btn-danger" @click="handleLogout">Logout</button>
      </view>

      <view class="card app-info-card">
        <text class="app-version">Green Go Mini App v1.0.0</text>
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
      return this.username ? this.username.charAt(0).toUpperCase() : 'G'
    },
    profileName() {
      return this.username || 'Guest'
    },
    profileNote() {
      return this.isLoggedIn
        ? 'You are ready for your next reservation, scan ride, and payment check.'
        : 'Sign in to reserve scooters, start scan rides, and track every order.'
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
    goStoreBooking() {
      uni.navigateTo({ url: '/pages/store-booking/store-booking' })
    },
    goScanRide() {
      uni.navigateTo({ url: '/pages/booking/booking' })
    },
    handleLogout() {
      uni.showModal({
        title: 'Logout',
        content: 'Are you sure you want to logout?',
        confirmText: 'Confirm',
        cancelText: 'Cancel',
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
.profile-card {
  margin-top: 38rpx;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar-circle {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #efff84 0%, #e2ff6b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20rpx 44rpx rgba(226, 255, 107, 0.24);
  flex-shrink: 0;
}

.avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #111111;
}

.profile-copy-block {
  flex: 1;
  min-width: 0;
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111111;
  word-break: break-all;
}

.profile-note {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #7d8677;
}

.profile-action {
  margin-top: 28rpx;
}

.profile-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 28rpx;
  min-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #5d8c22;
  font-size: 24rpx;
  font-weight: 700;
}

.menu-list {
  margin-top: 24rpx;
}

.menu-card {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
}

.menu-copy {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.menu-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.menu-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7d8677;
}

.menu-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112rpx;
  height: 60rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #5d8c22;
  font-size: 24rpx;
  font-weight: 700;
}

.logout-section {
  margin-top: 28rpx;
}

.app-info-card {
  margin-top: 28rpx;
  text-align: center;
}

.app-version {
  display: block;
  font-size: 24rpx;
  color: #7d8677;
}

.app-copy {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #a0a89a;
}
</style>
