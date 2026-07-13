export interface Note {
  id: number
  userId: number
  title: string
  fileName?: string | null
  summary?: string | null
  content: string
  tags?: string | null
  wordCount: number
  createdAt: string
  updatedAt: string
}

export interface NoteQuery {
  keyword?: string
  limit?: number
}

export interface NotePayload {
  title: string
  content: string
  tags?: string
}
