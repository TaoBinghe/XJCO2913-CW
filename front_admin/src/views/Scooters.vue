<template>
  <div class="scooters-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Scooter Management</h2>
        <p class="page-copy">Manage scooter modes, store assignments, lock states, and scan-ride coordinates.</p>
      </div>
      <div class="header-actions">
        <el-button :loading="tableLoading" @click="fetchScooters">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="primary" @click="openAddDialog">
          <el-icon><Plus /></el-icon>
          Add Scooter
        </el-button>
      </div>
    </div>

    <el-card class="scan-map-card" v-loading="tableLoading">
      <template #header>
        <div class="map-card-header">
          <div>
            <h3>Scan Ride Map Monitor</h3>
            <p>Live coordinate layer for all scan-ride scooters with valid positions.</p>
          </div>
          <div class="map-header-actions">
            <el-tag type="success" effect="plain">{{ scanRideScooters.length }} mapped</el-tag>
            <el-button size="small" plain @click="fitMapToScooters">
              Fit View
            </el-button>
          </div>
        </div>
      </template>

      <div class="scan-map-layout">
        <div class="campus-map-shell">
          <div ref="mapContainerRef" class="amap-container"></div>
          <div v-if="mapLoading" class="map-overlay">
            <el-icon class="is-loading"><Refresh /></el-icon>
            <span>Loading map...</span>
          </div>
          <div v-else-if="mapError" class="map-overlay map-overlay-error">
            <span>{{ mapError }}</span>
          </div>
          <div v-else-if="scanRideScooters.length === 0" class="map-overlay">
            <el-empty description="No scan-ride scooters with coordinates." />
          </div>
        </div>

        <aside class="map-side">
          <div class="monitor-stats">
            <div class="monitor-stat">
              <span>Total</span>
              <strong>{{ scanRideTotalCount }}</strong>
            </div>
            <div class="monitor-stat">
              <span>Mapped</span>
              <strong>{{ scanRideScooters.length }}</strong>
            </div>
            <div class="monitor-stat">
              <span>Available</span>
              <strong>{{ scanRideAvailableCount }}</strong>
            </div>
            <div class="monitor-stat">
              <span>In Use</span>
              <strong>{{ scanRideInUseCount }}</strong>
            </div>
          </div>

          <div v-if="selectedMapScooter" class="selected-scooter">
            <div class="selected-heading">
              <div>
                <span>Selected scooter</span>
                <strong>{{ selectedMapScooter.scooterCode }}</strong>
              </div>
              <el-tag :type="statusTagType(selectedMapScooter.status)">
                {{ selectedMapScooter.status }}
              </el-tag>
            </div>
            <div class="detail-row">
              <span>Lock</span>
              <strong>{{ selectedMapScooter.lockStatus || '-' }}</strong>
            </div>
            <div class="detail-row">
              <span>Location</span>
              <strong>{{ selectedMapScooter.location || selectedMapScooter.storeAddress || '-' }}</strong>
            </div>
            <div class="detail-row">
              <span>Coordinates</span>
              <strong>{{ formatCoordinate(selectedMapScooter.longitude) }}, {{ formatCoordinate(selectedMapScooter.latitude) }}</strong>
            </div>
            <div class="detail-row">
              <span>Updated</span>
              <strong>{{ formatDateTime(selectedMapScooter.updatedAt) }}</strong>
            </div>
            <el-button class="center-map-button" type="primary" plain size="small" @click="focusMapScooter(selectedMapScooter)">
              Center on map
            </el-button>
          </div>

          <div class="map-legend">
            <span><i class="legend-dot status-available"></i>Available</span>
            <span><i class="legend-dot status-in-use"></i>In use</span>
            <span><i class="legend-dot status-maintenance"></i>Maintenance</span>
            <span><i class="legend-dot status-disabled"></i>Disabled</span>
          </div>
        </aside>
      </div>
    </el-card>

    <el-card>
      <div class="table-scroll">
        <el-table :data="paginatedScooters" stripe style="width: 100%" v-loading="tableLoading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="scooterCode" label="Scooter Code" min-width="150">
            <template #default="{ row }">
              <el-tag type="success" effect="plain">{{ row.scooterCode }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="rentalMode" label="Rental Mode" width="150">
            <template #default="{ row }">
              <el-tag :type="row.rentalMode === 'SCAN_RIDE' ? 'warning' : 'primary'" effect="plain">
                {{ row.rentalMode }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="storeName" label="Store" min-width="180">
            <template #default="{ row }">
              <span>{{ row.storeName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="Status" width="140">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lockStatus" label="Lock" width="130">
            <template #default="{ row }">
              <el-tag :type="row.lockStatus === 'UNLOCKED' ? 'success' : 'info'" effect="plain">
                {{ row.lockStatus }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="location" label="Location" min-width="220">
            <template #default="{ row }">
              <span>{{ row.location || row.storeAddress || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Coordinates" min-width="220">
            <template #default="{ row }">
              <div class="coordinate-cell">
                <span>{{ formatCoordinate(row.longitude) }}</span>
                <span>{{ formatCoordinate(row.latitude) }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="Updated At" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="190" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openEditDialog(row)">
                <el-icon><Edit /></el-icon>
                Edit
              </el-button>
              <el-button type="danger" link @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
                Delete
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="scooters.length === 0 && !tableLoading" class="empty-tip">
        <el-empty description="No scooters available yet." />
      </div>

      <div v-else class="pagination-bar">
        <span class="pagination-summary">Total {{ scooters.length }} scooters</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          :total="scooters.length"
          layout="sizes, prev, pager, next, jumper"
          background
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Scooter' : 'Add Scooter'"
      width="min(720px, 94vw)"
      :close-on-click-modal="false"
      @close="resetDialog"
    >
      <el-form
        ref="dialogFormRef"
        :model="dialogForm"
        :rules="dialogRules"
        label-position="top"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Scooter Code" prop="scooterCode">
              <el-input v-model="dialogForm.scooterCode" placeholder="e.g. SC301" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Rental Mode" prop="rentalMode">
              <el-select v-model="dialogForm.rentalMode" style="width: 100%">
                <el-option label="STORE_PICKUP" value="STORE_PICKUP" />
                <el-option label="SCAN_RIDE" value="SCAN_RIDE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Status" prop="status">
              <el-select v-model="dialogForm.status" style="width: 100%">
                <el-option label="AVAILABLE" value="AVAILABLE" />
                <el-option label="IN_USE" value="IN_USE" />
                <el-option label="MAINTENANCE" value="MAINTENANCE" />
                <el-option label="DISABLED" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Lock Status" prop="lockStatus">
              <el-select v-model="dialogForm.lockStatus" style="width: 100%">
                <el-option label="LOCKED" value="LOCKED" />
                <el-option label="UNLOCKED" value="UNLOCKED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="isStorePickupMode">
          <el-form-item label="Store" prop="storeId">
            <el-select v-model="dialogForm.storeId" filterable placeholder="Select a store" style="width: 100%">
              <el-option
                v-for="store in stores"
                :key="store.id"
                :label="store.name"
                :value="store.id"
              />
            </el-select>
          </el-form-item>

          <div class="mode-panel">
            <div class="mode-panel-title">Store snapshot</div>
            <div class="mode-panel-copy">
              Store pickup scooters inherit their address and coordinates from the selected store.
            </div>
            <div v-if="selectedStore" class="mode-panel-meta">
              <div><strong>Address:</strong> {{ selectedStore.address || '-' }}</div>
              <div><strong>Coordinates:</strong> {{ formatCoordinate(selectedStore.longitude) }}, {{ formatCoordinate(selectedStore.latitude) }}</div>
            </div>
          </div>
        </template>

        <template v-else>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="Longitude" prop="longitude">
                <el-input-number
                  v-model="dialogForm.longitude"
                  :min="-180"
                  :max="180"
                  :precision="6"
                  :step="0.000001"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="Latitude" prop="latitude">
                <el-input-number
                  v-model="dialogForm.latitude"
                  :min="-90"
                  :max="90"
                  :precision="6"
                  :step="0.000001"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="Location Text" prop="location">
            <el-input
              v-model="dialogForm.location"
              type="textarea"
              :rows="2"
              placeholder="Optional location name for scan-ride display"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" plain :loading="resolving" @click="handleResolveLocation">
              Resolve Location From Coordinates
            </el-button>
          </el-form-item>

          <div class="mode-panel">
            <div class="mode-panel-title">Scan ride location</div>
            <div class="mode-panel-copy">
              Coordinates are required for map placement. The resolve action can fill the location text for you.
            </div>
          </div>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? 'Save Changes' : 'Create Scooter' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  addScooter,
  deleteScooter,
  listScooters,
  listStores,
  resolveLocationFromCoordinates,
  updateScooter,
  type ScooterDto,
  type StoreDto
} from '@/api/admin'
import { formatDateTime } from '@/utils/admin-display'

interface ScooterFormState {
  id?: number
  scooterCode: string
  rentalMode: string
  storeId: number | null
  status: string
  lockStatus: string
  location: string
  longitude: number | null
  latitude: number | null
}

const DEFAULT_CENTER = {
  latitude: 30.76732,
  longitude: 103.98212
}
const AMAP_SCRIPT_ID = 'greengo-admin-amap-sdk'
const DEFAULT_AMAP_KEY = '183a163e2b815dd72eda8f860692640b'
const AMAP_KEY = (import.meta.env.VITE_ADMIN_AMAP_KEY || DEFAULT_AMAP_KEY).trim()
const AMAP_SECURITY_CODE = (import.meta.env.VITE_ADMIN_AMAP_SECURITY_CODE || '').trim()
const SCOOTER_LOGO_URL = new URL('static/logo.png', document.baseURI).toString()

const scooters = ref<ScooterDto[]>([])
const stores = ref<StoreDto[]>([])
const tableLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const mapContainerRef = ref<HTMLElement | null>(null)
const mapLoading = ref(false)
const mapReady = ref(false)
const mapError = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const resolving = ref(false)
const dialogFormRef = ref<FormInstance>()
const selectedMapScooterKey = ref('')
let amapInstance: any = null
let amapInfoWindow: any = null
let amapMarkers: any[] = []

const dialogForm = reactive<ScooterFormState>({
  id: undefined,
  scooterCode: '',
  rentalMode: 'STORE_PICKUP',
  storeId: null,
  status: 'AVAILABLE',
  lockStatus: 'LOCKED',
  location: '',
  longitude: null,
  latitude: null
})

const isStorePickupMode = computed(() => dialogForm.rentalMode === 'STORE_PICKUP')
const selectedStore = computed(() => stores.value.find(store => store.id === dialogForm.storeId) || null)
const scanRideTotalCount = computed(() => scooters.value.filter(scooter => scooter.rentalMode === 'SCAN_RIDE').length)
const scanRideScooters = computed(() => scooters.value.filter(scooter => (
  scooter.rentalMode === 'SCAN_RIDE'
  && hasValidCoordinates(scooter.longitude, scooter.latitude)
)))
const scanRideAvailableCount = computed(() => scanRideScooters.value.filter(scooter => scooter.status === 'AVAILABLE').length)
const scanRideInUseCount = computed(() => scanRideScooters.value.filter(scooter => scooter.status === 'IN_USE').length)
const paginatedScooters = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return scooters.value.slice(start, start + pageSize.value)
})
const maxPage = computed(() => Math.max(Math.ceil(scooters.value.length / pageSize.value), 1))
const selectedMapScooter = computed(() => {
  const selected = scanRideScooters.value.find(scooter => scooterKey(scooter) === selectedMapScooterKey.value)
  return selected || scanRideScooters.value[0] || null
})

const dialogRules: FormRules = {
  scooterCode: [{ required: true, message: 'Please enter scooter code', trigger: 'blur' }],
  rentalMode: [{ required: true, message: 'Please select a rental mode', trigger: 'change' }],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }],
  lockStatus: [{ required: true, message: 'Please select lock status', trigger: 'change' }],
  storeId: [{
    validator: (_rule, value, callback) => {
      if (dialogForm.rentalMode === 'STORE_PICKUP' && !value) {
        callback(new Error('Please select a store for store pickup scooters'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  longitude: [{
    validator: (_rule, value, callback) => {
      if (dialogForm.rentalMode === 'SCAN_RIDE' && (value == null || Number.isNaN(value))) {
        callback(new Error('Please enter a longitude for scan ride scooters'))
        return
      }
      callback()
    },
    trigger: 'change'
  }],
  latitude: [{
    validator: (_rule, value, callback) => {
      if (dialogForm.rentalMode === 'SCAN_RIDE' && (value == null || Number.isNaN(value))) {
        callback(new Error('Please enter a latitude for scan ride scooters'))
        return
      }
      callback()
    },
    trigger: 'change'
  }]
}

function statusTagType(status: string) {
  const normalized = String(status || '').toUpperCase()
  if (normalized === 'AVAILABLE') return 'success'
  if (normalized === 'IN_USE') return 'primary'
  if (normalized === 'MAINTENANCE') return 'warning'
  return 'info'
}

function formatCoordinate(value: number | null | undefined) {
  return value == null ? '-' : Number(value).toFixed(6)
}

function hasValidCoordinates(longitude: number | null, latitude: number | null) {
  if (longitude == null || latitude == null) return false
  return longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90
}

function scooterKey(scooter: ScooterDto) {
  return String(scooter.id ?? scooter.scooterCode)
}

function escapeHtml(value: unknown) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => {
    const entities: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    }
    return entities[char] || char
  })
}

function scooterStatusClass(scooter: ScooterDto) {
  return String(scooter.status || 'unknown').toLowerCase().replace(/_/g, '-')
}

function loadAmapSdk() {
  const win = window as any
  if (win.AMap) {
    return Promise.resolve(win.AMap)
  }

  if (!AMAP_KEY) {
    return Promise.reject(new Error('AMap key is not configured.'))
  }

  const existingScript = document.getElementById(AMAP_SCRIPT_ID) as HTMLScriptElement | null
  if (existingScript) {
    return new Promise((resolve, reject) => {
      existingScript.addEventListener('load', () => resolve((window as any).AMap), { once: true })
      existingScript.addEventListener('error', () => reject(new Error('AMap script failed to load.')), { once: true })
    })
  }

  if (AMAP_SECURITY_CODE) {
    win._AMapSecurityConfig = {
      securityJsCode: AMAP_SECURITY_CODE
    }
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.id = AMAP_SCRIPT_ID
    script.async = true
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(AMAP_KEY)}&plugin=AMap.Scale,AMap.ToolBar`
    script.onload = () => resolve((window as any).AMap)
    script.onerror = () => reject(new Error('AMap script failed to load.'))
    document.head.appendChild(script)
  })
}

async function initAdminMap() {
  await nextTick()
  if (!mapContainerRef.value || amapInstance) return

  mapLoading.value = true
  mapError.value = ''
  try {
    const AMap = await loadAmapSdk()
    amapInstance = new AMap.Map(mapContainerRef.value, {
      center: [DEFAULT_CENTER.longitude, DEFAULT_CENTER.latitude],
      zoom: 16,
      viewMode: '2D',
      resizeEnable: true
    })
    amapInfoWindow = new AMap.InfoWindow({
      offset: new AMap.Pixel(0, -34)
    })
    amapInstance.addControl(new AMap.Scale())
    amapInstance.addControl(new AMap.ToolBar({ position: 'RB' }))
    mapReady.value = true
    renderMapMarkers(true)
  } catch {
    mapError.value = 'Map failed to load. Please check the AMap key or domain settings.'
  } finally {
    mapLoading.value = false
  }
}

function clearMapMarkers() {
  if (!amapInstance || amapMarkers.length === 0) {
    amapMarkers = []
    return
  }

  amapInstance.remove(amapMarkers)
  amapMarkers = []
}

function buildMarkerContent(scooter: ScooterDto) {
  const selectedClass = selectedMapScooter.value && scooterKey(selectedMapScooter.value) === scooterKey(scooter)
    ? ' is-selected'
    : ''
  return `
    <div class="amap-scooter-marker marker-${scooterStatusClass(scooter)}${selectedClass}">
      <img class="amap-scooter-icon" src="${SCOOTER_LOGO_URL}" alt="" />
      <span class="amap-scooter-code">${escapeHtml(scooter.scooterCode)}</span>
    </div>
  `
}

function buildInfoWindowContent(scooter: ScooterDto) {
  return `
    <div class="amap-scooter-info">
      <strong>${escapeHtml(scooter.scooterCode)}</strong>
      <span>${escapeHtml(scooter.status)} · ${escapeHtml(scooter.lockStatus || 'LOCKED')}</span>
      <small>${escapeHtml(scooter.location || scooter.storeAddress || 'Location unavailable')}</small>
    </div>
  `
}

function renderMapMarkers(shouldFit = false) {
  if (!amapInstance || !(window as any).AMap) return

  const AMap = (window as any).AMap
  clearMapMarkers()

  amapMarkers = scanRideScooters.value.map((scooter) => {
    const marker = new AMap.Marker({
      position: [Number(scooter.longitude), Number(scooter.latitude)],
      title: scooter.scooterCode,
      content: buildMarkerContent(scooter),
      offset: new AMap.Pixel(-24, -58),
      zIndex: selectedMapScooter.value && scooterKey(selectedMapScooter.value) === scooterKey(scooter) ? 120 : 100
    })
    marker.on('click', () => {
      selectMapScooter(scooter)
      openMapInfoWindow(scooter)
    })
    marker.setMap(amapInstance)
    return marker
  })

  if (shouldFit) {
    fitMapToScooters()
  }
}

function openMapInfoWindow(scooter: ScooterDto) {
  if (!amapInstance || !amapInfoWindow || !hasValidCoordinates(scooter.longitude, scooter.latitude)) return

  amapInfoWindow.setContent(buildInfoWindowContent(scooter))
  amapInfoWindow.open(amapInstance, [Number(scooter.longitude), Number(scooter.latitude)])
}

function focusMapScooter(scooter: ScooterDto | null) {
  if (!amapInstance || !scooter || !hasValidCoordinates(scooter.longitude, scooter.latitude)) return

  amapInstance.setZoomAndCenter(18, [Number(scooter.longitude), Number(scooter.latitude)])
  openMapInfoWindow(scooter)
}

function fitMapToScooters() {
  if (!amapInstance) return

  if (amapMarkers.length === 0) {
    amapInstance.setZoomAndCenter(16, [DEFAULT_CENTER.longitude, DEFAULT_CENTER.latitude])
    return
  }

  if (amapMarkers.length === 1) {
    const scooter = scanRideScooters.value[0]
    if (scooter) {
      amapInstance.setZoomAndCenter(17, [Number(scooter.longitude), Number(scooter.latitude)])
    }
    return
  }

  amapInstance.setFitView(amapMarkers, false, [64, 64, 64, 64], 17)
}

function selectMapScooter(scooter: ScooterDto) {
  selectedMapScooterKey.value = scooterKey(scooter)
}

async function bootstrapPage() {
  await Promise.all([fetchScooters(), fetchStores()])
}

async function fetchScooters() {
  tableLoading.value = true
  try {
    const res = await listScooters()
    scooters.value = res.data || []
  } catch {
    scooters.value = []
  } finally {
    tableLoading.value = false
  }
}

async function fetchStores() {
  try {
    const res = await listStores()
    stores.value = res.data || []
  } catch {
    stores.value = []
  }
}

function resetDialog() {
  dialogForm.id = undefined
  dialogForm.scooterCode = ''
  dialogForm.rentalMode = 'STORE_PICKUP'
  dialogForm.storeId = null
  dialogForm.status = 'AVAILABLE'
  dialogForm.lockStatus = 'LOCKED'
  dialogForm.location = ''
  dialogForm.longitude = null
  dialogForm.latitude = null
  dialogFormRef.value?.clearValidate()
}

function openAddDialog() {
  isEdit.value = false
  resetDialog()
  dialogVisible.value = true
}

function openEditDialog(row: ScooterDto) {
  isEdit.value = true
  dialogForm.id = row.id
  dialogForm.scooterCode = row.scooterCode
  dialogForm.rentalMode = row.rentalMode || 'STORE_PICKUP'
  dialogForm.storeId = row.storeId ?? null
  dialogForm.status = row.status
  dialogForm.lockStatus = row.lockStatus || 'LOCKED'
  dialogForm.location = row.location || ''
  dialogForm.longitude = row.longitude
  dialogForm.latitude = row.latitude
  dialogVisible.value = true
}

async function handleResolveLocation() {
  if (!hasValidCoordinates(dialogForm.longitude, dialogForm.latitude)) {
    ElMessage.warning('Please enter valid longitude and latitude values first.')
    return
  }

  resolving.value = true
  try {
    const res = await resolveLocationFromCoordinates(dialogForm.longitude!, dialogForm.latitude!)
    dialogForm.location = res.data || dialogForm.location
    ElMessage.success('Location resolved successfully.')
  } catch {
    // request interceptor handles backend messages
  } finally {
    resolving.value = false
  }
}

async function handleSubmit() {
  if (!dialogFormRef.value) return

  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return

    if (dialogForm.rentalMode === 'SCAN_RIDE' && !hasValidCoordinates(dialogForm.longitude, dialogForm.latitude)) {
      ElMessage.warning('Please enter valid coordinates for scan ride scooters.')
      return
    }

    submitting.value = true
    try {
      const payload = {
        scooterCode: dialogForm.scooterCode.trim(),
        rentalMode: dialogForm.rentalMode,
        status: dialogForm.status,
        lockStatus: dialogForm.lockStatus,
        storeId: dialogForm.rentalMode === 'STORE_PICKUP' ? dialogForm.storeId : null,
        location: dialogForm.rentalMode === 'SCAN_RIDE'
          ? (dialogForm.location.trim() || null)
          : null,
        longitude: dialogForm.rentalMode === 'SCAN_RIDE' ? dialogForm.longitude : null,
        latitude: dialogForm.rentalMode === 'SCAN_RIDE' ? dialogForm.latitude : null
      }

      if (isEdit.value && dialogForm.id != null) {
        await updateScooter({
          id: dialogForm.id,
          ...payload
        })
        ElMessage.success('Scooter updated successfully.')
      } else {
        await addScooter(payload)
        ElMessage.success('Scooter created successfully.')
      }

      dialogVisible.value = false
      await bootstrapPage()
    } catch {
      // request interceptor handles backend messages
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: ScooterDto) {
  if (!row.id) return

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete scooter ${row.scooterCode}?`,
      'Delete Scooter',
      {
        confirmButtonText: 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }
    )

    await deleteScooter(row.id)
    ElMessage.success('Scooter deleted successfully.')
    await bootstrapPage()
  } catch (error: any) {
    if (error !== 'cancel') {
      // request interceptor handles backend messages
    }
  }
}

watch(scanRideScooters, () => {
  if (mapReady.value) {
    renderMapMarkers(true)
  }
})

watch(selectedMapScooterKey, () => {
  if (mapReady.value) {
    renderMapMarkers(false)
  }
})

watch(maxPage, (value) => {
  if (currentPage.value > value) {
    currentPage.value = value
  }
})

onMounted(async () => {
  await Promise.all([
    bootstrapPage(),
    initAdminMap()
  ])
})

onBeforeUnmount(() => {
  clearMapMarkers()
  if (amapInfoWindow) {
    amapInfoWindow.close()
    amapInfoWindow = null
  }
  if (amapInstance) {
    amapInstance.destroy()
    amapInstance = null
  }
  mapReady.value = false
})
</script>

<style scoped>
.scooters-page {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-heading {
  font-size: 24px;
  font-weight: 700;
  color: #1d1e1f;
}

.page-copy {
  margin-top: 8px;
  color: #6b7280;
  line-height: 1.6;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.scan-map-card {
  margin-bottom: 20px;
}

.map-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.map-card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.map-card-header p {
  margin: 6px 0 0;
  color: #6b7280;
  line-height: 1.5;
}

.map-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.scan-map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: stretch;
  min-height: 460px;
}

.campus-map-shell {
  position: relative;
  height: 100%;
  min-height: 460px;
  border-radius: 16px;
  border: 1px solid #dbe3ef;
  overflow: hidden;
  background: #eef3f8;
}

.amap-container {
  width: 100%;
  height: 100%;
  min-height: 460px;
}

.map-overlay {
  position: absolute;
  inset: 0;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: rgba(248, 250, 252, 0.86);
  color: #475569;
  font-size: 14px;
  font-weight: 700;
}

.map-overlay-error {
  color: #b91c1c;
  background: rgba(254, 242, 242, 0.92);
}

.center-map-button {
  margin-top: 14px;
  width: 100%;
}

:deep(.amap-scooter-marker) {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 48px;
  transform-origin: center bottom;
  transition: transform 0.18s ease;
}

:deep(.amap-scooter-marker:hover),
:deep(.amap-scooter-marker.is-selected) {
  transform: scale(1.08);
}

:deep(.amap-scooter-icon) {
  width: 36px;
  height: 36px;
  padding: 5px;
  border: 3px solid #16a34a;
  border-radius: 50%;
  background: #fff;
  object-fit: contain;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.22);
}

:deep(.amap-scooter-code) {
  max-width: 78px;
  margin-top: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: #111827;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  box-shadow: 0 8px 16px rgba(15, 23, 42, 0.14);
}

:deep(.amap-scooter-marker.is-selected .amap-scooter-icon) {
  outline: 4px solid rgba(20, 83, 45, 0.22);
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.3);
}

:deep(.marker-available .amap-scooter-icon) {
  border-color: #16a34a;
}

:deep(.marker-in-use .amap-scooter-icon) {
  border-color: #2563eb;
}

:deep(.marker-maintenance .amap-scooter-icon) {
  border-color: #d97706;
}

:deep(.marker-disabled .amap-scooter-icon) {
  border-color: #64748b;
}

:deep(.marker-unknown .amap-scooter-icon) {
  border-color: #6b7280;
}

:deep(.amap-scooter-info) {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 180px;
  color: #111827;
  line-height: 1.35;
}

:deep(.amap-scooter-info strong) {
  font-size: 15px;
}

:deep(.amap-scooter-info span) {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

:deep(.amap-scooter-info small) {
  color: #64748b;
  font-size: 12px;
}

.map-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.monitor-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.monitor-stat,
.selected-scooter,
.map-legend {
  border-radius: 14px;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
}

.monitor-stat {
  padding: 14px;
}

.monitor-stat span,
.selected-heading span,
.detail-row span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.monitor-stat strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
  line-height: 1;
}

.selected-scooter {
  padding: 16px;
}

.selected-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.selected-heading strong {
  display: block;
  margin-top: 5px;
  color: #111827;
  font-size: 18px;
}

.detail-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid #e5e7eb;
}

.detail-row strong {
  color: #111827;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.map-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  color: #475569;
  font-size: 13px;
}

.map-legend span {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-available {
  background: #16a34a;
}

.status-in-use {
  background: #2563eb;
}

.status-maintenance {
  background: #d97706;
}

.status-disabled {
  background: #64748b;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 1240px;
}

.coordinate-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
}

.mode-panel {
  margin-top: 4px;
  padding: 16px;
  border-radius: 12px;
  background: #f6f8fb;
  border: 1px solid #dbe3ef;
}

.mode-panel-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.mode-panel-copy {
  margin-top: 6px;
  color: #4b5563;
  line-height: 1.6;
}

.mode-panel-meta {
  margin-top: 10px;
  color: #111827;
  line-height: 1.7;
}

.empty-tip {
  padding: 40px 0;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 18px;
}

.pagination-summary {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .map-card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .map-header-actions {
    width: 100%;
  }

  .scan-map-layout {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .campus-map-shell {
    height: 360px;
    min-height: 360px;
  }

  .amap-container {
    height: 360px;
    min-height: 360px;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }

  .pagination-bar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
