import type { Note, NoteCategory } from '@/types/note'

export interface NoteCategoryTreeNode {
  key: string
  label: string
  value: string
  count: number
  leafValues: string[]
  children: NoteCategoryTreeNode[]
}

export function splitCategoryPath(category: string) {
  return category.split(/\s*(?:\/|>|::|\\)\s*/).map((part) => part.trim()).filter(Boolean)
}

export function buildNoteCategoryTree(items: NoteCategory[]): NoteCategoryTreeNode[] {
  const roots: NoteCategoryTreeNode[] = []
  for (const item of items) {
    const parts = splitCategoryPath(item.name)
    if (!parts.length) continue
    let current = roots
    const path: string[] = []
    parts.forEach((part, index) => {
      path.push(part)
      let node = current.find((candidate) => candidate.label === part)
      if (!node) {
        node = {
          key: path.join('/'),
          label: part,
          value: index === parts.length - 1 ? item.name : path.join('/'),
          count: 0,
          leafValues: [],
          children: [],
        }
        current.push(node)
      }
      node.count += item.count
      if (!node.leafValues.includes(item.name)) node.leafValues.push(item.name)
      node.value = index === parts.length - 1 ? item.name : path.join('/')
      current = node.children
    })
  }
  return roots
}

export function findNoteCategoryNode(nodes: NoteCategoryTreeNode[], value: string): NoteCategoryTreeNode | null {
  if (!value) return null
  for (const node of nodes) {
    if (node.value === value) return node
    const match = findNoteCategoryNode(node.children, value)
    if (match) return match
  }
  return null
}

export function summarizeNoteCategories(notes: Note[]): NoteCategory[] {
  const counts = new Map<string, number>()
  for (const note of notes) {
    const category = note.category?.trim() || '未分类'
    counts.set(category, (counts.get(category) ?? 0) + 1)
  }
  return [...counts.entries()]
    .sort((left, right) => right[1] - left[1] || left[0].localeCompare(right[0]))
    .map(([name, count]) => ({ name, count }))
}
