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

export interface PublicNoteItem {
  id: number
  title: string
  category?: string | null
  tags?: string | null
  excerpt: string
  wordCount: number
  updatedAt: string
}

export interface PublicNoteQuery {
  keyword?: string
  category?: string
  descendants?: boolean
  page?: number
  pageSize?: number
}

export interface PublicNotePage {
  items: PublicNoteItem[]
  total: number
  page: number
  pageSize: number
}

export interface PublicNoteNavigation {
  previous: PublicNoteItem | null
  next: PublicNoteItem | null
  position: number
}
