import { request } from './request'
import type { Note, NotePayload, NoteQuery } from '@/types/note'

export function fetchNotes(params?: NoteQuery) {
  return request<Note[]>({ url: '/api/notes', method: 'GET', params })
}

export function fetchNote(id: number) {
  return request<Note>({ url: `/api/notes/${id}`, method: 'GET' })
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

export function uploadNote(file: File, options: { title?: string; tags?: string } = {}) {
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
  return request<Note[]>({ url: '/api/public/notes', method: 'GET', params })
}

export function fetchOfficialNote(id: number) {
  return request<Note>({ url: `/api/public/notes/${id}`, method: 'GET' })
}
