<template>
  <view class="auth-page">
    <view class="auth-glow auth-glow-top"></view>
    <view class="auth-glow auth-glow-bottom"></view>

    <view class="auth-shell">
      <view class="auth-copy">
        <text class="auth-kicker">LIGHT COMMUTING</text>
        <text class="auth-title">Create Account</text>
      </view>

      <view class="auth-form">
        <view class="auth-field">
          <text class="auth-label">Username</text>
          <input
            class="auth-input"
            v-model="username"
            placeholder="Enter a username"
            placeholder-style="color: #b7bdb5"
            maxlength="16"
            confirm-type="next"
          />
        </view>

        <view class="auth-field">
          <text class="auth-label">Password</text>
          <input
            class="auth-input"
            v-model="password"
            type="password"
            placeholder="Enter a password"
            placeholder-style="color: #b7bdb5"
            maxlength="16"
            confirm-type="next"
          />
        </view>

        <view class="auth-field">
          <text class="auth-label">Confirm Password</text>
          <input
            class="auth-input"
            v-model="confirmPassword"
            type="password"
            placeholder="Enter your password again"
            placeholder-style="color: #b7bdb5"
            maxlength="16"
            confirm-type="done"
            @confirm="handleRegister"
          />
        </view>

        <button class="btn-primary auth-submit" :loading="loading" @click="handleRegister">
          Sign Up
        </button>

        <view class="auth-links auth-links-center">
          <text class="auth-link" @click="goLogin">Already have an account? Log in</text>
        </view>
      </view>

      <view class="auth-illustration-block">
        <image class="auth-illustration" :src="illustrationSrc" mode="widthFix" />
      </view>
    </view>
  </view>
</template>

<script>
import loginBackground from '@/static/login_background.png'
import { register } from '@/api/user'

export default {
  data() {
    return {
      username: '',
      password: '',
      confirmPassword: '',
      loading: false,
      illustrationSrc: loginBackground
    }
  },
  methods: {
    async handleRegister() {
      if (this.username.length < 5 || this.username.length > 16) {
        uni.showToast({ title: 'Username must be 5-16 characters', icon: 'none' })
        return
      }
      if (this.password.length < 5 || this.password.length > 16) {
        uni.showToast({ title: 'Password must be 5-16 characters', icon: 'none' })
        return
      }
      if (this.password !== this.confirmPassword) {
        uni.showToast({ title: 'Passwords do not match', icon: 'none' })
        return
      }

      this.loading = true
      try {
        await register(this.username, this.password)
        uni.showToast({ title: 'Registration successful', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 1000)
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.loading = false
      }
    },
    goLogin() {
      uni.navigateBack()
    }
  }
}
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  background: #ffffff;
  overflow: hidden;
  padding: 0 36rpx;
  padding-top: calc(48rpx + constant(safe-area-inset-top));
  padding-top: calc(48rpx + env(safe-area-inset-top));
  padding-bottom: calc(40rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(40rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 680rpx;
  min-height: 100%;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.auth-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.auth-glow-top {
  top: -180rpx;
  right: -220rpx;
  width: 560rpx;
  height: 560rpx;
  background: radial-gradient(circle, rgba(210, 255, 38, 0.32) 0%, rgba(210, 255, 38, 0) 72%);
}

.auth-glow-bottom {
  left: -180rpx;
  bottom: 120rpx;
  width: 420rpx;
  height: 420rpx;
  background: radial-gradient(circle, rgba(164, 240, 103, 0.18) 0%, rgba(164, 240, 103, 0) 70%);
}

.auth-copy {
  margin-top: 12rpx;
}

.auth-kicker {
  display: block;
  font-size: 24rpx;
  letter-spacing: 6rpx;
  color: #89a54c;
}

.auth-title {
  display: block;
  margin-top: 24rpx;
  font-size: 64rpx;
  line-height: 1.14;
  font-weight: 700;
  color: #111111;
}

.auth-subtitle {
  display: block;
  max-width: 580rpx;
  margin-top: 20rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #687260;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
  margin-top: 56rpx;
}

.auth-field {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.auth-label {
  padding-left: 12rpx;
  font-size: 26rpx;
  color: #5f6b58;
}

.auth-input {
  width: 100%;
  height: 108rpx;
  padding: 0 34rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 54rpx;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 0 0 2rpx rgba(210, 218, 203, 0.18), 0 18rpx 40rpx rgba(17, 17, 17, 0.04);
  box-sizing: border-box;
  font-size: 30rpx;
  color: #111111;
}

.auth-submit {
  width: 100%;
  height: 108rpx;
  line-height: 108rpx;
  margin-top: 8rpx;
  margin-left: 0;
  margin-right: 0;
  padding: 0;
  border: none;
  border-radius: 54rpx;
  background: linear-gradient(135deg, #efff84 0%, #e2ff6b 100%);
  color: #111111;
  font-size: 34rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  box-shadow: 0 20rpx 44rpx rgba(226, 255, 107, 0.24);
}

.auth-submit:active {
  background: linear-gradient(135deg, #e6f973 0%, #d8f55a 100%);
}

.auth-links {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 8rpx;
  padding: 0 8rpx;
}

.auth-links-center {
  justify-content: center;
}

.auth-link {
  width: 100%;
  font-size: 26rpx;
  color: #24311f;
  text-align: center;
}

.auth-illustration-block {
  margin-top: auto;
  padding-top: 48rpx;
}

.auth-illustration-note {
  display: block;
  margin-bottom: 20rpx;
  font-size: 24rpx;
  text-align: center;
  letter-spacing: 2rpx;
  color: #98a093;
}

.auth-illustration {
  display: block;
  width: 100%;
  opacity: 0.72;
}
</style>
