<script>
const DEFAULT_WX_CLOUD_ENV_ID = 'prod-4g7i1ww2f71d4f7b'
const WX_CLOUD_ENV_ID = (import.meta.env.VITE_WX_CLOUD_ENV_ID || DEFAULT_WX_CLOUD_ENV_ID).trim()

export default {
  onLaunch() {
    console.log('App Launch')

    // #ifdef MP-WEIXIN
    if (!WX_CLOUD_ENV_ID) {
      console.error('VITE_WX_CLOUD_ENV_ID is not configured for mp-weixin')
      uni.showToast({ title: 'Cloud env not configured', icon: 'none' })
      return
    }

    if (!wx.cloud) {
      console.error('wx.cloud is unavailable. Please use a WeChat base library that supports cloud capabilities.')
      uni.showToast({ title: 'wx.cloud unavailable', icon: 'none' })
      return
    }

    wx.cloud.init({
      env: WX_CLOUD_ENV_ID
    })
    console.log(`wx.cloud initialized with env ${WX_CLOUD_ENV_ID}`)
    // #endif
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  }
}
</script>

<style>
page {
  background-color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #111111;
  line-height: 1.6;
}

view,
text,
button,
input,
picker,
image {
  box-sizing: border-box;
}

.container {
  padding: 0 32rpx;
}

.theme-page {
  position: relative;
  min-height: 100vh;
  background: #ffffff;
  overflow: hidden;
}

.theme-shell {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 960rpx;
  margin: 0 auto;
  padding: 28rpx 32rpx calc(40rpx + constant(safe-area-inset-bottom));
  padding: 28rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
}

.theme-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.theme-glow-top {
  top: -180rpx;
  right: -220rpx;
  width: 560rpx;
  height: 560rpx;
  background: radial-gradient(circle, rgba(210, 255, 38, 0.28) 0%, rgba(210, 255, 38, 0) 72%);
}

.theme-glow-bottom {
  left: -160rpx;
  bottom: 120rpx;
  width: 420rpx;
  height: 420rpx;
  background: radial-gradient(circle, rgba(164, 240, 103, 0.18) 0%, rgba(164, 240, 103, 0) 70%);
}

.theme-hero {
  margin-top: 12rpx;
}

.theme-kicker {
  display: block;
  font-size: 24rpx;
  letter-spacing: 6rpx;
  color: #89a54c;
}

.theme-headline {
  display: block;
  margin-top: 20rpx;
  font-size: 60rpx;
  line-height: 1.12;
  font-weight: 700;
  color: #111111;
}

.theme-copy {
  display: block;
  margin-top: 18rpx;
  max-width: 680rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: #687260;
}

.theme-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18rpx;
  margin: 52rpx 0 24rpx;
}

.theme-section-note {
  font-size: 24rpx;
  color: #98a093;
}

.btn-primary {
  background: linear-gradient(135deg, #efff84 0%, #e2ff6b 100%);
  color: #111111;
  border: none;
  border-radius: 54rpx;
  height: 96rpx;
  line-height: 96rpx;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  text-align: center;
  box-shadow: 0 20rpx 44rpx rgba(226, 255, 107, 0.24);
}

.btn-primary::after {
  border: none;
}

.btn-primary:active {
  background: linear-gradient(135deg, #e6f973 0%, #d8f55a 100%);
}

.btn-outline {
  background: #ffffff;
  color: #24311f;
  border: 3rpx solid #d2dacb;
  border-radius: 54rpx;
  height: 96rpx;
  line-height: 90rpx;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
}

.btn-outline::after {
  border: none;
}

.btn-danger {
  background: #fff7f6;
  color: #c85c55;
  border: 3rpx solid #f0ccc8;
  border-radius: 54rpx;
  height: 96rpx;
  line-height: 90rpx;
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
}

.btn-danger::after {
  border: none;
}

.card {
  background: #ffffff;
  border: 2rpx solid #eef2e7;
  border-radius: 36rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 18rpx 40rpx rgba(17, 17, 17, 0.04);
}

.input-group {
  margin-bottom: 26rpx;
}

.input-label {
  display: block;
  padding-left: 12rpx;
  font-size: 26rpx;
  color: #5f6b58;
  margin-bottom: 14rpx;
}

.input-field {
  width: 100%;
  height: 104rpx;
  padding: 0 30rpx;
  border: 3rpx solid #d2dacb;
  border-radius: 52rpx;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 0 0 2rpx rgba(210, 218, 203, 0.18), 0 18rpx 40rpx rgba(17, 17, 17, 0.04);
  font-size: 30rpx;
  color: #111111;
}

.page-title {
  font-size: 58rpx;
  font-weight: 700;
  color: #111111;
  margin-bottom: 14rpx;
}

.page-subtitle {
  font-size: 28rpx;
  color: #687260;
  margin-bottom: 0;
  line-height: 1.7;
}

.section-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #111111;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 52rpx;
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
}

.status-pending {
  background: #fff5db;
  color: #b98224;
}

.status-active {
  background: #effad7;
  color: #5d8c22;
}

.status-activated {
  background: #effad7;
  color: #5d8c22;
}

.status-completed {
  background: #edf6ea;
  color: #4a7c52;
}

.status-cancelled {
  background: #fff0ed;
  color: #c85c55;
}

.status-available {
  background: #effad7;
  color: #5d8c22;
}

.status-unavailable {
  background: #f2f4ef;
  color: #7d8677;
}

.link-text {
  color: #24311f;
  font-size: 28rpx;
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 72rpx 44rpx;
  text-align: center;
}

.empty-state text {
  color: #7d8677;
}
</style>
