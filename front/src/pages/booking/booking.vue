<template>
  <view class="booking-page container">
    <text class="page-title">Book a Scooter</text>
    <text class="page-subtitle">Enter scooter ID and select a plan</text>

    <!-- Scooter ID Input -->
    <view class="card">
      <text class="section-title">Scooter ID</text>
      <input
        class="input-field"
        v-model="scooterId"
        type="number"
        placeholder="Enter scooter ID (e.g. 1)"
      />
    </view>

    <!-- Pricing Plans -->
    <view class="card">
      <text class="section-title">Select Plan</text>

      <view v-if="loading" class="loading-state">
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
          <view class="plan-option-left">
            <view class="radio-circle" :class="{ checked: selectedPeriod === plan.hirePeriod }">
              <view v-if="selectedPeriod === plan.hirePeriod" class="radio-dot"></view>
            </view>
            <text class="plan-option-name">{{ formatPeriod(plan.hirePeriod) }}</text>
          </view>
          <text class="plan-option-price">£{{ plan.price.toFixed(2) }}</text>
        </view>
      </view>
    </view>

    <!-- Summary -->
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
        <text class="summary-value price-highlight">£{{ selectedPlan.price.toFixed(2) }}</text>
      </view>
    </view>

    <!-- Confirm Button -->
    <button class="btn-primary confirm-btn" :loading="submitting" @click="handleBook">
      Confirm Booking
    </button>
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
        'HOUR_1': '1 Hour',
        'HOUR_4': '4 Hours',
        'DAY_1': '1 Day',
        'WEEK_1': '1 Week'
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
        uni.showToast({ title: 'Booking created!', icon: 'success' })
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
.booking-page {
  min-height: 100vh;
  padding-top: 30rpx;
}

.loading-state {
  padding: 40rpx;
  text-align: center;
  color: #999999;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.plan-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 24rpx;
  background-color: #f5f7f5;
  border-radius: 12rpx;
  border: 2rpx solid transparent;
  transition: all 0.2s;
}

.plan-option.active {
  border-color: #07c160;
  background-color: #e8f5e9;
}

.plan-option-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.radio-circle {
  width: 40rpx;
  height: 40rpx;
  border-radius: 20rpx;
  border: 3rpx solid #cccccc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.radio-circle.checked {
  border-color: #07c160;
}

.radio-dot {
  width: 22rpx;
  height: 22rpx;
  border-radius: 11rpx;
  background-color: #07c160;
}

.plan-option-name {
  font-size: 30rpx;
  font-weight: 500;
  color: #333333;
}

.plan-option-price {
  font-size: 32rpx;
  font-weight: 600;
  color: #07c160;
}

.summary-card {
  margin-top: 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 28rpx;
  color: #666666;
}

.summary-value {
  font-size: 28rpx;
  font-weight: 500;
  color: #333333;
}

.price-highlight {
  font-size: 34rpx;
  color: #07c160;
  font-weight: 700;
}

.confirm-btn {
  margin-top: 40rpx;
  margin-bottom: 60rpx;
}
</style>
