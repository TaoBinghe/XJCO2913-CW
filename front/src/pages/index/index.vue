<template>
  <view class="theme-page home-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">CITY RIDES</text>
        <text class="theme-headline">Ride Light Through the City</text>
      </view>

      <view class="ride-flow-grid">
        <view class="card home-cta-card" @click="goStoreBooking">
          <view class="home-cta-copy">
            <text class="home-cta-title">Reserve at a Store</text>
            <text class="home-cta-desc">Pick a time window, compare inventory, and collect your scooter in person.</text>
          </view>
        </view>

        <view class="card home-cta-card" @click="goScanRide">
          <view class="home-cta-copy">
            <text class="home-cta-title">Scan to Ride</text>
            <text class="home-cta-desc">Browse the live map, preview the walking route, then scan or type a scooter code.</text>
          </view>
        </view>
      </view>

      <view class="theme-section-head">
        <view>
          <text class="section-title">Reservation Pricing</text>
        </view>
      </view>

      <view v-if="loading" class="card loading-card">
        <text>Loading reservation plans...</text>
      </view>

      <view v-else-if="plans.length" class="plan-grid">
        <view
          v-for="plan in plans"
          :key="plan.id"
          class="card plan-card"
          @click="goStoreBookingWithPlan(plan)"
        >
          <view class="plan-badge">{{ formatPeriodBadge(plan.hirePeriod) }}</view>
          <text class="plan-price">{{ formatCurrency(plan.price) }}</text>
          <text class="plan-label">{{ formatPeriod(plan.hirePeriod) }}</text>
        </view>
      </view>

      <view v-else class="card pricing-empty-card">
        <text class="pricing-empty-title">Sign in to view live reservation pricing</text>
        <text class="pricing-empty-copy">Store reservations use account-backed pricing plans, so we load the latest plans after login.</text>
      </view>

      <view v-if="!isLoggedIn" class="card login-card" @click="goLogin">
        <text class="login-card-title">Log in to reserve and ride</text>
        <text class="login-card-desc">Guest users can browse stores and scan scooters, while signed-in users can reserve, unlock, and settle orders.</text>
      </view>

      <view class="home-illustration-block">
        <image class="home-illustration" :src="illustrationSrc" mode="widthFix" />
      </view>
    </view>
  </view>
</template>

<script>
import { getReservationPricingPlans } from '@/api/booking'
import { getScooterList } from '@/api/scooter'
import { getStoreList } from '@/api/store'
import loginBackground from '@/static/login_background.png'
import { formatCurrency, formatPeriod, formatPeriodBadge, sortPricingPlans } from '@/utils/booking'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      plans: [],
      stores: [],
      scooterCount: 0,
      loading: true,
      isLoggedIn: false,
      illustrationSrc: loginBackground
    }
  },
  computed: {
    storeCountLabel() {
      return `${this.stores.length} stores`
    },
    scooterCountLabel() {
      return `${this.scooterCount} live scooters`
    },
    publicStoreSummary() {
      if (!this.stores.length) {
        return '0'
      }
      const bookableCount = this.stores.filter(store => Number(store.bookableInventory || 0) > 0).length
      return `${bookableCount}/${this.stores.length} with inventory`
    }
  },
  onShow() {
    this.isLoggedIn = !!getToken()
    this.loadHomeData()
  },
  methods: {
    async loadHomeData() {
      this.loading = true
      try {
        const [storesRes, scootersRes, plansRes] = await Promise.all([
          getStoreList(),
          getScooterList(),
          this.isLoggedIn ? getReservationPricingPlans() : Promise.resolve({ data: [] })
        ])
        this.stores = storesRes.data || []
        this.scooterCount = (scootersRes.data || []).length
        this.plans = sortPricingPlans(plansRes.data || [])
      } catch (e) {
        this.stores = []
        this.scooterCount = 0
        this.plans = []
      } finally {
        this.loading = false
      }
    },
    formatPeriod(period) {
      return formatPeriod(period)
    },
    formatPeriodBadge(period) {
      return formatPeriodBadge(period)
    },
    formatCurrency(value) {
      return formatCurrency(value)
    },
    goStoreBooking() {
      uni.navigateTo({ url: '/pages/store-booking/store-booking' })
    },
    goStoreBookingWithPlan(plan) {
      uni.navigateTo({
        url: `/pages/store-booking/store-booking?period=${plan.hirePeriod}`
      })
    },
    goScanRide() {
      uni.navigateTo({ url: '/pages/booking/booking' })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
.ride-flow-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20rpx;
  margin-top: 38rpx;
}

.home-cta-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
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

.home-cta-stat {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 156rpx;
  height: 64rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #5d8c22;
  font-size: 24rpx;
  font-weight: 700;
}

.summary-card {
  margin-top: 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  padding: 12rpx 0;
}

.summary-row + .summary-row {
  border-top: 1rpx solid #edf0e8;
}

.summary-label {
  font-size: 24rpx;
  color: #8c9587;
}

.summary-value {
  font-size: 24rpx;
  font-weight: 700;
  color: #111111;
}

.loading-card {
  text-align: center;
  color: #7d8677;
}

.pricing-empty-card {
  text-align: left;
}

.pricing-empty-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.pricing-empty-copy {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
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

.home-illustration {
  display: block;
  width: 100%;
  opacity: 0.58;
}
</style>
