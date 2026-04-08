<template>
  <view class="theme-page detail-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view v-if="!order" class="card empty-state">
        <text class="empty-title">Order not found</text>
        <text class="empty-copy">We could not load the booking details for this page.</text>
      </view>

      <view v-else>
        <view class="card status-card">
          <view class="status-marker" :class="'status-marker-' + statusTone">
            {{ statusShort }}
          </view>
          <text class="status-title">{{ displayStatus }}</text>
          <text class="status-copy">{{ statusDesc }}</text>
        </view>

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
            <text class="info-value info-value-strong">{{ formatCurrency(order.totalCost) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Created</text>
            <text class="info-value">{{ formatTime(order.createdAt) }}</text>
          </view>
        </view>

        <view v-if="paymentResult" class="card">
          <text class="section-title">Payment Details</text>
          <view class="info-row">
            <text class="info-label">Status</text>
            <text class="info-value" :class="paymentResult.status === 'SUCCESS' ? 'text-positive' : 'text-danger'">
              {{ paymentResult.status }}
            </text>
          </view>
          <view class="info-row">
            <text class="info-label">Amount</text>
            <text class="info-value">{{ formatCurrency(paymentResult.amount) }}</text>
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

        <view v-if="isSwitchable && !paymentResult" class="action-buttons">
          <button class="btn-outline action-button" :loading="switching" @click="handleToggleStatus">
            {{ toggleButtonText }}
          </button>
          <button class="btn-primary action-button" :loading="paying" @click="handlePay">
            Pay Now
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { updateBookingStatus } from '@/api/booking'
import { pay } from '@/api/payment'

export default {
  data() {
    return {
      order: null,
      paymentResult: null,
      switching: false,
      paying: false
    }
  },
  computed: {
    normalizedStatus() {
      if (!this.order) return ''
      return this.order.status === 'ACTIVE' ? 'ACTIVATED' : this.order.status
    },
    displayStatus() {
      return this.normalizedStatus
    },
    isSwitchable() {
      return this.normalizedStatus === 'PENDING' || this.normalizedStatus === 'ACTIVATED'
    },
    toggleTargetStatus() {
      if (this.normalizedStatus === 'ACTIVATED') {
        return 'PENDING'
      }
      if (this.normalizedStatus === 'PENDING') {
        return 'ACTIVATED'
      }
      return ''
    },
    toggleButtonText() {
      if (this.normalizedStatus === 'ACTIVATED') {
        return 'Lock Scooter'
      }
      if (this.normalizedStatus === 'PENDING') {
        return 'Activate Scooter'
      }
      return ''
    },
    statusTone() {
      if (!this.normalizedStatus) return 'pending'
      const map = {
        PENDING: 'pending',
        ACTIVATED: 'activated',
        ACTIVE: 'activated',
        COMPLETED: 'completed',
        CANCELLED: 'cancelled'
      }
      return map[this.normalizedStatus] || 'pending'
    },
    statusShort() {
      const map = {
        pending: 'P',
        activated: 'A',
        completed: 'C',
        cancelled: 'X'
      }
      return map[this.statusTone] || 'P'
    },
    statusDesc() {
      if (!this.normalizedStatus) return ''
      const map = {
        PENDING: 'The scooter is pending and locked. Activate it when you are ready to ride, or pay to end the order.',
        ACTIVATED: 'The scooter is activated. You can lock it at any time or pay to finish the order.',
        ACTIVE: 'The scooter is activated. You can lock it at any time or pay to finish the order.',
        COMPLETED: 'This booking has been paid and completed.',
        CANCELLED: 'This booking is no longer active.'
      }
      return map[this.normalizedStatus] || ''
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
    formatCurrency(value) {
      const amount = Number(value || 0)
      return `\u00A3${amount.toFixed(2)}`
    },
    async handleToggleStatus() {
      if (!this.toggleTargetStatus) return

      this.switching = true
      try {
        await updateBookingStatus(this.order.id, this.toggleTargetStatus)
        this.order.status = this.toggleTargetStatus
        uni.showToast({
          title: this.toggleTargetStatus === 'ACTIVATED' ? 'Scooter activated' : 'Scooter locked',
          icon: 'success'
        })
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.switching = false
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
.status-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  margin-top: 18rpx;
}

.status-marker {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 112rpx;
  height: 112rpx;
  border-radius: 50%;
  font-size: 40rpx;
  font-weight: 700;
}

.status-marker-pending {
  background: #fff5db;
  color: #b98224;
}

.status-marker-activated {
  background: #effad7;
  color: #5d8c22;
}

.status-marker-completed {
  background: #edf6ea;
  color: #4a7c52;
}

.status-marker-cancelled {
  background: #fff0ed;
  color: #c85c55;
}

.status-title {
  display: block;
  margin-top: 18rpx;
  font-size: 38rpx;
  font-weight: 700;
  color: #111111;
}

.status-copy {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #7d8677;
}

.empty-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #111111;
}

.empty-copy {
  display: block;
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #7d8677;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #edf0e8;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  font-size: 25rpx;
  color: #8c9587;
}

.info-value {
  flex: 1;
  min-width: 0;
  font-size: 27rpx;
  text-align: right;
  color: #111111;
  word-break: break-all;
}

.info-value-strong {
  font-weight: 700;
  color: #5d8c22;
}

.text-positive {
  color: #4a7c52;
}

.text-danger {
  color: #c85c55;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 20rpx;
}

.action-button {
  width: 100%;
}
</style>
