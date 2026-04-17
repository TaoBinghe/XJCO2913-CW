<template>
  <view class="theme-page detail-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view v-if="loading" class="card loading-card">
        <text>Loading order details...</text>
      </view>

      <view v-else-if="!order" class="card empty-state">
        <text class="empty-title">Order not found</text>
        <text class="empty-copy">We could not load the latest booking details for this page.</text>
        <button class="btn-outline empty-btn" @click="goBackToOrders">Back to orders</button>
      </view>

      <view v-else>
        <view class="card status-card">
          <view class="status-marker" :class="'status-marker-' + statusTone">
            {{ statusShort }}
          </view>
          <text class="status-title">{{ order.status }}</text>
          <text class="status-copy">{{ statusDesc }}</text>
        </view>

        <view class="card">
          <text class="section-title">Order Information</text>
          <view class="info-row">
            <text class="info-label">Order ID</text>
            <text class="info-value">#{{ order.id }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Flow</text>
            <text class="info-value">{{ order.rentalTypeLabel }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">{{ order.rentalType === 'STORE_PICKUP' ? 'Store' : 'Scooter' }}</text>
            <text class="info-value">{{ order.displayTitle }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Location</text>
            <text class="info-value">{{ order.displayLocation }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Hire Period</text>
            <text class="info-value">{{ order.hirePeriodLabel }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Start Time</text>
            <text class="info-value">{{ formatTime(order.startTime) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">End Time</text>
            <text class="info-value">{{ formatTime(order.endTime) }}</text>
          </view>
          <view v-if="order.pickupDeadline" class="info-row">
            <text class="info-label">Pickup Deadline</text>
            <text class="info-value">{{ formatTime(order.pickupDeadline) }}</text>
          </view>
          <view v-if="order.pickupTime" class="info-row">
            <text class="info-label">Pickup Time</text>
            <text class="info-value">{{ formatTime(order.pickupTime) }}</text>
          </view>
          <view v-if="order.returnTime" class="info-row">
            <text class="info-label">Return Time</text>
            <text class="info-value">{{ formatTime(order.returnTime) }}</text>
          </view>
          <view v-if="order.scooterCode" class="info-row">
            <text class="info-label">Scooter Code</text>
            <text class="info-value">{{ order.scooterCode }}</text>
          </view>
          <view v-if="order.lockStatus" class="info-row">
            <text class="info-label">Lock Status</text>
            <text class="info-value">{{ order.lockStatus }}</text>
          </view>
          <view v-if="order.returnLocation" class="info-row">
            <text class="info-label">Return Location</text>
            <text class="info-value">{{ order.returnLocation }}</text>
          </view>
          <view v-if="order.overdueCostValue > 0" class="info-row">
            <text class="info-label">Overdue Cost</text>
            <text class="info-value info-value-strong">{{ formatCurrency(order.overdueCostValue) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Total Cost</text>
            <text class="info-value info-value-strong">{{ formatCurrency(order.totalCostValue) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Created</text>
            <text class="info-value">{{ formatTime(order.createdAt) }}</text>
          </view>
        </view>

        <view v-if="paymentReceipt" class="card">
          <text class="section-title">Payment Receipt</text>
          <view class="info-row">
            <text class="info-label">Status</text>
            <text class="info-value" :class="paymentReceipt.status === 'SUCCESS' ? 'text-positive' : 'text-danger'">
              {{ paymentReceipt.status }}
            </text>
          </view>
          <view class="info-row">
            <text class="info-label">Amount</text>
            <text class="info-value">{{ formatCurrency(paymentReceipt.amount) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Transaction ID</text>
            <text class="info-value">{{ paymentReceipt.transactionId || '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Card</text>
            <text class="info-value">{{ paymentReceipt.cardLastFour ? `****${paymentReceipt.cardLastFour}` : '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Payment Time</text>
            <text class="info-value">{{ formatTime(paymentReceipt.paymentTime) }}</text>
          </view>
        </view>

        <view v-else-if="paymentUnavailable" class="card receipt-missing-card">
          <text class="section-title">Payment Receipt</text>
          <text class="receipt-missing-copy">This order is completed, but this device does not have the original payment receipt cached.</text>
        </view>

        <view v-if="hasActions" class="action-buttons">
          <button
            v-if="canPickup"
            class="btn-primary action-button"
            :loading="pickupLoading"
            @click="handlePickup"
          >
            Pick Up Scooter
          </button>
          <button
            v-if="canCancel"
            class="btn-danger action-button"
            :loading="cancelling"
            @click="handleCancel"
          >
            Cancel Reservation
          </button>
          <button
            v-if="canUnlock"
            class="btn-outline action-button"
            :loading="unlocking"
            @click="handleUnlock"
          >
            Unlock Scooter
          </button>
          <button
            v-if="canLock"
            class="btn-outline action-button"
            :loading="locking"
            @click="handleLock"
          >
            Lock Scooter
          </button>
          <button
            v-if="canReturn"
            class="btn-primary action-button"
            :loading="returning"
            @click="handleReturn"
          >
            Return and Settle
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import {
  cancelStoreBooking,
  getPickupScooters,
  lockBooking,
  pickupStoreBooking,
  returnScanRide,
  returnStoreBooking,
  unlockBooking
} from '@/api/booking'
import { getMyOrders } from '@/api/user'
import {
  buildBookingViewModel,
  formatCurrency,
  formatTime
} from '@/utils/booking'
import { getPaymentReceipt, savePaymentReceipt } from '@/utils/payment-receipts'
import { getToken } from '@/utils/auth'
import {
  getCurrentLocationWithPermission,
  LOCATION_ERROR_CODES
} from '@/utils/location'

export default {
  data() {
    return {
      bookingId: '',
      order: null,
      paymentReceipt: null,
      loading: true,
      cancelling: false,
      pickupLoading: false,
      locking: false,
      unlocking: false,
      returning: false
    }
  },
  computed: {
    statusTone() {
      if (!this.order) return 'reserved'

      const map = {
        RESERVED: 'reserved',
        IN_PROGRESS: 'active',
        OVERDUE: 'overdue',
        COMPLETED: 'completed',
        CANCELLED: 'cancelled',
        NO_SHOW_CANCELLED: 'cancelled'
      }
      return map[this.order.status] || 'cancelled'
    },
    statusShort() {
      const map = {
        reserved: 'R',
        active: 'A',
        overdue: 'O',
        completed: 'C',
        cancelled: 'X'
      }
      return map[this.statusTone] || 'R'
    },
    statusDesc() {
      if (!this.order) return ''

      const map = {
        RESERVED: this.order.rentalType === 'STORE_PICKUP'
          ? 'Your reservation is active. Cancel it, or pick up a scooter once the pickup window opens.'
          : 'This scan ride has not started yet.',
        IN_PROGRESS: 'Your ride is live. Lock or unlock the scooter as needed, then return it to settle the order.',
        OVERDUE: 'This ride has passed the planned end time. Return it as soon as possible to complete settlement.',
        COMPLETED: this.paymentReceipt
          ? 'This ride is closed and the payment receipt is stored on this device.'
          : 'This ride is closed. The order is still valid, but this device does not have the original payment receipt cached.',
        CANCELLED: 'This reservation was cancelled before the ride started.',
        NO_SHOW_CANCELLED: 'The reservation expired because pickup never happened before the deadline.'
      }
      return map[this.order.status] || ''
    },
    paymentUnavailable() {
      return !!this.order && this.order.status === 'COMPLETED' && !this.paymentReceipt
    },
    canCancel() {
      return !!this.order && this.order.rentalType === 'STORE_PICKUP' && this.order.status === 'RESERVED'
    },
    canPickup() {
      return !!this.order && this.order.rentalType === 'STORE_PICKUP' && this.order.status === 'RESERVED'
    },
    canLock() {
      return !!this.order
        && (this.order.status === 'IN_PROGRESS' || this.order.status === 'OVERDUE')
        && this.order.lockStatus !== 'LOCKED'
    },
    canUnlock() {
      return !!this.order
        && (this.order.status === 'IN_PROGRESS' || this.order.status === 'OVERDUE')
        && this.order.lockStatus !== 'UNLOCKED'
    },
    canReturn() {
      return !!this.order && (this.order.status === 'IN_PROGRESS' || this.order.status === 'OVERDUE')
    },
    hasActions() {
      return this.canCancel || this.canPickup || this.canLock || this.canUnlock || this.canReturn
    }
  },
  onLoad(options) {
    this.bookingId = options.bookingId || ''
  },
  onShow() {
    if (!getToken()) {
      uni.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      if (!this.bookingId) {
        this.order = null
        this.loading = false
        return
      }

      this.loading = true
      try {
        const ordersRes = await getMyOrders()
        const booking = (ordersRes.data || []).find(item => String(item.id) === String(this.bookingId))
        this.order = booking ? buildBookingViewModel(booking) : null
        this.paymentReceipt = this.order && this.order.status === 'COMPLETED'
          ? getPaymentReceipt(this.order.id)
          : null
      } catch (e) {
        this.order = null
        this.paymentReceipt = null
      } finally {
        this.loading = false
      }
    },
    applyBookingUpdate(booking) {
      this.order = buildBookingViewModel(booking)
      this.paymentReceipt = this.order && this.order.status === 'COMPLETED'
        ? getPaymentReceipt(this.order.id)
        : this.paymentReceipt
    },
    applySettlementResult(result) {
      if (!result) return
      if (result.booking) {
        this.applyBookingUpdate(result.booking)
      }
      if (result.payment) {
        savePaymentReceipt(this.order.id, result.payment)
        this.paymentReceipt = result.payment
      }
    },
    formatTime(timeStr) {
      return formatTime(timeStr)
    },
    formatCurrency(value) {
      return formatCurrency(value)
    },
    async confirmAction(title, content, confirmColor = '#5d8c22') {
      return new Promise((resolve) => {
        uni.showModal({
          title,
          content,
          confirmText: 'Confirm',
          cancelText: 'Cancel',
          confirmColor,
          success: (res) => resolve(res.confirm),
          fail: () => resolve(false)
        })
      })
    },
    async handleCancel() {
      if (!this.order) return

      const confirmed = await this.confirmAction(
        'Cancel reservation',
        'This will release the reservation before pickup happens.',
        '#c85c55'
      )
      if (!confirmed) return

      this.cancelling = true
      try {
        const res = await cancelStoreBooking(this.order.id)
        this.applyBookingUpdate(res.data)
        uni.showToast({ title: 'Reservation cancelled', icon: 'success' })
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.cancelling = false
      }
    },
    async handlePickup() {
      if (!this.order) return

      this.pickupLoading = true
      try {
        const res = await getPickupScooters(this.order.id)
        const scooters = res.data || []
        if (!scooters.length) {
          uni.showToast({ title: 'No pickup scooters are available right now', icon: 'none' })
          return
        }

        uni.showActionSheet({
          itemList: scooters.map(scooter => `${scooter.scooterCode} · ${scooter.status}`),
          success: async ({ tapIndex }) => {
            const scooter = scooters[tapIndex]
            if (!scooter) return

            this.pickupLoading = true
            try {
              const pickupRes = await pickupStoreBooking(this.order.id, scooter.id)
              this.applyBookingUpdate(pickupRes.data)
              uni.showToast({ title: 'Scooter picked up', icon: 'success' })
            } catch (e) {
              // request.js handles backend errors
            } finally {
              this.pickupLoading = false
            }
          },
          fail: () => {
            this.pickupLoading = false
          }
        })
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.pickupLoading = false
      }
    },
    async handleLock() {
      if (!this.order) return

      this.locking = true
      try {
        const res = await lockBooking(this.order.id)
        this.applyBookingUpdate(res.data)
        uni.showToast({ title: 'Scooter locked', icon: 'success' })
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.locking = false
      }
    },
    async handleUnlock() {
      if (!this.order) return

      this.unlocking = true
      try {
        const res = await unlockBooking(this.order.id)
        this.applyBookingUpdate(res.data)
        uni.showToast({ title: 'Scooter unlocked', icon: 'success' })
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.unlocking = false
      }
    },
    async returnScanRideWithLocation() {
      const location = await getCurrentLocationWithPermission({
        reasonTitle: 'Location permission needed',
        reasonContent: 'To return this scan ride, please enable location permission so we can upload your return coordinates.',
        successHint: 'Location enabled. Please tap again.'
      })
      return returnScanRide(this.order.id, location.longitude, location.latitude)
    },
    async handleReturn() {
      if (!this.order) return

      const confirmed = await this.confirmAction(
        'Return scooter',
        this.order.rentalType === 'SCAN_RIDE'
          ? 'This will upload your current coordinates, return the scooter, and settle the order.'
          : 'This will return the store pickup scooter and settle the order.',
        '#5d8c22'
      )
      if (!confirmed) return

      this.returning = true
      try {
        const res = this.order.rentalType === 'SCAN_RIDE'
          ? await this.returnScanRideWithLocation()
          : await returnStoreBooking(this.order.id)
        this.applySettlementResult(res.data)
        uni.showToast({ title: 'Ride completed', icon: 'success' })
      } catch (e) {
        if (e?.code === LOCATION_ERROR_CODES.LOCATION_UNAVAILABLE) {
          uni.showToast({ title: 'Could not get your location to return this ride', icon: 'none' })
        }
      } finally {
        this.returning = false
      }
    },
    goBackToOrders() {
      uni.switchTab({ url: '/pages/orders/orders' })
    }
  }
}
</script>

<style scoped>
.loading-card {
  text-align: center;
  color: #7d8677;
}

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

.status-marker-reserved {
  background: #fff5db;
  color: #b98224;
}

.status-marker-active {
  background: #effad7;
  color: #5d8c22;
}

.status-marker-overdue {
  background: #fff4df;
  color: #c67a10;
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

.empty-btn {
  width: 100%;
  max-width: 360rpx;
  margin-top: 28rpx;
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

.receipt-missing-card {
  margin-top: 0;
}

.receipt-missing-copy {
  display: block;
  margin-top: 12rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #7d8677;
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
