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
            <text class="info-label">Original Amount</text>
            <text class="info-value">{{ paymentReceipt.originalAmount != null ? formatCurrency(paymentReceipt.originalAmount) : '-' }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Discount</text>
            <text class="info-value">{{ discountLabel(paymentReceipt) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Paid Amount</text>
            <text class="info-value info-value-strong">{{ formatCurrency(paymentReceipt.amount) }}</text>
          </view>
          <view class="info-row">
            <text class="info-label">Method</text>
            <text class="info-value">{{ paymentReceipt.paymentMethod || '-' }}</text>
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

        <view v-if="canPay" class="card payment-card">
          <text class="section-title">Complete Payment</text>
          <text class="payment-copy">Choose wallet balance or a bound bank card to close this returned ride.</text>

          <view class="payment-summary">
            <view class="payment-summary-row">
              <text class="payment-summary-label">Amount Due</text>
              <text class="payment-summary-value">{{ formatCurrency(order.totalCostValue) }}</text>
            </view>
            <view class="payment-summary-row">
              <text class="payment-summary-label">Wallet Balance</text>
              <text class="payment-summary-value">{{ walletLoading ? 'Loading...' : formatCurrency(walletBalance) }}</text>
            </view>
          </view>

          <view class="payment-tabs">
            <view
              class="payment-tab"
              :class="{ 'payment-tab-active': activePaymentMethod === 'WALLET' }"
              @click="activePaymentMethod = 'WALLET'"
            >
              Wallet
            </view>
            <view
              class="payment-tab"
              :class="{ 'payment-tab-active': activePaymentMethod === 'CARD' }"
              @click="activePaymentMethod = 'CARD'"
            >
              Bank Card
            </view>
          </view>

          <view v-if="activePaymentMethod === 'CARD'" class="payment-card-fields">
            <template v-if="walletCards.length">
              <view class="input-group compact-input-group">
                <text class="input-label">Card</text>
                <picker mode="selector" :range="cardPickerLabels" :value="paymentForm.cardIndex" @change="handlePaymentCardChange">
                  <view class="detail-picker">{{ selectedPaymentCardLabel }}</view>
                </picker>
              </view>
              <view class="input-group compact-input-group">
                <text class="input-label">Card Password</text>
                <input
                  v-model="paymentForm.cardPassword"
                  class="input-field"
                  password
                  placeholder="Enter card password"
                  placeholder-style="color: #9ca59a"
                />
              </view>
            </template>
            <view v-else class="inline-empty">
              Bind a bank card in My Wallet before card payment.
            </view>
          </view>

          <button class="btn-primary full-action-btn" :loading="paying" @click="handlePayment">
            Pay Now
          </button>
        </view>

        <view v-if="canExtend" class="card extension-card">
          <text class="section-title">Extend Ride</text>
          <text class="payment-copy">Store pickup rides can be extended while active or overdue.</text>

          <view class="payment-tabs">
            <view
              class="payment-tab"
              :class="{ 'payment-tab-active': extensionMode === 'renew' }"
              @click="extensionMode = 'renew'"
            >
              Renew
            </view>
            <view
              class="payment-tab"
              :class="{ 'payment-tab-active': extensionMode === 'modify' }"
              @click="extensionMode = 'modify'"
            >
              Modify Period
            </view>
          </view>

          <view v-if="plansLoading" class="inline-empty">Loading plans...</view>
          <template v-else-if="extensionPlans.length">
            <view class="input-group compact-input-group">
              <text class="input-label">Plan</text>
              <picker mode="selector" :range="extensionPlanLabels" :value="extensionPlanIndex" @change="handleExtensionPlanChange">
                <view class="detail-picker">{{ selectedExtensionPlanLabel }}</view>
              </picker>
            </view>
            <button class="btn-outline full-action-btn" :loading="extending" @click="handleExtend">
              {{ extensionMode === 'renew' ? 'Renew Ride' : 'Modify Period' }}
            </button>
          </template>
          <view v-else class="inline-empty">No pricing plans are available right now.</view>
        </view>

        <view v-if="order" class="card feedback-entry-card">
          <text class="section-title">Need Help?</text>
          <text class="payment-copy">Send a short feedback report for this order and track the response.</text>
          <button class="btn-outline full-action-btn" @click="goFeedback">
            Report an Issue
          </button>
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
  getReservationPricingPlans,
  getPickupScooters,
  lockBooking,
  modifyBookingPeriod,
  pickupStoreBooking,
  renewBooking,
  returnScanRide,
  returnStoreBooking,
  unlockBooking
} from '@/api/booking'
import { getWalletSummary } from '@/api/wallet'
import { payBooking } from '@/api/payment'
import { getMyOrders } from '@/api/user'
import {
  buildBookingViewModel,
  formatCurrency,
  formatPeriod,
  formatTime,
  sortPricingPlans
} from '@/utils/booking'
import { getPaymentReceipt, savePaymentReceipt } from '@/utils/payment-receipts'
import { getToken } from '@/utils/auth'
import {
  getCurrentLocationWithPermission,
  LOCATION_ERROR_CODES
} from '@/utils/location'

function parseLocalDateTime(value) {
  if (!value) return NaN
  const parsed = new Date(String(value).replace(' ', 'T')).getTime()
  return Number.isFinite(parsed) ? parsed : NaN
}

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
      returning: false,
      walletLoading: false,
      wallet: {
        balance: 0,
        cards: []
      },
      activePaymentMethod: 'WALLET',
      paymentForm: {
        cardIndex: 0,
        cardPassword: ''
      },
      paying: false,
      plans: [],
      plansLoading: false,
      extensionMode: 'renew',
      extensionPlanIndex: 0,
      extending: false,
      nowTime: Date.now(),
      pickupTimer: null
    }
  },
  computed: {
    statusTone() {
      if (!this.order) return 'reserved'

      const map = {
        RESERVED: 'reserved',
        IN_PROGRESS: 'active',
        OVERDUE: 'overdue',
        AWAITING_PAYMENT: 'awaiting_payment',
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
        awaiting_payment: 'P',
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
        AWAITING_PAYMENT: 'The scooter has been returned. Complete payment to close this order.',
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
      if (!this.order || this.order.rentalType !== 'STORE_PICKUP' || this.order.status !== 'RESERVED') {
        return false
      }

      const startTime = parseLocalDateTime(this.order.startTime)
      const deadline = parseLocalDateTime(this.order.pickupDeadline)
      if (!Number.isFinite(startTime) || !Number.isFinite(deadline)) {
        return false
      }

      return this.nowTime >= startTime && this.nowTime <= deadline
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
    canPay() {
      return !!this.order && this.order.status === 'AWAITING_PAYMENT'
    },
    canExtend() {
      return !!this.order
        && this.order.rentalType === 'STORE_PICKUP'
        && (this.order.status === 'IN_PROGRESS' || this.order.status === 'OVERDUE')
    },
    hasActions() {
      return this.canCancel || this.canPickup || this.canLock || this.canUnlock || this.canReturn
    },
    walletBalance() {
      return Number(this.wallet?.balance || 0)
    },
    walletCards() {
      return Array.isArray(this.wallet?.cards) ? this.wallet.cards : []
    },
    cardPickerLabels() {
      return this.walletCards.map(card => `${card.bankName || 'Card'} ${card.maskedCardNumber || `**** ${card.cardLastFour}`}`)
    },
    selectedPaymentCard() {
      return this.walletCards[this.paymentForm.cardIndex] || null
    },
    selectedPaymentCardLabel() {
      return this.selectedPaymentCard
        ? this.cardPickerLabels[this.paymentForm.cardIndex]
        : 'Select a card'
    },
    extensionPlans() {
      return sortPricingPlans(this.plans)
    },
    extensionPlanLabels() {
      return this.extensionPlans.map(plan => `${formatPeriod(plan.hirePeriod)} · ${formatCurrency(plan.price)}`)
    },
    selectedExtensionPlan() {
      return this.extensionPlans[this.extensionPlanIndex] || null
    },
    selectedExtensionPlanLabel() {
      return this.selectedExtensionPlan
        ? this.extensionPlanLabels[this.extensionPlanIndex]
        : 'Select a plan'
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
    this.startPickupClock()
    this.loadDetail()
  },
  onHide() {
    this.stopPickupClock()
  },
  onUnload() {
    this.stopPickupClock()
  },
  methods: {
    startPickupClock() {
      this.stopPickupClock()
      this.nowTime = Date.now()
      this.pickupTimer = setInterval(() => {
        this.nowTime = Date.now()
      }, 30000)
    },
    stopPickupClock() {
      if (this.pickupTimer) {
        clearInterval(this.pickupTimer)
        this.pickupTimer = null
      }
    },
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
        if (this.canPay) {
          this.loadWallet()
        }
        if (this.canExtend) {
          this.loadPlans()
        }
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
      if (this.canPay) {
        this.loadWallet()
      }
      if (this.canExtend) {
        this.loadPlans()
      }
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
    async loadWallet() {
      if (this.walletLoading) {
        return
      }
      this.walletLoading = true
      try {
        const res = await getWalletSummary()
        this.wallet = {
          balance: res.data?.balance || 0,
          cards: res.data?.cards || []
        }
        if (this.paymentForm.cardIndex >= this.walletCards.length) {
          this.paymentForm.cardIndex = 0
        }
      } catch (e) {
        this.wallet = {
          balance: 0,
          cards: []
        }
      } finally {
        this.walletLoading = false
      }
    },
    async loadPlans() {
      if (this.plansLoading || this.plans.length) {
        return
      }
      this.plansLoading = true
      try {
        const res = await getReservationPricingPlans()
        this.plans = sortPricingPlans(res.data || [])
        if (this.extensionPlanIndex >= this.plans.length) {
          this.extensionPlanIndex = 0
        }
      } catch (e) {
        this.plans = []
      } finally {
        this.plansLoading = false
      }
    },
    formatTime(timeStr) {
      return formatTime(timeStr)
    },
    formatCurrency(value) {
      return formatCurrency(value)
    },
    discountLabel(payment) {
      const type = payment.discountType || 'NONE'
      const amount = Number(payment.discountAmount || 0)
      const rate = Number(payment.discountRate || 0)
      if (type === 'NONE' || amount <= 0) {
        return 'None'
      }
      return `${type} · ${(rate * 100).toFixed(0)}% · -${formatCurrency(amount)}`
    },
    handlePaymentCardChange(event) {
      this.paymentForm.cardIndex = Number(event.detail.value || 0)
    },
    handleExtensionPlanChange(event) {
      this.extensionPlanIndex = Number(event.detail.value || 0)
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
          ? 'This will upload your current coordinates and return the scooter. Payment is completed after return.'
          : 'This will return the store pickup scooter. Payment is completed after return.',
        '#5d8c22'
      )
      if (!confirmed) return

      this.returning = true
      try {
        const res = this.order.rentalType === 'SCAN_RIDE'
          ? await this.returnScanRideWithLocation()
          : await returnStoreBooking(this.order.id)
        this.applySettlementResult(res.data)
        if (this.order?.status === 'AWAITING_PAYMENT') {
          uni.showToast({ title: 'Returned. Please pay now.', icon: 'none' })
          this.loadWallet()
        } else {
          uni.showToast({ title: 'Ride returned', icon: 'success' })
        }
      } catch (e) {
        if (e?.code === LOCATION_ERROR_CODES.LOCATION_UNAVAILABLE) {
          uni.showToast({ title: 'Could not get your location to return this ride', icon: 'none' })
        }
      } finally {
        this.returning = false
      }
    },
    async handlePayment() {
      if (!this.order || this.paying) {
        return
      }
      const payload = {
        bookingId: this.order.id,
        paymentMethod: this.activePaymentMethod
      }

      if (this.activePaymentMethod === 'CARD') {
        if (!this.selectedPaymentCard) {
          uni.showToast({ title: 'Please bind or choose a card', icon: 'none' })
          return
        }
        const cardPassword = this.paymentForm.cardPassword.trim()
        if (!cardPassword) {
          uni.showToast({ title: 'Please enter card password', icon: 'none' })
          return
        }
        payload.cardId = this.selectedPaymentCard.id
        payload.cardPassword = cardPassword
      }

      this.paying = true
      try {
        const res = await payBooking(payload)
        savePaymentReceipt(this.order.id, res.data)
        this.paymentReceipt = res.data
        this.paymentForm.cardPassword = ''
        uni.showToast({ title: 'Payment successful', icon: 'success' })
        await this.loadDetail()
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.paying = false
      }
    },
    async handleExtend() {
      if (!this.order || !this.selectedExtensionPlan || this.extending) {
        return
      }
      this.extending = true
      try {
        const res = this.extensionMode === 'renew'
          ? await renewBooking(this.order.id, this.selectedExtensionPlan.hirePeriod)
          : await modifyBookingPeriod(this.order.id, this.selectedExtensionPlan.hirePeriod)
        this.applyBookingUpdate(res.data)
        uni.showToast({
          title: this.extensionMode === 'renew' ? 'Ride renewed' : 'Period updated',
          icon: 'success'
        })
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.extending = false
      }
    },
    goFeedback() {
      if (!this.order) return
      uni.navigateTo({
        url: `/pages/feedback/feedback?bookingId=${this.order.id}`
      })
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

.status-marker-awaiting_payment {
  background: #e8f1ff;
  color: #2463d6;
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

.payment-card,
.extension-card,
.feedback-entry-card {
  margin-top: 0;
}

.payment-copy {
  display: block;
  margin-top: 12rpx;
  color: #7d8677;
  font-size: 25rpx;
  line-height: 1.6;
}

.payment-summary {
  margin-top: 22rpx;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
}

.payment-summary-row {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  padding: 8rpx 0;
}

.payment-summary-label {
  color: #8c9587;
  font-size: 24rpx;
}

.payment-summary-value {
  color: #111111;
  font-size: 25rpx;
  font-weight: 700;
}

.payment-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 8rpx;
  border-radius: 28rpx;
  background: #f7f8f5;
}

.payment-tab {
  height: 70rpx;
  border-radius: 22rpx;
  color: #66715f;
  font-size: 25rpx;
  font-weight: 700;
  line-height: 70rpx;
  text-align: center;
}

.payment-tab-active {
  background: #efff84;
  color: #111111;
}

.payment-card-fields {
  margin-top: 24rpx;
}

.compact-input-group {
  margin-top: 22rpx;
  margin-bottom: 0;
}

.detail-picker {
  width: 100%;
  min-height: 96rpx;
  padding: 0 30rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 48rpx;
  background: rgba(255, 255, 255, 0.98);
  color: #111111;
  font-size: 28rpx;
  line-height: 90rpx;
}

.inline-empty {
  margin-top: 22rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
  color: #7d8677;
  font-size: 24rpx;
}

.full-action-btn {
  width: 100%;
  margin-top: 24rpx;
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
