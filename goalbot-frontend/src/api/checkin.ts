import { request } from './request'
import type { Checkin, CheckinPayload, CheckinStats } from '@/types/checkin'

export function createCheckin(data: CheckinPayload) {
  return request<Checkin>({ url: '/api/checkins', method: 'POST', data })
}

export function fetchRecentCheckins(limit = 10) {
  return request<Checkin[]>({ url: '/api/checkins/recent', method: 'GET', params: { limit } })
}

export function fetchCheckinStats(params?: { startDate?: string; endDate?: string }) {
  return request<CheckinStats>({ url: '/api/checkins/stats', method: 'GET', params })
}
