<template>
  <div class="bookings-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Guest Bookings</h2>
        <p class="page-copy">Create Sprint 4 store reservations for customers who have not registered yet.</p>
      </div>
      <el-button :loading="loadingOptions" @click="loadOptions">
        <el-icon><Refresh /></el-icon>
        Refresh Options
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :xl="14">
        <el-card>
          <template #header>
            <div class="card-header">
              <span class="card-title">Create Guest Reservation</span>
              <span class="card-note">The backend creates a guest user and sends the confirmation email.</span>
            </div>
          </template>

          <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="Customer Name" prop="customerName">
                  <el-input v-model="form.customerName" maxlength="128" placeholder="Guest Customer" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="Customer Email" prop="customerEmail">
                  <el-input v-model="form.customerEmail" placeholder="guest@example.com" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="Store" prop="storeId">
              <el-select v-model="form.storeId" filterable placeholder="Select a store" style="width: 100%">
                <el-option
                  v-for="store in enabledStores"
                  :key="store.id"
                  :label="storeLabel(store)"
                  :value="store.id"
                />
              </el-select>
            </el-form-item>

            <el-row :gutter="16">
              <el-col :xs="24" :sm="12">
                <el-form-item label="Appointment Date" prop="appointmentDate">
                  <el-date-picker
                    v-model="form.appointmentDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    placeholder="Pick a date"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="Appointment Time" prop="appointmentTime">
                  <el-time-picker
                    v-model="form.appointmentTime"
                    value-format="HH:mm"
                    format="HH:mm"
                    placeholder="Pick a time"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="Hire Period" prop="hiredPeriod">
              <el-select v-model="form.hiredPeriod" placeholder="Select a plan" style="width: 100%">
                <el-option
                  v-for="plan in plans"
                  :key="plan.id || plan.hirePeriod"
                  :label="`${formatPeriod(plan.hirePeriod)} · ${formatCurrency(plan.price)}`"
                  :value="plan.hirePeriod"
                />
              </el-select>
            </el-form-item>

            <div class="helper-panel">
              <div class="helper-title">Reservation preview</div>
              <div class="helper-copy">
                {{ selectedStore ? selectedStore.name : 'No store selected' }} ·
                {{ appointmentStart || 'No appointment time selected' }} ·
                {{ selectedPlan ? formatPeriod(selectedPlan.hirePeriod) : 'No plan selected' }}
              </div>
            </div>
          </el-form>

          <div class="form-actions">
            <el-button @click="resetForm">Reset</el-button>
            <el-button type="primary" :loading="submitting" @click="submitForm">
              Create Booking
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="result-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">Latest Result</span>
              <span class="card-note">The last created guest booking from this session.</span>
            </div>
          </template>

          <el-empty v-if="!createdBooking" description="No guest booking created yet." />

          <div v-else class="result-stack">
            <div class="result-id">#{{ createdBooking.id }}</div>
            <div class="result-row">
              <span>Customer</span>
              <strong>{{ createdBooking.customerName || '-' }}</strong>
            </div>
            <div class="result-row">
              <span>Email</span>
              <strong>{{ createdBooking.customerEmail || '-' }}</strong>
            </div>
            <div class="result-row">
              <span>Store</span>
              <strong>{{ createdBooking.storeName || '-' }}</strong>
            </div>
            <div class="result-row">
              <span>Status</span>
              <el-tag type="success" effect="plain">{{ createdBooking.status }}</el-tag>
            </div>
            <div class="result-row">
              <span>Pickup Window</span>
              <strong>{{ formatDateTime(createdBooking.startTime) }}</strong>
            </div>
            <div class="result-row">
              <span>Total Cost</span>
              <strong class="money-value">{{ formatCurrency(createdBooking.totalCost) }}</strong>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createUnregisteredBooking,
  getPricingPlanList,
  listStores,
  type PricingPlanDto,
  type StoreDto
} from '@/api/admin'
import {
  formatCurrency,
  formatDateTime,
  formatPeriod,
  sortPlansByPeriod
} from '@/utils/admin-display'

interface CreatedBooking {
  id: number
  customerName?: string | null
  customerEmail?: string | null
  storeName?: string | null
  status: string
  startTime: string
  totalCost: number
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const formRef = ref<FormInstance>()
const loadingOptions = ref(false)
const submitting = ref(false)
const stores = ref<StoreDto[]>([])
const plans = ref<PricingPlanDto[]>([])
const createdBooking = ref<CreatedBooking | null>(null)

const form = reactive({
  customerName: '',
  customerEmail: '',
  storeId: null as number | null,
  appointmentDate: '',
  appointmentTime: '',
  hiredPeriod: ''
})

const rules: FormRules = {
  customerName: [{ required: true, message: 'Please enter customer name', trigger: 'blur' }],
  customerEmail: [
    { required: true, message: 'Please enter customer email', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!EMAIL_PATTERN.test(String(value || '').trim())) {
          callback(new Error('Please enter a valid email.'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  storeId: [{ required: true, message: 'Please select a store', trigger: 'change' }],
  appointmentDate: [{ required: true, message: 'Please choose appointment date', trigger: 'change' }],
  appointmentTime: [{ required: true, message: 'Please choose appointment time', trigger: 'change' }],
  hiredPeriod: [{ required: true, message: 'Please choose a hire period', trigger: 'change' }]
}

const enabledStores = computed(() => stores.value.filter(store => store.status !== 'DISABLED'))
const selectedStore = computed(() => stores.value.find(store => store.id === form.storeId) || null)
const selectedPlan = computed(() => plans.value.find(plan => plan.hirePeriod === form.hiredPeriod) || null)
const appointmentStart = computed(() => {
  if (!form.appointmentDate || !form.appointmentTime) {
    return ''
  }
  return `${form.appointmentDate}T${form.appointmentTime}:00`
})

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function defaultAppointment() {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  date.setHours(10, 0, 0, 0)
  return {
    date: `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`,
    time: `${pad(date.getHours())}:${pad(date.getMinutes())}`
  }
}

function storeLabel(store: StoreDto) {
  return `${store.name} · bookable ${store.bookableInventory ?? 0}`
}

function resetForm() {
  const defaults = defaultAppointment()
  form.customerName = ''
  form.customerEmail = ''
  form.storeId = enabledStores.value[0]?.id ?? null
  form.appointmentDate = defaults.date
  form.appointmentTime = defaults.time
  form.hiredPeriod = plans.value[0]?.hirePeriod || ''
  formRef.value?.clearValidate()
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [storesRes, plansRes] = await Promise.all([
      listStores(),
      getPricingPlanList()
    ])
    stores.value = storesRes.data || []
    plans.value = sortPlansByPeriod(plansRes.data || [])
    if (!form.storeId) {
      form.storeId = enabledStores.value[0]?.id ?? null
    }
    if (!form.hiredPeriod) {
      form.hiredPeriod = plans.value[0]?.hirePeriod || ''
    }
  } catch {
    stores.value = []
    plans.value = []
  } finally {
    loadingOptions.value = false
  }
}

async function submitForm() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return
    if (!appointmentStart.value) {
      ElMessage.warning('Please choose a complete appointment time.')
      return
    }

    submitting.value = true
    try {
      const res: any = await createUnregisteredBooking({
        customerName: form.customerName.trim(),
        customerEmail: form.customerEmail.trim(),
        storeId: form.storeId!,
        appointmentStart: appointmentStart.value,
        hiredPeriod: form.hiredPeriod
      })
      createdBooking.value = res.data || null
      ElMessage.success('Guest booking created successfully.')
      resetForm()
    } catch {
      // request interceptor handles backend messages
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  const defaults = defaultAppointment()
  form.appointmentDate = defaults.date
  form.appointmentTime = defaults.time
  await loadOptions()
})
</script>

<style scoped>
.bookings-page {
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

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.card-note {
  font-size: 13px;
  color: #6b7280;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.result-card {
  height: 100%;
}

.result-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.result-id {
  font-size: 28px;
  font-weight: 700;
  color: #1d8f4d;
}

.result-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #eef1f6;
}

.result-row:last-child {
  border-bottom: none;
}

.result-row span {
  color: #6b7280;
}

.result-row strong {
  color: #111827;
  text-align: right;
}

.money-value {
  color: #1d8f4d !important;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .page-header > .el-button,
  .form-actions .el-button {
    width: 100%;
  }

  .form-actions,
  .result-row {
    flex-direction: column;
  }

  .result-row strong {
    text-align: left;
  }
}
</style>
