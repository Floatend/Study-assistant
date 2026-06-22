export interface Goal {
  id: number
  userId: number
  title: string
  description?: string
  startDate?: string
  endDate?: string
  priority: number
  status: number
  totalTaskCount?: number
  completedTaskCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface GoalPayload {
  title: string
  description?: string
  startDate?: string
  endDate?: string
  priority: number
  status: number
}

export interface GoalQuery {
  status?: number
  priority?: number
  keyword?: string
}
