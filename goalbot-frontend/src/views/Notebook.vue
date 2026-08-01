<template>
  <section class="page-section notebook-page" v-loading="loading">
    <section class="notebook-intro">
      <div class="notebook-heading">
        <p class="notebook-kicker">EDITORIAL DESK</p>
        <h2>内容工作台</h2>
        <p>集中管理草稿、分类和官网文章。</p>
      </div>
      <div class="notebook-actions">
        <el-radio-group v-model="viewMode" size="small" aria-label="视图切换">
          <el-radio-button value="board">看板</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
        <el-button :icon="Refresh" plain @click="loadNotes">刷新</el-button>
        <el-button :icon="Upload" plain :loading="uploading" @click="triggerUpload">导入 Markdown</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新建笔记</el-button>
        <input ref="fileInput" class="hidden-file" type="file" accept=".md,.markdown,.txt,text/markdown,text/plain" @change="handleFileChange" />
      </div>
    </section>

    <section v-if="viewMode === 'board'" class="notebook-board">
      <NoteBoard
        :notes="notes"
        :categories="categories"
        @open="openEdit"
        @move="handleMoveNote"
        @toggle-official="toggleOfficial"
        @remove="handleDelete"
      />
    </section>

    <section v-else class="notebook-layout">
      <aside class="notebook-library">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索标题、内容或标签" @clear="loadNotes" @keyup.enter="loadNotes" />
        <div class="library-filters">
          <el-select v-model="statusFilter" aria-label="按整理状态筛选" @change="loadNotes">
            <el-option label="全部状态" value="all" />
            <el-option label="草稿" value="draft" />
            <el-option label="已整理" value="published" />
          </el-select>
        </div>
        <div class="library-section-heading"><span>分类</span><small>{{ categories.length }} 个</small></div>
        <div class="category-browser"><NoteCategoryTree :categories="categories" :model-value="categoryFilter" all-label="全部笔记" @select="handleCategorySelect" /></div>
        <div class="library-section-heading"><span>笔记</span><small>{{ notes.length }} 篇</small></div>
        <el-scrollbar class="library-scroll">
          <button v-for="note in notes" :key="note.id" class="library-note" :class="{ active: activeNote?.id === note.id }" type="button" @click="selectNote(note)">
            <span class="note-state" :class="note.official ? 'official' : note.published ? 'published' : 'draft'" />
            <span class="library-note-copy">
              <small>{{ note.category || '未分类' }}</small>
              <strong>{{ note.title }}</strong>
              <em>{{ note.summary || '还没有摘要。' }}</em>
            </span>
          </button>
          <el-empty v-if="!notes.length && !loading" description="还没有站长笔记" :image-size="84" />
        </el-scrollbar>
      </aside>

      <article v-if="activeNote" class="notebook-reader" v-loading="activeLoading">
        <header class="reader-header">
          <div class="reader-meta"><span>{{ activeNote.category || '未分类' }}</span><span>{{ formatLongDate(activeNote.updatedAt) }}</span><span>{{ activeNote.wordCount }} 字</span></div>
          <div class="reader-title-row">
            <h3>{{ activeNote.title }}</h3>
            <div class="reader-actions">
              <el-tooltip :content="activeNote.official ? '从官网知识库下架' : '发布到官网知识库'" placement="top">
                <el-button :icon="activeNote.official ? Hide : Promotion" circle plain :type="activeNote.official ? 'success' : 'info'" :aria-label="activeNote.official ? '从官网知识库下架' : '发布到官网知识库'" @click="toggleOfficial(activeNote)" />
              </el-tooltip>
              <el-tooltip content="编辑笔记" placement="top"><el-button :icon="EditPen" circle plain aria-label="编辑笔记" @click="openEdit(activeNote)" /></el-tooltip>
              <el-tooltip content="删除笔记" placement="top"><el-button :icon="Delete" circle plain type="danger" aria-label="删除笔记" @click="handleDelete(activeNote)" /></el-tooltip>
            </div>
          </div>
          <p v-if="activeNote.summary" class="reader-summary">{{ activeNote.summary }}</p>
          <div class="reader-tags">
            <span v-if="activeNote.official" class="publication-mark">官网已发布</span>
            <span v-else-if="activeNote.published" class="publication-mark quiet">已整理</span>
            <span v-else class="publication-mark draft">草稿</span>
            <span v-for="tag in activeTags" :key="tag" class="reader-tag"># {{ tag }}</span>
          </div>
        </header>
        <div class="reader-content"><MarkdownContent :content="activeNote.content" /></div>
      </article>

      <section v-else class="reader-empty">
        <el-icon><DocumentAdd /></el-icon>
        <h3>从一篇学习笔记开始</h3>
        <p>支持直接写作或导入 Markdown 文件。完成整理后，再把值得公开的内容发布到官网。</p>
        <el-button type="primary" @click="openCreate">写第一篇</el-button>
      </section>

      <aside v-if="activeNote" class="reader-outline">
        <p>本页目录</p>
        <button v-for="heading in activeHeadings" :key="heading.id" class="outline-link" :style="{ paddingLeft: `${(heading.level - 1) * 12}px` }" type="button" @click="scrollToHeading(heading.id)">{{ heading.text }}</button>
        <span v-if="!activeHeadings.length">正文没有标题</span>
      </aside>
    </section>
    <BackToTopButton />

    <el-dialog v-model="editorVisible" class="notebook-editor-dialog" width="min(1220px, calc(100vw - 32px))" :close-on-click-modal="false" :destroy-on-close="false" top="4vh">
      <template #header>
        <div class="editor-dialog-header"><div><p>{{ editingNote ? 'EDIT NOTE' : 'NEW NOTE' }}</p><h3>{{ editingNote ? '编辑笔记' : '写一篇新笔记' }}</h3></div><span>{{ editorForm.content.replace(/\s+/g, '').length }} 字</span></div>
      </template>
      <el-form ref="editorRef" :model="editorForm" :rules="editorRules" label-position="top" class="editor-form">
        <div class="editor-fields">
          <el-form-item label="标题" prop="title"><el-input v-model="editorForm.title" maxlength="160" show-word-limit placeholder="给这篇笔记一个明确的标题" /></el-form-item>
          <el-form-item label="分类"><el-input v-model="editorForm.category" maxlength="64" placeholder="例如：课程/高等数学，用 / 建立层级" /></el-form-item>
          <el-form-item label="标签"><el-input v-model="editorForm.tags" maxlength="255" placeholder="用逗号分隔，例如：学习, Java" /></el-form-item>
          <div class="editor-publication"><el-checkbox v-model="editorForm.published">标记为已整理</el-checkbox><el-checkbox v-model="editorForm.official" @change="syncOfficialState">发布到官网知识库</el-checkbox></div>
        </div>
        <div class="editor-modebar">
          <div class="format-actions" aria-label="Markdown 快捷插入">
            <el-tooltip content="插入二级标题"><el-button :icon="Tickets" circle plain aria-label="插入二级标题" @click="insertSnippet('heading')" /></el-tooltip>
            <el-tooltip content="插入无序列表"><el-button :icon="List" circle plain aria-label="插入无序列表" @click="insertSnippet('list')" /></el-tooltip>
            <el-tooltip content="插入引用"><el-button :icon="ChatLineSquare" circle plain aria-label="插入引用" @click="insertSnippet('quote')" /></el-tooltip>
            <el-tooltip content="插入提示块"><el-button :icon="InfoFilled" circle plain aria-label="插入提示块" @click="insertSnippet('callout')" /></el-tooltip>
            <el-tooltip content="插入代码块"><el-button :icon="DocumentCopy" circle plain aria-label="插入代码块" @click="insertSnippet('code')" /></el-tooltip>
          </div>
          <el-radio-group v-model="editorMode" size="small"><el-radio-button value="write">编辑</el-radio-button><el-radio-button value="split">分栏</el-radio-button><el-radio-button value="preview">预览</el-radio-button></el-radio-group>
        </div>
        <el-form-item prop="content" class="editor-content-field">
          <div class="editor-workspace" :class="`mode-${editorMode}`">
            <div class="editor-source"><label for="note-markdown">Markdown</label><textarea id="note-markdown" ref="markdownInput" v-model="editorForm.content" spellcheck="true" /></div>
            <div class="editor-preview"><label>预览</label><div class="editor-preview-paper"><MarkdownContent :content="editorForm.content" /></div></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><div class="editor-footer"><span>快捷保存：Ctrl / Cmd + S</span><div><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="submitEditor">保存笔记</el-button></div></div></template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ChatLineSquare, Delete, DocumentAdd, DocumentCopy, EditPen, Hide, InfoFilled, List, Plus, Promotion, Refresh, Search, Tickets, Upload } from '@element-plus/icons-vue'
import { createNote, deleteNote, fetchNote, fetchNoteCategories, fetchNotes, updateNote, uploadNote } from '@/api/note'
import BackToTopButton from '@/components/BackToTopButton.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import NoteBoard from '@/components/NoteBoard.vue'
import NoteCategoryTree from '@/components/NoteCategoryTree.vue'
import type { Note, NoteCategory } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'
import { summarizeNoteCategories } from '@/utils/noteCategories'

const notes = ref<Note[]>([])
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const keyword = ref('')
const categoryFilter = ref('')
const categoryFilterDescendants = ref<string[] | null>(null)
const statusFilter = ref<'all' | 'draft' | 'published'>('all')
const viewMode = ref<'board' | 'list'>('board')
const loading = ref(false)
const activeLoading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const editingNote = ref<Note | null>(null)
const editorMode = ref<'write' | 'split' | 'preview'>('split')
const fileInput = ref<HTMLInputElement>()
const markdownInput = ref<HTMLTextAreaElement>()
const editorRef = ref<FormInstance>()
const editorForm = reactive({ title: '', category: '', tags: '', content: '', published: false, official: false })
const editorRules: FormRules = { title: [{ required: true, message: '请输入笔记标题', trigger: 'blur' }], content: [{ required: true, message: '请输入 Markdown 正文', trigger: 'blur' }] }
const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const activeHeadings = computed(() => extractMarkdownHeadings(activeNote.value?.content))

onMounted(() => { loadNotes(); window.addEventListener('keydown', handleShortcut) })
onBeforeUnmount(() => window.removeEventListener('keydown', handleShortcut))

async function loadNotes() {
  loading.value = true
  try {
    const published = statusFilter.value === 'all' ? undefined : statusFilter.value === 'published'
    const descendants = categoryFilterDescendants.value
    const items = await fetchNotes({ keyword: keyword.value.trim() || undefined, category: descendants ? undefined : categoryFilter.value || undefined, published, limit: 100 })
    notes.value = descendants ? items.filter((note) => descendants.includes(note.category || '未分类')) : items

    // 分类树是辅助导航，不能阻塞笔记正文加载。接口暂时不可用时，用当前结果恢复基本分类能力。
    try {
      categories.value = await fetchNoteCategories({ silent: true })
    } catch {
      categories.value = summarizeNoteCategories(items)
    }

    const candidate = activeNote.value ? notes.value.find((note) => note.id === activeNote.value?.id) : notes.value[0]
    if (candidate) await selectNote(candidate)
    else activeNote.value = null
  } finally { loading.value = false }
}

async function selectNote(note: Note) {
  activeLoading.value = true
  try { activeNote.value = await fetchNote(note.id) }
  finally { activeLoading.value = false }
}

function openCreate() {
  editingNote.value = null
  Object.assign(editorForm, { title: '', category: categoryFilterDescendants.value || categoryFilter.value === '未分类' ? '' : categoryFilter.value, tags: '', content: '# 新笔记\n\n', published: false, official: false })
  editorMode.value = defaultEditorMode()
  editorVisible.value = true
}

function openEdit(note: Note) {
  editingNote.value = note
  Object.assign(editorForm, { title: note.title, category: note.category ?? '', tags: note.tags ?? '', content: note.content, published: note.published, official: note.official })
  editorMode.value = defaultEditorMode()
  editorVisible.value = true
}

function defaultEditorMode(): 'write' | 'split' {
  return window.matchMedia('(max-width: 760px)').matches ? 'write' : 'split'
}

function triggerUpload() { fileInput.value?.click() }

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const note = await uploadNote(file, { category: categoryFilterDescendants.value || categoryFilter.value === '未分类' ? undefined : categoryFilter.value || undefined })
    ElMessage.success('Markdown 已导入为草稿')
    await loadNotes()
    await selectNote(note)
  } finally { uploading.value = false; input.value = '' }
}

async function submitEditor() {
  const valid = await editorRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { title: editorForm.title.trim(), category: editorForm.category.trim(), tags: editorForm.tags.trim(), content: editorForm.content.trim(), published: editorForm.published || editorForm.official, official: editorForm.official }
    const saved = editingNote.value ? await updateNote(editingNote.value.id, payload) : await createNote(payload)
    ElMessage.success(editingNote.value ? '笔记已更新' : '笔记已保存')
    editorVisible.value = false
    activeNote.value = saved
    await loadNotes()
    await selectNote(saved)
  } finally { saving.value = false }
}

async function toggleOfficial(note: Note) {
  const saved = await updateNote(note.id, { official: !note.official, published: note.official ? note.published : true })
  activeNote.value = saved
  const summary = notes.value.find((item) => item.id === saved.id)
  if (summary) Object.assign(summary, saved)
  ElMessage.success(saved.official ? '已发布到官网知识库' : '已从官网知识库下架')
}

async function handleMoveNote(note: Note, category: string) {
  const saved = await updateNote(note.id, { category })
  activeNote.value = saved
  const summary = notes.value.find((item) => item.id === saved.id)
  if (summary) Object.assign(summary, saved)
  ElMessage.success(category ? `已移动到「${category}」` : '已移动到「未分类」')
  await loadNotes()
}

async function handleDelete(note: Note) {
  await ElMessageBox.confirm(`确认删除「${note.title}」吗？此操作不可恢复。`, '删除笔记', { type: 'warning' })
  await deleteNote(note.id)
  ElMessage.success('笔记已删除')
  activeNote.value = null
  await loadNotes()
}

function syncOfficialState(value: boolean) { if (value) editorForm.published = true }
function handleCategorySelect(category: string, descendants: string[] | null) {
  categoryFilter.value = category
  categoryFilterDescendants.value = descendants
  loadNotes()
}
function insertSnippet(type: 'heading' | 'list' | 'quote' | 'callout' | 'code') {
  const snippets = { heading: '## 小节标题\n\n', list: '- 要点一\n- 要点二\n', quote: '> 记录一个值得回看的结论。\n', callout: '> [!NOTE] 提示标题\n> 在这里补充说明。\n\n', code: '```text\n在这里写代码或命令\n```\n' }
  const input = markdownInput.value
  const snippet = snippets[type]
  const start = input?.selectionStart ?? editorForm.content.length
  const end = input?.selectionEnd ?? editorForm.content.length
  editorForm.content = `${editorForm.content.slice(0, start)}${snippet}${editorForm.content.slice(end)}`
  nextTick(() => { input?.focus(); input?.setSelectionRange(start + snippet.length, start + snippet.length) })
}
function scrollToHeading(id: string) { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function handleShortcut(event: KeyboardEvent) { if (!editorVisible.value || !(event.ctrlKey || event.metaKey) || event.key.toLowerCase() !== 's') return; event.preventDefault(); submitEditor() }
function formatLongDate(value?: string) { return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : '刚刚更新' }
</script>

<style scoped>
.notebook-page {
  gap: 18px;
}

.notebook-intro {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  padding: 2px 2px 16px;
  border-bottom: 1px solid var(--gb-border);
}

.notebook-kicker,
.reader-outline > p,
.editor-dialog-header p {
  margin: 0;
  color: var(--gb-primary-dark);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .1em;
}

.notebook-heading h2 {
  margin: 6px 0 0;
  color: var(--gb-text);
  font-size: 26px;
  line-height: 1.2;
}

.notebook-heading > p:last-child {
  margin: 7px 0 0;
  color: var(--gb-muted);
  font-size: 13px;
}

.notebook-actions,
.reader-actions,
.format-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notebook-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hidden-file {
  display: none !important;
}

.notebook-layout {
  display: grid;
  min-height: 650px;
  grid-template-columns: 286px minmax(0, 1fr) 188px;
  align-items: stretch;
  overflow: hidden;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: var(--gb-surface);
  box-shadow: var(--gb-shadow);
}

.notebook-library {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
  padding: 15px;
  border-right: 1px solid var(--gb-border);
  background: #f7f8fc;
}

.library-filters {
  display: grid;
  grid-template-columns: 1fr;
}

.library-section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  color: #4c5870;
  font-size: 12px;
  font-weight: 750;
}

.library-section-heading small {
  color: var(--gb-subtle);
  font-size: 11px;
  font-weight: 600;
}

.category-browser {
  max-height: 190px;
  min-height: 42px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
}

.notebook-library :deep(.note-category-tree) {
  --category-accent: var(--gb-primary-dark);
  --category-active: var(--gb-primary-soft);
  --category-hover: #eef1f8;
}

.notebook-library :deep(.category-tree-empty) {
  margin-left: 10px;
  color: var(--gb-subtle);
}

.library-scroll {
  height: 368px;
  min-height: 220px;
  margin: 0 -5px;
  padding: 0 5px;
}

.library-note {
  display: grid;
  width: 100%;
  grid-template-columns: 7px minmax(0, 1fr);
  gap: 10px;
  margin-bottom: 4px;
  padding: 11px 9px;
  border: 1px solid transparent;
  border-radius: 7px;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: border-color .18s ease, background-color .18s ease, box-shadow .18s ease;
}

.library-note:hover {
  border-color: #dce2ef;
  background: rgba(255, 255, 255, .78);
}

.library-note.active {
  border-color: #cbd5f0;
  background: #fff;
  box-shadow: 0 8px 20px rgba(31, 42, 68, .07);
}

.note-state {
  width: 7px;
  height: 7px;
  margin-top: 6px;
  border-radius: 50%;
  background: #a7afbf;
}

.note-state.published {
  background: #d28a32;
}

.note-state.official {
  background: #2c9b7c;
}

.library-note-copy {
  min-width: 0;
}

.library-note-copy small,
.library-note-copy strong,
.library-note-copy em {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}

.library-note-copy small {
  color: var(--gb-primary-dark);
  font-size: 11px;
  font-style: normal;
}

.library-note-copy strong {
  margin-top: 3px;
  color: var(--gb-text);
  font-size: 14px;
  font-weight: 750;
  white-space: nowrap;
}

.library-note-copy em {
  display: -webkit-box;
  margin-top: 5px;
  color: #778198;
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notebook-reader {
  min-width: 0;
  padding: 38px clamp(28px, 4vw, 68px) 56px;
  background: #fff;
}

.reader-header,
.reader-content {
  width: min(100%, 840px);
  margin: 0 auto;
}

.reader-header {
  padding-bottom: 25px;
  border-bottom: 1px solid #e7eaf1;
}

.reader-meta,
.reader-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
}

.reader-meta {
  color: #7c879d;
  font-size: 12px;
}

.reader-meta span + span::before {
  margin-right: 9px;
  color: #c0c7d4;
  content: "•";
}

.reader-title-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 20px;
  margin-top: 11px;
}

.reader-title-row h3 {
  min-width: 0;
  margin: 0;
  color: #1f2a44;
  font-size: clamp(26px, 2.8vw, 38px);
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.reader-actions {
  flex: 0 0 auto;
}

.reader-summary {
  margin: 15px 0 0;
  color: #65708c;
  font-size: 15px;
  line-height: 1.75;
}

.reader-tags {
  margin-top: 17px;
}

.publication-mark,
.reader-tag {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  color: #18755f;
  background: #e4f4ee;
  font-size: 11px;
  font-weight: 700;
}

.publication-mark.quiet {
  color: #986218;
  background: #fff1d8;
}

.publication-mark.draft {
  color: #667085;
  background: #eef0f4;
}

.reader-tag {
  color: #536078;
  background: #f0f2f7;
  font-weight: 600;
}

.reader-content {
  padding-top: 30px;
}

.notebook-reader :deep(.markdown-content) {
  color: #2b3548;
  font-size: 16px;
  line-height: 1.92;
}

.notebook-reader :deep(.markdown-content h1),
.notebook-reader :deep(.markdown-content h2),
.notebook-reader :deep(.markdown-content h3),
.notebook-reader :deep(.markdown-content h4) {
  scroll-margin-top: 92px;
  color: #1f2a44;
}

.notebook-reader :deep(.markdown-content h2) {
  margin-top: 42px;
  padding-bottom: 9px;
  border-bottom: 1px solid #e8ebf2;
}

.reader-outline {
  align-self: start;
  min-height: 100%;
  padding: 42px 16px;
  border-left: 1px solid var(--gb-border);
  background: #fafbfe;
}

.reader-outline > p {
  color: #65708c;
  letter-spacing: .08em;
}

.reader-outline > span {
  display: block;
  margin-top: 15px;
  color: #929bad;
  font-size: 12px;
}

.outline-link {
  display: block;
  width: 100%;
  overflow: hidden;
  margin-top: 11px;
  border: 0;
  color: #67738b;
  background: transparent;
  font-size: 12px;
  line-height: 1.45;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.outline-link:hover {
  color: var(--gb-primary-dark);
}

.reader-empty {
  display: grid;
  grid-column: 2 / 4;
  min-height: 540px;
  align-content: center;
  justify-items: start;
  gap: 10px;
  padding: 48px;
  background: #fff;
}

.reader-empty .el-icon {
  color: var(--gb-primary);
  font-size: 34px;
}

.reader-empty h3,
.reader-empty p {
  margin: 0;
}

.reader-empty h3 {
  margin-top: 5px;
  color: var(--gb-text);
  font-size: 22px;
}

.reader-empty p {
  max-width: 400px;
  color: var(--gb-muted);
  font-size: 14px;
  line-height: 1.75;
}

.reader-empty .el-button {
  margin-top: 6px;
}

.editor-dialog-header {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 16px;
}

.editor-dialog-header h3 {
  margin: 5px 0 0;
  color: var(--gb-text);
  font-size: 22px;
}

.editor-dialog-header > span {
  color: var(--gb-subtle);
  font-size: 12px;
}

.editor-form {
  padding: 0 2px;
}

.editor-fields {
  display: grid;
  grid-template-columns: 1.35fr .72fr .9fr auto;
  gap: 12px;
  align-items: start;
}

.editor-publication {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-top: 33px;
  white-space: nowrap;
}

.editor-modebar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin: 4px 0 11px;
}

.editor-content-field {
  margin-bottom: 0;
}

.editor-content-field :deep(.el-form-item__content) {
  display: block;
}

.editor-workspace {
  display: grid;
  height: clamp(390px, 52vh, 600px);
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--gb-border);
  border-radius: 7px;
  background: #fff;
}

.editor-workspace.mode-split {
  grid-template-columns: 1fr 1fr;
}

.editor-source,
.editor-preview {
  display: flex;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
}

.editor-source {
  border-right: 1px solid var(--gb-border);
  background: #f8f9fc;
}

.editor-source label,
.editor-preview > label {
  padding: 10px 13px;
  border-bottom: 1px solid var(--gb-border);
  color: #737e94;
  font-size: 12px;
  font-weight: 700;
}

.editor-source textarea {
  width: 100%;
  min-height: 0;
  flex: 1;
  overflow: auto;
  resize: none;
  padding: 16px;
  border: 0;
  outline: 0;
  color: #263247;
  background: transparent;
  font-family: "JetBrains Mono", "Cascadia Code", "SFMono-Regular", Consolas, monospace;
  font-size: 13px;
  line-height: 1.75;
}

.editor-preview-paper {
  min-height: 0;
  flex: 1;
  padding: 18px 22px;
  overflow: auto;
}

.editor-preview-paper :deep(.markdown-content) {
  color: #2b3548;
  line-height: 1.82;
}

.editor-workspace.mode-write,
.editor-workspace.mode-preview {
  grid-template-columns: 1fr;
}

.editor-workspace.mode-write .editor-preview,
.editor-workspace.mode-preview .editor-source {
  display: none;
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.editor-footer > span {
  color: var(--gb-subtle);
  font-size: 12px;
}

:global(.notebook-editor-dialog) {
  display: flex;
  max-height: 92vh;
  flex-direction: column;
  overflow: hidden;
}

:global(.notebook-editor-dialog .el-dialog__header),
:global(.notebook-editor-dialog .el-dialog__footer) {
  flex: 0 0 auto;
}

:global(.notebook-editor-dialog .el-dialog__body) {
  min-height: 0;
  padding-top: 12px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

:global(.notebook-editor-dialog .el-dialog__footer) {
  border-top: 1px solid var(--gb-border);
  background: #fff;
}

@media (prefers-reduced-motion: reduce) {
  .library-note {
    transition: none;
  }
}

@media (max-height: 760px) and (min-width: 761px) {
  .editor-workspace {
    height: 360px;
  }
}

@media (max-width: 1180px) {
  .notebook-layout {
    grid-template-columns: 270px minmax(0, 1fr);
  }

  .reader-outline {
    display: none;
  }

  .reader-empty {
    grid-column: 2;
  }

  .editor-fields {
    grid-template-columns: 1fr 1fr;
  }

  .editor-publication {
    grid-row: 2;
    padding-top: 0;
  }
}

@media (max-width: 760px) {
  .notebook-intro {
    align-items: start;
    flex-direction: column;
  }

  .notebook-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .notebook-layout {
    grid-template-columns: 1fr;
  }

  .notebook-library {
    min-height: 0;
    border-right: 0;
    border-bottom: 1px solid var(--gb-border);
  }

  .category-browser {
    max-height: 120px;
  }

  .library-scroll {
    height: 180px;
  }

  .notebook-reader,
  .reader-empty {
    grid-column: auto;
    min-height: 0;
    padding: 28px 20px 40px;
  }

  .reader-title-row {
    flex-direction: column;
  }

  .reader-actions {
    align-self: flex-end;
  }

  .editor-fields {
    grid-template-columns: 1fr;
  }

  .editor-publication {
    grid-row: auto;
    padding-top: 0;
  }

  .editor-modebar {
    align-items: flex-start;
    flex-direction: column;
  }

  .editor-workspace.mode-split {
    grid-template-columns: 1fr;
  }

  .editor-workspace.mode-split .editor-source {
    border-right: 0;
    border-bottom: 1px solid var(--gb-border);
  }

  .editor-workspace {
    height: auto;
    min-height: 570px;
  }

  .editor-source textarea,
  .editor-preview-paper {
    min-height: 242px;
  }

  .editor-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .editor-footer > div {
    display: flex;
    justify-content: flex-end;
  }
}
</style>
