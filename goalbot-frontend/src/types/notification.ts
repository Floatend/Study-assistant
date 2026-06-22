export interface Notification {
  id: number
  userId: number
  title: string
  content: string
  notifyTime: string
  channel: number
  status: number
  createdAt?: string
  updatedAt?: string
}
