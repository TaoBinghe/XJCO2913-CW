import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { getAdminUiBase } from '@/utils/appBase'

const router = createRouter({
  history: createWebHashHistory(getAdminUiBase()),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layout/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: 'Dashboard', icon: 'Odometer' }
        },
        {
          path: 'scooters',
          name: 'Scooters',
          component: () => import('@/views/Scooters.vue'),
          meta: { title: 'Scooter Management', icon: 'Van' }
        },
        {
          path: 'stores',
          name: 'Stores',
          component: () => import('@/views/Stores.vue'),
          meta: { title: 'Store Management', icon: 'Shop' }
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/Users.vue'),
          meta: { title: 'User Management', icon: 'User' }
        },
        {
          path: 'pricing',
          name: 'Pricing',
          component: () => import('@/views/Pricing.vue'),
          meta: { title: 'Pricing Plans', icon: 'PriceTag' }
        },
        {
          path: 'bookings',
          name: 'Bookings',
          component: () => import('@/views/Bookings.vue'),
          meta: { title: 'Guest Bookings', icon: 'Odometer' }
        },
        {
          path: 'feedback',
          name: 'Feedback',
          component: () => import('@/views/Feedback.vue'),
          meta: { title: 'Feedback Issues', icon: 'User' }
        },
        {
          path: 'revenue',
          name: 'Revenue',
          component: () => import('@/views/Revenue.vue'),
          meta: { title: 'Revenue', icon: 'Histogram' }
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  const token = getToken()
  if (to.meta.requiresAuth === false) {
    if (token && to.path === '/login') {
      next('/dashboard')
    } else {
      next()
    }
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
