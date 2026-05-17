<template>
  <view class="theme-page my-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">PROFILE SPACE</text>
        <text class="theme-headline">My Page</text>
      </view>

      <view class="card profile-card">
        <view class="profile-head">
          <view class="avatar-circle">
            <text class="avatar-text">{{ avatarLetter }}</text>
          </view>

          <view class="profile-copy-block">
            <text class="profile-name">{{ profileName }}</text>
            <text class="profile-note">{{ profileNote }}</text>
          </view>
        </view>

        <button v-if="!isLoggedIn" class="btn-primary profile-action" @click="goLogin">
          Login
        </button>
        <view v-else class="profile-status">Customer account active</view>
      </view>

      <view v-if="isLoggedIn" class="card ai-support-card" :class="{ 'ai-support-card-collapsed': !aiSupportOpen }">
        <view class="ai-support-head" @click="toggleAiSupport">
          <view class="ai-support-copy-block">
            <text class="section-title">AI Assistant</text>
            <text v-if="aiSupportOpen" class="ai-support-copy">Report a scooter fault or book a store reservation by chat. The assistant will ask one question at a time.</text>
          </view>
          <text class="ai-support-pill">{{ aiSupportToggleLabel }}</text>
        </view>

        <template v-if="aiSupportOpen">
          <scroll-view
            scroll-y
            class="ai-chat-window"
            :scroll-into-view="chatScrollTarget"
            scroll-with-animation
          >
            <view
              v-for="(message, index) in chatMessages"
              :id="'profile-chat-msg-' + index"
              :key="index"
              class="chat-row"
              :class="'chat-row-' + message.role"
            >
              <view class="chat-bubble" :class="'chat-bubble-' + message.role">
                <text class="chat-label">{{ message.role === 'assistant' ? 'AI Support' : 'You' }}</text>
                <text class="chat-text">{{ message.content }}</text>
              </view>
            </view>

            <view v-if="chatLoading" id="profile-chat-loading" class="chat-row chat-row-assistant">
              <view class="chat-bubble chat-bubble-assistant">
                <text class="chat-label">AI Support</text>
                <text class="chat-text">Checking the fault report details...</text>
              </view>
            </view>
          </scroll-view>

          <view v-if="chatIssueSubmitted" class="ai-submit-status">
            <text>Fault report submitted.</text>
            <button class="btn-outline ai-track-btn" @click="goFeedback">Track in Feedback</button>
          </view>

          <view v-if="chatBookingSubmitted" class="ai-submit-status">
            <text>Booking created.</text>
            <button class="btn-outline ai-track-btn" @click="goOrders">View Orders</button>
          </view>

          <view class="chat-input-row">
            <textarea
              v-model="chatInput"
              class="chat-input"
              maxlength="1000"
              auto-height
              placeholder="Type your message — fault details, booking request, or ask a question"
              placeholder-style="color: #9ca59a"
              :disabled="chatLoading"
            />
            <button
              class="btn-primary chat-send"
              :loading="chatLoading"
              :disabled="chatSendDisabled"
              @click="handleChatSend"
            >
              Send
            </button>
          </view>
        </template>
      </view>

      <view class="menu-list">
        <view class="card menu-card" @click="goWallet">
          <view class="menu-copy">
            <text class="menu-title">My Wallet</text>
            <text class="menu-desc">Manage balance, bind cards, and recharge before your next ride.</text>
          </view>
          <text class="menu-pill">Wallet</text>
        </view>

        <view class="card menu-card" @click="goOrders">
          <view class="menu-copy">
            <text class="menu-title">My Orders</text>
            <text class="menu-desc">Review store reservations, scan rides, and payment receipts.</text>
          </view>
          <text class="menu-pill">Open</text>
        </view>

        <view class="card menu-card" @click="goFeedback">
          <view class="menu-copy">
            <text class="menu-title">Feedback</text>
            <text class="menu-desc">Report scooter faults, payment issues, or booking problems and track staff responses.</text>
          </view>
          <text class="menu-pill">Support</text>
        </view>

        <view class="card menu-card" @click="goStoreBooking">
          <view class="menu-copy">
            <text class="menu-title">Reserve at a Store</text>
            <text class="menu-desc">Book a pickup window and compare store inventory before you arrive.</text>
          </view>
          <text class="menu-pill">Reserve</text>
        </view>

        <view class="card menu-card" @click="goScanRide">
          <view class="menu-copy">
            <text class="menu-title">Scan to Ride</text>
            <text class="menu-desc">Open the live map, preview the route, and start a scan ride on demand.</text>
          </view>
          <text class="menu-pill">Ride</text>
        </view>
      </view>

      <view v-if="isLoggedIn" class="logout-section">
        <button class="btn-danger" @click="handleLogout">Logout</button>
      </view>

      <view class="card app-info-card">
        <text class="app-version">Green Go Mini App v1.0.0</text>
      </view>
    </view>
  </view>
</template>

<script>
import { chatFaultReport } from '@/api/feedback'
import { getToken, getUsername, clearAll } from '@/utils/auth'

const INITIAL_AI_MESSAGE = 'Hi, I can help with scooter fault reports and store reservations. For faults: send your order ID, scooter code, and what happened. For bookings: just tell me you want to book a scooter. I will ask one question at a time.'
const CHAT_HISTORY_LIMIT = 10

export default {
  data() {
    return {
      username: '',
      isLoggedIn: false,
      chatInput: '',
      chatMessages: [
        { role: 'assistant', content: INITIAL_AI_MESSAGE }
      ],
      aiSupportOpen: false,
      chatLoading: false,
      chatIssueSubmitted: false,
      chatBookingSubmitted: false,
      chatScrollTarget: ''
    }
  },
  computed: {
    avatarLetter() {
      return this.username ? this.username.charAt(0).toUpperCase() : 'G'
    },
    profileName() {
      return this.username || 'Guest'
    },
    profileNote() {
      return this.isLoggedIn
        ? 'You are ready for your next reservation, scan ride, and payment check.'
        : 'Sign in to reserve scooters, start scan rides, and track every order.'
    },
    aiSupportToggleLabel() {
      return this.aiSupportOpen ? 'Close' : 'Open'
    },
    chatSendDisabled() {
      return this.chatLoading || !this.chatInput.trim()
    }
  },
  onShow() {
    this.isLoggedIn = !!getToken()
    this.username = getUsername()
  },
  methods: {
    goLogin() {
      uni.navigateTo({ url: '/pages/login/login' })
    },
    goWallet() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.navigateTo({ url: '/pages/wallet/wallet' })
    },
    goOrders() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.switchTab({ url: '/pages/orders/orders' })
    },
    goFeedback() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      uni.navigateTo({ url: '/pages/feedback/feedback' })
    },
    goStoreBooking() {
      uni.navigateTo({ url: '/pages/store-booking/store-booking' })
    },
    goScanRide() {
      uni.navigateTo({ url: '/pages/booking/booking' })
    },
    toggleAiSupport() {
      this.aiSupportOpen = !this.aiSupportOpen
      if (this.aiSupportOpen) {
        this.scrollChatToEnd()
      }
    },
    normalizeChatHistory() {
      return this.chatMessages
        .filter(message => (message.role === 'user' || message.role === 'assistant') && message.content)
        .slice(-CHAT_HISTORY_LIMIT)
        .map(message => ({
          role: message.role,
          content: String(message.content).trim()
        }))
    },
    scrollChatToEnd() {
      this.$nextTick(() => {
        this.chatScrollTarget = this.chatLoading
          ? 'profile-chat-loading'
          : `profile-chat-msg-${Math.max(this.chatMessages.length - 1, 0)}`
      })
    },
    resetChat() {
      this.chatInput = ''
      this.chatMessages = [
        { role: 'assistant', content: INITIAL_AI_MESSAGE }
      ]
      this.aiSupportOpen = false
      this.chatLoading = false
      this.chatIssueSubmitted = false
      this.chatBookingSubmitted = false
      this.chatScrollTarget = ''
    },
    async handleChatSend() {
      if (!this.isLoggedIn) {
        uni.navigateTo({ url: '/pages/login/login' })
        return
      }
      if (this.chatSendDisabled) {
        return
      }

      const message = this.chatInput.trim()
      const history = this.normalizeChatHistory()
      this.chatMessages.push({ role: 'user', content: message })
      this.chatInput = ''
      this.chatLoading = true
      this.chatIssueSubmitted = false
      this.chatBookingSubmitted = false
      this.scrollChatToEnd()

      try {
        const res = await chatFaultReport({ message, history })
        const reply = res.data?.reply || 'I could not read that response. Please try again.'
        this.chatMessages.push({ role: 'assistant', content: reply })
        if (res.data?.issue) {
          this.chatIssueSubmitted = true
          uni.showToast({ title: 'Fault report submitted', icon: 'success' })
        }
        if (res.data?.booking) {
          this.chatBookingSubmitted = true
          uni.showToast({ title: 'Booking created', icon: 'success' })
        }
      } catch (e) {
        this.chatMessages.push({
          role: 'assistant',
          content: e?.message || 'AI Support is unavailable right now. You can still submit feedback from the Feedback page.'
        })
      } finally {
        this.chatLoading = false
        this.scrollChatToEnd()
      }
    },
    handleLogout() {
      uni.showModal({
        title: 'Logout',
        content: 'Are you sure you want to logout?',
        confirmText: 'Confirm',
        cancelText: 'Cancel',
        success: (res) => {
          if (res.confirm) {
            clearAll()
            this.isLoggedIn = false
            this.username = ''
            this.resetChat()
            uni.showToast({ title: 'Logged out', icon: 'success' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.profile-card {
  margin-top: 38rpx;
}

.ai-support-card {
  margin-top: 28rpx;
}

.ai-support-card-collapsed {
  padding-top: 26rpx;
  padding-bottom: 26rpx;
}

.ai-support-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.ai-support-copy-block {
  flex: 1;
  min-width: 0;
}

.ai-support-copy {
  display: block;
  margin-top: 10rpx;
  color: #7d8677;
  font-size: 25rpx;
  line-height: 1.6;
}

.ai-support-pill {
  min-width: 104rpx;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #4a7c52;
  font-size: 22rpx;
  font-weight: 700;
  text-align: center;
}

.ai-chat-window {
  height: 500rpx;
  margin-top: 24rpx;
  padding: 20rpx;
  border-radius: 28rpx;
  background: #f7f8f5;
  box-sizing: border-box;
}

.chat-row {
  display: flex;
  margin-bottom: 18rpx;
}

.chat-row-user {
  justify-content: flex-end;
}

.chat-row-assistant {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 86%;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
}

.chat-bubble-assistant {
  background: #ffffff;
  border: 1rpx solid #e2e8dc;
}

.chat-bubble-user {
  background: #4a7c52;
}

.chat-label {
  display: block;
  margin-bottom: 6rpx;
  font-size: 20rpx;
  font-weight: 700;
}

.chat-bubble-assistant .chat-label {
  color: #4a7c52;
}

.chat-bubble-user .chat-label {
  color: rgba(255, 255, 255, 0.82);
}

.chat-text {
  display: block;
  font-size: 25rpx;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-bubble-assistant .chat-text {
  color: #4b5548;
}

.chat-bubble-user .chat-text {
  color: #ffffff;
}

.ai-submit-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 22rpx;
  background: #edf6ea;
  color: #4a7c52;
  font-size: 23rpx;
  font-weight: 700;
}

.ai-track-btn {
  min-width: 210rpx;
  margin: 0;
  font-size: 22rpx;
}

.chat-input-row {
  display: flex;
  align-items: flex-end;
  gap: 14rpx;
  margin-top: 20rpx;
}

.chat-input {
  flex: 1;
  min-height: 88rpx;
  max-height: 180rpx;
  padding: 22rpx 24rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.98);
  color: #111111;
  font-size: 26rpx;
  line-height: 1.45;
}

.chat-send {
  width: 150rpx;
  min-width: 150rpx;
  margin: 0;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar-circle {
  width: 132rpx;
  height: 132rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #5a8b62 0%, #4a7c52 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20rpx 44rpx rgba(74, 124, 82, 0.28);
  flex-shrink: 0;
}

.avatar-text {
  font-size: 48rpx;
  font-weight: 700;
  color: #ffffff;
}

.profile-copy-block {
  flex: 1;
  min-width: 0;
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
  color: #111111;
  word-break: break-all;
}

.profile-note {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.6;
  color: #7d8677;
}

.profile-action {
  margin-top: 28rpx;
}

.profile-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 28rpx;
  min-height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #4a7c52;
  font-size: 24rpx;
  font-weight: 700;
}

.menu-list {
  margin-top: 24rpx;
}

.menu-card {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24rpx;
}

.menu-copy {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}

.menu-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #111111;
}

.menu-desc {
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.6;
  color: #7d8677;
}

.menu-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112rpx;
  height: 60rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #effad7;
  color: #4a7c52;
  font-size: 24rpx;
  font-weight: 700;
}

.logout-section {
  margin-top: 28rpx;
}

.app-info-card {
  margin-top: 28rpx;
  text-align: center;
}

.app-version {
  display: block;
  font-size: 24rpx;
  color: #7d8677;
}

.app-copy {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #a0a89a;
}
</style>
