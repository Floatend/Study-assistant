<template>
  <div ref="markdownRoot" class="markdown-content" v-html="html"></div>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
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
  xml: 'HTML / XML'
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

const defaultHeadingOpen = markdown.renderer.rules.heading_open
markdown.renderer.rules.heading_open = (tokens, index, options, env, self) => {
  const context = env as { headingIds?: Map<string, number> }
  const headingText = tokens[index + 1]?.content ?? ''
  tokens[index].attrSet('id', createHeadingId(headingText, context.headingIds ?? (context.headingIds = new Map())))
  return defaultHeadingOpen
    ? defaultHeadingOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

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
    USE_PROFILES: { html: true }
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
  if (button && markdownRoot.value?.contains(button)) void copyCode(button)
}

onMounted(() => markdownRoot.value?.addEventListener('click', handleMarkdownClick))
onBeforeUnmount(() => markdownRoot.value?.removeEventListener('click', handleMarkdownClick))
watch(html, () => nextTick(() => markdownRoot.value?.scrollTo({ left: 0, top: 0 })))
</script>

<style scoped>
.markdown-content {
  color: #25313f;
  font-size: 14px;
  line-height: 1.75;
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
  color: #1d2733;
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

.markdown-content :deep(strong) {
  color: #111827;
  font-weight: 750;
}

.markdown-content :deep(blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 3px solid #2f7d68;
  border-radius: 4px;
  color: #566273;
  background: #f4f8f6;
}

.markdown-content :deep(.obsidian-callout) {
  --callout-color: 46, 128, 242;
  margin: 18px 0;
  padding: 0;
  overflow: hidden;
  border: 1px solid rgba(var(--callout-color), .35);
  border-left: 4px solid rgb(var(--callout-color));
  border-radius: 8px;
  color: #30415f;
  background: rgba(var(--callout-color), .09);
  font-style: normal;
  box-shadow: 0 2px 8px rgba(31, 60, 140, .06);
}

.markdown-content :deep(.obsidian-callout[data-callout="tip"]),
.markdown-content :deep(.obsidian-callout[data-callout="success"]) {
  --callout-color: 36, 166, 151;
}

.markdown-content :deep(.obsidian-callout[data-callout="warning"]),
.markdown-content :deep(.obsidian-callout[data-callout="caution"]) {
  --callout-color: 218, 142, 41;
}

.markdown-content :deep(.obsidian-callout[data-callout="danger"]),
.markdown-content :deep(.obsidian-callout[data-callout="failure"]) {
  --callout-color: 215, 77, 92;
}

.markdown-content :deep(.obsidian-callout-title) {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 0;
  padding: 10px 13px 5px;
  color: rgb(var(--callout-color));
  font-style: normal;
  font-weight: 750;
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

.markdown-content :deep(.obsidian-callout > p:not(.obsidian-callout-title)),
.markdown-content :deep(.obsidian-callout > ul),
.markdown-content :deep(.obsidian-callout > ol),
.markdown-content :deep(.obsidian-callout > pre),
.markdown-content :deep(.obsidian-callout > h1),
.markdown-content :deep(.obsidian-callout > h2),
.markdown-content :deep(.obsidian-callout > h3),
.markdown-content :deep(.obsidian-callout > h4) {
  margin-right: 13px;
  margin-left: 13px;
}

.markdown-content :deep(.obsidian-callout[data-callout-fold="closed"] > :not(.obsidian-callout-title)) {
  display: none;
}

.markdown-content :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  color: #9f1239;
  background: #f8e7ed;
  font-family: inherit;
  font-size: 12px;
}

.markdown-content :deep(pre) {
  margin: 0;
  padding: 18px 20px 20px;
  overflow-x: auto;
  background: #f0f2f5;
}

.markdown-content :deep(.code-block-shell) {
  margin: 18px 0 22px;
  overflow: hidden;
  border: 1px solid #e0e5ec;
  border-radius: 8px;
  background: #f0f2f5;
  box-shadow: 0 5px 18px rgba(41, 54, 78, .06);
}

.markdown-content :deep(.code-block-toolbar) {
  display: flex;
  min-height: 32px;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 10px 0 14px;
  border-bottom: 1px solid #e0e5ec;
  color: #687487;
  background: #e8ebf0;
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
  color: #647086;
  background: transparent;
  font: inherit;
  cursor: pointer;
  transition: color .2s ease, background-color .2s ease;
}

.markdown-content :deep(.code-copy-button:hover) {
  color: #245de8;
  background: rgba(255, 255, 255, .7);
}

.markdown-content :deep(.code-block-shell pre code) {
  display: block;
  min-width: max-content;
  padding: 0;
  color: #263247;
  background: transparent;
  font-family: "JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.85;
  tab-size: 2;
  white-space: pre;
}

.markdown-content :deep(.hljs-comment),
.markdown-content :deep(.hljs-quote) {
  color: #9aa5b4;
  font-style: italic;
}

.markdown-content :deep(.hljs-keyword),
.markdown-content :deep(.hljs-selector-tag),
.markdown-content :deep(.hljs-literal),
.markdown-content :deep(.hljs-type) {
  color: #b51da8;
}

.markdown-content :deep(.hljs-string),
.markdown-content :deep(.hljs-attr),
.markdown-content :deep(.hljs-variable),
.markdown-content :deep(.hljs-template-variable) {
  color: #2765e8;
}

.markdown-content :deep(.hljs-number),
.markdown-content :deep(.hljs-symbol),
.markdown-content :deep(.hljs-bullet) {
  color: #d35c22;
}

.markdown-content :deep(.hljs-title),
.markdown-content :deep(.hljs-function),
.markdown-content :deep(.hljs-built_in) {
  color: #2753b8;
}

.markdown-content :deep(.hljs-meta),
.markdown-content :deep(.hljs-name) {
  color: #008d8a;
}

.markdown-content :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
</style>
