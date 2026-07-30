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
 * Convert the Obsidian callout forms used in imported notes into Markdown
 * blockquotes that markdown-it can render consistently on the web.
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

function formatCalloutLine(type: string, fold: string | undefined, title: string | undefined): string {
  const marker = `**[!${type.toUpperCase()}]${fold ?? ''}**`
  return `> ${marker}${title ? ` ${title}` : ''}`
}

/**
 * Turn the normalized marker blockquote into the DOM shape used by Obsidian
 * callouts. The HTML was sanitized before this controlled decoration step.
 */
export function enhanceObsidianCallouts(html: string): string {
  if (!html || typeof DOMParser === 'undefined') return html

  const document = new DOMParser().parseFromString(html, 'text/html')
  document.body.querySelectorAll('blockquote').forEach((blockquote) => {
    const firstChild = blockquote.firstElementChild
    const marker = firstChild?.firstElementChild
    if (!firstChild || firstChild.tagName !== 'P' || !marker || marker.tagName !== 'STRONG') return

    const match = marker.textContent?.trim().match(/^\[!([A-Za-z][\w-]*)\]([+-])?$/)
    if (!match) return

    const label = document.createElement('span')
    label.className = 'obsidian-callout-label'
    label.textContent = match[1].toUpperCase()
    marker.replaceWith(label)
    firstChild.classList.add('obsidian-callout-title')
    blockquote.classList.add('obsidian-callout')
    blockquote.setAttribute('data-callout', match[1].toLowerCase())
    if (match[2]) blockquote.setAttribute('data-callout-fold', match[2] === '-' ? 'closed' : 'open')
  })

  return document.body.innerHTML
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
