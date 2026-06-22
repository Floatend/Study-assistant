import { request } from './request'
import type { Review } from '@/types/review'

export function fetchReviews(params?: { type?: number; startDate?: string; endDate?: string }) {
  return request<Review[]>({ url: '/api/reviews', method: 'GET', params })
}

export function fetchLatestReview(type?: number) {
  return request<Review | null>({ url: '/api/reviews/latest', method: 'GET', params: { type } })
}
