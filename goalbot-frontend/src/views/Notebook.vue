<template>
  <section class="page-section notebook-page" v-loading="loading">
    <section class="notebook-intro">
      <div>
        <p class="notebook-kicker">EDITORIAL DESK</p>
        <h2>站长笔记工作台</h2>
        <p>整理学习成果，先沉淀为草稿，再决定是否作为官网知识库文章公开。</p>
      </div>
      <div class="notebook-actions">
        <el-button :icon="Refresh" plain @click="loadNotes">刷新</el-button>
        <el-button :icon="Upload" plain :loading="uploading" @click="triggerUpload">导入 Markdown</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新建笔记</el-button>
        <input ref="fileInput" class="hidden-file" type="file" accept=".md,.markdown,.txt,text/markdown,text/plain" @change="handleFileChange" />
      </div>
    </section>

    <section class="notebook-layout">
      <aside class="notebook-library">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索标题、内容或标签" @clear="loadNotes" @keyup.enter="loadNotes" />
        <div class="library-filters">
          <el-select v-model="categoryFilter" placeholder="全部分类" @change="loadNotes">
            <el-option label="全部分类" value="" />
            <el-option v-for="category in categories" :key="category.name" :label="`${category.name} · ${category.count}`" :value="category.name" />
          </el-select>
          <el-select v-model="statusFilter" @change="loadNotes">
            <el-option label="全部状态" value="all" />
            <el-option label="草稿" value="draft" />
            <el-option label="已整理" value="published" />
          </el-select>
        </div>
        <div class="library-meta"><span>{{ notes.length }} 篇笔记</span><span>{{ categories.length }} 个分类</span></div>
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
                <el-button :icon="activeNote.official ? Hide : Promotion" circle plain :type="activeNote.official ? 'success' : 'info'" @click="toggleOfficial(activeNote)" />
              </el-tooltip>
              <el-button :icon="EditPen" circle plain @click="openEdit(activeNote)" />
              <el-button :icon="Delete" circle plain type="danger" @click="handleDelete(activeNote)" />
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

    <el-dialog v-model="editorVisible" class="notebook-editor-dialog" width="min(1220px, calc(100vw - 32px))" :close-on-click-modal="false" :destroy-on-close="false" top="4vh">
      <template #header>
        <div class="editor-dialog-header"><div><p>{{ editingNote ? 'EDIT NOTE' : 'NEW NOTE' }}</p><h3>{{ editingNote ? '编辑笔记' : '写一篇新笔记' }}</h3></div><span>{{ editorForm.content.replace(/\s+/g, '').length }} 字</span></div>
      </template>
      <el-form ref="editorRef" :model="editorForm" :rules="editorRules" label-position="top" class="editor-form">
        <div class="editor-fields">
          <el-form-item label="标题" prop="title"><el-input v-model="editorForm.title" maxlength="160" show-word-limit placeholder="给这篇笔记一个明确的标题" /></el-form-item>
          <el-form-item label="分类"><el-input v-model="editorForm.category" maxlength="64" placeholder="例如：高等数学、Web 开发" /></el-form-item>
          <el-form-item label="标签"><el-input v-model="editorForm.tags" maxlength="255" placeholder="用逗号分隔，例如：学习, Java" /></el-form-item>
          <div class="editor-publication"><el-checkbox v-model="editorForm.published">标记为已整理</el-checkbox><el-checkbox v-model="editorForm.official" @change="syncOfficialState">发布到官网知识库</el-checkbox></div>
        </div>
        <div class="editor-modebar">
          <div class="format-actions" aria-label="Markdown 快捷插入">
            <el-tooltip content="插入二级标题"><el-button :icon="Tickets" circle plain @click="insertSnippet('heading')" /></el-tooltip>
            <el-tooltip content="插入无序列表"><el-button :icon="List" circle plain @click="insertSnippet('list')" /></el-tooltip>
            <el-tooltip content="插入引用"><el-button :icon="ChatLineSquare" circle plain @click="insertSnippet('quote')" /></el-tooltip>
            <el-tooltip content="插入代码块"><el-button :icon="Promotion" circle plain @click="insertSnippet('code')" /></el-tooltip>
          </div>
          <el-radio-group v-model="editorMode" size="small"><el-radio-button label="write">编辑</el-radio-button><el-radio-button label="split">分栏</el-radio-button><el-radio-button label="preview">预览</el-radio-button></el-radio-group>
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
import { ChatLineSquare, Delete, DocumentAdd, EditPen, Hide, List, Plus, Promotion, Refresh, Search, Tickets, Upload } from '@element-plus/icons-vue'
import { createNote, deleteNote, fetchNote, fetchNoteCategories, fetchNotes, updateNote, uploadNote } from '@/api/note'
import MarkdownContent from '@/components/MarkdownContent.vue'
import type { Note, NoteCategory } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'

const notes = ref<Note[]>([])
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const keyword = ref('')
const categoryFilter = ref('')
const statusFilter = ref<'all' | 'draft' | 'published'>('all')
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
    const [items, categoryItems] = await Promise.all([
      fetchNotes({ keyword: keyword.value.trim() || undefined, category: categoryFilter.value || undefined, published, limit: 100 }),
      fetchNoteCategories()
    ])
    notes.value = items
    categories.value = categoryItems
    const candidate = activeNote.value ? items.find((note) => note.id === activeNote.value?.id) : items[0]
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
  Object.assign(editorForm, { title: '', category: categoryFilter.value === '未分类' ? '' : categoryFilter.value, tags: '', content: '# 新笔记\n\n', published: false, official: false })
  editorMode.value = 'split'
  editorVisible.value = true
}

function openEdit(note: Note) {
  editingNote.value = note
  Object.assign(editorForm, { title: note.title, category: note.category ?? '', tags: note.tags ?? '', content: note.content, published: note.published, official: note.official })
  editorMode.value = 'split'
  editorVisible.value = true
}

function triggerUpload() { fileInput.value?.click() }

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const note = await uploadNote(file, { category: categoryFilter.value === '未分类' ? undefined : categoryFilter.value || undefined })
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

async function handleDelete(note: Note) {
  await ElMessageBox.confirm(`确认删除「${note.title}」吗？此操作不可恢复。`, '删除笔记', { type: 'warning' })
  await deleteNote(note.id)
  ElMessage.success('笔记已删除')
  activeNote.value = null
  await loadNotes()
}

function syncOfficialState(value: boolean) { if (value) editorForm.published = true }
function insertSnippet(type: 'heading' | 'list' | 'quote' | 'code') {
  const snippets = { heading: '## 小节标题\n\n', list: '- 要点一\n- 要点二\n', quote: '> 记录一个值得回看的结论。\n', code: '```text\n在这里写代码或命令\n```\n' }
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
.notebook-page{gap:20px}.notebook-intro{display:flex;align-items:end;justify-content:space-between;gap:24px;padding:4px 2px 20px;border-bottom:1px solid var(--gb-border)}.notebook-kicker,.reader-outline>p,.editor-dialog-header p{margin:0;color:#a84836;font-size:11px;font-weight:800;letter-spacing:.12em}.notebook-intro h2{margin:7px 0 0;color:var(--gb-text);font-size:29px;line-height:1.2}.notebook-intro>div>p:not(.notebook-kicker){max-width:600px;margin:8px 0 0;color:var(--gb-muted);font-size:14px;line-height:1.7}.notebook-actions{display:flex;flex-wrap:wrap;justify-content:flex-end;gap:8px}.hidden-file{display:none}.notebook-layout{display:grid;grid-template-columns:minmax(248px,.62fr) minmax(0,1.6fr) minmax(168px,.34fr);align-items:stretch;min-height:650px;overflow:hidden;border:1px solid var(--gb-border);border-radius:8px;background:#fff}.notebook-library{display:flex;min-width:0;flex-direction:column;gap:12px;padding:16px;border-right:1px solid var(--gb-border);background:#fafbf9}.library-filters{display:grid;grid-template-columns:1fr 1fr;gap:8px}.library-meta{display:flex;justify-content:space-between;color:var(--gb-subtle);font-size:12px}.library-scroll{height:545px;margin:0 -5px;padding:0 5px}.library-note{display:grid;width:100%;grid-template-columns:7px minmax(0,1fr);gap:10px;padding:12px 9px;border:1px solid transparent;border-radius:6px;color:inherit;background:transparent;text-align:left;cursor:pointer}.library-note:hover,.library-note.active{border-color:#e4dfd3;background:#fff}.library-note.active{box-shadow:0 10px 18px rgba(34,41,36,.05)}.note-state{width:7px;height:7px;margin-top:6px;border-radius:50%;background:#b3bab5}.note-state.published{background:#2c7965}.note-state.official{background:#ae4734}.library-note-copy{min-width:0}.library-note-copy small,.library-note-copy strong,.library-note-copy em{display:block;overflow:hidden;text-overflow:ellipsis}.library-note-copy small{color:#a05b48;font-size:11px;font-style:normal}.library-note-copy strong{margin-top:3px;color:#202923;font-size:14px;font-weight:750;white-space:nowrap}.library-note-copy em{display:-webkit-box;margin-top:5px;color:#7a857e;font-size:12px;font-style:normal;line-height:1.5;-webkit-box-orient:vertical;-webkit-line-clamp:2}.notebook-reader{min-width:0;padding:39px clamp(26px,4vw,64px) 54px;background:#fff}.reader-header{max-width:810px;padding-bottom:26px;border-bottom:1px solid #e8ebe6}.reader-meta,.reader-tags{display:flex;flex-wrap:wrap;gap:9px;align-items:center}.reader-meta{color:#8a948d;font-size:12px}.reader-meta span+span::before{margin-right:9px;color:#c4c9c5;content:'•'}.reader-title-row{display:flex;align-items:start;justify-content:space-between;gap:18px;margin-top:10px}.reader-title-row h3{margin:0;color:#202822;font-size:clamp(25px,2.6vw,36px);line-height:1.28}.reader-actions{display:flex;flex:0 0 auto;gap:7px}.reader-summary{margin:16px 0 0;color:#66746b;font-size:15px;line-height:1.75}.reader-tags{margin-top:18px}.publication-mark,.reader-tag{display:inline-flex;align-items:center;min-height:24px;padding:0 8px;border-radius:999px;color:#9c3f31;background:#fae9e4;font-size:11px;font-weight:700}.publication-mark.quiet{color:#276c5b;background:#e8f3ed}.publication-mark.draft{color:#69756d;background:#eef0ed}.reader-tag{color:#59645d;background:#f3f4f1;font-weight:600}.reader-content{max-width:810px;padding-top:31px}.notebook-reader :deep(.markdown-content){color:#29332d;font-size:16px;line-height:1.92}.notebook-reader :deep(.markdown-content h1),.notebook-reader :deep(.markdown-content h2),.notebook-reader :deep(.markdown-content h3),.notebook-reader :deep(.markdown-content h4){scroll-margin-top:22px;color:#1f2922}.notebook-reader :deep(.markdown-content h2){margin-top:44px;padding-bottom:9px;border-bottom:1px solid #eaede8}.reader-outline{padding:44px 16px;border-left:1px solid var(--gb-border);background:#fbfcfa}.reader-outline>p{color:#69746d;letter-spacing:.08em}.reader-outline>span{display:block;margin-top:15px;color:#9ba29d;font-size:12px}.outline-link{display:block;width:100%;overflow:hidden;margin-top:11px;border:0;color:#67726b;background:transparent;font-size:12px;line-height:1.45;text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer}.outline-link:hover{color:#a84836}.reader-empty{display:grid;grid-column:2 / 4;align-content:center;justify-items:start;gap:10px;min-height:500px;padding:44px;background:#fff}.reader-empty .el-icon{color:#b85745;font-size:34px}.reader-empty h3,.reader-empty p{margin:0}.reader-empty h3{margin-top:5px;color:#26322a;font-size:22px}.reader-empty p{max-width:380px;color:#748078;font-size:14px;line-height:1.75}.reader-empty .el-button{margin-top:6px}.editor-dialog-header{display:flex;align-items:end;justify-content:space-between;gap:16px}.editor-dialog-header h3{margin:5px 0 0;color:#1f2922;font-size:22px}.editor-dialog-header>span{color:#7e8881;font-size:12px}.editor-form{padding:0 2px}.editor-fields{display:grid;grid-template-columns:1.3fr .7fr .9fr auto;gap:12px;align-items:start}.editor-publication{display:flex;flex-direction:column;gap:12px;padding-top:33px;white-space:nowrap}.editor-modebar{display:flex;align-items:center;justify-content:space-between;gap:14px;margin:4px 0 11px}.format-actions{display:flex;gap:7px}.editor-content-field{margin-bottom:0}.editor-content-field :deep(.el-form-item__content){display:block}.editor-workspace{display:grid;min-height:470px;overflow:hidden;border:1px solid var(--gb-border);border-radius:7px;background:#fff}.editor-workspace.mode-split{grid-template-columns:1fr 1fr}.editor-source,.editor-preview{display:flex;min-width:0;flex-direction:column}.editor-source{border-right:1px solid var(--gb-border);background:#fbfcfa}.editor-source label,.editor-preview>label{padding:10px 13px;border-bottom:1px solid var(--gb-border);color:#7c8780;font-size:12px;font-weight:700}.editor-source textarea{width:100%;min-height:426px;flex:1;resize:none;padding:16px;border:0;outline:0;color:#273129;background:transparent;font-family:"SFMono-Regular",Consolas,"Liberation Mono",monospace;font-size:13px;line-height:1.75}.editor-preview-paper{min-height:426px;flex:1;padding:17px 22px;overflow:auto}.editor-preview-paper :deep(.markdown-content){color:#2b352e;line-height:1.8}.editor-workspace.mode-write{grid-template-columns:1fr}.editor-workspace.mode-write .editor-preview{display:none}.editor-workspace.mode-preview{grid-template-columns:1fr}.editor-workspace.mode-preview .editor-source{display:none}.editor-footer{display:flex;align-items:center;justify-content:space-between;gap:16px}.editor-footer>span{color:#89938c;font-size:12px}@media(max-width:1100px){.notebook-layout{grid-template-columns:minmax(238px,.64fr) minmax(0,1.36fr)}.reader-outline{display:none}.reader-empty{grid-column:2}.editor-fields{grid-template-columns:1fr 1fr}.editor-publication{grid-row:2;padding-top:0}}@media(max-width:760px){.notebook-intro{align-items:start;flex-direction:column}.notebook-actions{justify-content:flex-start}.notebook-layout{grid-template-columns:1fr}.notebook-library{min-height:0;border-right:0;border-bottom:1px solid var(--gb-border)}.library-scroll{height:248px}.notebook-reader,.reader-empty{grid-column:auto;min-height:0;padding:30px 22px 40px}.reader-title-row{flex-direction:column}.reader-actions{align-self:flex-end}.editor-fields{grid-template-columns:1fr}.editor-publication{grid-row:auto;padding-top:0}.editor-workspace.mode-split{grid-template-columns:1fr}.editor-workspace.mode-split .editor-source{border-right:0;border-bottom:1px solid var(--gb-border)}.editor-workspace{min-height:570px}.editor-source textarea,.editor-preview-paper{min-height:242px}.editor-footer{align-items:start;flex-direction:column}}
</style>
