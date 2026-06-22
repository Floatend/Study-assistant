import { defineStore } from 'pinia'
import { fetchGoals } from '@/api/goal'
import type { Goal, GoalQuery } from '@/types/goal'

export const useGoalStore = defineStore('goal', {
  state: () => ({
    goals: [] as Goal[],
    loading: false
  }),
  actions: {
    async loadGoals(params?: GoalQuery) {
      this.loading = true
      try {
        this.goals = await fetchGoals(params)
      } finally {
        this.loading = false
      }
    }
  }
})
