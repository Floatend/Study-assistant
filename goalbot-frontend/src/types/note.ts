export interface Note {
  id: number
  userId: number
  title: string
  fileName?: string | null
  summary?: string | null
  content: string
  tags?: string | null
  category?: string | null
  published: boolean
  official: boolean
  authorName?: string | null
  wordCount: number
  createdAt: string
  updatedAt: string
}

export interface NoteQuery {
  keyword?: string
  category?: string
  published?: boolean
  limit?: number
}

export interface NoteCategory {
  name: string
  count: number
}

export interface NotePayload {
  title: string
  content: string
  tags?: string
  category?: string
  published?: boolean
  official?: boolean
}
