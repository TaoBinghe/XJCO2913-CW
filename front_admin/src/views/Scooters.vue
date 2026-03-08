<template>
  <div class="scooters-page">
    <div class="page-header">
      <h2 class="page-heading">Scooter Management</h2>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        Add Scooter
      </el-button>
    </div>

    <!-- Scooter Table -->
    <el-card>
      <el-table :data="scooters" stripe style="width: 100%" v-loading="tableLoading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="scooterCode" label="Scooter Code" width="160">
          <template #default="{ row }">
            <el-tag type="success" effect="plain">{{ row.scooterCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="160">
          <template #default="{ row }">
            <el-tag :type="row.status === 'AVAILABLE' ? 'success' : 'danger'">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="Location" min-width="200">
          <template #default="{ row }">
            {{ row.location || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="200" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" link @click="openEditDialog(row, $index)">
              <el-icon><Edit /></el-icon> Edit
            </el-button>
            <el-button type="danger" link @click="handleDelete(row, $index)">
              <el-icon><Delete /></el-icon> Delete
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="scooters.length === 0 && !tableLoading" class="empty-tip">
        <el-empty description="No scooters yet. Click 'Add Scooter' to create one." />
      </div>
    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Scooter' : 'Add New Scooter'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="dialogFormRef"
        :model="dialogForm"
        :rules="dialogRules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="Scooter Code" prop="scooterCode">
          <el-input
            v-model="dialogForm.scooterCode"
            placeholder="e.g. SC001"
            :disabled="isEdit"
          />
        </el-form-item>

        <el-form-item label="Status" prop="status">
          <el-select v-model="dialogForm.status" style="width: 100%;">
            <el-option label="AVAILABLE" value="AVAILABLE" />
            <el-option label="UNAVAILABLE" value="UNAVAILABLE" />
          </el-select>
        </el-form-item>

        <el-form-item label="Location" prop="location">
          <el-input
            v-model="dialogForm.location"
            placeholder="e.g. Campus North Gate"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? 'Update' : 'Add' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { listScooters, addScooter, updateScooter, deleteScooter } from '@/api/admin'

interface Scooter {
  id?: number
  scooterCode: string
  status: string
  location: string
}

const scooters = ref<Scooter[]>([])
const tableLoading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editIndex = ref(-1)
const submitting = ref(false)
const dialogFormRef = ref<FormInstance>()

const dialogForm = reactive<Scooter>({
  id: undefined,
  scooterCode: '',
  status: 'AVAILABLE',
  location: ''
})

const dialogRules: FormRules = {
  scooterCode: [{ required: true, message: 'Please enter scooter code', trigger: 'blur' }],
  status: [{ required: true, message: 'Please select status', trigger: 'change' }]
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

function openAddDialog() {
  isEdit.value = false
  editIndex.value = -1
  dialogForm.id = undefined
  dialogForm.scooterCode = ''
  dialogForm.status = 'AVAILABLE'
  dialogForm.location = ''
  dialogVisible.value = true
}

function openEditDialog(row: Scooter, index: number) {
  isEdit.value = true
  editIndex.value = index
  dialogForm.id = row.id
  dialogForm.scooterCode = row.scooterCode
  dialogForm.status = row.status
  dialogForm.location = row.location
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!dialogFormRef.value) return
  await dialogFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateScooter({
          id: dialogForm.id!,
          scooterCode: dialogForm.scooterCode,
          status: dialogForm.status,
          location: dialogForm.location
        })
        ElMessage.success('Scooter updated successfully')
      } else {
        await addScooter({
          scooterCode: dialogForm.scooterCode,
          status: dialogForm.status,
          location: dialogForm.location
        })
        ElMessage.success('Scooter added successfully')
      }
      dialogVisible.value = false
      await fetchScooters()
    } catch {
      // error handled by request interceptor
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row: Scooter, index: number) {
  if (!row.id) {
    ElMessage.warning('Cannot delete: scooter has no server ID')
    return
  }

  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete scooter "${row.scooterCode}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )

    await deleteScooter(row.id)
    ElMessage.success('Scooter deleted successfully')
    await fetchScooters()
  } catch (e: any) {
    if (e !== 'cancel') {
      // error handled by request interceptor
    }
  }
}

onMounted(() => {
  fetchScooters()
})
</script>

<style scoped>
.scooters-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-heading {
  font-size: 22px;
  font-weight: 600;
  color: #1d1e1f;
}

.empty-tip {
  padding: 40px 0;
}
</style>
