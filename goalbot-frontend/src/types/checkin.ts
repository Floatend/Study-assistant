export interface Checkin {
  id: number
  userId: number
  taskId: number
  taskTitle?: string
  actualMinutes: number
  content?: string
  mood?: number
  difficulty?: number
  createdAt: string
}

export interface CheckinPayload {
  taskId: number
  actualMinutes?: number
  content?: string
  mood?: number
  difficulty?: number
}

export interface CheckinStats {
  totalMinutes: number
  checkinCount: number
  completedTaskCount: number
  averageMood: number
  averageDifficulty: number
}
