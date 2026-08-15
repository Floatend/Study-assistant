<template>
  <div ref="markdownRoot" class="markdown-content" v-html="html"></div>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import bash from 'highlight.js/lib/languages/bash'
import cpp from 'highlight.js/lib/languages/cpp'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdownLanguage from 'highlight.js/lib/languages/markdown'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import MarkdownIt from 'markdown-it'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { createHeadingId, enhanceObsidianCallouts, normalizeObsidianMarkdown } from '@/utils/markdown'

const props = defineProps<{
  content?: string | null
}>()

const markdownRoot = ref<HTMLDivElement>()

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('cpp', cpp)
hljs.registerLanguage('css', css)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdownLanguage)
hljs.registerLanguage('python', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('yaml', yaml)

const languageAliases: Record<string, string> = {
  c: 'cpp',
  'c++': 'cpp',
  cc: 'cpp',
  h: 'cpp',
  hpp: 'cpp',
  html: 'xml',
  jsx: 'javascript',
  md: 'markdown',
  py: 'python',
  sh: 'bash',
  shell: 'bash',
  tsx: 'typescript',
  vue: 'xml',
  yml: 'yaml'
}

const languageLabels: Record<string, string> = {
  bash: 'Shell',
  cpp: 'C++',
  css: 'CSS',
  java: 'Java',
  javascript: 'JavaScript',
  json: 'JSON',
  markdown: 'Markdown',
  python: 'Python',
  sql: 'SQL',
  typescript: 'TypeScript',
  xml: 'HTML / XML',
  yaml: 'YAML'
}

function normalizeLanguage(info: string) {
  const raw = info.trim().split(/\s+/)[0]?.replace(/^\{\.?|\}$/g, '').toLowerCase() ?? ''
  return languageAliases[raw] ?? raw
}

function highlightCode(code: string, info: string) {
  const language = normalizeLanguage(info)
  if (!language || !hljs.getLanguage(language)) return ''
  try {
    return hljs.highlight(code, { language, ignoreIllegals: true }).value
  } catch {
    return ''
  }
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[character] ?? character)
}

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true,
  highlight: (code, language) => highlightCode(code, language)
})

function renderMath(latex: string, displayMode: boolean) {
  try {
    return katex.renderToString(latex, {
      displayMode,
      output: 'htmlAndMathml',
      throwOnError: false,
      strict: 'ignore',
      trust: false
    })
  } catch {
    return `<span class="math-render-error">${escapeHtml(latex)}</span>`
  }
}

function isEscaped(source: string, index: number) {
  let slashCount = 0
  for (let cursor = index - 1; cursor >= 0 && source[cursor] === '\\'; cursor -= 1) slashCount += 1
  return slashCount % 2 === 1
}

function findClosingDelimiter(source: string, start: number, delimiter: string) {
  for (let cursor = start; cursor <= source.length - delimiter.length; cursor += 1) {
    if (source.startsWith(delimiter, cursor) && !isEscaped(source, cursor)) return cursor
  }
  return -1
}

function mathInline(state: any, silent: boolean) {
  const start = state.pos
  const source = state.src as string
  let opener = ''
  let closer = ''

  if (source.startsWith('\\(', start)) {
    opener = '\\('
    closer = '\\)'
  } else if (source[start] === '$' && source[start + 1] !== '$' && !isEscaped(source, start)) {
    opener = '$'
    closer = '$'
  } else {
    return false
  }

  const contentStart = start + opener.length
  const closeIndex = findClosingDelimiter(source, contentStart, closer)
  if (closeIndex < 0) return false

  const content = source.slice(contentStart, closeIndex)
  if (!content.trim() || /^\s|\s$/.test(content)) return false

  if (!silent) {
    const token = state.push('math_inline', 'math', 0)
    token.content = content
    token.markup = opener
  }
  state.pos = closeIndex + closer.length
  return true
}

function mathBlock(state: any, startLine: number, endLine: number, silent: boolean) {
  const start = state.bMarks[startLine] + state.tShift[startLine]
  const maximum = state.eMarks[startLine]
  const firstLine = state.src.slice(start, maximum).trim()
  const opener = firstLine.startsWith('$$') ? '$$' : firstLine.startsWith('\\[') ? '\\[' : ''
  if (!opener) return false

  const closer = opener === '$$' ? '$$' : '\\]'
  const firstContent = firstLine.slice(opener.length)
  const sameLineClose = findClosingDelimiter(firstContent, 0, closer)
  let nextLine = startLine
  let content = ''

  if (sameLineClose >= 0) {
    content = firstContent.slice(0, sameLineClose)
  } else {
    const lines = firstContent.trim() ? [firstContent] : []
    nextLine += 1
    while (nextLine < endLine) {
      const lineStart = state.bMarks[nextLine] + state.tShift[nextLine]
      const lineEnd = state.eMarks[nextLine]
      const line = state.src.slice(lineStart, lineEnd)
      const closeIndex = findClosingDelimiter(line, 0, closer)
      if (closeIndex >= 0) {
        lines.push(line.slice(0, closeIndex))
        break
      }
      lines.push(line)
      nextLine += 1
    }
    if (nextLine >= endLine) return false
    content = lines.join('\n')
  }

  if (silent) return true
  const token = state.push('math_block', 'math', 0)
  token.block = true
  token.content = content.trim()
  token.map = [startLine, nextLine + 1]
  token.markup = opener
  state.line = nextLine + 1
  return true
}

markdown.inline.ruler.after('backticks', 'math_inline', mathInline)
markdown.block.ruler.before('fence', 'math_block', mathBlock, { alt: ['paragraph', 'reference', 'blockquote', 'list'] })
markdown.renderer.rules.math_inline = (tokens, index) => renderMath(tokens[index].content, false)
markdown.renderer.rules.math_block = (tokens, index) => `<div class="math-display">${renderMath(tokens[index].content, true)}</div>\n`

const defaultHeadingOpen = markdown.renderer.rules.heading_open
markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
  const context = env as { headingIds?: Map<string, number> }
  const headingText = tokens[index + 1]?.content ?? ''
  tokens[index].attrSet('id', createHeadingId(headingText, context.headingIds ?? (context.headingIds = new Map())))
  return defaultHeadingOpen
    ? defaultHeadingOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

const defaultTableOpen = markdown.renderer.rules.table_open
const defaultTableClose = markdown.renderer.rules.table_close
markdown.renderer.rules.table_open = (tokens, index, options, env, self) => {
  const table = defaultTableOpen
    ? defaultTableOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
  return `<div class="markdown-table-wrap">${table}`
}
markdown.renderer.rules.table_close = (tokens, index, options, env, self) => {
  const table = defaultTableClose
    ? defaultTableClose(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
  return `${table}</div>`
}

const defaultImage = markdown.renderer.rules.image
markdown.renderer.rules.image = (tokens, index, options, env, self) => {
  tokens[index].attrSet('loading', 'lazy')
  tokens[index].attrSet('decoding', 'async')
  return defaultImage
    ? defaultImage(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

markdown.core.ruler.after('inline', 'task-list-items', (state) => {
  state.tokens.forEach((token, index) => {
    if (token.type !== 'inline' || state.tokens[index - 2]?.type !== 'list_item_open') return
    const firstChild = token.children?.[0]
    const match = firstChild?.type === 'text' ? firstChild.content.match(/^\[([ xX])\]\s+/) : null
    if (!firstChild || !match) return

    const checkbox = new state.Token('html_inline', '', 0)
    checkbox.content = `<input class="task-list-checkbox" type="checkbox" disabled${match[1].toLowerCase() === 'x' ? ' checked' : ''}>`
    firstChild.content = firstChild.content.slice(match[0].length)
    token.children?.unshift(checkbox)
    state.tokens[index - 2].attrJoin('class', 'task-list-item')
  })
})

markdown.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index]
  const language = normalizeLanguage(token.info)
  const label = languageLabels[language] ?? (language ? language.toUpperCase() : 'TEXT')
  const highlighted = highlightCode(token.content, token.info)
  const renderedCode = highlighted || markdown.utils.escapeHtml(token.content)
  const languageClass = language ? ` class="language-${escapeHtml(language)}"` : ''
  return `<div class="code-block-shell"><div class="code-block-toolbar"><span class="code-block-language">${escapeHtml(label)}</span><button class="code-copy-button" type="button" aria-label="复制代码">复制</button></div><pre><code${languageClass}>${renderedCode}</code></pre></div>`
}

const html = computed(() => {
  const raw = props.content?.trim()
  if (!raw) {
    return ''
  }
  const sanitized = DOMPurify.sanitize(markdown.render(normalizeObsidianMarkdown(raw), { headingIds: new Map() }), {
    USE_PROFILES: { html: true, mathMl: true }
  })
  return enhanceObsidianCallouts(sanitized)
})

async function copyCode(button: HTMLButtonElement) {
  const code = button.closest('.code-block-shell')?.querySelector('code')?.textContent ?? ''
  if (!code) return
  try {
    await navigator.clipboard.writeText(code)
    const originalLabel = button.textContent ?? '复制'
    button.textContent = '已复制'
    window.setTimeout(() => { button.textContent = originalLabel }, 1400)
  } catch {
    button.textContent = '复制失败'
    window.setTimeout(() => { button.textContent = '复制' }, 1400)
  }
}

function handleMarkdownClick(event: Event) {
  const target = event.target as HTMLElement | null
  const button = target?.closest<HTMLButtonElement>('.code-copy-button')
  if (button && markdownRoot.value?.contains(button)) {
    void copyCode(button)
    return
  }

  const title = target?.closest<HTMLElement>('.obsidian-callout.is-foldable > .obsidian-callout-title')
  if (title && markdownRoot.value?.contains(title)) toggleCallout(title)
}

function handleMarkdownKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' && event.key !== ' ') return
  const title = (event.target as HTMLElement | null)?.closest<HTMLElement>('.obsidian-callout.is-foldable > .obsidian-callout-title')
  if (!title || !markdownRoot.value?.contains(title)) return
  event.preventDefault()
  toggleCallout(title)
}

function toggleCallout(title: HTMLElement) {
  const callout = title.parentElement
  if (!callout?.classList.contains('obsidian-callout')) return
  const closed = callout.getAttribute('data-callout-fold') === 'closed'
  callout.setAttribute('data-callout-fold', closed ? 'open' : 'closed')
  title.setAttribute('aria-expanded', String(closed))
}

onMounted(() => {
  markdownRoot.value?.addEventListener('click', handleMarkdownClick)
  markdownRoot.value?.addEventListener('keydown', handleMarkdownKeydown)
})
onBeforeUnmount(() => {
  markdownRoot.value?.removeEventListener('click', handleMarkdownClick)
  markdownRoot.value?.removeEventListener('keydown', handleMarkdownKeydown)
})
watch(html, () => nextTick(() => markdownRoot.value?.scrollTo({ left: 0, top: 0 })))
</script>

<style scoped>
.markdown-content {
  color: var(--text);
  font-size: 14px;
  line-height: 1.75;
}

.markdown-content:empty::before {
  color: var(--subtle);
  content: "正文预览会显示在这里";
}

.markdown-content :deep(*) {
  overflow-wrap: anywhere;
}

.markdown-content :deep(p) {
  margin: 0 0 10px;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4) {
  margin: 14px 0 8px;
  color: var(--text);
  font-weight: 750;
  line-height: 1.35;
}

.markdown-content :deep(h1) {
  font-size: 20px;
}

.markdown-content :deep(h2) {
  font-size: 18px;
}

.markdown-content :deep(h3) {
  font-size: 16px;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0 12px;
  padding-left: 22px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
}

.markdown-content :deep(.task-list-item) {
  margin-left: -20px;
  list-style: none;
}

.markdown-content :deep(.task-list-checkbox) {
  width: 15px;
  height: 15px;
  margin: 0 8px 0 0;
  vertical-align: -2px;
  accent-color: var(--brand);
}

.markdown-content :deep(strong) {
  color: var(--text);
  font-weight: 750;
}

.markdown-content :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 3px solid var(--brand);
  border-radius: 4px;
  color: var(--muted);
  background: var(--brand-soft);
}

.markdown-content :deep(.obsidian-callout) {
  --callout-color: var(--brand);
  margin: 18px 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--callout-color) 35%, var(--surface));
  border-left: 4px solid var(--callout-color);
  border-radius: 8px;
  color: var(--text);
  background: color-mix(in srgb, var(--callout-color) 9%, var(--surface));
  font-style: normal;
  box-shadow: var(--shadow-soft);
}

.markdown-content :deep(.obsidian-callout[data-callout="tip"]),
.markdown-content :deep(.obsidian-callout[data-callout="success"]) {
  --callout-color: var(--brand-strong);
}

.markdown-content :deep(.obsidian-callout[data-callout="warning"]),
.markdown-content :deep(.obsidian-callout[data-callout="caution"]) {
  --callout-color: var(--accent);
}

.markdown-content :deep(.obsidian-callout[data-callout="danger"]),
.markdown-content :deep(.obsidian-callout[data-callout="failure"]) {
  --callout-color: var(--accent);
}

.markdown-content :deep(.obsidian-callout-title) {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 10px 13px 5px;
  color: var(--callout-color);
  font-style: normal;
  font-weight: 750;
}

.markdown-content :deep(.obsidian-callout.is-foldable > .obsidian-callout-title) {
  cursor: pointer;
  outline: none;
}

.markdown-content :deep(.obsidian-callout.is-foldable > .obsidian-callout-title:focus-visible) {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--callout-color) 28%, transparent) inset;
}

.markdown-content :deep(.obsidian-callout-fold) {
  width: 8px;
  height: 8px;
  margin-left: auto;
  border-right: 2px solid currentColor;
  border-bottom: 2px solid currentColor;
  transform: rotate(45deg) translateY(-2px);
  transition: transform .18s ease;
}

.markdown-content :deep(.obsidian-callout[data-callout-fold="closed"] .obsidian-callout-fold) {
  transform: rotate(-45deg) translate(-1px, -1px);
}

.markdown-content :deep(.obsidian-callout-label) {
  display: inline-flex;
  width: 17px;
  height: 17px;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.markdown-content :deep(.obsidian-callout-icon) {
  display: inline-flex;
  width: 17px;
  height: 17px;
  align-items: center;
  justify-content: center;
}

.markdown-content :deep(.obsidian-callout-svg) {
  width: 100%;
  height: 100%;
  fill: currentColor;
}

.markdown-content :deep(.obsidian-callout-content) {
  padding: 0 13px 12px;
  color: var(--text);
}

.markdown-content :deep(.obsidian-callout-content > :first-child) {
  margin-top: 0;
}

.markdown-content :deep(.obsidian-callout-content > :last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(.obsidian-callout[data-callout-fold="closed"] > :not(.obsidian-callout-title)) {
  display: none;
}

.markdown-content :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  color: var(--accent);
  background: var(--accent-soft);
  font-family: "JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}

.markdown-content :deep(.math-display) {
  margin: 20px 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 3px 0;
  scrollbar-width: thin;
}

.markdown-content :deep(.katex-display) {
  margin: 0;
  text-align: center;
}

.markdown-content :deep(.katex) {
  color: var(--text);
  font-size: 1.08em;
  overflow-wrap: normal;
  white-space: nowrap;
}

.markdown-content :deep(.math-render-error) {
  padding: 1px 5px;
  border-radius: 4px;
  color: var(--accent);
  background: var(--accent-soft);
  font-family: "JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace;
}

.markdown-content :deep(.markdown-table-wrap) {
  width: 100%;
  margin: 18px 0 24px;
  overflow-x: auto;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.markdown-content :deep(table) {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  background: var(--surface);
  font-size: 13px;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 10px 12px;
  border-right: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  text-align: left;
  vertical-align: top;
}

.markdown-content :deep(th:last-child),
.markdown-content :deep(td:last-child) {
  border-right: 0;
}

.markdown-content :deep(tr:last-child td) {
  border-bottom: 0;
}

.markdown-content :deep(th) {
  color: var(--text);
  background: var(--surface-soft);
  font-weight: 750;
}

.markdown-content :deep(tr:nth-child(even) td) {
  background: var(--surface-raised);
}

.markdown-content :deep(img) {
  display: block;
  max-width: 100%;
  height: auto;
  margin: 20px auto;
  border: 1px solid var(--line);
  border-radius: 8px;
}

.markdown-content :deep(hr) {
  margin: 30px 0;
  border: 0;
  border-top: 1px solid var(--line);
}

.markdown-content :deep(pre) {
  margin: 0;
  padding: 18px 20px 20px;
  overflow-x: auto;
  background: var(--surface-soft);
}

.markdown-content :deep(.code-block-shell) {
  margin: 18px 0 22px;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-soft);
  box-shadow: var(--shadow-soft);
}

.markdown-content :deep(.code-block-toolbar) {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 10px 0 14px;
  border-bottom: 1px solid var(--line);
  color: var(--muted);
  background: var(--surface-soft);
  font-size: 11px;
}

.markdown-content :deep(.code-block-language) {
  margin-right: auto;
  font-weight: 700;
  letter-spacing: .04em;
}

.markdown-content :deep(.code-copy-button) {
  padding: 4px 6px;
  border: 0;
  border-radius: 4px;
  color: var(--muted);
  background: transparent;
  font: inherit;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease;
}

.markdown-content :deep(.code-copy-button:hover) {
  color: var(--brand-strong);
  background: var(--glass);
}

@media (prefers-reduced-motion: reduce) {
  .markdown-content :deep(.obsidian-callout-fold),
  .markdown-content :deep(.code-copy-button) {
    transition: none;
  }
}

.markdown-content :deep(.code-block-shell pre code) {
  display: block;
  min-width: max-content;
  padding: 0;
  color: var(--text);
  background: transparent;
  font-family: "JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.85;
  tab-size: 2;
  white-space: pre;
}

.markdown-content :deep(.hljs-comment),
.markdown-content :deep(.hljs-quote) {
  color: var(--subtle);
  font-style: italic;
}

.markdown-content :deep(.hljs-keyword),
.markdown-content :deep(.hljs-selector-tag),
.markdown-content :deep(.hljs-literal),
.markdown-content :deep(.hljs-type) {
  color: var(--accent);
}

.markdown-content :deep(.hljs-string),
.markdown-content :deep(.hljs-attr),
.markdown-content :deep(.hljs-variable),
.markdown-content :deep(.hljs-template-variable) {
  color: var(--brand);
}

.markdown-content :deep(.hljs-number),
.markdown-content :deep(.hljs-symbol),
.markdown-content :deep(.hljs-bullet) {
  color: var(--accent);
}

.markdown-content :deep(.hljs-title),
.markdown-content :deep(.hljs-function),
.markdown-content :deep(.hljs-built_in) {
  color: var(--brand-strong);
}

.markdown-content :deep(.hljs-meta),
.markdown-content :deep(.hljs-name) {
  color: var(--brand);
}

.markdown-content :deep(a) {
  color: var(--brand);
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
</style>
