<template>
  <view class="theme-page store-booking-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">STORE PICKUP</text>
        <text class="theme-headline">Reserve a Scooter</text>
      </view>

      <view class="card filter-card">
        <text class="section-title">Appointment Window</text>

        <view class="filter-grid">
          <view class="filter-field">
            <text class="input-label">Date</text>
            <picker mode="date" :value="appointmentDate" @change="handleDateChange">
              <view class="picker-field">{{ appointmentDate }}</view>
            </picker>
          </view>

          <view class="filter-field">
            <text class="input-label">Time</text>
            <picker mode="time" :value="appointmentTime" @change="handleTimeChange">
              <view class="picker-field">{{ appointmentTime }}</view>
            </picker>
          </view>
        </view>

        <view v-if="isLoggedIn" class="plan-block">
          <text class="input-label">Reservation Plan</text>
          <view v-if="plansLoading" class="mini-note">Loading reservation plans...</view>
          <view v-else-if="plans.length" class="plan-grid">
            <view
              v-for="plan in plans"
              :key="plan.id"
              class="plan-chip"
              :class="{ 'plan-chip-active': selectedPeriod === plan.hirePeriod }"
              @click="selectPlan(plan)"
            >
              <text class="plan-chip-title">{{ formatPeriod(plan.hirePeriod) }}</text>
              <text class="plan-chip-price">{{ formatCurrency(plan.price) }}</text>
            </view>
          </view>
          <view v-else class="mini-note">No reservation pricing plans are available right now.</view>
        </view>

        <view v-else class="guest-note">
          <text class="guest-note-title">Guest browsing is enabled</text>
          <text class="guest-note-copy">
            Sign in to unlock reservation pricing and time-window-specific inventory.
          </text>
          <button class="btn-outline guest-login-btn" @click="goLogin">Log in</button>
        </view>
      </view>

      <view class="theme-section-head">
        <view class="stores-head-copy">
          <text class="section-title">Available Stores</text>
          <text class="theme-section-note">{{ availabilitySummary }}</text>
        </view>
        <button class="btn-outline refresh-btn" :loading="storesLoading" @click="loadStores">
          <text class="refresh-icon">&#x21bb;</text>
        </button>
      </view>

      <view v-if="storesLoading" class="card loading-card">
        <text>Loading stores...</text>
      </view>

      <view v-else-if="stores.length === 0" class="card empty-state">
        <text class="empty-title">No stores available</text>
        <text class="empty-copy">Try a different appointment window or refresh the public store list.</text>
      </view>

      <view v-else class="store-list">
        <view
          v-for="store in stores"
          :key="store.id"
          class="card store-card"
          :class="{ 'store-card-expanded': isStoreExpanded(store.id) }"
        >
          <view class="store-card-toggle" @click="toggleStore(store.id)">
            <view class="store-card-head">
              <view class="store-copy">
                <text class="store-title">{{ store.name }}</text>
                <text class="store-address">{{ store.address || 'Address unavailable' }}</text>
              </view>
              <view class="store-card-side">
                <text class="status-badge" :class="storeStatusClass(store)">
                  {{ storeStatusLabel(store) }}
                </text>
                <text class="store-expand-indicator">
                  {{ isStoreExpanded(store.id) ? '-' : '+' }}
                </text>
              </view>
            </view>

            <view class="store-summary-row">
              <text class="store-summary-pill">Bookable {{ Number(store.bookableInventory || 0) }}</text>
              <text class="store-summary-pill">Current {{ Number(store.currentAvailableInventory || 0) }}</text>
              <text class="store-summary-pill">Tap to {{ isStoreExpanded(store.id) ? 'hide' : 'view' }} details</text>
            </view>
          </view>

          <view v-if="isStoreExpanded(store.id)" class="store-card-body">
            <view class="inventory-grid">
              <view class="inventory-pill">
                <text class="inventory-label">Bookable</text>
                <text class="inventory-value">{{ Number(store.bookableInventory || 0) }}</text>
              </view>
              <view class="inventory-pill">
                <text class="inventory-label">Current</text>
                <text class="inventory-value">{{ Number(store.currentAvailableInventory || 0) }}</text>
              </view>
              <view class="inventory-pill">
                <text class="inventory-label">Total</text>
                <text class="inventory-value">{{ Number(store.totalInventory || 0) }}</text>
              </view>
            </view>

            <view class="store-meta">
              <text class="store-meta-line">Window start: {{ formatTime(store.appointmentStart || appointmentStart) }}</text>
              <text class="store-meta-line">Window end: {{ formatTime(store.appointmentEnd) }}</text>
            </view>

            <button
              class="btn-primary reserve-btn"
              :disabled="!canReserveStore(store)"
              :loading="submittingStoreId === store.id"
              @click="handleReserve(store)"
            >
              {{ reserveButtonLabel(store) }}
            </button>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { createStoreBooking, getReservationPricingPlans } from '@/api/booking'
import { getStoreList } from '@/api/store'
import { formatCurrency, formatPeriod, formatTime, sortPricingPlans } from '@/utils/booking'
import { getToken } from '@/utils/auth'

function pad(value) {
  return String(value).padStart(2, '0')
}

function createDefaultAppointment() {
  const baseDate = new Date()
  baseDate.setDate(baseDate.getDate() + 1)
  baseDate.setHours(10, 0, 0, 0)
  return {
    date: `${baseDate.getFullYear()}-${pad(baseDate.getMonth() + 1)}-${pad(baseDate.getDate())}`,
    time: `${pad(baseDate.getHours())}:${pad(baseDate.getMinutes())}`
  }
}

export default {
  data() {
    const defaults = createDefaultAppointment()
    return {
      plans: [],
      stores: [],
      selectedPeriod: '',
      appointmentDate: defaults.date,
      appointmentTime: defaults.time,
      isLoggedIn: false,
      plansLoading: false,
      storesLoading: false,
      submittingStoreId: null,
      expandedStoreId: null,
      initialPeriod: ''
    }
  },
  computed: {
    appointmentStart() {
      return `${this.appointmentDate}T${this.appointmentTime}:00`
    },
    selectedPlan() {
      return this.plans.find(plan => plan.hirePeriod === this.selectedPeriod) || null
    },
    availabilitySummary() {
      if (!this.isLoggedIn) {
        return 'Guest mode shows each store’s current public inventory.'
      }
      if (!this.selectedPlan) {
        return 'Choose a reservation plan to load window-specific inventory.'
      }
      return `Inventory for ${formatPeriod(this.selectedPlan.hirePeriod)} starting ${formatTime(this.appointmentStart)}.`
    }
  },
  onLoad(options) {
    this.initialPeriod = options.period || ''
  },
  onShow() {
    this.isLoggedIn = !!getToken()
    this.bootstrapPage()
  },
  methods: {
    async bootstrapPage() {
      if (this.isLoggedIn) {
        await this.loadPlans()
      } else {
        this.plans = []
        this.selectedPeriod = ''
      }
      await this.loadStores()
    },
    async loadPlans() {
      this.plansLoading = true
      try {
        const res = await getReservationPricingPlans()
        this.plans = sortPricingPlans(res.data || [])
        const initialPlan = this.plans.find(plan => plan.hirePeriod === this.initialPeriod)
        this.selectedPeriod = initialPlan
          ? initialPlan.hirePeriod
          : (this.selectedPeriod || this.plans[0]?.hirePeriod || '')
      } catch (e) {
        this.plans = []
        this.selectedPeriod = ''
      } finally {
        this.plansLoading = false
      }
    },
    async loadStores() {
      this.storesLoading = true
      try {
        const params = this.isLoggedIn && this.selectedPlan
          ? {
              appointmentStart: this.appointmentStart,
              hiredPeriod: this.selectedPlan.hirePeriod
            }
          : {}
        const res = await getStoreList(params)
        this.stores = res.data || []
        if (!this.stores.some(store => store.id === this.expandedStoreId)) {
          this.expandedStoreId = null
        }
      } catch (e) {
        this.stores = []
        this.expandedStoreId = null
      } finally {
        this.storesLoading = false
      }
    },
    selectPlan(plan) {
      this.selectedPeriod = plan.hirePeriod
      this.loadStores()
    },
    handleDateChange(event) {
      this.appointmentDate = event.detail.value
      this.loadStores()
    },
    handleTimeChange(event) {
      this.appointmentTime = event.detail.value
      this.loadStores()
    },
    formatPeriod(period) {
      return formatPeriod(period)
    },
    formatCurrency(value) {
      return formatCurrency(value)
    },
    formatTime(value) {
      return formatTime(value)
    },
    storeStatusLabel(store) {
      if (Number(store.bookableInventory || 0) > 0) {
        return 'BOOKABLE'
      }
      if (store.status === 'DISABLED') {
        return 'DISABLED'
      }
      return 'FULL'
    },
    storeStatusClass(store) {
      if (Number(store.bookableInventory || 0) > 0) {
        return 'status-available'
      }
      if (store.status === 'DISABLED') {
        return 'status-disabled'
      }
      return 'status-overdue'
    },
    isStoreExpanded(storeId) {
      return this.expandedStoreId === storeId
    },
    toggleStore(storeId) {
      this.expandedStoreId = this.expandedStoreId === storeId ? null : storeId
    },
    canReserveStore(store) {
      return this.isLoggedIn
        && !!this.selectedPlan
        && Number(store.bookableInventory || 0) > 0
        && this.submittingStoreId !== store.id
    },
    reserveButtonLabel(store) {
      if (!this.isLoggedIn) {
        return 'Log in to Reserve'
      }
      if (!this.selectedPlan) {
        return 'Select a Plan First'
      }
      if (Number(store.bookableInventory || 0) <= 0) {
        return 'No Inventory Left'
      }
      return 'Reserve Here'
    },
    async handleReserve(store) {
      if (!this.isLoggedIn) {
        this.goLogin()
        return
      }
      if (!this.selectedPlan) {
        uni.showToast({ title: 'Please choose a reservation plan', icon: 'none' })
        return
      }
      if (Number(store.bookableInventory || 0) <= 0) {
        uni.showToast({ title: 'This store is fully booked for the selected window', icon: 'none' })
        return
      }

      this.submittingStoreId = store.id
      try {
        const res = await createStoreBooking({
          storeId: store.id,
          appointmentStart: this.appointmentStart,
          hiredPeriod: this.selectedPlan.hirePeriod
        })
        uni.showToast({ title: 'Reservation created', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({
            url: `/pages/order-detail/order-detail?bookingId=${res.data.id}`
          })
        }, 500)
      } catch (e) {
        // request.js shows backend errors
      } finally {
        this.submittingStoreId = null
      }
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    }
  }
}
</script>

<style scoped>
.filter-card {
  margin-top: 38rpx;
}

.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
  margin-top: 22rpx;
}

.filter-field {
  min-width: 0;
}

.picker-field {
  width: 100%;
  min-height: 104rpx;
  padding: 0 30rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 52rpx;
  background: rgba(255, 255, 255, 0.98);
  display: flex;
  align-items: center;
  font-size: 30rpx;
  color: #111111;
}

.plan-block {
  margin-top: 24rpx;
}

.mini-note {
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #7d8677;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 16rpx;
  width: 100%;
}

.plan-chip {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  width: 100%;
  min-width: 0;
  padding: 22rpx 24rpx;
  border-radius: 24rpx;
  border: 2rpx solid #eef2e7;
  background: #f7f8f5;
  box-sizing: border-box;
}

.plan-chip:last-child:nth-child(odd) {
  grid-column: 1 / -1;
}

.plan-chip-active {
  background: #f7fbeb;
  border-color: #d8ef8c;
}

.plan-chip-title {
  font-size: 26rpx;
  font-weight: 700;
  color: #111111;
}

.plan-chip-price {
  font-size: 24rpx;
  color: #5d8c22;
  font-weight: 700;
}

.guest-note {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 28rpx;
  background: #f7f8f5;
}

.guest-note-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: #111111;
}

.guest-note-copy {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7d8677;
}

.guest-login-btn,
.refresh-btn {
  margin-top: 18rpx;
}

.stores-head-copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 8rpx;
}

.stores-head-copy .theme-section-note {
  display: block;
}

.refresh-btn {
  width: 92rpx;
  min-width: 92rpx;
  height: 92rpx;
  padding: 0;
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-icon {
  font-size: 38rpx;
  font-weight: 600;
  line-height: 1;
}

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

.store-card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18rpx;
}

.store-card-toggle {
  cursor: pointer;
}

.store-card-side {
  display: flex;
  align-items: center;
  gap: 16rpx;
  flex-shrink: 0;
}

.store-copy {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.store-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #111111;
}

.store-address {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7d8677;
}

.store-summary-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.store-summary-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #f7f8f5;
  font-size: 22rpx;
  color: #66715f;
}

.store-expand-indicator {
  width: 48rpx;
  height: 48rpx;
  border-radius: 999rpx;
  background: #f7f8f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 700;
  color: #55614e;
}

.store-card-body {
  margin-top: 24rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #eef2e7;
}

.inventory-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 24rpx;
}

.inventory-pill {
  padding: 20rpx 18rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
  text-align: center;
}

.inventory-label {
  display: block;
  font-size: 22rpx;
  color: #8c9587;
}

.inventory-value {
  display: block;
  margin-top: 10rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.store-meta {
  margin-top: 22rpx;
}

.store-meta-line {
  display: block;
  font-size: 24rpx;
  color: #7d8677;
  line-height: 1.6;
}

.reserve-btn {
  width: 100%;
  margin-top: 24rpx;
}

@media screen and (max-width: 480px) {
  .filter-grid,
  .inventory-grid {
    grid-template-columns: 1fr;
  }
}
</style>
