export type UserRole = 'ADMIN'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  role: UserRole
  status: 0 | 1
  lastLoginAt?: string
  createdAt: string
  updatedAt: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  expiresAt: string
  user: UserProfile
}

export interface PasswordChangePayload {
  currentPassword: string
  newPassword: string
}
