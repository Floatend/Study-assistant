import { createRouter, createWebHistory } from 'vue-router'

const TOKEN_KEY = 'linge-owner-auth-token'
const USER_KEY = 'linge-owner-auth-user'

function hasStoredAdmin() {
  if (!localStorage.getItem(TOKEN_KEY)) return false
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')?.role === 'ADMIN'
  } catch {
    return false
  }
}

function clearStoredSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    // In-page article changes scroll after the asynchronous content is rendered.
    if (to.path === from.path) return false
    return savedPosition ?? { top: 0, behavior: 'instant' }
  },
  routes: [
    { path: '/', name: 'PublicBlog', component: () => import('@/views/PublicBlog.vue'), meta: { public: true } },
    { path: '/notes', name: 'OfficialNotes', component: () => import('@/views/OfficialNotes.vue'), meta: { public: true } },
    { path: '/journey', name: 'Journey', component: () => import('@/views/Journey.vue'), meta: { public: true } },
    { path: '/about', name: 'About', component: () => import('@/views/About.vue'), meta: { public: true } },
    { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true } },
    { path: '/notebook', name: 'Notebook', component: () => import('@/views/Notebook.vue'), meta: { admin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach((to) => {
  const isAdmin = hasStoredAdmin()
  if (to.path === '/login') {
    if (isAdmin) return '/notebook'
    if (localStorage.getItem(TOKEN_KEY)) clearStoredSession()
    return true
  }
  if (to.meta.public) return true
  if (!isAdmin) {
    clearStoredSession()
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
