<template>
  <section class="project-notes" aria-labelledby="project-notes-title">
    <header class="notes-heading">
      <div><h2 id="project-notes-title">延伸阅读</h2><p>同主题的公开学习笔记</p></div>
      <label class="notes-topic">主题
        <select v-model="topic"><option v-for="item in topics" :key="item" :value="item">{{ item }}</option></select>
      </label>
    </header>
    <div :aria-busy="loading" aria-live="polite" class="notes-results">
      <p v-if="loading" class="notes-state" role="status"><el-icon class="notes-loading"><Loading /></el-icon>正在查找笔记…</p>
      <div v-else-if="failed" class="notes-state"><p>笔记暂时无法加载，项目内容仍可正常浏览。</p><button type="button" class="portfolio-link" @click="retry += 1"><el-icon><RefreshRight /></el-icon>重试</button></div>
      <ul v-else-if="notes.length" class="notes-list">
        <li v-for="note in notes" :key="note.id">
          <RouterLink :to="{ path: '/notes', query: { note: note.id, q: topic } }"><span>{{ note.title }}</span><el-icon aria-hidden="true"><ArrowRight /></el-icon></RouterLink>
          <span v-if="note.category" class="note-category">{{ note.category }}</span>
        </li>
      </ul>
      <p v-else class="notes-state">暂时没有“{{ topic }}”主题的公开笔记。</p>
    </div>
    <RouterLink class="portfolio-link" :to="{ path: '/notes', query: { q: topic } }">在笔记库中查看“{{ topic }}” <el-icon><ArrowRight /></el-icon></RouterLink>
  </section>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ArrowRight, Loading, RefreshRight } from '@element-plus/icons-vue'
import { searchOfficialNotes } from '@/api/note'
import type { PublicNoteItem } from '@/types/note'

const props = defineProps<{ topics: string[] }>()
const topic = ref(props.topics[0] ?? '')
const notes = ref<PublicNoteItem[]>([])
const loading = ref(false)
const failed = ref(false)
const retry = ref(0)

watch(() => props.topics, (topics) => { topic.value = topics[0] ?? '' })
watch([topic, retry], async ([keyword], _old, onCleanup) => {
  // Navigation or topic changes must never show a previous request's results.
  let current = true
  const controller = new AbortController()
  onCleanup(() => { current = false; controller.abort() })
  notes.value = []
  failed.value = false
  loading.value = true
  try {
    const page = await searchOfficialNotes({ keyword, page: 1, pageSize: 3 }, controller.signal)
    if (current) notes.value = page.items
  } catch {
    if (current) failed.value = true
  } finally {
    if (current) loading.value = false
  }
}, { immediate: true })
</script>

<style scoped>
.project-notes { display: flex; flex-direction: column; gap: var(--space-5); padding-block: var(--space-7); border-top: 1px solid var(--line); }
.notes-heading { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--space-4); }
.notes-heading>div { display: flex; flex-direction: column; gap: var(--space-2); }
.notes-heading h2 { margin: 0; font-size: 26px; }
.notes-heading p { margin: 0; color: var(--muted); font-size: 14px; }
.notes-topic { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-3); color: var(--muted); font-size: 14px; }
select { max-width: 100%; min-height: 44px; padding: var(--space-2) var(--space-3); border: 1px solid var(--line-strong); border-radius: var(--radius-sm); color: var(--text); background: var(--surface); }
.notes-results { min-height: 90px; }
.notes-state { display: flex; flex-wrap: wrap; gap: var(--space-3); align-items: center; color: var(--muted); margin: 0; padding-block: var(--space-4); }
.notes-state p { margin: 0; }
.notes-list { display: flex; flex-direction: column; margin: 0; padding: 0; list-style: none; }
.notes-list li { display: flex; flex-direction: column; gap: var(--space-2); padding-block: var(--space-4); border-bottom: 1px solid var(--line); }
.notes-list a { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); min-height: 44px; color: var(--text); text-decoration: none; font-size: 17px; }
.notes-list a span { overflow-wrap: anywhere; }
.notes-list a .el-icon { flex-shrink: 0; color: var(--brand); }
.notes-list a:hover { color: var(--brand); text-decoration: underline; text-underline-offset: 4px; }
.note-category { color: var(--muted); font-size: 14px; }
.notes-loading { animation: notes-spin 1s linear infinite; }
@keyframes notes-spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .notes-loading { animation: none; } }
</style>
