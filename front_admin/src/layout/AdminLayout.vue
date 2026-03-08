<template>
  <el-container class="admin-layout">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-area">
        <el-icon :size="28" color="#07c160"><Van /></el-icon>
        <span v-if="!isCollapse" class="logo-text">E-Scooter Admin</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#1d1e1f"
        text-color="#bfcbd9"
        active-text-color="#07c160"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>Dashboard</template>
        </el-menu-item>

        <el-menu-item index="/scooters">
          <el-icon><Van /></el-icon>
          <template #title>Scooter Management</template>
        </el-menu-item>

        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>User Management</template>
        </el-menu-item>

        <el-menu-item index="/pricing">
          <el-icon><PriceTag /></el-icon>
          <template #title>Pricing Plans</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main Content -->
    <el-container>
      <!-- Header -->
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="isCollapse = !isCollapse"
          >
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">Home</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  Logout
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Page Content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getUsername, clearAll } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const username = ref(getUsername() || 'Admin')

const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  return (route.meta.title as string) || 'Dashboard'
})

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('Are you sure you want to logout?', 'Confirm', {
      confirmButtonText: 'Yes',
      cancelButtonText: 'Cancel',
      type: 'warning'
    }).then(() => {
      clearAll()
      router.push('/login')
    }).catch(() => {})
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.sidebar {
  background-color: #1d1e1f;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid #2d2d2d;
  padding: 0 16px;
  white-space: nowrap;
  overflow: hidden;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.header {
  background-color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #666;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #07c160;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
}

.user-info:hover {
  color: #07c160;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
