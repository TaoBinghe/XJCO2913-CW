<template>
  <view class="home-page">
    <!-- Banner -->
    <view class="banner">
      <view class="banner-content">
        <text class="banner-title">Green Travel</text>
        <text class="banner-desc">Rent an e-scooter, explore the campus</text>
      </view>
      <text class="banner-icon">🛴</text>
    </view>

    <!-- Quick Booking Entry -->
    <view class="container">
      <view class="quick-entry card" @click="goBooking">
        <view class="quick-entry-left">
          <text class="quick-entry-title">Start a Ride</text>
          <text class="quick-entry-desc">Choose a scooter and pricing plan</text>
        </view>
        <text class="quick-entry-arrow">→</text>
      </view>

      <!-- Pricing Plans -->
      <text class="section-title">Pricing Plans</text>

      <view v-if="loading" class="loading-state">
        <text>Loading plans...</text>
      </view>

      <view v-else class="plan-grid">
        <view
          class="plan-card card"
          v-for="plan in plans"
          :key="plan.id"
          @click="goBookingWithPlan(plan)"
        >
          <text class="plan-period">{{ formatPeriod(plan.hirePeriod) }}</text>
          <view class="plan-price-row">
            <text class="plan-currency">£</text>
            <text class="plan-price">{{ plan.price.toFixed(2) }}</text>
          </view>
          <text class="plan-label">{{ formatPeriodLabel(plan.hirePeriod) }}</text>
        </view>
      </view>

      <!-- Login Hint (when not logged in) -->
      <view v-if="!isLoggedIn" class="login-hint card" @click="goLogin">
        <text class="login-hint-text">Login to book a scooter →</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getPricingPlans } from '@/api/booking'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      plans: [],
      loading: true,
      isLoggedIn: false
    }
  },
  onShow() {
    this.isLoggedIn = !!getToken()
    this.loadPlans()
  },
  methods: {
    async loadPlans() {
      this.loading = true
      try {
        const res = await getPricingPlans()
        this.plans = res.data || []
      } catch (e) {
        this.plans = []
      } finally {
        this.loading = false
      }
    },
    formatPeriod(period) {
      const map = {
        'HOUR_1': '1h',
        'HOUR_4': '4h',
        'DAY_1': '1d',
        'WEEK_1': '7d'
      }
      return map[period] || period
    },
    formatPeriodLabel(period) {
      const map = {
        'HOUR_1': '1 Hour',
        'HOUR_4': '4 Hours',
        'DAY_1': '1 Day',
        'WEEK_1': '1 Week'
      }
      return map[period] || period
    },
    goBooking() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.navigateTo({ url: '/pages/booking/booking' })
    },
    goBookingWithPlan(plan) {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.navigateTo({
        url: `/pages/booking/booking?planId=${plan.id}&period=${plan.hirePeriod}&price=${plan.price}`
      })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f7f5;
}

.banner {
  background: linear-gradient(135deg, #07c160, #10b981);
  padding: 48rpx 40rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.banner-content {
  display: flex;
  flex-direction: column;
}

.banner-title {
  font-size: 44rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 8rpx;
}

.banner-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.banner-icon {
  font-size: 80rpx;
}

.quick-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 36rpx 30rpx;
  margin-top: -20rpx;
}

.quick-entry-left {
  display: flex;
  flex-direction: column;
}

.quick-entry-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #07c160;
  margin-bottom: 6rpx;
}

.quick-entry-desc {
  font-size: 24rpx;
  color: #999999;
}

.quick-entry-arrow {
  font-size: 40rpx;
  color: #07c160;
}

.loading-state {
  padding: 60rpx;
  text-align: center;
  color: #999999;
}

.plan-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.plan-card {
  width: calc(50% - 10rpx);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 36rpx 20rpx;
}

.plan-period {
  font-size: 36rpx;
  font-weight: 700;
  color: #07c160;
  background-color: #e8f5e9;
  width: 80rpx;
  height: 80rpx;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16rpx;
}

.plan-price-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8rpx;
}

.plan-currency {
  font-size: 26rpx;
  color: #333333;
  margin-top: 6rpx;
  margin-right: 4rpx;
  font-weight: 600;
}

.plan-price {
  font-size: 44rpx;
  font-weight: 700;
  color: #333333;
}

.plan-label {
  font-size: 24rpx;
  color: #999999;
}

.login-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30rpx;
}

.login-hint-text {
  color: #07c160;
  font-size: 30rpx;
  font-weight: 500;
}
</style>
