export interface Review {
  id: number
  userId: number
  reviewDate: string
  type: number
  summary?: string
  aiAdvice?: string
  createdAt?: string
}
