import { request } from './request'
import type { Review } from '@/types/review'

export function generateAdvice(days = 2) {
  return request<Review>({ url: '/api/ai/advice', method: 'POST', params: { days } })
}

export function generateDailyReview(date?: string) {
  return request<Review>({ url: '/api/ai/daily-review', method: 'POST', data: { date } })
}

export function generateWeeklyReview(weekStart?: string, weekEnd?: string) {
  return request<Review>({ url: '/api/ai/weekly-review', method: 'POST', data: { weekStart, weekEnd } })
}
