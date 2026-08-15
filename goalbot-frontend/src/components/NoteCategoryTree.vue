<template>
  <div class="note-category-tree">
    <button class="category-all" :class="{ active: !modelValue }" type="button" @click="selectAll">
      <el-icon class="category-all-icon"><Collection /></el-icon>
      <span>{{ allLabel }}</span>
      <small>{{ allCount ?? totalCount }}</small>
    </button>

    <el-tree
      v-if="categoryTree.length"
      class="category-tree-control"
      :data="categoryTree"
      node-key="key"
      :props="{ label: 'label', children: 'children' }"
      :default-expand-all="true"
      :expand-on-click-node="false"
      highlight-current
      @node-click="selectNode"
    >
      <template #default="{ data }">
        <div class="category-tree-node" :class="{ active: modelValue === data.value, group: data.children.length }">
          <el-icon class="category-tree-icon"><FolderOpened v-if="data.children.length" /><Document v-else /></el-icon>
          <span class="category-tree-label">{{ data.label }}</span>
          <small class="category-tree-count">{{ data.count }}</small>
        </div>
      </template>
    </el-tree>
    <p v-else class="category-tree-empty">暂无分类</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Collection, Document, FolderOpened } from '@element-plus/icons-vue'
import type { NoteCategory } from '@/types/note'
import { buildNoteCategoryTree, type NoteCategoryTreeNode } from '@/utils/noteCategories'

const props = withDefaults(defineProps<{
  categories: NoteCategory[]
  modelValue?: string
  allLabel?: string
  allCount?: number
}>(), {
  modelValue: '',
  allLabel: '全部笔记',
})

const emit = defineEmits<{
  (event: 'select', category: string, descendants: string[] | null): void
}>()

const categoryTree = computed(() => buildNoteCategoryTree(props.categories))
const totalCount = computed(() => props.categories.reduce((total, category) => total + category.count, 0))

function selectAll() {
  emit('select', '', null)
}

function selectNode(node: NoteCategoryTreeNode) {
  emit('select', node.value, node.children.length ? [...node.leafValues] : null)
}
</script>

<style scoped>
.note-category-tree { --category-active: var(--brand-soft); --category-hover: var(--surface-soft); color: var(--muted); }
.category-all { display:flex; width:100%; min-height:40px; align-items:center; gap:var(--space-2); padding:0 10px; border:0; border-radius:var(--radius-sm); color:var(--muted); background:transparent; font-size:14px; font-weight:650; text-align:left; cursor:pointer; transition:background-color .22s ease,color .22s ease,transform .22s ease; }
.category-all:hover,.category-all.active { color:var(--brand-strong); background:var(--category-active); }
.category-all:hover { transform:translateX(3px); }
.category-all-icon,.category-tree-node.group .category-tree-icon { flex:0 0 auto; color:var(--brand); font-size:15px; }
.category-all small,.category-tree-count { margin-left:auto; color:var(--subtle); font-size:11px; font-weight:600; }
.category-tree-control { margin-top:var(--space-1); padding:0; background:transparent; }
.category-tree-control :deep(.el-tree-node__content) { height:40px; margin:2px 0; border-radius:var(--radius-sm); transition:background-color .22s ease,transform .22s ease; }
.category-tree-control :deep(.el-tree-node__content:hover) { background:var(--category-hover); transform:translateX(3px); }
.category-tree-control :deep(.el-tree-node.is-current > .el-tree-node__content) { background:var(--category-active); }
.category-tree-control :deep(.el-tree-node__expand-icon),.category-tree-icon { color:var(--subtle); transition:color .22s ease,transform .22s ease; }
.category-tree-control :deep(.el-tree-node__expand-icon.expanded),.category-tree-node.active { color:var(--brand-strong); }
.category-tree-control :deep(.el-tree-node__children) { position:relative; }
.category-tree-control :deep(.el-tree-node__children)::before { position:absolute; top:0; bottom:0; left:13px; border-left:1px dashed var(--line); content:''; }
.category-tree-node { display:flex; min-width:0; width:100%; align-items:center; gap:var(--space-2); color:var(--muted); font-size:14px; transition:color .22s ease; }
.category-tree-node.active { font-weight:750; }
.category-tree-label { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.category-tree-empty { margin:var(--space-2) 10px; color:var(--subtle); font-size:12px; }
</style>
