<template>
  <div class="note-library" :aria-busy="loading">
    <form class="library-search" role="search" @submit.prevent="$emit('search')">
      <el-input :model-value="keyword" clearable aria-label="搜索学习笔记" placeholder="搜索学习笔记"
        @update:model-value="$emit('update:keyword', $event)" @clear="$emit('search')" />
      <button type="submit" title="搜索" aria-label="搜索"><el-icon><Search /></el-icon></button>
    </form>
    <section class="library-section" aria-label="分类索引">
      <h2>分类索引</h2>
      <NoteCategoryTree :categories="categories" :model-value="category" :all-count="totalCount" all-label="全部文章" @select="(value) => $emit('category', value)" />
    </section>
    <section class="library-section" aria-label="文章列表">
      <h2>文章列表 <span>{{ notes.length }}</span></h2>
      <p v-if="loading" role="status">正在查找笔记…</p>
      <div v-else-if="error" class="library-error" role="alert"><p>{{ error }}</p><button type="button" @click="$emit('retry')">重试</button></div>
      <div v-else class="library-notes">
        <button v-for="note in notes" :key="note.id" class="public-note-link" :class="{ active: activeId === note.id }"
          :aria-current="activeId === note.id ? 'page' : undefined" type="button" @click="$emit('select', note)">
          <small>{{ note.category || '未分类' }} · {{ formatDate(note.updatedAt) }}</small>
          <strong>{{ note.title }}</strong>
        </button>
        <p v-if="!notes.length" role="status">没有找到对应笔记。</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import NoteCategoryTree from '@/components/NoteCategoryTree.vue'
import type { Note, NoteCategory } from '@/types/note'

const props = defineProps<{
  keyword: string
  category: string
  categories: NoteCategory[]
  notes: Note[]
  activeId?: number
  loading: boolean
  error: string
}>()
defineEmits<{
  'update:keyword': [value: string]
  search: []
  category: [value: string]
  select: [note: Note]
  retry: []
}>()
const totalCount = computed(() => props.categories.reduce((total, item) => total + item.count, 0))
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
</style>
