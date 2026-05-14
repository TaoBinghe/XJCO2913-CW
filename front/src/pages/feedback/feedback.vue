<template>
  <view class="theme-page feedback-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">SUPPORT</text>
        <text class="theme-headline">Feedback</text>
        <text class="theme-copy">Report scooter faults, booking issues, payment problems, or anything else about a ride.</text>
      </view>

      <view class="card feedback-form-card">
        <text class="section-title">Submit Feedback</text>

        <view v-if="ordersLoading" class="feedback-empty">Loading your orders...</view>
        <template v-else-if="orders.length">
          <view class="input-group compact-input-group">
            <text class="input-label">Order</text>
            <picker mode="selector" :range="orderPickerLabels" :value="selectedOrderIndex" @change="handleOrderChange">
              <view class="feedback-picker">{{ selectedOrderLabel }}</view>
            </picker>
          </view>

          <view class="input-group compact-input-group">
            <text class="input-label">Category</text>
            <picker mode="selector" :range="categoryLabels" :value="selectedCategoryIndex" @change="handleCategoryChange">
              <view class="feedback-picker">{{ selectedCategoryLabel }}</view>
            </picker>
          </view>

          <view class="input-group compact-input-group">
            <text class="input-label">Details</text>
            <textarea
              v-model="content"
              class="feedback-textarea"
              maxlength="500"
              placeholder="Describe the issue in 5 to 500 characters"
              placeholder-style="color: #9ca59a"
            />
            <text class="feedback-count">{{ content.length }}/500</text>
          </view>

          <button class="btn-primary feedback-submit" :loading="submitting" @click="handleSubmit">
            Submit Feedback
          </button>
        </template>

        <view v-else class="feedback-empty">
          You need an order before submitting ride feedback.
        </view>
      </view>

      <view class="theme-section-head">
        <view>
          <text class="section-title">My Feedback</text>
          <text class="theme-section-note">{{ feedbackSummary }}</text>
        </view>
        <button class="btn-outline refresh-btn" :loading="issuesLoading" @click="loadIssues">
          <text class="refresh-icon">&#x21bb;</text>
        </button>
      </view>

      <view v-if="issuesLoading" class="card loading-card">
        <text>Loading feedback...</text>
      </view>

      <view v-else-if="issues.length === 0" class="card empty-state">
        <text class="empty-title">No feedback yet</text>
        <text class="empty-copy">Submitted issues and staff responses will appear here.</text>
      </view>

      <view v-else class="feedback-list">
        <view v-for="issue in issues" :key="issue.id" class="card issue-card">
          <view class="issue-head">
            <view class="issue-copy">
              <text class="issue-title">{{ categoryText(issue.category) }}</text>
              <text class="issue-meta">Order #{{ issue.bookingId }} · {{ formatTime(issue.createdAt) }}</text>
            </view>
            <view class="issue-tags">
              <text class="issue-tag" :class="issueResolutionClass(issue)">
                {{ issueResolutionLabel(issue) }}
              </text>
            </view>
          </view>

          <text class="issue-content">{{ issue.content }}</text>

          <view v-if="issue.resolutionNote" class="resolution-box">
            <text class="resolution-label">Resolution</text>
            <text class="resolution-copy">{{ issue.resolutionNote }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { createFeedbackIssue, getMyFeedbackIssues } from '@/api/feedback'
import { getMyOrders } from '@/api/user'
import { buildBookingViewModel, formatTime, sortBookings } from '@/utils/booking'
import { getToken } from '@/utils/auth'

const CATEGORY_OPTIONS = [
  { label: 'Scooter Fault', value: 'SCOOTER_FAULT' },
  { label: 'Booking Issue', value: 'BOOKING' },
  { label: 'Payment Issue', value: 'PAYMENT' },
  { label: 'Other', value: 'OTHER' }
]

export default {
  data() {
    return {
      initialBookingId: '',
      orders: [],
      issues: [],
      selectedOrderIndex: 0,
      selectedCategoryIndex: 0,
      content: '',
      ordersLoading: false,
      issuesLoading: false,
      submitting: false
    }
  },
  computed: {
    orderPickerLabels() {
      return this.orders.map(order => `#${order.id} · ${order.displayTitle} · ${order.status}`)
    },
    selectedOrder() {
      return this.orders[this.selectedOrderIndex] || null
    },
    selectedOrderLabel() {
      return this.selectedOrder ? this.orderPickerLabels[this.selectedOrderIndex] : 'Select an order'
    },
    categoryLabels() {
      return CATEGORY_OPTIONS.map(item => item.label)
    },
    selectedCategory() {
      return CATEGORY_OPTIONS[this.selectedCategoryIndex] || CATEGORY_OPTIONS[0]
    },
    selectedCategoryLabel() {
      return this.selectedCategory?.label || 'Scooter Fault'
    },
    feedbackSummary() {
      if (!this.issues.length) {
        return 'No submitted issues yet.'
      }
      const resolvedCount = this.issues.filter(issue => this.isIssueResolved(issue)).length
      return `${this.issues.length} total, ${resolvedCount} resolved.`
    }
  },
  onLoad(options) {
    this.initialBookingId = options.bookingId || ''
  },
  onShow() {
    if (!getToken()) {
      uni.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.bootstrapPage()
  },
  methods: {
    async bootstrapPage() {
      await this.loadOrders()
      await this.loadIssues()
    },
    async loadOrders() {
      this.ordersLoading = true
      try {
        const res = await getMyOrders()
        this.orders = sortBookings((res.data || []).map(order => buildBookingViewModel(order)))
        const initialIndex = this.orders.findIndex(order => String(order.id) === String(this.initialBookingId))
        if (initialIndex >= 0) {
          this.selectedOrderIndex = initialIndex
        } else if (this.selectedOrderIndex >= this.orders.length) {
          this.selectedOrderIndex = 0
        }
      } catch (e) {
        this.orders = []
        this.selectedOrderIndex = 0
      } finally {
        this.ordersLoading = false
      }
    },
    async loadIssues() {
      this.issuesLoading = true
      try {
        const res = await getMyFeedbackIssues()
        this.issues = res.data || []
      } catch (e) {
        this.issues = []
      } finally {
        this.issuesLoading = false
      }
    },
    handleOrderChange(event) {
      this.selectedOrderIndex = Number(event.detail.value || 0)
    },
    handleCategoryChange(event) {
      this.selectedCategoryIndex = Number(event.detail.value || 0)
    },
    isIssueResolved(issue) {
      return String(issue?.status || '').toUpperCase() === 'RESOLVED'
    },
    issueResolutionLabel(issue) {
      return this.isIssueResolved(issue) ? 'Resolved' : 'Unresolved'
    },
    issueResolutionClass(issue) {
      return this.isIssueResolved(issue) ? 'issue-status-resolved' : 'issue-status-unresolved'
    },
    async handleSubmit() {
      if (this.submitting) {
        return
      }
      if (!this.selectedOrder) {
        uni.showToast({ title: 'Please choose an order', icon: 'none' })
        return
      }
      const content = this.content.trim()
      if (content.length < 5 || content.length > 500) {
        uni.showToast({ title: 'Feedback must be 5 to 500 characters', icon: 'none' })
        return
      }

      this.submitting = true
      try {
        await createFeedbackIssue({
          bookingId: this.selectedOrder.id,
          category: this.selectedCategory.value,
          content
        })
        this.content = ''
        uni.showToast({ title: 'Feedback submitted', icon: 'success' })
        await this.loadIssues()
      } catch (e) {
        // request.js shows backend errors
      } finally {
        this.submitting = false
      }
    },
    categoryText(category) {
      return CATEGORY_OPTIONS.find(item => item.value === category)?.label || category || '-'
    },
    formatTime(value) {
      return formatTime(value)
    }
  }
}
</script>

<style scoped>
.feedback-form-card {
  margin-top: 38rpx;
}

.compact-input-group {
  margin-top: 24rpx;
  margin-bottom: 0;
}

.feedback-picker {
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

.feedback-textarea {
  width: 100%;
  min-height: 220rpx;
  padding: 28rpx 30rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.98);
  font-size: 28rpx;
  line-height: 1.6;
  color: #111111;
}

.feedback-count {
  display: block;
  margin-top: 8rpx;
  padding-right: 8rpx;
  color: #98a093;
  font-size: 22rpx;
  text-align: right;
}

.feedback-submit {
  width: 100%;
  margin-top: 28rpx;
}

.feedback-empty,
.loading-card {
  margin-top: 18rpx;
  color: #7d8677;
  font-size: 24rpx;
}

.loading-card {
  text-align: center;
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

.issue-card {
  padding: 30rpx;
}

.issue-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.issue-copy {
  flex: 1;
  min-width: 0;
}

.issue-title {
  display: block;
  color: #111111;
  font-size: 30rpx;
  font-weight: 700;
}

.issue-meta {
  display: block;
  margin-top: 8rpx;
  color: #98a093;
  font-size: 23rpx;
}

.issue-tags {
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  flex-shrink: 0;
}

.issue-tag {
  min-width: 112rpx;
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  font-weight: 700;
  text-align: center;
}

.issue-status-unresolved {
  background: #fff5db;
  color: #b98224;
}

.issue-status-resolved {
  background: #edf6ea;
  color: #4a7c52;
}

.issue-content {
  display: block;
  margin-top: 22rpx;
  color: #4b5548;
  font-size: 25rpx;
  line-height: 1.65;
}

.resolution-box {
  margin-top: 22rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
}

.resolution-label {
  display: block;
  color: #4a7c52;
  font-size: 23rpx;
  font-weight: 700;
}

.resolution-copy {
  display: block;
  margin-top: 8rpx;
  color: #4b5548;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
