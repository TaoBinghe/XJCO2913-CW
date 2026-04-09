<template>
  <view class="theme-page booking-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">READY TO GO</text>
        <text class="theme-headline">Book a Scooter</text>
        <text class="theme-copy">Enter a scooter ID, choose a pricing plan, and confirm the booking. The scooter will stay pending until you activate it.</text>
      </view>

      <view class="card">
        <text class="section-title">Scooter ID</text>
        <text class="field-note">Type the scooter number you want to reserve first.</text>
        <input
          class="input-field"
          v-model="scooterId"
          type="number"
          placeholder="Enter scooter ID"
          placeholder-style="color: #b7bdb5"
        />
      </view>

      <view class="card">
        <text class="section-title">Select Plan</text>
        <text class="field-note">Choose the duration that matches your route.</text>

        <view v-if="loading" class="loading-card">
          <text>Loading plans...</text>
        </view>

        <view v-else class="plan-list">
          <view
            class="plan-option"
            :class="{ active: selectedPeriod === plan.hirePeriod }"
            v-for="plan in plans"
            :key="plan.id"
            @click="selectPlan(plan)"
          >
            <view class="plan-option-copy">
              <view class="plan-check" :class="{ active: selectedPeriod === plan.hirePeriod }"></view>
              <text class="plan-name">{{ formatPeriod(plan.hirePeriod) }}</text>
            </view>
            <text class="plan-price">£{{ plan.price.toFixed(2) }}</text>
          </view>
        </view>
      </view>

      <view v-if="selectedPlan" class="card summary-card">
        <text class="section-title">Booking Summary</text>
        <view class="summary-row">
          <text class="summary-label">Scooter</text>
          <text class="summary-value">#{{ scooterId || '-' }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">Plan</text>
          <text class="summary-value">{{ formatPeriod(selectedPlan.hirePeriod) }}</text>
        </view>
        <view class="summary-row">
          <text class="summary-label">Price</text>
          <text class="summary-value summary-price">£{{ selectedPlan.price.toFixed(2) }}</text>
        </view>
      </view>

      <button class="btn-primary booking-button" :loading="submitting" @click="handleBook">
        Confirm Booking
      </button>
    </view>
  </view>
</template>

<script>
import { getPricingPlans, createBooking } from '@/api/booking'

export default {
  data() {
    return {
      scooterId: '',
      plans: [],
      selectedPeriod: '',
      selectedPlan: null,
      loading: true,
      submitting: false
    }
  },
  onLoad(options) {
    if (options.period) {
      this.selectedPeriod = options.period
    }
    this.loadPlans()
  },
  methods: {
    async loadPlans() {
      this.loading = true
      try {
        const res = await getPricingPlans()
        this.plans = res.data || []
        if (this.selectedPeriod) {
          this.selectedPlan = this.plans.find(p => p.hirePeriod === this.selectedPeriod) || null
        }
      } catch (e) {
        this.plans = []
      } finally {
        this.loading = false
      }
    },
    selectPlan(plan) {
      this.selectedPeriod = plan.hirePeriod
      this.selectedPlan = plan
    },
    formatPeriod(period) {
      const map = {
        HOUR_1: '1 Hour',
        HOUR_4: '4 Hours',
        DAY_1: '1 Day',
        WEEK_1: '1 Week'
      }
      return map[period] || period
    },
    async handleBook() {
      if (!this.scooterId) {
        uni.showToast({ title: 'Please enter scooter ID', icon: 'none' })
        return
      }
      if (!this.selectedPeriod) {
        uni.showToast({ title: 'Please select a plan', icon: 'none' })
        return
      }

      this.submitting = true
      try {
        await createBooking(this.scooterId, this.selectedPeriod)
        uni.showToast({ title: 'Booking pending', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/orders/orders' })
        }, 1000)
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.field-note {
  display: block;
  margin-top: 10rpx;
  margin-bottom: 18rpx;
  font-size: 24rpx;
  color: #98a093;
}

.loading-card {
  padding: 28rpx 0 4rpx;
  text-align: center;
  color: #7d8677;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 20rpx;
}

.plan-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 24rpx 24rpx;
  border: 2rpx solid #e8ede3;
  border-radius: 28rpx;
  background: #fbfcf8;
}

.plan-option.active {
  border-color: #d8ef8c;
  background: #f7fbeb;
}

.plan-option-copy {
  display: flex;
  align-items: center;
  gap: 18rpx;
  flex: 1;
  min-width: 0;
}

.plan-check {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  border: 3rpx solid #c6cec0;
  background: #ffffff;
  flex-shrink: 0;
}

.plan-check.active {
  border-color: #9abf3e;
  background: #e2ff6b;
}

.plan-name {
  flex: 1;
  min-width: 0;
  font-size: 28rpx;
  font-weight: 600;
  color: #111111;
}

.plan-price {
  font-size: 28rpx;
  font-weight: 700;
  color: #5d8c22;
}

.summary-card {
  margin-top: 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #edf0e8;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 25rpx;
  color: #8c9587;
}

.summary-value {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  text-align: right;
  color: #111111;
  word-break: break-all;
}

.summary-price {
  font-weight: 700;
  color: #5d8c22;
}

.booking-button {
  margin-top: 18rpx;
}
</style>
