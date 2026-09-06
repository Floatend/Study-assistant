<template>
  <div class="note-library" :class="{ 'results-library': resultsView }" :aria-busy="loading">
    <form v-if="showSearch" class="library-search" role="search" @submit.prevent="$emit('search')">
      <el-input :model-value="keyword" clearable maxlength="100" aria-label="搜索学习笔记" placeholder="搜索标题或正文"
        @update:model-value="$emit('update:keyword', $event)" @clear="$emit('search')" />
      <button type="submit" title="搜索" aria-label="搜索"><el-icon><Search /></el-icon></button>
    </form>
    <section v-if="!resultsView" class="library-section" aria-label="分类索引">
      <h2>分类索引</h2>
      <NoteCategoryTree :categories="categories" :model-value="category" :all-count="totalCount" all-label="全部文章" @select="(value) => $emit('category', value)" />
    </section>
    <section v-if="showNotes" class="library-section" aria-label="文章列表">
      <h2>{{ matchedKeyword ? '搜索结果' : '文章列表' }} <span>共 {{ total }} 篇</span></h2>
      <p v-if="loading" role="status">正在查找笔记…</p>
      <div v-else-if="error" class="library-error" role="alert"><p>{{ error }}</p><button type="button" @click="$emit('retry')">重试</button></div>
      <div v-else class="library-notes">
        <button v-for="note in notes" :key="note.id" class="public-note-link" :class="{ active: activeId === note.id }"
          :aria-current="activeId === note.id ? 'page' : undefined" type="button" @click="$emit('select', note)">
          <small>{{ note.category || '未分类' }} · {{ formatDate(note.updatedAt) }}</small>
          <strong><NoteSearchText :text="note.title" :keyword="matchedKeyword" /></strong>
          <p v-if="note.excerpt && (resultsView || matchedKeyword)" class="note-excerpt"><NoteSearchText :text="note.excerpt" :keyword="matchedKeyword" /></p>
        </button>
        <p v-if="!notes.length" role="status">没有找到对应笔记。</p>
      </div>
      <nav v-if="total > pageSize && !loading && !error" class="library-pagination" aria-label="笔记分页">
        <button type="button" :disabled="page <= 1" title="上一页" aria-label="上一页" @click="$emit('page', page - 1)"><el-icon><ArrowLeft /></el-icon></button>
        <label>第 <input :key="page" type="number" :value="page" min="1" :max="pageCount" aria-label="页码" @change="jumpPage" @keydown.enter="jumpPage" /> / {{ pageCount }} 页</label>
        <button type="button" :disabled="page >= pageCount" title="下一页" aria-label="下一页" @click="$emit('page', page + 1)"><el-icon><ArrowRight /></el-icon></button>
      </nav>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue'
import NoteCategoryTree from '@/components/NoteCategoryTree.vue'
import NoteSearchText from '@/components/NoteSearchText.vue'
import type { PublicNoteItem, NoteCategory } from '@/types/note'

const props = withDefaults(defineProps<{
  keyword: string
  category: string
  categories: NoteCategory[]
  notes: PublicNoteItem[]
  total: number
  page: number
  pageSize: number
  matchedKeyword: string
  resultsView?: boolean
  showNotes?: boolean
  showSearch?: boolean
  activeId?: number
  loading: boolean
  error: string
}>(), { resultsView: false, showNotes: true, showSearch: true })
const emit = defineEmits<{
  'update:keyword': [value: string]
  search: []
  category: [value: string]
  select: [note: PublicNoteItem]
  page: [page: number]
  retry: []
}>()
const totalCount = computed(() => props.categories.reduce((total, item) => total + item.count, 0))
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
function jumpPage(event: Event) {
  const input = event.target as HTMLInputElement
  const page = Number(input.value)
  if (Number.isInteger(page) && page >= 1 && page <= pageCount.value) emit('page', page)
  else input.value = String(props.page)
}
function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.note-library, .library-section, .library-notes { display:flex; min-width:0; flex-direction:column; gap:var(--space-2); }
.note-library { gap:var(--space-5); color:var(--text); }
.library-search { display:flex; gap:var(--space-2); }
.library-search .el-input { min-width:0; }
.library-search button { display:grid; width:44px; height:44px; flex:none; place-items:center; border:1px solid var(--line); border-radius:var(--radius-sm); color:var(--brand-strong); background:var(--surface); cursor:pointer; }
.library-section h2 { display:flex; justify-content:space-between; gap:var(--space-2); margin:0; font:700 14px/1.5 var(--font-body); color:var(--muted); }
.library-section h2 span { font-weight:400; }
.library-section p { margin:0; color:var(--muted); font-size:14px; }
.public-note-link { display:flex; width:100%; flex-direction:column; gap:var(--space-1); padding:var(--space-3); border:0; border-radius:var(--radius-sm); color:var(--text); background:transparent; text-align:left; cursor:pointer; transition:background-color .2s ease; }
.public-note-link:hover, .public-note-link.active { color:var(--brand-strong); background:var(--brand-soft); }
.public-note-link small { color:var(--muted); font-size:14px; }
.public-note-link strong { font-size:15px; line-height:1.5; overflow-wrap:anywhere; }
.library-error { display:flex; flex-direction:column; align-items:flex-start; gap:var(--space-2); }
.library-error button { border:0; padding:var(--space-2); color:var(--brand-strong); background:var(--brand-soft); cursor:pointer; }
.library-pagination { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:var(--space-1); padding-top:var(--space-4); }
.library-pagination button { display:grid; place-items:center; width:36px; height:44px; flex:none; border:1px solid var(--line); border-radius:var(--radius-sm); color:var(--brand-strong); background:var(--surface); cursor:pointer; }
.library-pagination button:disabled { opacity:.45; cursor:default; }
.library-pagination label { display:flex; align-items:center; gap:var(--space-1); color:var(--muted); font-size:14px; }
.library-pagination input { width:48px; height:36px; padding:var(--space-1); border:1px solid var(--line); border-radius:var(--radius-sm); color:var(--text); background:var(--surface); font-size:14px; text-align:center; appearance:textfield; }
.library-pagination input::-webkit-inner-spin-button { appearance:none; }
.note-excerpt { display:-webkit-box; -webkit-line-clamp:3; -webkit-box-orient:vertical; overflow:hidden; overflow-wrap:anywhere; font-weight:400; }
.results-library .public-note-link { padding:var(--space-5) 0; border-bottom:1px solid var(--line); border-radius:0; }
.results-library .public-note-link:hover { background:transparent; }
.results-library .public-note-link:hover strong { text-decoration:underline; text-underline-offset:4px; }
.results-library .public-note-link strong { color:var(--brand-strong); font-size:22px; font-family:var(--font-display); }
.results-library .library-notes { gap:0; }
.results-library .note-excerpt { font-size:15px; line-height:1.6; }
</style>
