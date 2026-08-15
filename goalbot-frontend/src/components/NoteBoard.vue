<template>
  <section class="note-board">
    <div class="board-toolbar">
      <p>把笔记拖进对应的分类框，即可完成归类。</p>
      <span>{{ notes.length }} 篇笔记 · {{ columns.length }} 个分类框</span>
    </div>

    <div class="board-grid">
      <section
        v-for="(column, index) in columns"
        :key="column.category"
        class="board-column"
        :class="{ 'is-drag-over': dragOverCategory === column.category }"
        @dragover.prevent="onDragOver(column, $event)"
        @dragleave="onDragLeave(column)"
        @drop.prevent="onDrop(column, $event)"
      >
        <header class="board-column-head" :class="`tint-${index % 3}`">
          <div class="board-column-title">
            <span class="board-column-dot" aria-hidden="true" />
            <strong>{{ column.category || '未分类' }}</strong>
            <small>{{ column.notes.length }}</small>
          </div>
          <el-tooltip v-if="column.empty && column.category !== '未分类'" content="删除空分类框" placement="top">
            <button class="column-remove" type="button" aria-label="删除空分类框" @click="removeEmptyColumn(column.category)">
              <el-icon><Close /></el-icon>
            </button>
          </el-tooltip>
        </header>

        <div class="board-column-body">
          <article
            v-for="note in column.notes"
            :key="note.id"
            class="note-tile"
            :class="{ dragging: draggingId === note.id }"
            draggable="true"
            @dragstart="onDragStart(note, $event)"
            @dragend="onDragEnd"
            @click="emit('open', note)"
          >
            <span class="note-state" :class="note.official ? 'official' : note.published ? 'published' : 'draft'" />
            <div class="note-tile-copy">
              <small>{{ note.category || '未分类' }} · {{ formatDate(note.updatedAt) }}</small>
              <strong>{{ note.title }}</strong>
              <em v-if="note.summary">{{ note.summary }}</em>
            </div>
            <el-dropdown trigger="click" @command="(command: string) => onTileCommand(command, note)" @click.stop>
              <button class="tile-more" type="button" aria-label="笔记操作" @click.stop>
                <el-icon><MoreFilled /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit" :icon="EditPen">编辑</el-dropdown-item>
                  <el-dropdown-item command="move" :icon="Switch">移动到分类…</el-dropdown-item>
                  <el-dropdown-item :command="note.official ? 'unpublish' : 'publish'" :icon="note.official ? Hide : Promotion">
                    {{ note.official ? '从官网下架' : '发布到官网' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" :icon="Delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </article>

          <div v-if="!column.notes.length" class="column-empty" :class="{ 'is-drag-over': dragOverCategory === column.category }">
            <el-icon><FolderOpened /></el-icon>
            <span>空分类框，把笔记拖进来</span>
          </div>
        </div>
      </section>

      <button class="board-add" type="button" @click="promptCreate">
        <el-icon><Plus /></el-icon>
        <span>新建分类框</span>
      </button>
    </div>

    <el-dialog v-model="moveDialog" title="移动到分类" width="min(420px, calc(100vw - 32px))">
      <p class="move-hint">选择已有分类，或直接输入新分类名（可用 <code>/</code> 建立层级，如 课程/Java）。</p>
      <el-select
        v-model="moveTarget"
        filterable
        allow-create
        default-first-option
        placeholder="选择或输入分类"
        style="width: 100%"
        @keyup.enter="confirmMove"
      >
        <el-option label="未分类" value="" />
        <el-option v-for="name in allCategoryNames" :key="name" :label="name" :value="name" />
      </el-select>
      <template #footer>
        <el-button @click="moveDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmMove">移动</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Delete, EditPen, FolderOpened, Hide, MoreFilled, Plus, Promotion, Switch } from '@element-plus/icons-vue'
import type { Note, NoteCategory } from '@/types/note'

const EXTRA_KEY = 'linge-board-extra-categories'

const props = defineProps<{
  notes: Note[]
  categories: NoteCategory[]
}>()

const emit = defineEmits<{
  (event: 'open', note: Note): void
  (event: 'move', note: Note, category: string): void
  (event: 'toggle-official', note: Note): void
  (event: 'remove', note: Note): void
}>()

const draggingId = ref<number | null>(null)
const dragOverCategory = ref<string | null>(null)
const moveDialog = ref(false)
const moveTarget = ref<string>('')
const moveNote = ref<Note | null>(null)

const extraCategories = ref<string[]>(loadExtraCategories())

function loadExtraCategories(): string[] {
  try {
    const parsed = JSON.parse(localStorage.getItem(EXTRA_KEY) || '[]')
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === 'string') : []
  } catch {
    return []
  }
}
function saveExtraCategories() {
  localStorage.setItem(EXTRA_KEY, JSON.stringify(extraCategories.value))
}

const knownCategoryNames = computed(() => props.categories.map((category) => category.name))
const allCategoryNames = computed(() => {
  const names = new Set<string>(knownCategoryNames.value)
  extraCategories.value.forEach((name) => names.add(name))
  return [...names]
})

const columns = computed(() => {
  const grouped = new Map<string, Note[]>()
  for (const note of props.notes) {
    const key = note.category || '未分类'
    const list = grouped.get(key) ?? []
    list.push(note)
    grouped.set(key, list)
  }
  const columns: { category: string; notes: Note[]; empty: boolean }[] = []
  const seen = new Set<string>()
  for (const name of knownCategoryNames.value) {
    const notes = grouped.get(name) ?? []
    columns.push({ category: name, notes, empty: !notes.length })
    seen.add(name)
  }
  for (const name of extraCategories.value) {
    if (seen.has(name)) continue
    columns.push({ category: name, notes: [], empty: true })
    seen.add(name)
  }
  // 未分类框始终显示：方便把笔记拖出来归零
  columns.push({ category: '未分类', notes: grouped.get('未分类') ?? [], empty: !grouped.has('未分类') })
  return columns
})

function onDragStart(note: Note, event: DragEvent) {
  draggingId.value = note.id
  if (event.dataTransfer) {
    event.dataTransfer.setData('text/plain', String(note.id))
    event.dataTransfer.effectAllowed = 'move'
  }
}
function onDragEnd() {
  draggingId.value = null
  dragOverCategory.value = null
}
function onDragOver(column: { category: string }, event: DragEvent) {
  event.preventDefault()
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move'
  dragOverCategory.value = column.category
}
function onDragLeave(column: { category: string }) {
  if (dragOverCategory.value === column.category) dragOverCategory.value = null
}
function onDrop(column: { category: string }, event: DragEvent) {
  const id = Number(event.dataTransfer?.getData('text/plain') || draggingId.value)
  dragOverCategory.value = null
  draggingId.value = null
  const note = props.notes.find((item) => item.id === id)
  if (!note) return
  const target = column.category === '未分类' ? '' : column.category
  if ((note.category || '') === target) return
  emit('move', note, target)
}

function onTileCommand(command: string, note: Note) {
  if (command === 'edit') { emit('open', note); return }
  if (command === 'move') {
    moveNote.value = note
    moveTarget.value = note.category ?? ''
    moveDialog.value = true
    return
  }
  if (command === 'publish' || command === 'unpublish') { emit('toggle-official', note); return }
  if (command === 'delete') { emit('remove', note) }
}

function confirmMove() {
  if (!moveNote.value) return
  const note = moveNote.value
  emit('move', note, moveTarget.value)
  moveDialog.value = false
  moveNote.value = null
}

async function promptCreate() {
  try {
    const { value } = await ElMessageBox.prompt('输入新分类框名称（可用 / 建立层级，如 课程/Java）', '新建分类框', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPattern: /.+/,
      inputErrorMessage: '分类名不能为空',
      inputPlaceholder: '例如：课程/数据结构',
    })
    const name = value.trim()
    if (!name) return
    if (allCategoryNames.value.includes(name)) {
      ElMessage.info('这个分类框已经存在了')
      return
    }
    extraCategories.value.push(name)
    saveExtraCategories()
    ElMessage.success(`已创建「${name}」`)
  } catch {
    /* 用户取消 */
  }
}

function removeEmptyColumn(category: string) {
  extraCategories.value = extraCategories.value.filter((name) => name !== category)
  saveExtraCategories()
}

function formatDate(value?: string) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<style scoped>
.note-board { display: grid; gap: 16px; }

.board-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 0 2px; }
.board-toolbar p { margin: 0; color: var(--gb-muted); font-size: 13px; }
.board-toolbar span { color: var(--gb-subtle); font-size: 12px; }

.board-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(264px, 1fr)); gap: 18px; align-items: start; }

.board-column {
  display: flex; min-height: 210px; flex-direction: column;
  border: 1px solid var(--gb-border); border-radius: var(--gb-radius);
  background: var(--gb-surface); box-shadow: var(--gb-shadow);
  transition: border-color .22s ease, box-shadow .22s ease, transform .22s ease;
}
.board-column.is-drag-over { border-color: var(--brand); box-shadow: 0 0 0 3px var(--focus-ring), var(--shadow-float); transform: translateY(-2px); }

.board-column-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px 12px;
  border-bottom: 1px solid var(--gb-border); border-radius: var(--gb-radius) var(--gb-radius) 0 0;
}
.board-column-head.tint-0 { background: var(--brand-soft); }
.board-column-head.tint-1 { background: var(--surface-soft); }
.board-column-head.tint-2 { background: var(--accent-soft); }

.board-column-title { display: flex; min-width: 0; align-items: center; gap: 8px; }
.board-column-dot { width: 8px; height: 8px; flex: 0 0 auto; border-radius: 50%; background: var(--gb-primary); }
.tint-1 .board-column-dot { background: var(--brand-strong); }
.tint-2 .board-column-dot { background: var(--accent); }
.board-column-title strong { overflow: hidden; color: var(--gb-text); font-size: 14px; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.board-column-title small { min-width: 22px; padding: 1px 7px; border-radius: 999px; color: var(--brand-strong); background: var(--glass-strong); font-size: 11px; font-weight: 750; text-align: center; }
.tint-1 .board-column-title small { color: var(--brand-strong); }
.tint-2 .board-column-title small { color: var(--accent); }

.column-remove { display: grid; width: 24px; height: 24px; place-items: center; border: 0; border-radius: 8px; color: var(--gb-subtle); background: transparent; cursor: pointer; transition: color .2s ease, background-color .2s ease; }
.column-remove:hover { color: var(--accent); background: var(--accent-soft); }

.board-column-body { display: grid; gap: 8px; padding: 12px; }

.note-tile {
  position: relative; display: grid; grid-template-columns: 7px minmax(0, 1fr) auto; gap: 10px;
  padding: 11px 10px 11px 12px;
  border: 1px solid var(--gb-border); border-radius: var(--gb-radius-sm);
  background: var(--surface); cursor: grab;
  transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease, opacity .18s ease;
}
.note-tile:hover { border-color: var(--line-strong); box-shadow: var(--shadow-soft); transform: translateY(-1px); }
.note-tile.dragging { opacity: .45; }
.note-tile .note-state { width: 7px; height: 7px; margin-top: 6px; border-radius: 50%; background: var(--subtle); }
.note-tile .note-state.published { background: var(--accent); }
.note-tile .note-state.official { background: var(--brand); }

.note-tile-copy { min-width: 0; }
.note-tile-copy small, .note-tile-copy strong, .note-tile-copy em { display: block; overflow: hidden; text-overflow: ellipsis; }
.note-tile-copy small { color: var(--gb-primary); font-size: 11px; font-weight: 700; }
.note-tile-copy strong { margin-top: 3px; color: var(--gb-text); font-size: 13px; font-weight: 750; white-space: nowrap; }
.note-tile-copy em { display: -webkit-box; margin-top: 4px; color: var(--gb-muted); font-size: 12px; font-style: normal; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }

.tile-more { display: grid; width: 26px; height: 26px; place-items: center; align-self: start; border: 0; border-radius: 8px; color: var(--gb-subtle); background: transparent; cursor: pointer; transition: color .2s ease, background-color .2s ease; }
.tile-more:hover { color: var(--gb-primary-dark); background: var(--gb-primary-soft); }

.column-empty {
  display: grid; min-height: 96px; place-items: center; align-content: center; gap: 7px;
  border: 1.5px dashed var(--gb-border-strong); border-radius: var(--gb-radius-sm);
  color: var(--gb-subtle); font-size: 12px;
  transition: border-color .22s ease, background-color .22s ease, color .22s ease;
}
.column-empty .el-icon { font-size: 20px; }
.column-empty.is-drag-over { border-color: var(--gb-primary); color: var(--gb-primary); background: var(--gb-primary-soft); }

.board-add {
  display: grid; min-height: 150px; place-items: center; align-content: center; gap: 8px;
  border: 1.5px dashed var(--gb-border-strong); border-radius: var(--gb-radius);
  color: var(--gb-muted); background: transparent; cursor: pointer;
  transition: border-color .22s ease, color .22s ease, background-color .22s ease;
}
.board-add:hover { border-color: var(--gb-primary); color: var(--gb-primary); background: var(--gb-primary-soft); }
.board-add .el-icon { font-size: 22px; }
.board-add span { font-size: 13px; font-weight: 700; }

.move-hint { margin: 0 0 12px; color: var(--gb-muted); font-size: 12px; line-height: 1.7; }
.move-hint code { padding: 1px 6px; border-radius: 6px; color: var(--gb-primary-dark); background: var(--gb-primary-soft); font-size: 11px; }

@media (max-width: 760px) {
  .board-toolbar { align-items: flex-start; flex-direction: column; }
  .board-grid { grid-template-columns: 1fr; }
}
</style>
