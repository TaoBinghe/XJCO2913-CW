<template>
  <div class="revenue-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Revenue</h2>
        <p class="page-copy">Sprint 4 revenue summary with weekly hire-period buckets and the latest 7-day daily view.</p>
      </div>
      <el-button :loading="loading" @click="loadSummaries">
        <el-icon><Refresh /></el-icon>
        Refresh
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="revenue-tabs">
      <el-tab-pane label="Weekly by Hire Period" name="weekly">
        <el-row :gutter="20" class="stats-row">
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Window Start</div>
              <div class="stat-value stat-value-small">{{ formatDateTime(weeklySummary?.windowStart) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Window End</div>
              <div class="stat-value stat-value-small">{{ formatDateTime(weeklySummary?.windowEnd) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Weekly Orders</div>
              <div class="stat-value">{{ weeklyOrders }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Weekly Revenue</div>
              <div class="stat-value">{{ formatCurrency(weeklyRevenue) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :xl="8">
            <el-card class="summary-card" v-loading="loading">
              <template #header>
                <div class="card-header">
                  <span class="card-title">Summary</span>
                  <span class="card-note">Most popular period plus current weekly totals.</span>
                </div>
              </template>

              <div class="summary-row">
                <span class="summary-label">Most Popular Period</span>
                <span class="summary-value">{{ weeklyPopularLabel }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">Tracked Hire Periods</span>
                <span class="summary-value">{{ weeklyBuckets.length }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">Average Revenue / Order</span>
                <span class="summary-value">{{ weeklyAverageLabel }}</span>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :xl="16">
            <el-card class="visual-card" v-loading="loading">
              <template #header>
                <div class="card-header">
                  <span class="card-title">Weekly Visualization</span>
                  <span class="card-note">Revenue and booking volume grouped by hire period.</span>
                </div>
              </template>

              <div class="chart-shell">
                <div class="revenue-chart">
                  <div
                    v-for="item in weeklyVisualizationItems"
                    :key="item.hirePeriod"
                    class="chart-column"
                  >
                    <div class="chart-value">{{ formatCurrency(item.totalRevenue) }}</div>
                    <div class="chart-track">
                      <div
                        class="chart-bar"
                        :style="{ height: `${Math.max(item.revenuePercent, item.totalRevenue > 0 ? 8 : 0)}%` }"
                      ></div>
                    </div>
                    <div class="chart-period">{{ formatPeriod(item.hirePeriod) }}</div>
                    <div class="chart-code">{{ item.hirePeriod }}</div>
                  </div>
                </div>

                <div class="orders-breakdown">
                  <div
                    v-for="item in weeklyVisualizationItems"
                    :key="`${item.hirePeriod}-orders`"
                    class="breakdown-row"
                  >
                    <div class="breakdown-copy">
                      <span class="breakdown-label">{{ formatPeriod(item.hirePeriod) }}</span>
                      <span class="breakdown-meta">{{ item.orderCount }} orders</span>
                    </div>
                    <div class="breakdown-track">
                      <div
                        class="breakdown-fill"
                        :style="{ width: `${Math.max(item.orderPercent, item.orderCount > 0 ? 10 : 0)}%` }"
                      ></div>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="table-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Weekly Revenue Buckets</span>
              <span class="card-note">Every current hire period is rendered, even when no orders exist.</span>
            </div>
          </template>

          <div class="table-scroll">
            <el-table :data="weeklyBuckets" stripe>
              <el-table-column prop="hirePeriod" label="Hire Period" min-width="180">
                <template #default="{ row }">
                  <div class="period-cell">
                    <span class="period-name">{{ formatPeriod(row.hirePeriod) }}</span>
                    <span class="period-code">{{ row.hirePeriod }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="orderCount" label="Orders" width="120" />
              <el-table-column prop="totalRevenue" label="Revenue" min-width="160">
                <template #default="{ row }">
                  <span class="revenue-value">{{ formatCurrency(row.totalRevenue) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Daily Last 7 Days" name="daily">
        <el-row :gutter="20" class="stats-row">
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Window Start</div>
              <div class="stat-value stat-value-small">{{ formatDate(dailySummary?.windowStartDate) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Window End</div>
              <div class="stat-value stat-value-small">{{ formatDate(dailySummary?.windowEndDate) }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">Daily Orders</div>
              <div class="stat-value">{{ dailyOrders }}</div>
            </el-card>
          </el-col>
          <el-col :xs="24" :sm="12" :xl="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-label">7-Day Revenue</div>
              <div class="stat-value">{{ formatCurrency(dailyRevenue) }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :xs="24" :xl="8">
            <el-card class="summary-card" v-loading="loading">
              <template #header>
                <div class="card-header">
                  <span class="card-title">Daily Summary</span>
                  <span class="card-note">Revenue grouped by payment date.</span>
                </div>
              </template>

              <div class="summary-row">
                <span class="summary-label">Most Popular Date</span>
                <span class="summary-value">{{ dailySummary?.mostPopularRevenueDate || 'No activity yet' }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">Tracked Days</span>
                <span class="summary-value">{{ dailyBuckets.length }}</span>
              </div>
              <div class="summary-row">
                <span class="summary-label">Average Revenue / Day</span>
                <span class="summary-value">{{ dailyAverageLabel }}</span>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="24" :xl="16">
            <el-card class="visual-card" v-loading="loading">
              <template #header>
                <div class="card-header">
                  <span class="card-title">Daily Visualization</span>
                  <span class="card-note">The fixed seven daily buckets returned by the Sprint 4 API.</span>
                </div>
              </template>

              <div class="daily-chart">
                <div
                  v-for="item in dailyVisualizationItems"
                  :key="item.revenueDate"
                  class="chart-column"
                >
                  <div class="chart-value">{{ formatCurrency(item.totalRevenue) }}</div>
                  <div class="chart-track">
                    <div
                      class="chart-bar chart-bar-blue"
                      :style="{ height: `${Math.max(item.revenuePercent, item.totalRevenue > 0 ? 8 : 0)}%` }"
                    ></div>
                  </div>
                  <div class="chart-period">{{ shortDate(item.revenueDate) }}</div>
                  <div class="chart-code">{{ item.orderCount }} orders</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="table-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Daily Revenue Buckets</span>
              <span class="card-note">Seven calendar days, including zero-revenue dates.</span>
            </div>
          </template>

          <div class="table-scroll">
            <el-table :data="dailyBuckets" stripe>
              <el-table-column prop="revenueDate" label="Date" min-width="180" />
              <el-table-column prop="orderCount" label="Orders" width="120" />
              <el-table-column prop="totalRevenue" label="Revenue" min-width="160">
                <template #default="{ row }">
                  <span class="revenue-value">{{ formatCurrency(row.totalRevenue) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  getDailyRevenueSummary,
  getWeeklyRevenueSummary,
  type AdminDailyRevenueSummary,
  type AdminWeeklyRevenueSummary
} from '@/api/admin'
import {
  formatCurrency,
  formatDateTime,
  formatPeriod,
  normalizeRevenueBuckets,
  sumOrders,
  sumRevenue
} from '@/utils/admin-display'

const activeTab = ref('weekly')
const loading = ref(false)
const weeklySummary = ref<AdminWeeklyRevenueSummary | null>(null)
const dailySummary = ref<AdminDailyRevenueSummary | null>(null)

const weeklyBuckets = computed(() => normalizeRevenueBuckets(weeklySummary.value?.buckets))
const weeklyOrders = computed(() => sumOrders(weeklyBuckets.value))
const weeklyRevenue = computed(() => sumRevenue(weeklyBuckets.value))
const weeklyMaxRevenue = computed(() => Math.max(...weeklyBuckets.value.map(bucket => bucket.totalRevenue), 0))
const weeklyMaxOrders = computed(() => Math.max(...weeklyBuckets.value.map(bucket => bucket.orderCount), 0))
const weeklyPopularLabel = computed(() => {
  return weeklySummary.value?.mostPopularHirePeriod
    ? formatPeriod(weeklySummary.value.mostPopularHirePeriod)
    : 'No activity yet'
})
const weeklyAverageLabel = computed(() => {
  if (!weeklyOrders.value) return formatCurrency(0)
  return formatCurrency(weeklyRevenue.value / weeklyOrders.value)
})
const weeklyVisualizationItems = computed(() => {
  return weeklyBuckets.value.map((bucket) => ({
    ...bucket,
    revenuePercent: weeklyMaxRevenue.value > 0 ? (bucket.totalRevenue / weeklyMaxRevenue.value) * 100 : 0,
    orderPercent: weeklyMaxOrders.value > 0 ? (bucket.orderCount / weeklyMaxOrders.value) * 100 : 0
  }))
})

const dailyBuckets = computed(() => {
  return (dailySummary.value?.buckets || []).map((bucket) => ({
    revenueDate: bucket.revenueDate,
    orderCount: Number(bucket.orderCount || 0),
    totalRevenue: Number(bucket.totalRevenue || 0)
  }))
})
const dailyOrders = computed(() => sumOrders(dailyBuckets.value))
const dailyRevenue = computed(() => Number(dailySummary.value?.totalRevenue ?? sumRevenue(dailyBuckets.value)))
const dailyMaxRevenue = computed(() => Math.max(...dailyBuckets.value.map(bucket => bucket.totalRevenue), 0))
const dailyAverageLabel = computed(() => {
  if (!dailyBuckets.value.length) return formatCurrency(0)
  return formatCurrency(dailyRevenue.value / dailyBuckets.value.length)
})
const dailyVisualizationItems = computed(() => {
  return dailyBuckets.value.map((bucket) => ({
    ...bucket,
    revenuePercent: dailyMaxRevenue.value > 0 ? (bucket.totalRevenue / dailyMaxRevenue.value) * 100 : 0
  }))
})

function formatDate(value: string | null | undefined) {
  return value || '-'
}

function shortDate(value: string) {
  return value ? value.slice(5) : '-'
}

async function loadSummaries() {
  loading.value = true
  try {
    const [weeklyRes, dailyRes] = await Promise.all([
      getWeeklyRevenueSummary(),
      getDailyRevenueSummary()
    ])
    weeklySummary.value = weeklyRes.data || null
    dailySummary.value = dailyRes.data || null
  } catch {
    weeklySummary.value = null
    dailySummary.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSummaries()
})
</script>

<style scoped>
.revenue-page {
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

.revenue-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.stats-row {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}

.stat-card {
  min-height: 132px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
  line-height: 1.3;
}

.stat-value-small {
  font-size: 20px;
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

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #eef1f6;
}

.summary-row:last-child {
  border-bottom: none;
}

.summary-label {
  color: #6b7280;
}

.summary-value {
  color: #111827;
  font-weight: 600;
  text-align: right;
}

.chart-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  gap: 24px;
  align-items: stretch;
}

.revenue-chart,
.daily-chart {
  min-height: 320px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: end;
  padding: 12px 0 6px;
}

.daily-chart {
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.chart-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chart-value {
  font-size: 12px;
  font-weight: 600;
  color: #1d8f4d;
}

.chart-track {
  width: 100%;
  height: 220px;
  border-radius: 20px;
  background: #f3f6fb;
  padding: 8px;
  display: flex;
  align-items: flex-end;
}

.chart-bar {
  width: 100%;
  border-radius: 14px;
  background: #1d8f4d;
  box-shadow: 0 12px 24px rgba(29, 143, 77, 0.18);
  transition: height 0.25s ease;
}

.chart-bar-blue {
  background: #2463d6;
  box-shadow: 0 12px 24px rgba(36, 99, 214, 0.18);
}

.chart-period {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  text-align: center;
}

.chart-code {
  font-size: 12px;
  color: #6b7280;
  text-align: center;
}

.orders-breakdown {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
}

.breakdown-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.breakdown-copy {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.breakdown-label {
  font-weight: 600;
  color: #111827;
}

.breakdown-meta {
  font-size: 12px;
  color: #6b7280;
}

.breakdown-track {
  height: 14px;
  width: 100%;
  border-radius: 999px;
  background: #edf2f7;
  overflow: hidden;
}

.breakdown-fill {
  height: 100%;
  border-radius: 999px;
  background: #2463d6;
  transition: width 0.25s ease;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 600px;
}

.period-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.period-name {
  color: #111827;
  font-weight: 600;
}

.period-code {
  color: #6b7280;
  font-size: 12px;
}

.revenue-value {
  font-weight: 600;
  color: #1d8f4d;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
  }

  .page-header > .el-button {
    width: 100%;
  }

  .summary-row {
    flex-direction: column;
  }

  .summary-value {
    text-align: left;
  }

  .chart-shell {
    grid-template-columns: 1fr;
  }

  .revenue-chart,
  .daily-chart {
    min-height: 260px;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-track {
    height: 180px;
  }
}
</style>
