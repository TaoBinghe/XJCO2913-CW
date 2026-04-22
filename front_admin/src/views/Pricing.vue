<template>
  <div class="pricing-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Pricing Plan Management</h2>
        <p class="page-copy">Create Sprint 3 reservation and scan-ride pricing plans, including minute-based periods.</p>
      </div>
      <div class="header-actions">
        <el-button :loading="loading" @click="loadPlans">
          <el-icon><Refresh /></el-icon>
          Refresh
        </el-button>
        <el-button type="primary" @click="openAdd">
          <el-icon><Plus /></el-icon>
          Add Plan
        </el-button>
      </div>
    </div>

    <el-row :gutter="20" class="plan-cards">
      <el-col :xs="24" :sm="12" :xl="6" v-for="plan in plans" :key="plan.id">
        <el-card shadow="hover" class="plan-card">
          <div class="plan-card-top">
            <div class="plan-icon">
              <el-icon :size="30"><Timer /></el-icon>
            </div>
            <el-tag effect="plain">{{ plan.hirePeriod }}</el-tag>
          </div>
          <div class="plan-period">{{ formatPeriod(plan.hirePeriod) }}</div>
          <div class="plan-price">{{ formatCurrency(plan.price) }}</div>
          <div class="plan-updated">Updated {{ formatDateTime(plan.updatedAt) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div class="table-header">
          <div>
            <div class="card-title">All Pricing Plans</div>
            <div class="card-note">Backend uniqueness and price validation remain the source of truth for both reservation and scan-ride plans.</div>
          </div>
        </div>
      </template>

      <div class="table-scroll">
        <el-table :data="plans" stripe v-loading="loading">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="hirePeriod" label="Hire Period Code" min-width="170">
            <template #default="{ row }">
              <el-tag effect="plain">{{ row.hirePeriod }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Display Name" min-width="140">
            <template #default="{ row }">
              {{ formatPeriod(row.hirePeriod) }}
            </template>
          </el-table-column>
          <el-table-column prop="price" label="Price" min-width="140">
            <template #default="{ row }">
              <span class="price-value">{{ formatCurrency(row.price) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="Updated At" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="Actions" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openEdit(row)">Edit</el-button>
              <el-button link type="danger" @click="confirmDelete(row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? 'Edit Pricing Plan' : 'Add Pricing Plan'"
      width="min(460px, 92vw)"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Duration Value" prop="durationValue">
          <el-input-number
            v-model="form.durationValue"
            :min="1"
            :step="1"
            :step-strictly="true"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="Duration Unit" prop="durationUnit">
          <el-select v-model="form.durationUnit" placeholder="Select a duration unit" style="width: 100%">
            <el-option
              v-for="option in DURATION_UNIT_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Hire Period Code">
          <el-input :model-value="hirePeriodCode" readonly />
          <div class="field-help">
            Generated in `UNIT_NUMBER` format, for example `HOUR_2`, `DAY_3`, `WEEK_2`, or `MONTH_1`.
          </div>
        </el-form-item>

        <el-form-item label="Price" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">
          {{ isEdit ? 'Save Changes' : 'Create Plan' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createPricingPlan,
  deletePricingPlan,
  getPricingPlanList,
  updatePricingPlan,
  type PricingPlanDto
} from '@/api/admin'
import {
  DURATION_UNIT_OPTIONS,
  buildHirePeriodCode,
  formatCurrency,
  formatDateTime,
  formatPeriod,
  parseHirePeriod,
  sortPlansByPeriod
} from '@/utils/admin-display'

const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const plans = ref<PricingPlanDto[]>([])

function createDefaultForm() {
  return {
    durationUnit: 'HOUR',
    durationValue: 1,
    price: 1
  }
}

const form = ref(createDefaultForm())

const hirePeriodCode = computed(() => buildHirePeriodCode(form.value.durationUnit, form.value.durationValue))

const rules: FormRules = {
  durationValue: [{ required: true, message: 'Please enter a duration value', trigger: 'change' }],
  durationUnit: [{ required: true, message: 'Please select a duration unit', trigger: 'change' }],
  price: [{ required: true, message: 'Please enter a price', trigger: 'change' }]
}

async function loadPlans() {
  loading.value = true
  try {
    const res = await getPricingPlanList()
    plans.value = sortPlansByPeriod(res.data || [])
  } catch {
    plans.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  isEdit.value = false
  editingId.value = null
  form.value = createDefaultForm()
  dialogVisible.value = true
}

function openEdit(row: PricingPlanDto) {
  const parsedPeriod = parseHirePeriod(row.hirePeriod)

  isEdit.value = true
  editingId.value = row.id ?? null
  form.value = {
    durationUnit: parsedPeriod?.unit || 'HOUR',
    durationValue: parsedPeriod?.value || 1,
    price: Number(row.price || 0)
  }
  dialogVisible.value = true
}

function resetForm() {
  editingId.value = null
  form.value = createDefaultForm()
  formRef.value?.clearValidate()
}

async function submitForm() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (form.value.price <= 0) {
      ElMessage.warning('Price must be greater than 0.')
      return
    }

    if (!hirePeriodCode.value) {
      ElMessage.warning('Please enter a valid duration.')
      return
    }

    submitLoading.value = true
    try {
      if (isEdit.value && editingId.value != null) {
        await updatePricingPlan(editingId.value, {
          hirePeriod: hirePeriodCode.value,
          price: form.value.price
        })
        ElMessage.success('Pricing plan updated successfully.')
      } else {
        await createPricingPlan({
          hirePeriod: hirePeriodCode.value,
          price: form.value.price
        })
        ElMessage.success('Pricing plan created successfully.')
      }

      dialogVisible.value = false
      await loadPlans()
    } catch {
      // request interceptor handles the backend message
    } finally {
      submitLoading.value = false
    }
  })
}

function confirmDelete(row: PricingPlanDto) {
  if (row.id == null) return

  ElMessageBox.confirm(
    `Are you sure you want to delete ${formatPeriod(row.hirePeriod)} (${row.hirePeriod})?`,
    'Delete Pricing Plan',
    {
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deletePricingPlan(row.id!)
      ElMessage.success('Pricing plan deleted successfully.')
      await loadPlans()
    } catch {
      // request interceptor handles the backend message
    }
  }).catch(() => {})
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.pricing-page {
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

.plan-cards {
  margin-bottom: 20px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 660px;
}

.plan-card {
  height: 100%;
}

.plan-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.plan-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.plan-icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: #e9f8ee;
  color: #1d8f4d;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plan-period {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.plan-price,
.price-value {
  color: #1d8f4d;
  font-weight: 700;
}

.plan-price {
  font-size: 28px;
}

.plan-updated,
.card-note,
.field-help {
  font-size: 13px;
  color: #6b7280;
}

.field-help {
  margin-top: 8px;
  line-height: 1.6;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
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
