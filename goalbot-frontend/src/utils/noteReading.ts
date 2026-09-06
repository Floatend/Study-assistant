export interface ReadingPosition {
  noteId: number
  revision: string
  progress: number
  savedAt: number
}

export const READING_STORAGE_KEY = 'linge-note-reading-v1'
const MAX_RECORDS = 50
const MAX_AGE = 90 * 24 * 60 * 60 * 1000

function readPositions(): ReadingPosition[] {
  try {
    const parsed: unknown = JSON.parse(localStorage.getItem(READING_STORAGE_KEY) || '[]')
    if (!Array.isArray(parsed)) return []
    return parsed.filter((item): item is ReadingPosition => !!item && Number.isSafeInteger(item.noteId) && item.noteId > 0
      && typeof item.revision === 'string' && Number.isFinite(item.progress) && item.progress >= .03 && item.progress < .98
      && Number.isFinite(item.savedAt) && item.savedAt <= Date.now() && Date.now() - item.savedAt < MAX_AGE)
      .sort((a, b) => b.savedAt - a.savedAt).slice(0, MAX_RECORDS)
  } catch { return [] }
}

export function loadReadingPosition(noteId: number, revision: string): ReadingPosition | null {
  return readPositions().find((item) => item.noteId === noteId && item.revision === revision) ?? null
}

export function saveReadingPosition(noteId: number, revision: string, progress: number) {
  if (!Number.isSafeInteger(noteId) || noteId <= 0 || !Number.isFinite(progress)) return
  try {
    const records = readPositions().filter((item) => item.noteId !== noteId)
    // Start/end positions are not useful resume destinations.
    if (progress >= .03 && progress < .98) records.unshift({ noteId, revision, progress, savedAt: Date.now() })
    localStorage.setItem(READING_STORAGE_KEY, JSON.stringify(records.slice(0, MAX_RECORDS)))
  } catch { /* Private browsing and full storage must not interrupt reading. */ }
}

export function highlightMatches(text: string, keyword: string): { text: string; match: boolean }[] {
  if (!keyword.trim()) return [{ text, match: false }]
  const literal = keyword.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const matches = text.matchAll(new RegExp(literal, 'gi'))
  const parts: { text: string; match: boolean }[] = []
  let offset = 0
  for (const match of matches) {
    const index = match.index!
    if (index > offset) parts.push({ text: text.slice(offset, index), match: false })
    parts.push({ text: match[0], match: true })
    offset = index + match[0].length
  }
  if (offset < text.length) parts.push({ text: text.slice(offset), match: false })
  return parts
}
