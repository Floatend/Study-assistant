export interface MarkdownHeading {
  id: string
  level: number
  text: string
}

type HeadingCounter = Map<string, number>

const calloutIconPaths: Record<string, string[]> = {
  note: ['m199.04 672.64 193.984 112 224-387.968-193.92-112-224 388.032zm-23.872 60.16 32.896 148.288 144.896-45.696zM455.04 229.248l193.92 112 56.704-98.112-193.984-112zM104.32 708.8l384-665.024 304.768 175.936L409.152 884.8h.064l-248.448 78.336zm384 254.272v-64h448v64z'],
  lightning: ['M288 671.36v64.128A239.81 239.81 0 0 1 63.744 496.192a240.32 240.32 0 0 1 199.488-236.8 256.128 256.128 0 0 1 487.872-30.976A256.064 256.064 0 0 1 736 734.016v-64.768a192 192 0 0 0 3.328-377.92l-35.2-6.592-12.8-33.408a192.064 192.064 0 0 0-365.952 23.232l-9.92 40.896-41.472 7.04a176.32 176.32 0 0 0-146.24 173.568c0 91.968 70.464 167.36 160.256 175.232z', 'M416 736a32 32 0 0 1-27.776-47.872l128-224a32 32 0 1 1 55.552 31.744L471.168 672H608a32 32 0 0 1 27.776 47.872l-128 224a32 32 0 1 1-55.68-31.744L552.96 736z'],
  warning: ['M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896m0 832a384 384 0 0 0 0-768 384 384 0 0 0 0 768m48-176a48 48 0 1 1-96 0 48 48 0 0 1 96 0m-48-464a32 32 0 0 1 32 32v288a32 32 0 0 1-64 0V288a32 32 0 0 1 32-32']
}

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
 * Convert Obsidian callout markers into a stable Markdown form. The marker is
 * replaced by an icon during HTML enhancement, while its type text stays out
 * of the rendered title.
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

function createCalloutIcon(document: Document, type: string): SVGSVGElement | null {
  const key = type === 'danger' || type === 'failure' || type === 'tip' ? 'lightning' : type === 'warning' || type === 'caution' ? 'warning' : type
  const paths = calloutIconPaths[key]
  if (!paths) return null

  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  svg.classList.add('obsidian-callout-svg')
  svg.setAttribute('viewBox', '0 0 1024 1024')
  svg.setAttribute('aria-hidden', 'true')
  paths.forEach((pathData) => {
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    path.setAttribute('fill', 'currentColor')
    path.setAttribute('d', pathData)
    svg.append(path)
  })
  return svg
}

/**
 * Restore the visual callout container while leaving the type label empty.
 * The icon and custom title remain useful; only NOTE/WARNING/TIP is hidden.
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
    const icon = document.createElement('span')
    icon.className = 'obsidian-callout-icon'
    icon.setAttribute('data-callout-icon', match[1].toLowerCase())
    icon.setAttribute('aria-hidden', 'true')
    const iconSvg = createCalloutIcon(document, match[1].toLowerCase())
    if (iconSvg) icon.append(iconSvg)
    label.append(icon)
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
