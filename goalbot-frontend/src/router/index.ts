import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'PublicBlog', component: () => import('@/views/PublicBlog.vue'), meta: { public: true } },
    { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true } },
    { path: '/dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue') },
    { path: '/goals', redirect: '/dashboard' },
    { path: '/tasks', name: 'Tasks', component: () => import('@/views/Tasks.vue') },
    { path: '/calendar', name: 'Calendar', component: () => import('@/views/Calendar.vue') },
    { path: '/checkin', redirect: '/dashboard' },
    { path: '/review', name: 'Review', component: () => import('@/views/Review.vue') },
    { path: '/analytics', name: 'Analytics', component: () => import('@/views/Analytics.vue') },
    { path: '/settings', name: 'Settings', component: () => import('@/views/Settings.vue') },
    { path: '/users', name: 'Users', component: () => import('@/views/Users.vue'), meta: { admin: true } }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('goalbot-auth-token')
  if (to.meta.public) {
    if (to.path === '/login' && token) return '/dashboard'
    return true
  }
  if (!token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.admin) {
    try {
      const user = JSON.parse(localStorage.getItem('goalbot-auth-user') || 'null')
      if (user?.role !== 'ADMIN') return '/dashboard'
    } catch {
      return '/dashboard'
    }
  }
  return true
})

export default router
