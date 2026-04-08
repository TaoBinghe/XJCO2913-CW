<template>
  <view class="theme-page home-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">CITY RIDES</text>
        <text class="theme-headline">Ride Light Through the City</text>
      </view>

      <view class="card home-cta-card" @click="goBooking">
        <view class="home-cta-copy">
          <text class="home-cta-title">Start a Ride</text>
          <text class="home-cta-desc">Book a scooter in a few taps and get moving right away.</text>
        </view>
        <text class="home-cta-pill">Explore</text>
      </view>

      <view class="theme-section-head">
        <view>
          <text class="section-title">Pricing Plans</text>
        </view>
      </view>

      <view v-if="loading" class="card loading-card">
        <text>Loading plans...</text>
      </view>

      <view v-else class="plan-grid">
        <view
          class="card plan-card"
          v-for="plan in plans"
          :key="plan.id"
          @click="goBookingWithPlan(plan)"
        >
          <view class="plan-badge">{{ formatPeriod(plan.hirePeriod) }}</view>
          <text class="plan-price">£{{ plan.price.toFixed(2) }}</text>
          <text class="plan-label">{{ formatPeriodLabel(plan.hirePeriod) }}</text>
        </view>
      </view>

      <view v-if="!isLoggedIn" class="card login-card" @click="goLogin">
        <text class="login-card-title">Log in to make a booking</text>
        <text class="login-card-desc">Save your ride details, activation, and payment in one place.</text>
      </view>

      <view class="home-illustration-block">
        <image class="home-illustration" :src="illustrationSrc" mode="widthFix" />
      </view>
    </view>
  </view>
</template>

<script>
import { getPricingPlans } from '@/api/booking'
import loginBackground from '@/static/login_background.png'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      plans: [],
      loading: true,
      isLoggedIn: false,
      illustrationSrc: loginBackground
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
        HOUR_1: '1H',
        HOUR_4: '4H',
        DAY_1: '1D',
        WEEK_1: '7D'
      }
      return map[period] || period
    },
    formatPeriodLabel(period) {
      const map = {
        HOUR_1: '1 Hour',
        HOUR_4: '4 Hours',
        DAY_1: '1 Day',
        WEEK_1: '1 Week'
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
.home-cta-card {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
  margin-top: 38rpx;
  border-width: 3rpx;
  border-color: #d5dfc8;
}

.home-cta-copy {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.home-cta-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #111111;
}

.home-cta-desc {
  margin-top: 10rpx;
  font-size: 26rpx;
  line-height: 1.6;
  color: #6f776a;
}

.home-cta-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 128rpx;
  height: 64rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #5d8c22;
  font-size: 24rpx;
  font-weight: 700;
}

.loading-card {
  text-align: center;
  color: #7d8677;
}

.plan-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
}

.plan-card {
  flex: 1 1 280rpx;
  min-width: 0;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14rpx;
}

.plan-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 92rpx;
  height: 76rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #5d8c22;
  font-size: 28rpx;
  font-weight: 700;
}

.plan-price {
  font-size: 42rpx;
  line-height: 1.1;
  font-weight: 700;
  color: #111111;
}

.plan-label {
  font-size: 24rpx;
  color: #7f8879;
}

.login-card {
  margin-top: 24rpx;
}

.login-card-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.login-card-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7d8677;
}

.home-illustration-block {
  margin-top: 36rpx;
  padding-top: 12rpx;
}

.home-illustration-note {
  display: block;
  margin-bottom: 18rpx;
  font-size: 24rpx;
  text-align: center;
  color: #98a093;
}

.home-illustration {
  display: block;
  width: 100%;
  opacity: 0.58;
}
</style>
