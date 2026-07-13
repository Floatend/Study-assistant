<template>
  <div class="markdown-content" v-html="html"></div>
</template>

<script setup lang="ts">
import DOMPurify from 'dompurify'
import MarkdownIt from 'markdown-it'
import { computed } from 'vue'

const props = defineProps<{
  content?: string | null
}>()

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true
})

const html = computed(() => {
  const raw = props.content?.trim()
  if (!raw) {
    return ''
  }
  return DOMPurify.sanitize(markdown.render(raw), {
    USE_PROFILES: { html: true }
  })
})
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

.markdown-content :deep(code) {
  padding: 2px 5px;
  border-radius: 4px;
  color: #9f1239;
  background: #f8e7ed;
  font-family: inherit;
  font-size: 12px;
}

.markdown-content :deep(pre) {
  margin: 10px 0;
  padding: 12px;
  overflow-x: auto;
  border-radius: 8px;
  background: #111827;
}

.markdown-content :deep(pre code) {
  padding: 0;
  color: #e5e7eb;
  background: transparent;
}

.markdown-content :deep(a) {
  color: #2563eb;
  text-decoration: none;
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
}
</style>
