<template>
  <view class="theme-page orders-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">BOOKING HISTORY</text>
        <text class="theme-headline">Orders</text>
        <text class="theme-copy">{{ ordersSummary }}</text>
      </view>

      <view v-if="loading" class="card loading-card">
        <text>Loading orders...</text>
      </view>

      <view v-else-if="orders.length === 0" class="card empty-state">
        <text class="empty-title">No orders yet</text>
        <text class="empty-copy">Your bookings will show up here once you start your first ride.</text>
        <button class="btn-outline empty-btn" @click="goHome">Book a Scooter</button>
      </view>

      <view v-else class="order-list">
        <view
          class="card order-card"
          v-for="order in orders"
          :key="order.id"
          @click="goDetail(order)"
        >
          <view class="order-card-header">
            <view class="order-header-copy">
              <text class="order-id">Order #{{ order.id }}</text>
              <text class="order-created">Created {{ formatTime(order.createdAt) }}</text>
            </view>
            <text class="status-badge" :class="'status-' + normalizeStatus(order.status).toLowerCase()">
              {{ normalizeStatus(order.status) }}
            </text>
          </view>

          <view class="order-details">
            <view class="order-row">
              <text class="order-label">Scooter</text>
              <text class="order-value">#{{ order.scooterId }}</text>
            </view>
            <view class="order-row">
              <text class="order-label">Cost</text>
              <text class="order-value order-value-strong">£{{ order.totalCost.toFixed(2) }}</text>
            </view>
            <view class="order-row">
              <text class="order-label">Start</text>
              <text class="order-value">{{ formatTime(order.startTime) }}</text>
            </view>
            <view class="order-row">
              <text class="order-label">End</text>
              <text class="order-value">{{ formatTime(order.endTime) }}</text>
            </view>
          </view>

          <view class="order-footer">
            <text class="order-link">View details</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyOrders } from '@/api/user'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      orders: [],
      loading: true
    }
  },
  computed: {
    ordersSummary() {
      if (this.loading) {
        return 'Loading your latest bookings and payment states.'
      }
      if (!this.orders.length) {
        return 'Keep track of bookings, lock and activate states, and completed rides in one clean view.'
      }
      return `${this.orders.length} booking(s) ready to review, switch status, or pay.`
    }
  },
  onShow() {
    if (!getToken()) {
      uni.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.loadOrders()
  },
  methods: {
    async loadOrders() {
      this.loading = true
      try {
        const res = await getMyOrders()
        this.orders = res.data || []
      } catch (e) {
        this.orders = []
      } finally {
        this.loading = false
      }
    },
    formatTime(timeStr) {
      if (!timeStr) return '-'
      return timeStr.replace('T', ' ').substring(0, 16)
    },
    normalizeStatus(status) {
      return status === 'ACTIVE' ? 'ACTIVATED' : status
    },
    goDetail(order) {
      uni.navigateTo({
        url: `/pages/order-detail/order-detail?order=${encodeURIComponent(JSON.stringify(order))}`
      })
    },
    goHome() {
      uni.switchTab({ url: '/pages/index/index' })
    }
  }
}
</script>

<style scoped>
.loading-card {
  text-align: center;
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

.empty-btn {
  width: 100%;
  max-width: 360rpx;
  margin-top: 28rpx;
}

.order-list {
  margin-top: 34rpx;
}

.order-card {
  padding: 30rpx;
}

.order-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
  padding-bottom: 22rpx;
  border-bottom: 1rpx solid #edf0e8;
}

.order-header-copy {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.order-id {
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
  word-break: break-all;
}

.order-created {
  margin-top: 8rpx;
  font-size: 23rpx;
  color: #98a093;
}

.order-details {
  padding: 18rpx 0 0;
}

.order-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20rpx;
  padding: 12rpx 0;
}

.order-label {
  font-size: 25rpx;
  color: #8c9587;
}

.order-value {
  flex: 1;
  min-width: 0;
  font-size: 26rpx;
  text-align: right;
  color: #111111;
  word-break: break-all;
}

.order-value-strong {
  font-weight: 700;
  color: #5d8c22;
}

.order-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 14rpx;
}

.order-link {
  font-size: 24rpx;
  font-weight: 700;
  color: #5d8c22;
}
</style>
