<template>
  <view class="theme-page wallet-page">
    <view class="theme-glow theme-glow-top"></view>
    <view class="theme-glow theme-glow-bottom"></view>

    <view class="theme-shell">
      <view class="theme-hero">
        <text class="theme-kicker">WALLET</text>
        <text class="theme-headline">My Wallet</text>
      </view>

      <view class="card wallet-card">
        <view class="wallet-head">
          <view>
            <text class="wallet-kicker">BALANCE</text>
            <text class="wallet-balance">{{ formatCurrency(walletBalance) }}</text>
            <text class="wallet-note">Automatically refreshed when this page opens.</text>
          </view>
        </view>

        <view class="wallet-tabs">
          <view
            class="wallet-tab"
            :class="{ 'wallet-tab-active': activeWalletPanel === 'recharge' }"
            @click="activeWalletPanel = 'recharge'"
          >
            Recharge
          </view>
          <view
            class="wallet-tab"
            :class="{ 'wallet-tab-active': activeWalletPanel === 'card' }"
            @click="activeWalletPanel = 'card'"
          >
            Add Card
          </view>
        </view>

        <view class="wallet-card-list">
          <text class="wallet-subtitle">Bound Cards</text>
          <view v-if="walletLoading" class="wallet-empty">Loading wallet...</view>
          <view v-else-if="walletCards.length" class="bank-card-list">
            <view v-for="card in walletCards" :key="card.id" class="bank-card-row">
              <view class="bank-card-copy">
                <text class="bank-card-name">{{ card.bankName || 'Bank Card' }}</text>
                <text class="bank-card-number">{{ card.maskedCardNumber || `**** ${card.cardLastFour}` }}</text>
              </view>
              <text class="bank-card-last">{{ card.cardLastFour }}</text>
            </view>
          </view>
          <view v-else class="wallet-empty">No bank card bound yet.</view>
        </view>

        <view v-if="activeWalletPanel === 'recharge'" class="wallet-form">
          <template v-if="walletCards.length">
            <view class="input-group compact-input-group">
              <text class="input-label">Card</text>
              <picker mode="selector" :range="cardPickerLabels" :value="rechargeForm.cardIndex" @change="handleCardChange">
                <view class="wallet-picker">{{ selectedCardLabel }}</view>
              </picker>
            </view>
            <view class="input-group compact-input-group">
              <text class="input-label">Amount</text>
              <input
                v-model="rechargeForm.amount"
                class="input-field"
                type="digit"
                placeholder="Enter amount"
                placeholder-style="color: #9ca59a"
              />
            </view>
            <view class="input-group compact-input-group">
              <text class="input-label">Card Password</text>
              <input
                v-model="rechargeForm.cardPassword"
                class="input-field"
                password
                placeholder="Enter card password"
                placeholder-style="color: #9ca59a"
              />
            </view>
            <button class="btn-primary wallet-submit" :loading="recharging" @click="handleRecharge">
              Recharge Wallet
            </button>
          </template>
          <view v-else class="wallet-empty wallet-action-empty">
            Bind a card first, then recharge your wallet.
          </view>
        </view>

        <view v-else class="wallet-form">
          <view class="input-group compact-input-group">
            <text class="input-label">Bank Name</text>
            <input
              v-model="bindForm.bankName"
              class="input-field"
              placeholder="e.g. Green Bank"
              placeholder-style="color: #9ca59a"
            />
          </view>
          <view class="input-group compact-input-group">
            <text class="input-label">Holder Name</text>
            <input
              v-model="bindForm.holderName"
              class="input-field"
              placeholder="Card holder name"
              placeholder-style="color: #9ca59a"
            />
          </view>
          <view class="input-group compact-input-group">
            <text class="input-label">Card Number</text>
            <input
              v-model="bindForm.cardNumber"
              class="input-field"
              type="number"
              placeholder="12-19 digit card number"
              placeholder-style="color: #9ca59a"
            />
          </view>
          <view class="input-group compact-input-group">
            <text class="input-label">Card Password</text>
            <input
              v-model="bindForm.cardPassword"
              class="input-field"
              password
              placeholder="Set or enter card password"
              placeholder-style="color: #9ca59a"
            />
          </view>
          <button class="btn-outline wallet-submit" :loading="bindingCard" @click="handleBindCard">
            Bind Card
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { bindBankCard, getWalletSummary, rechargeWallet } from '@/api/wallet'
import { getToken } from '@/utils/auth'
import { formatCurrency } from '@/utils/booking'

export default {
  data() {
    return {
      walletLoading: false,
      wallet: {
        balance: 0,
        cards: []
      },
      activeWalletPanel: 'recharge',
      bindForm: {
        bankName: '',
        holderName: '',
        cardNumber: '',
        cardPassword: ''
      },
      rechargeForm: {
        cardIndex: 0,
        amount: '',
        cardPassword: ''
      },
      bindingCard: false,
      recharging: false
    }
  },
  computed: {
    walletBalance() {
      return Number(this.wallet?.balance || 0)
    },
    walletCards() {
      return Array.isArray(this.wallet?.cards) ? this.wallet.cards : []
    },
    cardPickerLabels() {
      return this.walletCards.map(card => `${card.bankName || 'Card'} ${card.maskedCardNumber || `**** ${card.cardLastFour}`}`)
    },
    selectedCard() {
      return this.walletCards[this.rechargeForm.cardIndex] || null
    },
    selectedCardLabel() {
      return this.selectedCard
        ? this.cardPickerLabels[this.rechargeForm.cardIndex]
        : 'Select a card'
    }
  },
  onShow() {
    if (!getToken()) {
      uni.navigateTo({ url: '/pages/login/login' })
      return
    }
    this.loadWallet()
  },
  methods: {
    formatCurrency(value) {
      return formatCurrency(value)
    },
    async loadWallet(force = false) {
      if (this.walletLoading && !force) {
        return
      }

      this.walletLoading = true
      try {
        const res = await getWalletSummary()
        this.wallet = {
          balance: res.data?.balance || 0,
          cards: res.data?.cards || []
        }
        if (this.rechargeForm.cardIndex >= this.walletCards.length) {
          this.rechargeForm.cardIndex = 0
        }
        if (!this.walletCards.length) {
          this.activeWalletPanel = 'card'
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
    handleCardChange(event) {
      this.rechargeForm.cardIndex = Number(event.detail.value || 0)
    },
    async handleBindCard() {
      if (this.bindingCard) {
        return
      }
      const bankName = this.bindForm.bankName.trim()
      const holderName = this.bindForm.holderName.trim()
      const cardNumber = this.bindForm.cardNumber.trim()
      const cardPassword = this.bindForm.cardPassword.trim()

      if (!bankName || !holderName || !cardNumber || !cardPassword) {
        uni.showToast({ title: 'Please complete card information', icon: 'none' })
        return
      }
      if (!/^\d{12,19}$/.test(cardNumber.replace(/\s+/g, ''))) {
        uni.showToast({ title: 'Card number must be 12-19 digits', icon: 'none' })
        return
      }

      this.bindingCard = true
      try {
        await bindBankCard({
          bankName,
          holderName,
          cardNumber,
          cardPassword
        })
        uni.showToast({ title: 'Card bound', icon: 'success' })
        this.bindForm = {
          bankName: '',
          holderName: '',
          cardNumber: '',
          cardPassword: ''
        }
        await this.loadWallet(true)
        this.activeWalletPanel = 'recharge'
      } catch (e) {
        // request.js shows backend errors
      } finally {
        this.bindingCard = false
      }
    },
    async handleRecharge() {
      if (this.recharging) {
        return
      }
      if (!this.selectedCard) {
        uni.showToast({ title: 'Please bind a card first', icon: 'none' })
        this.activeWalletPanel = 'card'
        return
      }

      const amount = Number(this.rechargeForm.amount)
      const cardPassword = this.rechargeForm.cardPassword.trim()
      if (!Number.isFinite(amount) || amount <= 0) {
        uni.showToast({ title: 'Please enter a valid amount', icon: 'none' })
        return
      }
      if (!cardPassword) {
        uni.showToast({ title: 'Please enter card password', icon: 'none' })
        return
      }

      this.recharging = true
      try {
        await rechargeWallet({
          cardId: this.selectedCard.id,
          amount: amount.toFixed(2),
          cardPassword
        })
        this.rechargeForm.amount = ''
        this.rechargeForm.cardPassword = ''
        uni.showToast({ title: 'Recharge successful', icon: 'success' })
        await this.loadWallet(true)
      } catch (e) {
        // request.js shows backend errors
      } finally {
        this.recharging = false
      }
    }
  }
}
</script>

<style scoped>
.wallet-card {
  margin-top: 38rpx;
}

.wallet-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}

.wallet-kicker {
  display: block;
  color: #89a54c;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
}

.wallet-balance {
  display: block;
  margin-top: 10rpx;
  color: #111111;
  font-size: 54rpx;
  font-weight: 700;
  line-height: 1.1;
}

.wallet-note {
  display: block;
  margin-top: 8rpx;
  color: #7d8677;
  font-size: 24rpx;
}

.wallet-tabs {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
  margin-top: 28rpx;
  padding: 8rpx;
  border-radius: 28rpx;
  background: #f7f8f5;
}

.wallet-tab {
  height: 70rpx;
  border-radius: 22rpx;
  color: #66715f;
  font-size: 25rpx;
  font-weight: 700;
  line-height: 70rpx;
  text-align: center;
}

.wallet-tab-active {
  background: #efff84;
  color: #111111;
}

.wallet-card-list {
  margin-top: 28rpx;
}

.wallet-subtitle {
  display: block;
  color: #111111;
  font-size: 28rpx;
  font-weight: 700;
}

.bank-card-list {
  margin-top: 14rpx;
}

.bank-card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  min-height: 94rpx;
  padding: 18rpx 20rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
}

.bank-card-row + .bank-card-row {
  margin-top: 12rpx;
}

.bank-card-copy {
  flex: 1;
  min-width: 0;
}

.bank-card-name {
  display: block;
  color: #111111;
  font-size: 26rpx;
  font-weight: 700;
}

.bank-card-number {
  display: block;
  margin-top: 4rpx;
  color: #7d8677;
  font-size: 23rpx;
}

.bank-card-last {
  flex-shrink: 0;
  min-width: 76rpx;
  color: #5d8c22;
  font-size: 24rpx;
  font-weight: 700;
  text-align: right;
}

.wallet-empty {
  margin-top: 14rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
  color: #7d8677;
  font-size: 24rpx;
}

.wallet-action-empty {
  margin-top: 0;
}

.wallet-form {
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 2rpx solid #eef2e7;
}

.compact-input-group {
  margin-bottom: 18rpx;
}

.wallet-picker {
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

.wallet-submit {
  width: 100%;
  margin-top: 10rpx;
}
</style>
