<template>
  <div class="revenue-page">
    <div class="page-header">
      <div>
        <h2 class="page-heading">Weekly Revenue</h2>
        <p class="page-copy">Weekly Sprint 3 revenue summary grouped by the current hire periods, including minute-based scan rides.</p>
      </div>
      <el-button :loading="loading" @click="loadSummary">
        <el-icon><Refresh /></el-icon>
        Refresh
      </el-button>
    </div>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Window Start</div>
          <div class="stat-value stat-value-small">{{ formatDateTime(summary?.windowStart) }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Window End</div>
          <div class="stat-value stat-value-small">{{ formatDateTime(summary?.windowEnd) }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Weekly Orders</div>
          <div class="stat-value">{{ totalOrders }}</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :xl="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">Weekly Revenue</div>
          <div class="stat-value">{{ formatCurrency(totalRevenue) }}</div>
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
            <span class="summary-value">{{ mostPopularLabel }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">Tracked Hire Periods</span>
            <span class="summary-value">{{ revenueBuckets.length }}</span>
          </div>
          <div class="summary-row">
            <span class="summary-label">Average Revenue / Order</span>
            <span class="summary-value">{{ averageRevenueLabel }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :xl="16">
        <el-card class="visual-card" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Revenue Visualization</span>
              <span class="card-note">A quick visual comparison of weekly revenue and booking volume by hire period.</span>
            </div>
          </template>

          <div class="chart-shell">
            <div class="revenue-chart">
              <div
                v-for="item in visualizationItems"
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
                v-for="item in visualizationItems"
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

    <el-row :gutter="20" class="table-row">
      <el-col :xs="24">
        <el-card v-loading="loading">
          <template #header>
            <div class="card-header">
              <span class="card-title">Revenue Buckets</span>
              <span class="card-note">Every current hire period is rendered, even when no orders exist.</span>
            </div>
          </template>

          <div class="table-scroll">
            <el-table :data="revenueBuckets" stripe>
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
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getWeeklyRevenueSummary, type AdminWeeklyRevenueSummary } from '@/api/admin'
import {
  formatCurrency,
  formatDateTime,
  formatPeriod,
  normalizeRevenueBuckets,
  sumOrders,
  sumRevenue
} from '@/utils/admin-display'

const loading = ref(false)
const summary = ref<AdminWeeklyRevenueSummary | null>(null)

const revenueBuckets = computed(() => normalizeRevenueBuckets(summary.value?.buckets))
const totalOrders = computed(() => sumOrders(revenueBuckets.value))
const totalRevenue = computed(() => sumRevenue(revenueBuckets.value))
const maxRevenue = computed(() => Math.max(...revenueBuckets.value.map(bucket => bucket.totalRevenue), 0))
const maxOrders = computed(() => Math.max(...revenueBuckets.value.map(bucket => bucket.orderCount), 0))
const mostPopularLabel = computed(() => {
  return summary.value?.mostPopularHirePeriod
    ? formatPeriod(summary.value.mostPopularHirePeriod)
    : 'No activity yet'
})
const visualizationItems = computed(() => {
  return revenueBuckets.value.map((bucket) => ({
    ...bucket,
    revenuePercent: maxRevenue.value > 0 ? (bucket.totalRevenue / maxRevenue.value) * 100 : 0,
    orderPercent: maxOrders.value > 0 ? (bucket.orderCount / maxOrders.value) * 100 : 0
  }))
})
const averageRevenueLabel = computed(() => {
  if (!totalOrders.value) {
    return formatCurrency(0)
  }
  return formatCurrency(totalRevenue.value / totalOrders.value)
})

async function loadSummary() {
  loading.value = true
  try {
    const res = await getWeeklyRevenueSummary()
    summary.value = res.data || null
  } catch {
    summary.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSummary()
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

.stats-row {
  margin-bottom: 20px;
}

.table-row {
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

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.table-scroll :deep(.el-table) {
  min-width: 600px;
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

.revenue-chart {
  min-height: 320px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: end;
  padding: 12px 0 6px;
}

.chart-column {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
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

.chart-period {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
  text-align: center;
}

.chart-code {
  font-size: 12px;
  color: #6b7280;
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

  .revenue-chart {
    min-height: 260px;
  }

  .chart-track {
    height: 180px;
  }
}
</style>
