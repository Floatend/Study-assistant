export interface AssistantSettings {
  id?: number
  userId?: number
  proactiveEnabled: boolean
  feishuEnabled: boolean
  feishuChatId?: string | null
  morningEnabled: boolean
  morningTime: string
  reviewEnabled: boolean
  reviewTime: string
  weeklyEnabled: boolean
  weeklyDay: number
  weeklyTime: string
  periodicNudgeEnabled: boolean
  periodicNudgeIntervalHours: number
  aiBriefingEnabled: boolean
  aiBriefingTime: string
  aiBriefingSourceName: string
  aiBriefingSourceUrl?: string | null
  adviceDays: number
  quietEnabled: boolean
  quietStartTime?: string | null
  quietEndTime?: string | null
  globalReminderEnabled: boolean
  feishuAppConfigured: boolean
  feishuDefaultChatConfigured: boolean
  effectiveFeishuChatConfigured: boolean
  feishuLongConnectionEnabled: boolean
  difyChatConfigured: boolean
  difyWorkflowConfigured: boolean
  aiBriefingSourceConfigured: boolean
  createdAt?: string
  updatedAt?: string
}

export type AssistantSettingsUpdateRequest = Partial<
  Pick<
    AssistantSettings,
    | 'proactiveEnabled'
    | 'feishuEnabled'
    | 'feishuChatId'
    | 'morningEnabled'
    | 'morningTime'
    | 'reviewEnabled'
    | 'reviewTime'
    | 'weeklyEnabled'
    | 'weeklyDay'
    | 'weeklyTime'
    | 'periodicNudgeEnabled'
    | 'periodicNudgeIntervalHours'
    | 'aiBriefingEnabled'
    | 'aiBriefingTime'
    | 'aiBriefingSourceName'
    | 'aiBriefingSourceUrl'
    | 'adviceDays'
    | 'quietEnabled'
    | 'quietStartTime'
    | 'quietEndTime'
  >
>

export type ProactiveMessageType =
  | 'daily-task'
  | 'daily-review'
  | 'weekly-review'
  | 'ai-briefing'
  | 'periodic-nudge'
