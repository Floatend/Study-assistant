import { request } from '@/api/request'
import type { LoginPayload, LoginResult, PasswordChangePayload, UserProfile } from '@/types/auth'

export function login(payload: LoginPayload) {
  return request<LoginResult>({ url: '/api/auth/login', method: 'POST', data: payload })
}

export function fetchCurrentUser() {
  return request<UserProfile>({ url: '/api/auth/me', method: 'GET' })
}

export function logout() {
  return request<void>({ url: '/api/auth/logout', method: 'POST' })
}

export function changePassword(payload: PasswordChangePayload) {
  return request<void>({ url: '/api/auth/password', method: 'PUT', data: payload })
}
