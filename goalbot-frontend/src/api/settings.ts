import { request } from './request'
import type {
  AssistantSettings,
  AssistantSettingsUpdateRequest,
  ProactiveMessageType
} from '@/types/settings'
import type { Notification } from '@/types/notification'

export function fetchAssistantSettings() {
  return request<AssistantSettings>({ url: '/api/settings/assistant', method: 'GET' })
}

export function updateAssistantSettings(data: AssistantSettingsUpdateRequest) {
  return request<AssistantSettings>({ url: '/api/settings/assistant', method: 'PUT', data })
}

export function sendAssistantTestMessage() {
  return request<Notification>({ url: '/api/settings/assistant/test-message', method: 'POST' })
}

export function sendAssistantMessageNow(type: ProactiveMessageType) {
  return request<Notification>({ url: `/api/settings/assistant/send-now/${type}`, method: 'POST' })
}
