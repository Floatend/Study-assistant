import { request } from './request'
import type { IcsImportOptions, IcsImportResult, Task, TaskPayload, TaskQuery } from '@/types/task'

export function fetchTasks(params?: TaskQuery) {
  return request<Task[]>({ url: '/api/tasks', method: 'GET', params })
}

export function fetchTodayTasks() {
  return request<Task[]>({ url: '/api/tasks/today', method: 'GET' })
}

export function fetchCalendarTasks(params?: { startDate?: string; endDate?: string }) {
  return request<Task[]>({ url: '/api/tasks/calendar', method: 'GET', params })
}

export function createTask(data: TaskPayload) {
  return request<Task>({ url: '/api/tasks', method: 'POST', data })
}

export function updateTask(id: number, data: Partial<TaskPayload>) {
  return request<Task>({ url: `/api/tasks/${id}`, method: 'PUT', data })
}

export function completeTask(id: number) {
  return request<Task>({ url: `/api/tasks/${id}/complete`, method: 'PUT' })
}

export function deleteTask(id: number) {
  return request<void>({ url: `/api/tasks/${id}`, method: 'DELETE' })
}

export function importIcs(file: File, options: IcsImportOptions = {}) {
  const formData = new FormData()
  formData.append('file', file)
  return request<IcsImportResult>({
    url: '/api/tasks/import/ics',
    method: 'POST',
    data: formData,
    params: options,
    timeout: 60000
  })
}
