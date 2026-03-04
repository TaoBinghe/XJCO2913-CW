<script>
export default {
  onLaunch: function () {
    console.log('App Launch')
  },
  onShow: function () {
    console.log('App Show')
  },
  onHide: function () {
    console.log('App Hide')
  },
}
</script>

<style>
page {
  background-color: #f5f7f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  color: #333333;
  line-height: 1.6;
}

.container {
  padding: 30rpx;
}

.btn-primary {
  background-color: #07c160;
  color: #ffffff;
  border: none;
  border-radius: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 500;
  text-align: center;
}

.btn-primary::after {
  border: none;
}

.btn-primary:active {
  background-color: #06ad56;
}

.btn-danger {
  background-color: #ee0a24;
  color: #ffffff;
  border: none;
  border-radius: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 500;
  text-align: center;
}

.btn-danger::after {
  border: none;
}

.btn-outline {
  background-color: #ffffff;
  color: #07c160;
  border: 2rpx solid #07c160;
  border-radius: 12rpx;
  height: 88rpx;
  line-height: 88rpx;
  font-size: 32rpx;
  font-weight: 500;
  text-align: center;
}

.btn-outline::after {
  border: none;
}

.card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 24rpx;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.05);
}

.input-group {
  margin-bottom: 30rpx;
}

.input-label {
  font-size: 28rpx;
  color: #666666;
  margin-bottom: 12rpx;
}

.input-field {
  width: 100%;
  height: 88rpx;
  background-color: #f5f7f5;
  border-radius: 12rpx;
  padding: 0 24rpx;
  font-size: 30rpx;
  color: #333333;
  box-sizing: border-box;
}

.page-title {
  font-size: 48rpx;
  font-weight: 700;
  color: #333333;
  margin-bottom: 16rpx;
}

.page-subtitle {
  font-size: 28rpx;
  color: #999999;
  margin-bottom: 48rpx;
}

.status-badge {
  display: inline-block;
  padding: 6rpx 20rpx;
  border-radius: 20rpx;
  font-size: 24rpx;
  font-weight: 500;
}

.status-pending {
  background-color: #fff3e0;
  color: #ff9800;
}

.status-active {
  background-color: #e8f5e9;
  color: #4caf50;
}

.status-completed {
  background-color: #e3f2fd;
  color: #2196f3;
}

.status-cancelled {
  background-color: #ffebee;
  color: #f44336;
}

.link-text {
  color: #07c160;
  font-size: 28rpx;
  text-align: center;
}

.section-title {
  font-size: 34rpx;
  font-weight: 600;
  color: #333333;
  margin-bottom: 24rpx;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 60rpx;
}

.empty-state text {
  font-size: 30rpx;
  color: #999999;
  margin-top: 20rpx;
}
</style>
