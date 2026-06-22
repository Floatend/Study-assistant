import type { Task } from './task'

export interface TrendPoint {
  date: string
  minutes: number
  completedTasks: number
}

export interface DashboardData {
  todayActualMinutes: number
  todayTaskCount: number
  completedTaskCount: number
  todayTasks: Task[]
  adviceDays: number
  adviceStartDate: string
  adviceEndDate: string
  latestAiAdvice: string
  aiAdviceStale: boolean
  aiAdviceSourceHash: string
}

export interface TaskStatusCount {
  status: number
  count: number
}
