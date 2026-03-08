<template>
  <div class="users-page">
    <div class="page-header">
      <h2 class="page-heading">User Management</h2>
      <el-button @click="fetchUsers" :loading="loading">
        <el-icon><Refresh /></el-icon>
        Refresh
      </el-button>
    </div>

    <!-- Search / Filter -->
    <el-card class="filter-card">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchText"
            placeholder="Search by username"
            prefix-icon="Search"
            clearable
            @clear="onSearch"
            @keyup.enter="onSearch"
          />
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterRole" placeholder="Filter by role" clearable @change="onSearch">
            <el-option label="CUSTOMER" value="CUSTOMER" />
            <el-option label="MANAGER" value="MANAGER" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filterStatus" placeholder="Filter by status" clearable @change="onSearch">
            <el-option label="Enabled" :value="1" />
            <el-option label="Disabled" :value="0" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="onSearch" style="width: 100%;">
            <el-icon><Search /></el-icon>
            Search
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- User Table -->
    <el-card>
      <el-table :data="filteredUsers" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="username" label="Username" width="180">
          <template #default="{ row }">
            <span style="font-weight: 500;">{{ row.username }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="Email" min-width="200">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="role" label="Role" width="140">
          <template #default="{ row }">
            <el-tag :type="row.role === 'MANAGER' ? 'danger' : 'primary'" effect="plain">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="Status" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? 'Enabled' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="Updated At" width="180">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredUsers.length === 0 && !loading" class="empty-tip">
        <el-empty description="No users found" />
      </div>

      <!-- Summary -->
      <div class="table-footer" v-if="allUsers.length > 0">
        <span class="summary-text">
          Total: {{ allUsers.length }} users
          ({{ allUsers.filter(u => u.role === 'CUSTOMER').length }} customers,
          {{ allUsers.filter(u => u.role === 'MANAGER').length }} managers)
        </span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { listUsers } from '@/api/admin'

interface UserItem {
  id: number
  username: string
  email: string | null
  role: string
  status: number
  createdAt: string
  updatedAt: string
}

const allUsers = ref<UserItem[]>([])
const loading = ref(false)
const searchText = ref('')
const filterRole = ref('')
const filterStatus = ref<number | ''>('')

const filteredUsers = computed(() => {
  let result = allUsers.value

  if (searchText.value) {
    const keyword = searchText.value.toLowerCase()
    result = result.filter(u => u.username.toLowerCase().includes(keyword))
  }

  if (filterRole.value) {
    result = result.filter(u => u.role === filterRole.value)
  }

  if (filterStatus.value !== '' && filterStatus.value !== null) {
    result = result.filter(u => u.status === filterStatus.value)
  }

  return result
})

function formatTime(timeStr: string): string {
  if (!timeStr) return '-'
  return timeStr.replace('T', ' ').substring(0, 19)
}

function onSearch() {
  // filteredUsers is computed, triggers automatically
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await listUsers()
    allUsers.value = res.data || []
  } catch {
    allUsers.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.users-page {
  max-width: 1400px;
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

.filter-card {
  margin-bottom: 20px;
}

.empty-tip {
  padding: 40px 0;
}

.table-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.summary-text {
  font-size: 13px;
  color: #909399;
}
</style>
