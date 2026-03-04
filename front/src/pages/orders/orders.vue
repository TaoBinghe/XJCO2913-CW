<template>
  <view class="orders-page">
    <view class="orders-header">
      <text class="page-title">My Orders</text>
    </view>

    <view class="container">
      <view v-if="loading" class="loading-state">
        <text>Loading orders...</text>
      </view>

      <view v-else-if="orders.length === 0" class="empty-state">
        <text class="empty-icon">📋</text>
        <text>No orders yet</text>
        <button class="btn-outline empty-btn" @click="goHome">
          Book a Scooter
        </button>
      </view>

      <view v-else class="order-list">
        <view
          class="order-card card"
          v-for="order in orders"
          :key="order.id"
          @click="goDetail(order)"
        >
          <view class="order-card-header">
            <text class="order-id">Order #{{ order.id }}</text>
            <text
              class="status-badge"
              :class="'status-' + order.status.toLowerCase()"
            >
              {{ order.status }}
            </text>
          </view>

          <view class="order-card-body">
            <view class="order-info-row">
              <text class="order-info-label">Scooter</text>
              <text class="order-info-value">#{{ order.scooterId }}</text>
            </view>
            <view class="order-info-row">
              <text class="order-info-label">Cost</text>
              <text class="order-info-value price">£{{ order.totalCost.toFixed(2) }}</text>
            </view>
            <view class="order-info-row">
              <text class="order-info-label">Start</text>
              <text class="order-info-value">{{ formatTime(order.startTime) }}</text>
            </view>
            <view class="order-info-row">
              <text class="order-info-label">End</text>
              <text class="order-info-value">{{ formatTime(order.endTime) }}</text>
            </view>
          </view>

          <view class="order-card-footer">
            <text class="order-created">Created: {{ formatTime(order.createdAt) }}</text>
            <text class="order-arrow">→</text>
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
.orders-page {
  min-height: 100vh;
  background-color: #f5f7f5;
}

.orders-header {
  background: linear-gradient(135deg, #07c160, #10b981);
  padding: 40rpx;
  padding-bottom: 50rpx;
}

.orders-header .page-title {
  color: #ffffff;
}

.loading-state {
  padding: 80rpx;
  text-align: center;
  color: #999999;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}

.empty-btn {
  margin-top: 30rpx;
  width: 360rpx;
}

.order-list {
  margin-top: -20rpx;
}

.order-card {
  padding: 0;
  overflow: hidden;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.order-id {
  font-size: 30rpx;
  font-weight: 600;
  color: #333333;
}

.order-card-body {
  padding: 20rpx 30rpx;
}

.order-info-row {
  display: flex;
  justify-content: space-between;
  padding: 10rpx 0;
}

.order-info-label {
  font-size: 26rpx;
  color: #999999;
}

.order-info-value {
  font-size: 26rpx;
  color: #333333;
}

.order-info-value.price {
  color: #07c160;
  font-weight: 600;
}

.order-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 30rpx;
  background-color: #fafafa;
}

.order-created {
  font-size: 24rpx;
  color: #bbbbbb;
}

.order-arrow {
  font-size: 30rpx;
  color: #cccccc;
}
</style>
