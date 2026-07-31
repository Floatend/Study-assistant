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

/**
 * Convert Obsidian callout markers into ordinary blockquote lines. The type
 * label and fold marker are intentionally discarded so public notes read as
 * normal content instead of exposing Obsidian-specific UI.
 */
export function normalizeObsidianMarkdown(content?: string | null): string {
  if (!content) return ''

  let fence: string | null = null
  return content
    .replace(/\r\n?/g, '\n')
    .split('\n')
    .map((line) => {
      const fenceMatch = line.match(/^\s*(`{3,}|~{3,})/)
      if (fenceMatch) {
        if (!fence) fence = fenceMatch[1][0]
        else if (fence === fenceMatch[1][0]) fence = null
        return line
      }
      if (fence) return line

      const callout = line.match(/^\s*>\s*\[!([A-Za-z][\w-]*)\]([+-])?(?:\s+(.*?))?\s*$/)
      if (callout) return formatCalloutLine(callout[1], callout[2], callout[3])

      const standaloneCallout = line.match(/^\s*\[!([A-Za-z][\w-]*)\]([+-])?(?:\s+(.*?))?\s*$/)
      if (standaloneCallout) return formatCalloutLine(standaloneCallout[1], standaloneCallout[2], standaloneCallout[3])

      const shorthandCallout = line.match(/^\s*!([A-Za-z][\w-]*)\b(?:\s+(.*?))?\s*$/)
      if (shorthandCallout) return formatCalloutLine(shorthandCallout[1], undefined, shorthandCallout[2])

      return line
    })
    .join('\n')
}

function formatCalloutLine(_type: string, _fold: string | undefined, title: string | undefined): string {
  return title ? `> ${title}` : '>'
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
