<template>
  <view class="booking-screen">
    <map
      class="booking-fullscreen-map"
      :latitude="mapCenter.latitude"
      :longitude="mapCenter.longitude"
      :scale="fullscreenMapScale"
      :markers="mapMarkers"
      :circles="mapCircles"
      :polyline="routePolylines"
      :enable-scroll="true"
      :enable-zoom="true"
      @markertap="handleMarkerTap"
    >
      <cover-view class="booking-bottom-sheet">
        <template v-if="scooterLoading">
          <cover-view class="sheet-title">Loading scan scooters...</cover-view>
          <cover-view class="sheet-copy">Pulling the public map fleet right now.</cover-view>
        </template>

        <template v-else-if="!scooters.length">
          <cover-view class="sheet-title">No scan scooters available</cover-view>
          <cover-view class="sheet-copy">Try again later or switch to store pickup reservations.</cover-view>
        </template>

        <template v-else>
          <cover-view class="sheet-kicker">SCAN RIDE</cover-view>
          <cover-view class="sheet-title">{{ selectedScooter ? selectedScooter.scooterCode : 'Browse live scooters' }}</cover-view>
          <cover-view class="sheet-copy">
            {{ selectedScooter ? (selectedScooter.location || 'Location unavailable') : 'Tap a marker, scan a QR code, or type a scooter code above.' }}
          </cover-view>

          <cover-view v-if="selectedScooter" class="sheet-info-card">
            <cover-view class="sheet-info-row">
              <cover-view class="sheet-info-label">Status</cover-view>
              <cover-view class="sheet-info-value">{{ selectedScooter.status }}</cover-view>
            </cover-view>
            <cover-view class="sheet-info-row">
              <cover-view class="sheet-info-label">Lock</cover-view>
              <cover-view class="sheet-info-value">{{ selectedScooter.lockStatus || 'LOCKED' }}</cover-view>
            </cover-view>
            <cover-view v-if="routeInfo" class="sheet-info-row">
              <cover-view class="sheet-info-label">Walk</cover-view>
              <cover-view class="sheet-info-value">
                {{ formatDistance(routeInfo.distanceMeters) }} · {{ formatDuration(routeInfo.durationSeconds) }}
              </cover-view>
            </cover-view>
          </cover-view>

          <cover-view v-else class="sheet-note-card">
            <cover-view class="sheet-note-line">Live scooters: {{ availableCount }}</cover-view>
            <cover-view class="sheet-note-line">Manual entry: {{ manualCode || 'Type a scooter code to search or ride' }}</cover-view>
          </cover-view>

          <cover-view class="sheet-actions">
            <cover-view class="sheet-button sheet-button-secondary" @tap="handleScanCode">
              {{ scanning ? 'Scanning...' : 'Scan QR' }}
            </cover-view>
            <cover-view class="sheet-button sheet-button-secondary" @tap="handlePreviewRoute">
              {{ routeLoading ? 'Loading...' : 'Preview Route' }}
            </cover-view>
          </cover-view>

          <cover-view class="sheet-actions">
            <cover-view class="sheet-button sheet-button-secondary" @tap="recenterToUser">
              Recenter
            </cover-view>
            <cover-view
              class="sheet-button sheet-button-primary"
              :class="{ 'sheet-button-disabled': !canStartRide }"
              @tap="handleStartRide"
            >
              {{ startingRide ? 'Starting...' : startRideLabel }}
            </cover-view>
          </cover-view>
        </template>
      </cover-view>
    </map>

    <view class="booking-topbar" :style="bookingTopbarStyle">
      <view class="booking-back-button" @click="handlePageBack">
        <text class="booking-back-icon">‹</text>
      </view>
      <view class="booking-search-shell" :style="bookingSearchStyle">
        <input
          v-model="manualCode"
          class="booking-search-input"
          type="text"
          confirm-type="search"
          placeholder="Enter scooter code"
          placeholder-style="color: #8f9892"
          @confirm="handleFindScooter"
        />
        <view class="booking-search-button" @click="handleFindScooter">
          Find
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { startScanRide } from '@/api/booking'
import { getScooterList, getScooterRoute } from '@/api/scooter'
import { sortScooters } from '@/utils/booking'
import { getToken } from '@/utils/auth'
import {
  getCurrentLocationWithPermission,
  LOCATION_ERROR_CODES
} from '@/utils/location'

const DEFAULT_CENTER = {
  latitude: 30.76732,
  longitude: 103.98212
}
const USER_MARKER_ID = 900000001

function parseScooterCode(rawValue) {
  const rawText = String(rawValue || '').trim()
  if (!rawText) return ''

  const queryMatch = rawText.match(/[?&]scooterCode=([^&#]+)/i)
  if (queryMatch && queryMatch[1]) {
    return decodeURIComponent(queryMatch[1]).trim()
  }

  const tokenMatch = rawText.match(/([A-Za-z]{2,}[A-Za-z0-9_-]*)$/)
  if (tokenMatch && tokenMatch[1]) {
    return tokenMatch[1].trim()
  }

  return rawText
}

export default {
  data() {
    return {
      scooters: [],
      selectedScooterId: null,
      manualCode: '',
      scooterLoading: true,
      routeLoading: false,
      startingRide: false,
      scanning: false,
      routeInfo: null,
      routePolylines: [],
      userLocation: null,
      locatingUser: false,
      mapCenter: { ...DEFAULT_CENTER },
      mapScale: 16,
      topbarTop: '56px',
      topbarWidth: '280px'
    }
  },
  computed: {
    selectedScooter() {
      return this.scooters.find(scooter => scooter.id === this.selectedScooterId) || null
    },
    availableCount() {
      return this.scooters.filter(scooter => scooter.status === 'AVAILABLE').length
    },
    mapMarkers() {
      const scooterMarkers = this.scooters
        .filter(scooter => this.hasCoordinates(scooter))
        .map((scooter) => ({
          id: Number(scooter.id),
          latitude: Number(scooter.latitude),
          longitude: Number(scooter.longitude),
          iconPath: '/static/logo.png',
          width: this.selectedScooterId === scooter.id ? 34 : 28,
          height: this.selectedScooterId === scooter.id ? 34 : 28,
          alpha: scooter.status === 'AVAILABLE' ? 1 : 0.55,
          callout: {
            content: scooter.scooterCode,
            color: scooter.status === 'AVAILABLE' ? '#111111' : '#6f776a',
            fontSize: 11,
            borderRadius: 12,
            bgColor: this.selectedScooterId === scooter.id ? '#efff84' : '#ffffff',
            padding: 6,
            display: this.selectedScooterId === scooter.id ? 'ALWAYS' : 'BYCLICK'
          }
        }))

      if (!this.userLocation) {
        return scooterMarkers
      }

      return [
        {
          id: USER_MARKER_ID,
          latitude: this.userLocation.latitude,
          longitude: this.userLocation.longitude,
          iconPath: '/static/tab-my-active.png',
          width: 36,
          height: 36,
          zIndex: 99,
          anchor: { x: 0.5, y: 0.5 },
          callout: {
            content: 'You are here',
            color: '#111111',
            fontSize: 11,
            borderRadius: 12,
            bgColor: '#efff84',
            padding: 6,
            display: 'ALWAYS'
          }
        },
        ...scooterMarkers
      ]
    },
    mapCircles() {
      if (!this.selectedScooter || !this.hasCoordinates(this.selectedScooter)) {
        return []
      }

      return [
        {
          latitude: Number(this.selectedScooter.latitude),
          longitude: Number(this.selectedScooter.longitude),
          radius: 38,
          strokeWidth: 2,
          color: '#94c83d66',
          fillColor: '#efff8440'
        }
      ]
    },
    fullscreenMapScale() {
      return Math.max(this.mapScale, 16)
    },
    bookingTopbarStyle() {
      return {
        top: this.topbarTop
      }
    },
    bookingSearchStyle() {
      return {
        width: this.topbarWidth
      }
    },
    selectedRideCode() {
      return this.selectedScooter?.scooterCode || parseScooterCode(this.manualCode)
    },
    canStartRide() {
      return !!this.selectedRideCode && !this.startingRide
    },
    startRideLabel() {
      return this.selectedScooter
        ? `Ride ${this.selectedScooter.scooterCode}`
        : (this.manualCode ? 'Ride with Code' : 'Select or Type a Scooter')
    }
  },
  onLoad() {
    this.initTopLayout()
    this.locateCurrentUser()
    this.loadScooters()
  },
  methods: {
    initTopLayout() {
      let top = 56
      let width = 280

      try {
        const windowInfo = typeof uni.getWindowInfo === 'function'
          ? uni.getWindowInfo()
          : uni.getSystemInfoSync()
        const windowWidth = typeof windowInfo?.windowWidth === 'number' ? windowInfo.windowWidth : 375
        const screenPadding = Math.round((windowWidth * 24) / 750)
        const backButtonWidth = Math.round((windowWidth * 82) / 750)
        const backButtonGap = Math.round((windowWidth * 18) / 750)
        const leftReserved = screenPadding + backButtonWidth + backButtonGap

        const safeTop = typeof windowInfo?.safeAreaInsets?.top === 'number'
          ? windowInfo.safeAreaInsets.top
          : (typeof windowInfo?.statusBarHeight === 'number' ? windowInfo.statusBarHeight : 0)

        top = Math.max(top, safeTop + 20)

        const halfWindow = windowWidth / 2
        let rightLimit = windowWidth - screenPadding

        if (typeof uni.getMenuButtonBoundingClientRect === 'function') {
          const menuButtonRect = uni.getMenuButtonBoundingClientRect()
          if (menuButtonRect && typeof menuButtonRect.bottom === 'number') {
            top = Math.max(top, menuButtonRect.bottom + 12)
          }
          if (menuButtonRect && typeof menuButtonRect.left === 'number') {
            rightLimit = Math.min(rightLimit, menuButtonRect.left - 12)
          }
        }

        const centeredHalfSpan = Math.min(
          halfWindow - leftReserved,
          rightLimit - halfWindow
        )
        const maxCenteredWidth = Math.floor(Math.max(200, centeredHalfSpan * 2))
        const maxScreenWidth = windowWidth - (screenPadding * 2)
        width = Math.min(maxScreenWidth, maxCenteredWidth)
      } catch (e) {
        // Fallback dimensions are enough if platform metrics are unavailable.
      }

      this.topbarTop = `${top}px`
      this.topbarWidth = `${width}px`
    },
    async loadScooters() {
      this.scooterLoading = true
      try {
        const res = await getScooterList()
        this.scooters = sortScooters(res.data || [])
        if (!this.locatingUser && !this.userLocation) {
          this.focusAvailableScooter()
        }
      } catch (e) {
        this.scooters = []
      } finally {
        this.scooterLoading = false
      }
    },
    hasCoordinates(scooter) {
      return scooter
        && Number.isFinite(Number(scooter.longitude))
        && Number.isFinite(Number(scooter.latitude))
    },
    async locateCurrentUser() {
      this.locatingUser = true
      try {
        const location = await getCurrentLocationWithPermission({
          reasonTitle: 'Location permission needed',
          reasonContent: 'To show your position on the map, please enable location permission.',
          successHint: 'Location enabled. Please tap again.'
        })
        this.setUserLocation(location, true)
      } catch (e) {
        if (e?.code === LOCATION_ERROR_CODES.PERMISSION_JUST_ENABLED) {
          setTimeout(() => this.locateCurrentUser(), 500)
          return
        }
        if (e?.code === LOCATION_ERROR_CODES.LOCATION_UNAVAILABLE) {
          uni.showToast({ title: 'Could not get your current location', icon: 'none' })
        }
        if (!this.userLocation && this.scooters.length) {
          this.focusAvailableScooter()
        }
      } finally {
        this.locatingUser = false
      }
    },
    setUserLocation(location, shouldCenter = false) {
      if (!this.hasCoordinates(location)) {
        return
      }

      const userLocation = {
        latitude: Number(location.latitude),
        longitude: Number(location.longitude)
      }
      this.userLocation = userLocation

      if (shouldCenter) {
        this.mapCenter = { ...userLocation }
        this.mapScale = 17
      }
    },
    recenterToUser() {
      if (this.userLocation) {
        this.mapCenter = { ...this.userLocation }
        this.mapScale = 17
        return
      }

      this.locateCurrentUser()
    },
    focusAvailableScooter() {
      const focusScooter = this.selectedScooter && this.hasCoordinates(this.selectedScooter)
        ? this.selectedScooter
        : this.scooters.find(scooter => scooter.status === 'AVAILABLE' && this.hasCoordinates(scooter))
          || this.scooters.find(scooter => this.hasCoordinates(scooter))

      if (!focusScooter) {
        this.mapCenter = { ...DEFAULT_CENTER }
        return
      }

      this.selectScooter(focusScooter)
    },
    selectScooter(scooter) {
      this.selectedScooterId = scooter.id
      this.clearRoutePreview()
      if (this.hasCoordinates(scooter)) {
        this.mapCenter = {
          latitude: Number(scooter.latitude),
          longitude: Number(scooter.longitude)
        }
        this.mapScale = 18
      }
    },
    clearRoutePreview() {
      this.routeInfo = null
      this.routePolylines = []
    },
    handleMarkerTap(event) {
      if (Number(event.detail.markerId) === USER_MARKER_ID) {
        if (this.userLocation) {
          this.mapCenter = { ...this.userLocation }
          this.mapScale = 17
        }
        return
      }

      const scooter = this.scooters.find(item => Number(item.id) === Number(event.detail.markerId))
      if (scooter) {
        this.selectScooter(scooter)
      }
    },
    handlePageBack() {
      if (getCurrentPages().length > 1) {
        uni.navigateBack()
        return
      }
      uni.switchTab({ url: '/pages/index/index' })
    },
    handleFindScooter() {
      const code = parseScooterCode(this.manualCode)
      if (!code) {
        uni.showToast({ title: 'Please enter a scooter code', icon: 'none' })
        return
      }

      this.manualCode = code
      const scooter = this.scooters.find(item => String(item.scooterCode || '').toUpperCase() === code.toUpperCase())
      if (scooter) {
        this.selectScooter(scooter)
        return
      }

      uni.showToast({ title: 'Scooter not on the public map list. You can still try Ride with Code.', icon: 'none' })
    },
    ensureLoggedIn() {
      if (getToken()) {
        return true
      }
      uni.navigateTo({ url: '/pages/login/login' })
      return false
    },
    async handlePreviewRoute() {
      if (!this.selectedScooter) {
        uni.showToast({ title: 'Please choose a scooter on the map', icon: 'none' })
        return
      }
      if (!this.ensureLoggedIn()) {
        return
      }

      this.routeLoading = true
      try {
        const location = await getCurrentLocationWithPermission({
          reasonTitle: 'Location permission needed',
          reasonContent: 'To preview the walking route, please enable location permission in settings.',
          successHint: 'Location enabled. Please tap again.'
        })
        this.setUserLocation(location, false)
        const res = await getScooterRoute(
          this.selectedScooter.id,
          location.longitude,
          location.latitude
        )
        const route = res.data
        this.routeInfo = route
        this.routePolylines = [{
          points: (route.points || []).map(point => ({
            latitude: Number(point.latitude),
            longitude: Number(point.longitude)
          })),
          color: '#5d8c22',
          width: 6,
          arrowLine: true
        }]
      } catch (e) {
        this.clearRoutePreview()
        if (e?.code === LOCATION_ERROR_CODES.LOCATION_UNAVAILABLE) {
          uni.showToast({ title: 'Could not get your location for route preview', icon: 'none' })
        }
      } finally {
        this.routeLoading = false
      }
    },
    async startRideWithCode(code) {
      if (!code) {
        uni.showToast({ title: 'Please select or enter a scooter code', icon: 'none' })
        return
      }
      if (!this.ensureLoggedIn()) {
        return
      }
      if (this.startingRide) {
        return
      }

      this.startingRide = true
      try {
        const res = await startScanRide(code)
        uni.showToast({ title: 'Ride started', icon: 'success' })
        setTimeout(() => {
          uni.redirectTo({
            url: `/pages/order-detail/order-detail?bookingId=${res.data.id}`
          })
        }, 500)
      } catch (e) {
        // request.js handles backend errors
      } finally {
        this.startingRide = false
      }
    },
    handleStartRide() {
      this.startRideWithCode(this.selectedRideCode)
    },
    handleScanCode() {
      if (!this.ensureLoggedIn()) {
        return
      }
      if (this.scanning) {
        return
      }

      this.scanning = true
      uni.scanCode({
        success: ({ result }) => {
          const scooterCode = parseScooterCode(result)
          this.manualCode = scooterCode
          if (!scooterCode) {
            uni.showToast({ title: 'Could not parse a scooter code from the QR result', icon: 'none' })
            return
          }
          this.handleFindScooter()
          this.startRideWithCode(scooterCode)
        },
        fail: () => {
          uni.showToast({ title: 'QR scan cancelled', icon: 'none' })
        },
        complete: () => {
          this.scanning = false
        }
      })
    },
    formatDistance(distanceMeters) {
      const distance = Number(distanceMeters || 0)
      if (distance >= 1000) {
        return `${(distance / 1000).toFixed(1)} km`
      }
      return `${Math.round(distance)} m`
    },
    formatDuration(durationSeconds) {
      const duration = Number(durationSeconds || 0)
      if (duration >= 3600) {
        return `${Math.ceil(duration / 3600)} hr`
      }
      return `${Math.ceil(duration / 60)} min`
    }
  }
}
</script>

<style scoped>
.booking-screen {
  position: relative;
  min-height: 100vh;
  background: #dce8f3;
}

.booking-fullscreen-map {
  width: 100%;
  height: 100vh;
}

.booking-topbar {
  position: absolute;
  top: calc(56rpx + env(safe-area-inset-top));
  left: 0;
  right: 0;
  min-height: 82rpx;
  z-index: 10;
}

.booking-back-button {
  position: absolute;
  top: 0;
  left: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 82rpx;
  height: 82rpx;
  border-radius: 41rpx;
  background: rgba(255, 255, 255, 0.96);
  color: #111111;
}

.booking-back-icon {
  color: #111111;
  font-size: 48rpx;
  font-weight: 700;
  line-height: 1;
}

.booking-search-shell {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  height: 82rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.96);
  display: flex;
  align-items: center;
  padding: 0 12rpx 0 24rpx;
}

.booking-search-input {
  flex: 1;
  min-width: 0;
  height: 82rpx;
  color: #24311f;
  font-size: 24rpx;
}

.booking-search-button {
  min-width: 86rpx;
  height: 58rpx;
  padding: 0 18rpx;
  border-radius: 18rpx;
  background: #efff84;
  color: #111111;
  font-size: 24rpx;
  font-weight: 700;
  line-height: 58rpx;
  text-align: center;
}

.booking-bottom-sheet {
  position: absolute;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  padding: 30rpx 28rpx 24rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, 0.97);
}

.sheet-kicker {
  color: #89a54c;
  font-size: 22rpx;
  line-height: 1.2;
  letter-spacing: 2rpx;
  text-transform: uppercase;
}

.sheet-title {
  margin-top: 12rpx;
  color: #111111;
  font-size: 36rpx;
  font-weight: 700;
  line-height: 1.24;
}

.sheet-copy {
  margin-top: 10rpx;
  color: #6f776a;
  font-size: 24rpx;
  line-height: 1.5;
}

.sheet-info-card,
.sheet-note-card {
  margin-top: 22rpx;
  padding: 22rpx 22rpx 10rpx;
  border-radius: 24rpx;
  background: #f7f8f5;
}

.sheet-info-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding-bottom: 14rpx;
}

.sheet-info-label {
  width: 180rpx;
  color: #98a093;
  font-size: 22rpx;
  line-height: 1.5;
}

.sheet-info-value {
  flex: 1;
  min-width: 0;
  color: #111111;
  font-size: 24rpx;
  line-height: 1.5;
  text-align: right;
}

.sheet-note-line {
  padding-bottom: 12rpx;
  color: #6f776a;
  font-size: 24rpx;
}

.sheet-actions {
  display: flex;
  margin-top: 24rpx;
}

.sheet-button {
  flex: 1;
  height: 88rpx;
  border-radius: 28rpx;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 88rpx;
  text-align: center;
}

.sheet-button + .sheet-button {
  margin-left: 16rpx;
}

.sheet-button-secondary {
  background: #f3f5f1;
  color: #24311f;
}

.sheet-button-primary {
  background: #efff84;
  color: #111111;
}

.sheet-button-disabled {
  background: #edf0e8;
  color: #96a091;
}
</style>
