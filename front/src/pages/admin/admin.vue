<template>
  <view class="admin-page">
    <!-- Header -->
    <view class="admin-header">
      <view class="header-top">
        <text class="page-title">Scooter Management</text>
        <text class="header-logout" @click="handleLogout">Logout</text>
      </view>
      <text class="scooter-count">{{ scooters.length }} scooter(s) managed</text>
    </view>

    <view class="container">
      <!-- Add Button -->
      <button class="btn-primary add-btn" @click="showAddForm">
        + Add New Scooter
      </button>

      <!-- Scooter List -->
      <view v-if="scooters.length === 0" class="empty-state">
        <text class="empty-icon">🛴</text>
        <text>No scooters added yet</text>
        <text style="font-size: 24rpx; color: #bbbbbb; margin-top: 10rpx;">
          Tap the button above to add one
        </text>
      </view>

      <view v-else class="scooter-list">
        <view class="scooter-card card" v-for="(scooter, index) in scooters" :key="index">
          <view class="scooter-card-header">
            <view class="scooter-code-badge">
              <text class="scooter-code">{{ scooter.scooterCode }}</text>
            </view>
            <text
              class="status-badge"
              :class="scooter.status === 'AVAILABLE' ? 'status-active' : 'status-cancelled'"
            >
              {{ scooter.status }}
            </text>
          </view>

          <view class="scooter-card-body">
            <view class="scooter-info-row" v-if="scooter.id">
              <text class="scooter-info-label">ID</text>
              <text class="scooter-info-value">#{{ scooter.id }}</text>
            </view>
            <view class="scooter-info-row">
              <text class="scooter-info-label">Location</text>
              <text class="scooter-info-value">{{ scooter.location || '-' }}</text>
            </view>
          </view>

          <view class="scooter-card-actions">
            <text class="action-btn edit-btn" @click="showEditForm(scooter, index)">Edit</text>
            <text class="action-btn delete-btn" @click="handleDelete(scooter, index)">Delete</text>
          </view>
        </view>
      </view>
    </view>

    <!-- Add/Edit Modal -->
    <view v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <view class="modal-content">
        <text class="modal-title">{{ isEdit ? 'Edit Scooter' : 'Add Scooter' }}</text>

        <view class="input-group">
          <text class="input-label">Scooter Code</text>
          <input
            class="input-field"
            v-model="form.scooterCode"
            placeholder="e.g. SC001"
            :disabled="isEdit"
          />
        </view>

        <view class="input-group">
          <text class="input-label">Status</text>
          <picker
            :range="statusOptions"
            :value="statusIndex"
            @change="onStatusChange"
          >
            <view class="input-field picker-field">
              <text>{{ form.status || 'Select status' }}</text>
            </view>
          </picker>
        </view>

        <view class="input-group">
          <text class="input-label">Location</text>
          <input
            class="input-field"
            v-model="form.location"
            placeholder="e.g. Campus North Gate"
          />
        </view>

        <view class="modal-buttons">
          <button class="btn-outline modal-btn" @click="closeModal">Cancel</button>
          <button class="btn-primary modal-btn" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? 'Update' : 'Add' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { addScooter, updateScooter, deleteScooter } from '@/api/admin'
import { clearAll, getUserRole } from '@/utils/auth'

export default {
  data() {
    return {
      scooters: [],
      showModal: false,
      isEdit: false,
      editIndex: -1,
      form: {
        id: null,
        scooterCode: '',
        status: 'AVAILABLE',
        location: ''
      },
      statusOptions: ['AVAILABLE', 'UNAVAILABLE'],
      statusIndex: 0,
      submitting: false
    }
  },
  onLoad() {
    if (getUserRole() !== 'MANAGER') {
      uni.showToast({ title: 'Admin access required', icon: 'none' })
      setTimeout(() => {
        uni.navigateBack()
      }, 1000)
    }
  },
  methods: {
    showAddForm() {
      this.isEdit = false
      this.editIndex = -1
      this.form = { id: null, scooterCode: '', status: 'AVAILABLE', location: '' }
      this.statusIndex = 0
      this.showModal = true
    },
    showEditForm(scooter, index) {
      this.isEdit = true
      this.editIndex = index
      this.form = { ...scooter }
      this.statusIndex = this.statusOptions.indexOf(scooter.status)
      if (this.statusIndex < 0) this.statusIndex = 0
      this.showModal = true
    },
    closeModal() {
      this.showModal = false
    },
    onStatusChange(e) {
      this.statusIndex = e.detail.value
      this.form.status = this.statusOptions[this.statusIndex]
    },
    async handleSubmit() {
      if (!this.form.scooterCode) {
        uni.showToast({ title: 'Please enter scooter code', icon: 'none' })
        return
      }

      this.submitting = true
      try {
        if (this.isEdit) {
          await updateScooter({
            id: this.form.id,
            scooterCode: this.form.scooterCode,
            status: this.form.status,
            location: this.form.location
          })
          this.scooters[this.editIndex] = { ...this.form }
          uni.showToast({ title: 'Scooter updated!', icon: 'success' })
        } else {
          await addScooter({
            scooterCode: this.form.scooterCode,
            status: this.form.status,
            location: this.form.location
          })
          this.scooters.push({ ...this.form })
          uni.showToast({ title: 'Scooter added!', icon: 'success' })
        }
        this.closeModal()
      } catch (e) {
        // error toast handled by request.js
      } finally {
        this.submitting = false
      }
    },
    handleDelete(scooter, index) {
      if (!scooter.id) {
        uni.showToast({ title: 'Cannot delete: no server ID', icon: 'none' })
        return
      }
      uni.showModal({
        title: 'Delete Scooter',
        content: `Delete scooter ${scooter.scooterCode}?`,
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteScooter(scooter.id)
              this.scooters.splice(index, 1)
              uni.showToast({ title: 'Scooter deleted!', icon: 'success' })
            } catch (e) {
              // error toast handled by request.js
            }
          }
        }
      })
    },
    handleLogout() {
      uni.showModal({
        title: 'Logout',
        content: 'Exit admin portal?',
        success: (res) => {
          if (res.confirm) {
            clearAll()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background-color: #f5f7f5;
}

.admin-header {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  padding: 40rpx;
  padding-bottom: 50rpx;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-header .page-title {
  color: #ffffff;
  font-size: 38rpx;
}

.header-logout {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.7);
  padding: 10rpx 24rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.3);
  border-radius: 30rpx;
}

.scooter-count {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 12rpx;
}

.add-btn {
  margin-top: -20rpx;
  margin-bottom: 30rpx;
}

.empty-icon {
  font-size: 80rpx;
  margin-bottom: 16rpx;
}

.scooter-list {
  margin-top: 10rpx;
}

.scooter-card {
  padding: 0;
  overflow: hidden;
}

.scooter-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  border-bottom: 1rpx solid #f0f0f0;
}

.scooter-code-badge {
  background-color: #e8f5e9;
  padding: 8rpx 20rpx;
  border-radius: 8rpx;
}

.scooter-code {
  font-size: 28rpx;
  font-weight: 600;
  color: #07c160;
}

.scooter-card-body {
  padding: 20rpx 30rpx;
}

.scooter-info-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
}

.scooter-info-label {
  font-size: 26rpx;
  color: #999999;
}

.scooter-info-value {
  font-size: 26rpx;
  color: #333333;
}

.scooter-card-actions {
  display: flex;
  border-top: 1rpx solid #f0f0f0;
}

.action-btn {
  flex: 1;
  text-align: center;
  padding: 24rpx 0;
  font-size: 28rpx;
  font-weight: 500;
}

.edit-btn {
  color: #07c160;
  border-right: 1rpx solid #f0f0f0;
}

.delete-btn {
  color: #ee0a24;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-content {
  width: 85%;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 48rpx 36rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #333333;
  margin-bottom: 36rpx;
  text-align: center;
}

.picker-field {
  display: flex;
  align-items: center;
}

.modal-buttons {
  display: flex;
  gap: 20rpx;
  margin-top: 36rpx;
}

.modal-btn {
  flex: 1;
  height: 80rpx;
  line-height: 80rpx;
  font-size: 30rpx;
}
</style>
