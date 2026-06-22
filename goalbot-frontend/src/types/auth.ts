export type UserRole = 'ADMIN' | 'USER'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  feishuUserId?: string
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

export interface UserCreatePayload {
  username: string
  password: string
  nickname?: string
  feishuUserId?: string
  role: UserRole
}

export interface UserUpdatePayload {
  nickname?: string
  feishuUserId?: string
  role?: UserRole
  status?: 0 | 1
}
