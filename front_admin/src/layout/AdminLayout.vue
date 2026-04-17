<template>
  <el-container class="admin-layout" :class="{ 'admin-layout-mobile': isMobile }">
    <div v-if="isMobile && mobileMenuOpen" class="mobile-mask" @click="mobileMenuOpen = false"></div>

    <el-aside
      :width="asideWidth"
      class="sidebar"
      :class="{ 'sidebar-mobile': isMobile, 'sidebar-mobile-open': mobileMenuOpen }"
    >
      <div class="logo-area">
        <el-icon :size="28" color="#07c160"><Van /></el-icon>
        <span v-if="!menuCollapse" class="logo-text">E-Scooter Admin</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="menuCollapse"
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

        <el-menu-item index="/stores">
          <el-icon><Shop /></el-icon>
          <template #title>Store Management</template>
        </el-menu-item>

        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>User Management</template>
        </el-menu-item>

        <el-menu-item index="/pricing">
          <el-icon><PriceTag /></el-icon>
          <template #title>Pricing Plans</template>
        </el-menu-item>

        <el-menu-item index="/revenue">
          <el-icon><Histogram /></el-icon>
          <template #title>Weekly Revenue</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="content-shell">
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="toggleSidebar"
          >
            <Menu v-if="isMobile" />
            <Fold v-else-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb v-if="!isMobile" separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">Home</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div v-else class="header-title">{{ currentTitle }}</div>
        </div>

        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span v-if="!isMobile" class="user-name">{{ username }}</span>
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

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { getUsername, clearAll } from '@/utils/auth'

const MOBILE_BREAKPOINT = 960

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const isMobile = ref(false)
const mobileMenuOpen = ref(false)
const username = ref(getUsername() || 'Admin')

const activeMenu = computed(() => route.path)
const menuCollapse = computed(() => !isMobile.value && isCollapse.value)
const asideWidth = computed(() => {
  return isMobile.value ? '220px' : (isCollapse.value ? '64px' : '220px')
})

const currentTitle = computed(() => {
  return (route.meta.title as string) || 'Dashboard'
})

function updateLayout() {
  const nextIsMobile = window.innerWidth < MOBILE_BREAKPOINT
  isMobile.value = nextIsMobile

  if (!nextIsMobile) {
    mobileMenuOpen.value = false
  }
}

function toggleSidebar() {
  if (isMobile.value) {
    mobileMenuOpen.value = !mobileMenuOpen.value
    return
  }
  isCollapse.value = !isCollapse.value
}

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

watch(
  () => route.path,
  () => {
    mobileMenuOpen.value = false
  }
)

onMounted(() => {
  updateLayout()
  window.addEventListener('resize', updateLayout)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateLayout)
})
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  position: relative;
}

.sidebar {
  background-color: #1d1e1f;
  transition: width 0.3s ease, transform 0.3s ease;
  overflow: hidden;
  z-index: 30;
}

.sidebar-mobile {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 220px !important;
  transform: translateX(-100%);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.24);
}

.sidebar-mobile-open {
  transform: translateX(0);
}

.mobile-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(17, 24, 39, 0.4);
}

.content-shell {
  min-width: 0;
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
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0 20px;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.collapse-btn {
  cursor: pointer;
  color: #666;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #07c160;
}

.header-title {
  min-width: 0;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-right {
  display: flex;
  align-items: center;
  min-width: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #555;
  white-space: nowrap;
}

.user-info:hover {
  color: #07c160;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  min-width: 0;
}

@media (max-width: 960px) {
  .logo-area {
    justify-content: flex-start;
    padding: 0 20px;
  }

  .header {
    padding: 12px 16px;
    min-height: 60px;
    height: auto;
    gap: 12px;
  }

  .main-content {
    padding: 12px;
  }
}

@media (max-width: 640px) {
  .header {
    flex-wrap: wrap;
  }

  .header-left {
    width: 100%;
  }

  .header-right {
    margin-left: auto;
  }
}
</style>
