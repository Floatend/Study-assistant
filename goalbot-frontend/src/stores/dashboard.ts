import { defineStore } from 'pinia'
import { fetchDashboard, refreshDashboardAdvice } from '@/api/dashboard'
import type { DashboardData } from '@/types/dashboard'

export const useDashboardStore = defineStore('dashboard', {
  state: () => ({
    data: null as DashboardData | null,
    loading: false,
    adviceLoading: false,
    adviceDays: Number(localStorage.getItem('goalbot-advice-days') || 2)
  }),
  actions: {
    setAdviceDays(days: number) {
      const normalized = Math.max(1, Math.min(3, days))
      this.adviceDays = normalized
      localStorage.setItem('goalbot-advice-days', String(normalized))
    },
    async loadDashboard(options?: { silent?: boolean }) {
      if (!options?.silent) {
        this.loading = true
      }
      try {
        this.data = await fetchDashboard(this.adviceDays)
      } finally {
        if (!options?.silent) {
          this.loading = false
        }
      }
    },
    async refreshAdvice() {
      if (this.adviceLoading) {
        return
      }
      this.adviceLoading = true
      try {
        const review = await refreshDashboardAdvice(this.adviceDays)
        if (this.data) {
          this.data.latestAiAdvice = review.aiAdvice ?? ''
        }
        await this.loadDashboard({ silent: true })
      } finally {
        this.adviceLoading = false
      }
    }
  }
})
