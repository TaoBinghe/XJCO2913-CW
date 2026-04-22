<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Dashboard</h2>
        <p class="page-copy">Sprint 3 admin summary for stores, fleet, pricing, and weekly revenue.</p>
      </div>
      <el-button :loading="loading" @click="loadDashboardData">
        <el-icon><Refresh /></el-icon>
        Refresh
      </el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon stat-icon-green">
            <el-icon :size="26"><Van /></el-icon>
          </div>
          <div class="stat-copy">
            <div class="stat-label">Scooters Managed</div>
            <div class="stat-value">{{ scooterCount }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon stat-icon-blue">
            <el-icon :size="26"><PriceTag /></el-icon>
          </div>
          <div class="stat-copy">
            <div class="stat-label">Pricing Plans</div>
            <div class="stat-value">{{ planCount }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon stat-icon-gold">
            <el-icon :size="26"><Money /></el-icon>
          </div>
          <div class="stat-copy">
            <div class="stat-label">Weekly Revenue</div>
            <div class="stat-value">{{ formatCurrency(totalRevenue) }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon stat-icon-purple">
            <el-icon :size="26"><Histogram /></el-icon>
          </div>
          <div class="stat-copy">
            <div class="stat-label">Most Popular Plan</div>
            <div class="stat-value stat-value-small">{{ mostPopularLabel }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="dashboard-grid">
      <el-col :xs="24" :xl="14">
        <el-card class="quick-actions-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">Sprint 3 Actions</span>
              <span class="card-note">Jump straight into the key admin workflows.</span>
            </div>
          </template>

          <div class="actions-grid">
            <el-button type="primary" plain class="action-btn" @click="router.push('/scooters')">
              <el-icon><Van /></el-icon>
              Manage Scooters
            </el-button>
            <el-button type="info" plain class="action-btn" @click="router.push('/stores')">
              <el-icon><Shop /></el-icon>
              Manage Stores ({{ storeCount }})
            </el-button>
            <el-button type="success" plain class="action-btn" @click="router.push('/pricing')">
              <el-icon><PriceTag /></el-icon>
              Edit Pricing
            </el-button>
            <el-button type="warning" plain class="action-btn" @click="router.push('/revenue')">
              <el-icon><Histogram /></el-icon>
              Review Revenue
            </el-button>
            <el-button plain class="action-btn" @click="router.push('/users')">
              <el-icon><User /></el-icon>
              View Users ({{ userCount }})
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="revenue-window-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Weekly Revenue Window</span>
              <span class="card-note">The same source of truth used by the dedicated revenue page.</span>
            </div>
          </template>

          <div class="window-row">
            <span class="window-label">Window Start</span>
            <span class="window-value">{{ formatDateTime(revenueSummary?.windowStart) }}</span>
          </div>
          <div class="window-row">
            <span class="window-label">Window End</span>
            <span class="window-value">{{ formatDateTime(revenueSummary?.windowEnd) }}</span>
          </div>
          <div class="window-row">
            <span class="window-label">Orders in Window</span>
            <span class="window-value">{{ totalOrders }}</span>
          </div>
          <div class="window-row">
            <span class="window-label">Most Popular Period</span>
            <span class="window-value">{{ mostPopularLabel }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="dashboard-grid">
      <el-col :xs="24" :xl="14">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Weekly Revenue Buckets</span>
              <span class="card-note">All current hire periods are shown, including minute-based scan ride plans.</span>
            </div>
          </template>

          <div class="table-scroll">
            <el-table :data="revenueBuckets" stripe>
              <el-table-column prop="hirePeriod" label="Hire Period" min-width="160">
                <template #default="{ row }">
                  <el-tag effect="plain">{{ formatPeriod(row.hirePeriod) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="orderCount" label="Orders" width="110" />
              <el-table-column prop="totalRevenue" label="Revenue" min-width="160">
                <template #default="{ row }">
                  <span class="revenue-value">{{ formatCurrency(row.totalRevenue) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="10">
        <el-card class="mini-chart-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Revenue Snapshot</span>
              <span class="card-note">Compact visual overview of the weekly revenue buckets.</span>
            </div>
          </template>

          <div class="mini-chart">
            <div
              v-for="item in revenueChartItems"
              :key="item.hirePeriod"
              class="mini-chart-row"
            >
              <div class="mini-chart-copy">
                <span class="mini-chart-label">{{ formatPeriod(item.hirePeriod) }}</span>
                <span class="mini-chart-meta">{{ item.orderCount }} orders</span>
              </div>
              <div class="mini-chart-track">
                <div
                  class="mini-chart-fill"
                  :style="{ width: `${Math.max(item.revenuePercent, item.totalRevenue > 0 ? 10 : 0)}%` }"
                ></div>
              </div>
              <div class="mini-chart-value">{{ formatCurrency(item.totalRevenue) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="dashboard-grid">
      <el-col :xs="24" :xl="24">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Current Pricing Plans</span>
              <span class="card-note">Admin pricing API only, including minute-based scan ride plans.</span>
            </div>
          </template>

          <div class="table-scroll">
            <el-table :data="plans" stripe>
              <el-table-column prop="hirePeriod" label="Plan" min-width="150">
                <template #default="{ row }">
                  <div class="plan-stack">
                    <span class="plan-name">{{ formatPeriod(row.hirePeriod) }}</span>
                    <span class="plan-code">{{ row.hirePeriod }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="price" label="Price" width="130">
                <template #default="{ row }">
                  <span class="revenue-value">{{ formatCurrency(row.price) }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="Updated" min-width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.updatedAt) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getPricingPlanList,
  getWeeklyRevenueSummary,
  listScooters,
  listStores,
  listUsers,
  type AdminWeeklyRevenueSummary,
  type PricingPlanDto
} from '@/api/admin'
import {
  formatCurrency,
  formatDateTime,
  formatPeriod,
  normalizeRevenueBuckets,
  sortPlansByPeriod,
  sumOrders,
  sumRevenue
} from '@/utils/admin-display'

const router = useRouter()
const loading = ref(false)

const scooterCount = ref(0)
const storeCount = ref(0)
const userCount = ref(0)
const planCount = ref(0)
const plans = ref<PricingPlanDto[]>([])
const revenueSummary = ref<AdminWeeklyRevenueSummary | null>(null)

const revenueBuckets = computed(() => normalizeRevenueBuckets(revenueSummary.value?.buckets))
const totalRevenue = computed(() => sumRevenue(revenueBuckets.value))
const totalOrders = computed(() => sumOrders(revenueBuckets.value))
const maxRevenue = computed(() => Math.max(...revenueBuckets.value.map(bucket => bucket.totalRevenue), 0))
const mostPopularLabel = computed(() => {
  return revenueSummary.value?.mostPopularHirePeriod
    ? formatPeriod(revenueSummary.value.mostPopularHirePeriod)
    : 'No activity yet'
})
const revenueChartItems = computed(() => {
  return revenueBuckets.value.map((bucket) => ({
    ...bucket,
    revenuePercent: maxRevenue.value > 0 ? (bucket.totalRevenue / maxRevenue.value) * 100 : 0
  }))
})

async function loadDashboardData() {
  loading.value = true
  try {
    const [scootersRes, storesRes, usersRes, plansRes, revenueRes] = await Promise.all([
      listScooters(),
      listStores(),
      listUsers(),
      getPricingPlanList(),
      getWeeklyRevenueSummary()
    ])

    scooterCount.value = (scootersRes.data || []).length
    storeCount.value = (storesRes.data || []).length
    userCount.value = (usersRes.data || []).length
    plans.value = sortPlansByPeriod(plansRes.data || [])
    planCount.value = plans.value.length
    revenueSummary.value = revenueRes.data || null
  } catch {
    scooterCount.value = 0
    storeCount.value = 0
    userCount.value = 0
    plans.value = []
    planCount.value = 0
    revenueSummary.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped>
.dashboard-page {
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

.stats-row,
.dashboard-grid {
  margin-bottom: 20px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 18px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-green {
  background: #e9f8ee;
  color: #1d8f4d;
}

.stat-icon-blue {
  background: #e8f1ff;
  color: #2463d6;
}

.stat-icon-gold {
  background: #fff4df;
  color: #c67a10;
}

.stat-icon-purple {
  background: #f3edff;
  color: #7758d1;
}

.stat-copy {
  min-width: 0;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  margin-top: 6px;
  font-size: 30px;
  font-weight: 700;
  color: #111827;
}

.stat-value-small {
  font-size: 24px;
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

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 560px;
}

.actions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.action-btn {
  width: 100%;
  height: 48px;
}

.window-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #eef1f6;
}

.window-row:last-child {
  border-bottom: none;
}

.window-label {
  color: #6b7280;
}

.window-value {
  color: #111827;
  font-weight: 600;
  text-align: right;
}

.revenue-value {
  font-weight: 600;
  color: #1d8f4d;
}

.mini-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mini-chart-row {
  display: grid;
  grid-template-columns: minmax(0, 110px) minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
}

.mini-chart-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mini-chart-label {
  font-weight: 600;
  color: #111827;
}

.mini-chart-meta {
  font-size: 12px;
  color: #6b7280;
}

.mini-chart-track {
  height: 12px;
  width: 100%;
  background: #edf2f7;
  border-radius: 999px;
  overflow: hidden;
}

.mini-chart-fill {
  height: 100%;
  border-radius: 999px;
  background: #1d8f4d;
  transition: width 0.25s ease;
}

.mini-chart-value {
  font-size: 13px;
  font-weight: 700;
  color: #1d8f4d;
  white-space: nowrap;
}

.plan-stack {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plan-name {
  font-weight: 600;
  color: #111827;
}

.plan-code {
  font-size: 12px;
  color: #6b7280;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .page-header > .el-button {
    width: 100%;
  }

  .actions-grid {
    grid-template-columns: 1fr;
  }

  .window-row {
    flex-direction: column;
  }

  .window-value {
    text-align: left;
  }

  .mini-chart-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
}
</style>
