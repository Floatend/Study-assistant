import { request } from './request'
import type { Note, NoteCategory, NotePayload, NoteQuery, PublicNoteItem, PublicNotePage, PublicNoteQuery, PublicNoteNavigation } from '@/types/note'

export function fetchNotes(params?: NoteQuery) {
  return request<Note[]>({ url: '/api/notes', method: 'GET', params })
}

export function fetchNote(id: number) {
  return request<Note>({ url: `/api/notes/${id}`, method: 'GET' })
}

export function fetchNoteCategories(options: { silent?: boolean } = {}) {
  return request<NoteCategory[]>({ url: '/api/notes/categories', method: 'GET', ...options })
}

export function createNote(data: NotePayload) {
  return request<Note>({ url: '/api/notes', method: 'POST', data })
}

export function updateNote(id: number, data: Partial<NotePayload>) {
  return request<Note>({ url: `/api/notes/${id}`, method: 'PUT', data })
}

export function deleteNote(id: number) {
  return request<void>({ url: `/api/notes/${id}`, method: 'DELETE' })
}

export function uploadNote(file: File, options: { title?: string; tags?: string; category?: string } = {}) {
  const formData = new FormData()
  formData.append('file', file)
  return request<Note>({
    url: '/api/notes/upload',
    method: 'POST',
    data: formData,
    params: options,
    timeout: 60000
  })
}

export function fetchOfficialNotes(params?: NoteQuery) {
  return request<Note[]>({ url: '/api/public/notes', method: 'GET', params, silent: true })
}

export function fetchOfficialNote(id: number) {
  return request<Note>({ url: `/api/public/notes/${id}`, method: 'GET', silent: true })
}

export function fetchOfficialNoteCategories() {
  return request<NoteCategory[]>({ url: '/api/public/notes/categories', method: 'GET', silent: true })
}

export function searchOfficialNotes(params: PublicNoteQuery, signal?: AbortSignal) {
  return request<PublicNotePage>({ url: '/api/public/notes/search', method: 'GET', params, signal, silent: true })
}

export function fetchRelatedNotes(id: number) {
  return request<PublicNoteItem[]>({ url: `/api/public/notes/${id}/related`, method: 'GET', silent: true })
}

export function fetchNoteNavigation(id: number, params: PublicNoteQuery) {
  return request<PublicNoteNavigation>({ url: `/api/public/notes/${id}/navigation`, method: 'GET', params, silent: true })
}
