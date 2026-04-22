<template>
  <view class="theme-page orders-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">BOOKING HISTORY</text>
        <text class="theme-headline">Orders</text>
        <text v-if="ordersSummary" class="theme-copy">{{ ordersSummary }}</text>
      </view>

      <view v-if="loading" class="card loading-card">
        <text>Loading orders...</text>
      </view>

      <view v-else-if="orders.length === 0" class="card empty-state">
        <text class="empty-title">No orders yet</text>
        <text class="empty-copy">Reservations, active rides, and completed trips will show up here once you start riding.</text>
        <button class="btn-outline empty-btn" @click="goHome">Explore Ride Options</button>
      </view>

      <view v-else class="orders-content">
        <view v-if="currentOrders.length" class="orders-section">
          <view class="theme-section-head orders-section-head">
            <view>
              <text class="section-title">Current Orders</text>
              <text class="theme-section-note">Reserved bookings, active rides, and overdue returns stay here.</text>
            </view>
          </view>

          <view class="order-list">
            <view
              v-for="order in currentOrders"
              :key="order.id"
              class="card order-card"
              @click="goDetail(order)"
            >
              <view class="order-card-header">
                <view class="order-header-copy">
                  <text class="order-id">{{ order.displayTitle }}</text>
                  <text class="order-created">Order #{{ order.id }} · {{ order.rentalTypeLabel }}</text>
                </view>
                <text class="status-badge" :class="'status-' + order.status.toLowerCase()">
                  {{ order.status }}
                </text>
              </view>

              <view class="order-details">
                <view class="order-row">
                  <text class="order-label">{{ order.rentalType === 'STORE_PICKUP' ? 'Store' : 'Scooter' }}</text>
                  <text class="order-value">{{ order.displayTitle }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">Location</text>
                  <text class="order-value">{{ order.displayLocation }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">Plan</text>
                  <text class="order-value">{{ order.hirePeriodLabel }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">{{ order.rentalType === 'STORE_PICKUP' ? 'Pickup Window' : 'Ride Started' }}</text>
                  <text class="order-value">{{ formatTime(order.rentalType === 'STORE_PICKUP' ? order.startTime : order.pickupTime || order.startTime) }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">{{ order.status === 'OVERDUE' ? 'Overdue Cost' : 'Current Cost' }}</text>
                  <text class="order-value order-value-strong">{{ formatCurrency(order.totalCostValue) }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">{{ order.rentalType === 'STORE_PICKUP' ? 'Pickup Deadline' : 'Lock Status' }}</text>
                  <text class="order-value">{{ order.rentalType === 'STORE_PICKUP' ? formatTime(order.pickupDeadline) : (order.lockStatus || '-') }}</text>
                </view>
              </view>

              <view class="order-footer">
                <text class="order-link">Manage order</text>
              </view>
            </view>
          </view>
        </view>

        <view v-if="historyOrders.length" class="orders-section">
          <view class="theme-section-head orders-section-head">
          </view>

          <view class="order-list">
            <view
              v-for="order in historyOrders"
              :key="order.id"
              class="card order-card"
              @click="goDetail(order)"
            >
              <view class="order-card-header">
                <view class="order-header-copy">
                  <text class="order-id">{{ order.displayTitle }}</text>
                  <text class="order-created">Order #{{ order.id }} · {{ order.rentalTypeLabel }}</text>
                </view>
                <text class="status-badge" :class="'status-' + order.status.toLowerCase()">
                  {{ order.status }}
                </text>
              </view>

              <view class="order-details">
                <view class="order-row">
                  <text class="order-label">{{ order.rentalType === 'STORE_PICKUP' ? 'Store' : 'Scooter' }}</text>
                  <text class="order-value">{{ order.displayTitle }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">Location</text>
                  <text class="order-value">{{ order.displayLocation }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">Plan</text>
                  <text class="order-value">{{ order.hirePeriodLabel }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">Paid</text>
                  <text class="order-value order-value-strong">{{ formatCurrency(order.totalCostValue) }}</text>
                </view>
                <view class="order-row">
                  <text class="order-label">{{ order.status === 'NO_SHOW_CANCELLED' ? 'Cancelled At' : 'Closed At' }}</text>
                  <text class="order-value">{{ formatTime(order.returnTime || order.updatedAt || order.endTime) }}</text>
                </view>
              </view>

              <view class="order-footer">
                <text class="order-link">View details</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMyOrders } from '@/api/user'
import {
  buildBookingViewModel,
  formatCurrency,
  formatTime,
  isOpenBooking,
  sortBookings
} from '@/utils/booking'
import { getToken } from '@/utils/auth'

export default {
  data() {
    return {
      orders: [],
      loading: true
    }
  },
  computed: {
    currentOrders() {
      return this.orders.filter(order => isOpenBooking(order.status))
    },
    historyOrders() {
      return this.orders.filter(order => !isOpenBooking(order.status))
    },
    ordersSummary() {
      if (this.loading) {
        return 'Loading the latest reservation and ride states.'
      }
      if (!this.orders.length) {
        return 'Keep store reservations, scan rides, and completed trips in one clean timeline.'
      }
      return ''
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
        const ordersRes = await getMyOrders()
        this.orders = sortBookings((ordersRes.data || []).map(order => buildBookingViewModel(order)))
      } catch (e) {
        this.orders = []
      } finally {
        this.loading = false
      }
    },
    formatTime(timeStr) {
      return formatTime(timeStr)
    },
    formatCurrency(value) {
      return formatCurrency(value)
    },
    goDetail(order) {
      uni.navigateTo({
        url: `/pages/order-detail/order-detail?bookingId=${order.id}`
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

.orders-content {
  margin-top: 10rpx;
}

.orders-section {
  margin-top: 18rpx;
}

.orders-section-head {
  margin-top: 0;
}

.order-list {
  margin-top: 8rpx;
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
