import { request } from './request'
import type { Goal, GoalPayload, GoalQuery } from '@/types/goal'

export function fetchGoals(params?: GoalQuery) {
  return request<Goal[]>({ url: '/api/goals', method: 'GET', params })
}

export function fetchGoal(id: number) {
  return request<Goal>({ url: `/api/goals/${id}`, method: 'GET' })
}

export function createGoal(data: GoalPayload) {
  return request<Goal>({ url: '/api/goals', method: 'POST', data })
}

export function updateGoal(id: number, data: Partial<GoalPayload>) {
  return request<Goal>({ url: `/api/goals/${id}`, method: 'PUT', data })
}

export function deleteGoal(id: number) {
  return request<void>({ url: `/api/goals/${id}`, method: 'DELETE' })
}
