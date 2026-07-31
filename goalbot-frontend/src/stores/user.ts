import { defineStore } from 'pinia'
import { fetchCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import type { LoginPayload, UserProfile } from '@/types/auth'

const TOKEN_KEY = 'linge-owner-auth-token'
const USER_KEY = 'linge-owner-auth-user'

function storedUser(): UserProfile | null {
  const value = localStorage.getItem(USER_KEY)
  if (!value) return null
  try {
    return JSON.parse(value) as UserProfile
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: storedUser(),
    loading: false
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
    isAdmin: (state) => state.profile?.role === 'ADMIN',
    displayName: (state) => state.profile?.nickname || state.profile?.username || '用户'
  },
  actions: {
    async login(payload: LoginPayload) {
      this.loading = true
      try {
        const result = await loginApi(payload)
        this.token = result.token
        this.profile = result.user
        localStorage.setItem(TOKEN_KEY, result.token)
        localStorage.setItem(USER_KEY, JSON.stringify(result.user))
        return result.user
      } finally {
        this.loading = false
      }
    },
    async loadCurrentUser() {
      if (!this.token) return null
      const user = await fetchCurrentUser()
      this.profile = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },
    async logout() {
      try {
        if (this.token) await logoutApi()
      } finally {
        this.clearSession()
      }
    },
    clearSession() {
      this.token = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
