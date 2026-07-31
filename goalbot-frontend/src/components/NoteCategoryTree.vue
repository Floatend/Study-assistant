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
.note-category-tree{--category-accent:#4d6bfe;--category-active:#e7edff;--category-hover:#edf2ff;color:#52617c}.category-all{display:flex;width:100%;min-height:38px;align-items:center;gap:7px;padding:0 10px;border:0;border-radius:9px;color:#41516f;background:transparent;font:inherit;font-size:13px;font-weight:650;text-align:left;cursor:pointer;transition:background-color .22s ease,color .22s ease,transform .22s ease}.category-all:hover,.category-all.active{color:#3559e8;background:var(--category-active)}.category-all:hover{transform:translateX(3px)}.category-all-icon{flex:0 0 auto;color:var(--category-accent);font-size:15px}.category-all small{margin-left:auto;color:#9ba7bc;font-size:11px;font-weight:600}.category-all.active small{color:#6c82e9}.category-tree-control{margin:2px 0 0;padding:0;background:transparent}.category-tree-control :deep(.el-tree-node__content){height:38px;margin:2px 0;border-radius:9px;transition:background-color .22s ease,transform .22s ease}.category-tree-control :deep(.el-tree-node__content:hover){background:var(--category-hover);transform:translateX(3px)}.category-tree-control :deep(.el-tree-node.is-current > .el-tree-node__content){background:var(--category-active)}.category-tree-control :deep(.el-tree-node__expand-icon){color:#8995b0;transition:color .22s ease,transform .22s ease}.category-tree-control :deep(.el-tree-node__expand-icon.expanded){color:var(--category-accent)}.category-tree-control :deep(.el-tree-node__children){position:relative}.category-tree-control :deep(.el-tree-node__children)::before{position:absolute;top:0;bottom:0;left:13px;border-left:1px dashed #d9e0f1;content:''}.category-tree-node{display:flex;min-width:0;width:100%;align-items:center;gap:7px;color:#52617c;font-size:13px;transition:color .22s ease}.category-tree-node.active{color:#3559e8;font-weight:750}.category-tree-node.group .category-tree-icon{color:var(--category-accent)}.category-tree-icon{flex:0 0 auto;color:#8390aa;font-size:15px}.category-tree-label{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.category-tree-count{margin-left:auto;padding-left:8px;color:#9ba7bc;font-size:11px;font-weight:600}.category-tree-node.active .category-tree-count{color:#6c82e9}.category-tree-empty{margin:8px 10px;color:#9ba7bc;font-size:12px}@media(max-width:760px){.category-tree-control :deep(.el-tree-node__content){height:40px}.category-tree-control :deep(.el-tree-node__children)::before{left:13px}}
</style>
