import { request } from '@/api/request'
import type { UserCreatePayload, UserProfile, UserUpdatePayload } from '@/types/auth'

export function fetchUsers(params?: { keyword?: string; status?: number }) {
  return request<UserProfile[]>({ url: '/api/admin/users', method: 'GET', params })
}

export function createUser(payload: UserCreatePayload) {
  return request<UserProfile>({ url: '/api/admin/users', method: 'POST', data: payload })
}

export function updateUser(id: number, payload: UserUpdatePayload) {
  return request<UserProfile>({ url: `/api/admin/users/${id}`, method: 'PUT', data: payload })
}

export function resetUserPassword(id: number, newPassword: string) {
  return request<void>({ url: `/api/admin/users/${id}/password`, method: 'PUT', data: { newPassword } })
}
