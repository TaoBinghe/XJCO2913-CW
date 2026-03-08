<template>
  <div class="dashboard">
    <h2 class="page-heading">Dashboard</h2>

    <!-- Stats Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #e8f5e9;">
            <el-icon :size="28" color="#07c160"><Van /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-number">{{ scooterCount }}</span>
            <span class="stat-label">Scooters Managed</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #e3f2fd;">
            <el-icon :size="28" color="#2196f3"><PriceTag /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-number">{{ planCount }}</span>
            <span class="stat-label">Pricing Plans</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" style="background-color: #fff3e0;">
            <el-icon :size="28" color="#ff9800"><User /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-number">{{ userCount }}</span>
            <span class="stat-label">Registered Users</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Quick Actions -->
    <el-card class="quick-actions">
      <template #header>
        <span class="card-title">Quick Actions</span>
      </template>

      <el-row :gutter="16">
        <el-col :span="8">
          <el-button type="primary" plain class="action-btn" @click="$router.push('/scooters')">
            <el-icon><Van /></el-icon>
            Manage Scooters
          </el-button>
        </el-col>
        <el-col :span="8">
          <el-button type="success" plain class="action-btn" @click="$router.push('/pricing')">
            <el-icon><PriceTag /></el-icon>
            View Pricing Plans
          </el-button>
        </el-col>
        <el-col :span="8">
          <el-button type="warning" plain class="action-btn" @click="$router.push('/scooters')">
            <el-icon><Plus /></el-icon>
            Add New Scooter
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- Pricing Plans Preview -->
    <el-card class="plans-preview">
      <template #header>
        <span class="card-title">Current Pricing Plans</span>
      </template>

      <el-table :data="plans" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="hirePeriod" label="Hire Period">
          <template #default="{ row }">
            <el-tag>{{ formatPeriod(row.hirePeriod) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="Price">
          <template #default="{ row }">
            <span style="font-weight: 600; color: #07c160;">£{{ row.price.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated At">
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
import { listScooters, listUsers } from '@/api/admin'

const scooterCount = ref(0)
const planCount = ref(0)
const userCount = ref(0)
const plans = ref<any[]>([])

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
  try {
    const res = await getPricingPlans()
    plans.value = res.data || []
    planCount.value = plans.value.length
  } catch {
    plans.value = []
  }
}

async function loadScooterCount() {
  try {
    const res = await listScooters()
    scooterCount.value = (res.data || []).length
  } catch {
    scooterCount.value = 0
  }
}

async function loadUserCount() {
  try {
    const res = await listUsers()
    userCount.value = (res.data || []).length
  } catch {
    userCount.value = 0
  }
}

onMounted(() => {
  loadPlans()
  loadScooterCount()
  loadUserCount()
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

.page-heading {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #1d1e1f;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1d1e1f;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.quick-actions {
  margin-bottom: 20px;
}

.action-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
}

.plans-preview {
  margin-bottom: 20px;
}
</style>
