export interface Task {
  id: number
  userId: number
  goalId?: number
  goalTitle?: string
  title: string
  description?: string
  planDate: string
  startTime?: string
  endTime?: string
  plannedMinutes: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface TaskPayload {
  goalId?: number
  title: string
  description?: string
  planDate: string
  startTime?: string
  endTime?: string
  plannedMinutes: number
  status: number
}

export interface TaskQuery {
  date?: string
  goalId?: number
  status?: number
}

export interface IcsImportEvent {
  uid?: string
  title: string
  description?: string
  location?: string
  planDate: string
  startTime?: string
  endTime?: string
  plannedMinutes: number
  allDay: boolean
  skipped: boolean
  skipReason?: string
}

export interface IcsImportResult {
  sourceEventCount: number
  expandedEventCount: number
  importedCount: number
  skippedCount: number
  dryRun: boolean
  events: IcsImportEvent[]
  importedTasks: Task[]
  warnings: string[]
}

export interface IcsImportOptions {
  dryRun?: boolean
  startDate?: string
  endDate?: string
  skipExisting?: boolean
}
