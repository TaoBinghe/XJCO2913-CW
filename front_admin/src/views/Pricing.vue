<template>
  <div class="pricing-page">
    <h2 class="page-heading">Pricing Plans</h2>

    <!-- Plan Cards -->
    <el-row :gutter="20" class="plan-cards">
      <el-col :span="6" v-for="plan in plans" :key="plan.id">
        <el-card shadow="hover" class="plan-card">
          <div class="plan-icon">
            <el-icon :size="32" color="#07c160"><Timer /></el-icon>
          </div>
          <div class="plan-period">{{ formatPeriod(plan.hirePeriod) }}</div>
          <div class="plan-price">£{{ plan.price.toFixed(2) }}</div>
          <div class="plan-code">{{ plan.hirePeriod }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Plan Table -->
    <el-card>
      <template #header>
        <div class="table-header">
          <span class="card-title">All Pricing Plans</span>
          <el-button :loading="loading" @click="loadPlans">
            <el-icon><Refresh /></el-icon>
            Refresh
          </el-button>
        </div>
      </template>

      <el-table :data="plans" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="hirePeriod" label="Hire Period Code" width="200">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.hirePeriod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Display Name" width="200">
          <template #default="{ row }">
            {{ formatPeriod(row.hirePeriod) }}
          </template>
        </el-table-column>
        <el-table-column prop="price" label="Price (£)" width="150">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #07c160; font-size: 16px;">
              £{{ row.price.toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Last Updated" min-width="200">
          <template #default="{ row }">
            {{ row.updatedAt?.replace('T', ' ') || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPricingPlans } from '@/api/booking'

const plans = ref<any[]>([])
const loading = ref(false)

function formatPeriod(period: string): string {
  const map: Record<string, string> = {
    'HOUR_1': '1 Hour',
    'HOUR_4': '4 Hours',
    'DAY_1': '1 Day',
    'WEEK_1': '1 Week'
  }
  return map[period] || period
}

async function loadPlans() {
  loading.value = true
  try {
    const res = await getPricingPlans()
    plans.value = res.data || []
  } catch {
    plans.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPlans()
})
</script>

<style scoped>
.pricing-page {
  max-width: 1200px;
}

.page-heading {
  font-size: 22px;
  font-weight: 600;
  color: #1d1e1f;
  margin-bottom: 20px;
}

.plan-cards {
  margin-bottom: 20px;
}

.plan-card {
  text-align: center;
  padding: 10px 0;
}

.plan-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.plan-icon {
  width: 60px;
  height: 60px;
  background-color: #e8f5e9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plan-period {
  font-size: 18px;
  font-weight: 600;
  color: #1d1e1f;
}

.plan-price {
  font-size: 28px;
  font-weight: 700;
  color: #07c160;
}

.plan-code {
  font-size: 12px;
  color: #999;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
