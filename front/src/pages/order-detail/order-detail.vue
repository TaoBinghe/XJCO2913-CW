<template>
  <view class="detail-page container">
    <view v-if="!order" class="empty-state">
      <text>Order not found</text>
    </view>

    <view v-else>
      <!-- Status Header -->
      <view class="status-header" :class="'bg-' + order.status.toLowerCase()">
        <text class="status-icon">{{ statusIcon }}</text>
        <text class="status-text">{{ order.status }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
      </view>

      <!-- Order Info -->
      <view class="card">
        <text class="section-title">Order Information</text>
        <view class="info-row">
          <text class="info-label">Order ID</text>
          <text class="info-value">#{{ order.id }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Scooter ID</text>
          <text class="info-value">#{{ order.scooterId }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Pricing Plan</text>
          <text class="info-value">Plan #{{ order.pricingPlanId }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Start Time</text>
          <text class="info-value">{{ formatTime(order.startTime) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">End Time</text>
          <text class="info-value">{{ formatTime(order.endTime) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Total Cost</text>
          <text class="info-value cost">£{{ order.totalCost.toFixed(2) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Created</text>
          <text class="info-value">{{ formatTime(order.createdAt) }}</text>
        </view>
      </view>

      <!-- Payment Result -->
      <view v-if="paymentResult" class="card">
        <text class="section-title">Payment Details</text>
        <view class="info-row">
          <text class="info-label">Status</text>
          <text class="info-value" :class="paymentResult.status === 'SUCCESS' ? 'success-text' : 'danger-text'">
            {{ paymentResult.status }}
          </text>
        </view>
        <view class="info-row">
          <text class="info-label">Amount</text>
          <text class="info-value">£{{ paymentResult.amount.toFixed(2) }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Transaction ID</text>
          <text class="info-value">{{ paymentResult.transactionId }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Card</text>
          <text class="info-value">****{{ paymentResult.cardLastFour }}</text>
        </view>
        <view class="info-row">
          <text class="info-label">Payment Time</text>
          <text class="info-value">{{ formatTime(paymentResult.paymentTime) }}</text>
        </view>
      </view>

      <!-- Action Buttons -->
      <view v-if="order.status === 'PENDING'" class="action-buttons">
        <button class="btn-primary" :loading="activating" @click="handleActivate">
          Activate Booking
        </button>
        <view style="height: 24rpx;"></view>
        <button class="btn-outline" :loading="paying" @click="handlePay">
          Pay Now
        </button>
      </view>

      <view v-else-if="order.status === 'ACTIVE' && !paymentResult" class="action-buttons">
        <button class="btn-primary" :loading="paying" @click="handlePay">
          Pay Now
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import { activateBooking } from '@/api/booking'
import { pay } from '@/api/payment'

export default {
  data() {
    return {
      order: null,
      paymentResult: null,
      activating: false,
      paying: false
    }
  },
  computed: {
    statusIcon() {
      if (!this.order) return ''
      const map = {
        'PENDING': '⏳',
        'ACTIVE': '🛴',
        'COMPLETED': '✅',
        'CANCELLED': '❌'
      }
      return map[this.order.status] || '📋'
    },
    statusDesc() {
      if (!this.order) return ''
      const map = {
        'PENDING': 'Waiting for activation',
        'ACTIVE': 'Ride in progress',
        'COMPLETED': 'Ride completed',
        'CANCELLED': 'Order cancelled'
      }
      return map[this.order.status] || ''
    }
  },
  onLoad(options) {
    if (options.order) {
      try {
        this.order = JSON.parse(decodeURIComponent(options.order))
      } catch (e) {
        this.order = null
      }
    }
  },
  methods: {
    formatTime(timeStr) {
      if (!timeStr) return '-'
      return timeStr.replace('T', ' ').substring(0, 16)
    },
    async handleActivate() {
      this.activating = true
      try {
        await activateBooking(this.order.id)
        uni.showToast({ title: 'Booking activated!', icon: 'success' })
        this.order.status = 'ACTIVE'
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.activating = false
      }
    },
    async handlePay() {
      this.paying = true
      try {
        const res = await pay(this.order.id)
        this.paymentResult = res.data
        uni.showToast({ title: 'Payment successful!', icon: 'success' })
        this.order.status = 'COMPLETED'
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.paying = false
      }
    }
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding-top: 0;
}

.status-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60rpx 40rpx;
  margin: 0 -30rpx 30rpx -30rpx;
}

.bg-pending {
  background: linear-gradient(135deg, #ff9800, #ffc107);
}

.bg-active {
  background: linear-gradient(135deg, #07c160, #10b981);
}

.bg-completed {
  background: linear-gradient(135deg, #2196f3, #42a5f5);
}

.bg-cancelled {
  background: linear-gradient(135deg, #f44336, #e57373);
}

.status-icon {
  font-size: 72rpx;
  margin-bottom: 16rpx;
}

.status-text {
  font-size: 40rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 8rpx;
}

.status-desc {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.85);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f5f5f5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 28rpx;
  color: #999999;
}

.info-value {
  font-size: 28rpx;
  color: #333333;
  font-weight: 500;
}

.info-value.cost {
  font-size: 34rpx;
  color: #07c160;
  font-weight: 700;
}

.success-text {
  color: #4caf50 !important;
}

.danger-text {
  color: #f44336 !important;
}

.action-buttons {
  margin-top: 40rpx;
  margin-bottom: 60rpx;
}
</style>
