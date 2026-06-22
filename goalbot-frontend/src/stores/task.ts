import { defineStore } from 'pinia'
import { fetchTasks, fetchTodayTasks } from '@/api/task'
import type { Task, TaskQuery } from '@/types/task'

export const useTaskStore = defineStore('task', {
  state: () => ({
    tasks: [] as Task[],
    todayTasks: [] as Task[],
    loading: false
  }),
  actions: {
    async loadTasks(params?: TaskQuery) {
      this.loading = true
      try {
        this.tasks = await fetchTasks(params)
      } finally {
        this.loading = false
      }
    },
    async loadTodayTasks() {
      this.todayTasks = await fetchTodayTasks()
    }
  }
})
