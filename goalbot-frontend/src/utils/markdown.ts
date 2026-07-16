export interface MarkdownHeading {
  id: string
  level: number
  text: string
}

type HeadingCounter = Map<string, number>

export function extractMarkdownHeadings(content?: string | null): MarkdownHeading[] {
  if (!content) return []

  const counters: HeadingCounter = new Map()
  return content
    .split(/\r?\n/)
    .map((line) => line.match(/^\s{0,3}(#{1,4})\s+(.+?)\s*#*\s*$/))
    .filter((match): match is RegExpMatchArray => Boolean(match))
    .map((match) => {
      const text = cleanHeadingText(match[2])
      return {
        level: match[1].length,
        text,
        id: createHeadingId(text, counters)
      }
    })
}

export function createHeadingId(text: string, counters: HeadingCounter = new Map()): string {
  const base = cleanHeadingText(text)
    .normalize('NFKD')
    .toLowerCase()
    .replace(/[^\w\u3400-\u9fff\s-]/g, '')
    .trim()
    .replace(/\s+/g, '-') || 'section'
  const count = counters.get(base) ?? 0
  counters.set(base, count + 1)
  return count === 0 ? `note-${base}` : `note-${base}-${count + 1}`
}

function cleanHeadingText(value: string): string {
  return value
    .replace(/!?(?:\[([^\]]*)\]\([^)]*\))/g, '$1')
    .replace(/[`*_~]/g, '')
    .trim()
}
