<template>
  <div class="scooters-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Scooter Management</h2>
        <p class="page-copy">Manage Sprint 3 scooter modes, store assignments, lock states, and scan-ride coordinates.</p>
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

    <el-card>
      <div class="table-scroll">
        <el-table :data="scooters" stripe style="width: 100%" v-loading="tableLoading">
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
import { computed, onMounted, reactive, ref } from 'vue'
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

const scooters = ref<ScooterDto[]>([])
const stores = ref<StoreDto[]>([])
const tableLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const resolving = ref(false)
const dialogFormRef = ref<FormInstance>()

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

onMounted(() => {
  bootstrapPage()
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

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions :deep(.el-button) {
    flex: 1;
    min-width: 0;
  }
}
</style>
