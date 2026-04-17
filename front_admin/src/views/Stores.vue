<template>
  <div class="stores-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Store Management</h2>
        <p class="page-copy">Create and maintain Sprint 3 pickup stores, inventory visibility, and map coordinates.</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadStores">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          Add Store
        </el-button>
      </div>
    </div>

    <el-card>
      <div class="table-scroll">
        <el-table :data="stores" stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="name" label="Store Name" min-width="180" />
          <el-table-column prop="status" label="Status" width="130">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="address" label="Address" min-width="260">
            <template #default="{ row }">
              <span>{{ row.address || '-' }}</span>
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
          <el-table-column label="Inventory" min-width="240">
            <template #default="{ row }">
              <div class="inventory-cell">
                <span>Bookable: {{ row.bookableInventory ?? 0 }}</span>
                <span>Current: {{ row.currentAvailableInventory ?? 0 }}</span>
                <span>Total: {{ row.totalInventory ?? 0 }}</span>
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
              <el-button type="danger" link @click="confirmDelete(row)">
                <el-icon><Delete /></el-icon>
                Delete
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="stores.length === 0 && !loading" class="empty-tip">
        <el-empty description="No stores available yet." />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Store' : 'Add Store'"
      width="min(680px, 94vw)"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Store Name" prop="name">
              <el-input v-model="form.name" placeholder="e.g. Xipu Campus Store" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="Status" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="ENABLED" value="ENABLED" />
                <el-option label="DISABLED" value="DISABLED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="Longitude" prop="longitude">
              <el-input-number
                v-model="form.longitude"
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
                v-model="form.latitude"
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

        <el-form-item label="Address (Optional)">
          <el-input
            v-model="form.address"
            type="textarea"
            :rows="2"
            placeholder="Leave blank to keep the current address or to let backend geocoding help on creation."
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" plain :loading="resolving" @click="handleResolveAddress">
            Resolve Address Preview
          </el-button>
        </el-form-item>

        <div class="helper-panel">
          <div class="helper-title">Address guidance</div>
          <div class="helper-copy">
            Coordinates are always required. You can type an address manually or use the resolve action to preview one from the backend geocoder.
          </div>
          <div v-if="resolvedAddress" class="helper-preview">
            <strong>Preview:</strong> {{ resolvedAddress }}
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ isEdit ? 'Save Changes' : 'Create Store' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createStore,
  deleteStore,
  listStores,
  resolveLocationFromCoordinates,
  updateStore,
  type StoreDto
} from '@/api/admin'
import { formatDateTime } from '@/utils/admin-display'

interface StoreFormState {
  id?: number
  name: string
  address: string
  longitude: number | null
  latitude: number | null
  status: string
}

const stores = ref<StoreDto[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const resolving = ref(false)
const resolvedAddress = ref('')
const formRef = ref<FormInstance>()

const form = reactive<StoreFormState>({
  id: undefined,
  name: '',
  address: '',
  longitude: null,
  latitude: null,
  status: 'ENABLED'
})

const rules: FormRules = {
  name: [{ required: true, message: 'Please enter a store name', trigger: 'blur' }],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }],
  longitude: [{ required: true, message: 'Please enter longitude', trigger: 'change' }],
  latitude: [{ required: true, message: 'Please enter latitude', trigger: 'change' }]
}

function formatCoordinate(value: number | null | undefined) {
  return value == null ? '-' : Number(value).toFixed(6)
}

function hasValidCoordinates(longitude: number | null, latitude: number | null) {
  if (longitude == null || latitude == null) return false
  return longitude >= -180 && longitude <= 180 && latitude >= -90 && latitude <= 90
}

async function loadStores() {
  loading.value = true
  try {
    const res = await listStores()
    stores.value = res.data || []
  } catch {
    stores.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.id = undefined
  form.name = ''
  form.address = ''
  form.longitude = null
  form.latitude = null
  form.status = 'ENABLED'
  resolvedAddress.value = ''
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(store: StoreDto) {
  isEdit.value = true
  form.id = store.id
  form.name = store.name
  form.address = store.address || ''
  form.longitude = store.longitude
  form.latitude = store.latitude
  form.status = store.status || 'ENABLED'
  resolvedAddress.value = ''
  dialogVisible.value = true
}

async function handleResolveAddress() {
  if (!hasValidCoordinates(form.longitude, form.latitude)) {
    ElMessage.warning('Please enter valid longitude and latitude values first.')
    return
  }

  resolving.value = true
  try {
    const res = await resolveLocationFromCoordinates(form.longitude!, form.latitude!)
    resolvedAddress.value = res.data || ''
    if (!form.address.trim()) {
      form.address = resolvedAddress.value
    }
    ElMessage.success('Address resolved successfully.')
  } catch {
    // request interceptor handles backend messages
  } finally {
    resolving.value = false
  }
}

async function submitForm() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!hasValidCoordinates(form.longitude, form.latitude)) {
      ElMessage.warning('Please enter valid coordinates before saving.')
      return
    }

    submitting.value = true
    try {
      const payload = {
        name: form.name.trim(),
        address: form.address.trim() || null,
        longitude: form.longitude,
        latitude: form.latitude,
        status: form.status
      }

      if (isEdit.value && form.id != null) {
        await updateStore(form.id, payload)
        ElMessage.success('Store updated successfully.')
      } else {
        await createStore(payload)
        ElMessage.success('Store created successfully.')
      }

      dialogVisible.value = false
      await loadStores()
    } catch {
      // request interceptor handles backend messages
    } finally {
      submitting.value = false
    }
  })
}

function confirmDelete(store: StoreDto) {
  if (store.id == null) return

  ElMessageBox.confirm(
    `Are you sure you want to delete store ${store.name}?`,
    'Delete Store',
    {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteStore(store.id!)
      ElMessage.success('Store deleted successfully.')
      await loadStores()
    } catch {
      // request interceptor handles backend messages
    }
  }).catch(() => {})
}

onMounted(() => {
  loadStores()
})
</script>

<style scoped>
.stores-page {
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
  min-width: 1260px;
}

.coordinate-cell,
.inventory-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #4b5563;
}

.helper-panel {
  margin-top: 4px;
  padding: 16px;
  border-radius: 12px;
  background: #f6f8fb;
  border: 1px solid #dbe3ef;
}

.helper-title {
  font-size: 14px;
  font-weight: 700;
  color: #111827;
}

.helper-copy {
  margin-top: 6px;
  color: #4b5563;
  line-height: 1.6;
}

.helper-preview {
  margin-top: 10px;
  color: #111827;
  line-height: 1.6;
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
