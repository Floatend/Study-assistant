import { request } from './request'
import type { DashboardData, TaskStatusCount, TrendPoint } from '@/types/dashboard'
import type { Checkin } from '@/types/checkin'
import type { Review } from '@/types/review'

export function fetchDashboard(adviceDays = 2) {
  return request<DashboardData>({ url: '/api/dashboard', method: 'GET', params: { adviceDays } })
}

export function refreshDashboardAdvice(adviceDays = 2) {
  return request<Review>({ url: '/api/dashboard/advice/refresh', method: 'POST', params: { adviceDays } })
}

export function fetchStudyDuration(params?: { startDate?: string; endDate?: string }) {
  return request<TrendPoint[]>({ url: '/api/analytics/study-duration', method: 'GET', params })
}

export function fetchTaskStatus(params?: { startDate?: string; endDate?: string }) {
  return request<TaskStatusCount[]>({ url: '/api/analytics/task-status', method: 'GET', params })
}

export function fetchAnalyticsRecentCheckins(limit = 10) {
  return request<Checkin[]>({ url: '/api/analytics/recent-checkins', method: 'GET', params: { limit } })
}
