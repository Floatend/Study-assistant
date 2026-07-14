<template>
  <section class="panel blog-panel">
    <div class="panel-header blog-header">
        <div>
          <div class="blog-kicker">Notebook Blog</div>
          <h2 class="panel-title">个人笔记</h2>
          <p class="panel-subtitle">记录只属于你的学习与思考；管理员可将指定笔记发布到官网的官方笔记页。</p>
      </div>
      <div class="blog-actions">
        <el-button :icon="Plus" type="primary" @click="openCreate">写一篇</el-button>
        <el-button :icon="Upload" plain :loading="uploading" @click="triggerUpload">上传笔记</el-button>
        <el-button :icon="Refresh" circle plain @click="loadNotes" />
        <input
          ref="fileInput"
          class="hidden-file"
          type="file"
          accept=".md,.markdown,.txt,text/markdown,text/plain"
          @change="handleFileChange"
        />
      </div>
    </div>

    <div class="blog-body" v-loading="loading">
      <aside class="note-rail">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索标题、摘要或标签"
          @clear="loadNotes"
          @keyup.enter="loadNotes"
        />

        <div class="note-stat-strip">
          <span>{{ notes.length }} 篇</span>
          <span>{{ activeNote?.wordCount ?? 0 }} 字</span>
        </div>

        <el-scrollbar class="note-list-scroll">
          <button
            v-for="note in notes"
            :key="note.id"
            class="note-list-item"
            :class="{ active: activeNote?.id === note.id }"
            type="button"
            @click="selectNote(note)"
          >
            <span class="note-dot" />
            <span class="note-list-copy">
              <strong>{{ note.title }}</strong>
              <small>{{ note.summary || '没有摘要，打开看看正文。' }}</small>
            </span>
          </button>
          <el-empty v-if="!notes.length && !loading" description="还没有笔记" :image-size="82" />
        </el-scrollbar>
      </aside>

      <article v-if="activeNote" class="things-reader" v-loading="activeLoading">
        <header class="reader-titlebar">
          <div class="reader-icon">
            <el-icon><Notebook /></el-icon>
          </div>
          <div class="reader-heading">
            <div class="reader-meta">
              <span>{{ formatDate(activeNote.updatedAt) }}</span>
              <span v-if="activeNote.fileName">{{ activeNote.fileName }}</span>
            </div>
            <h3>{{ activeNote.title }}</h3>
            <div v-if="activeTags.length" class="reader-tags">
              <el-tag v-for="tag in activeTags" :key="tag" effect="plain" round>{{ tag }}</el-tag>
            </div>
          </div>
          <div class="reader-actions">
            <el-tooltip v-if="userStore.isAdmin" :content="activeNote.official ? '从官网撤下' : '发布到官网官方笔记'" placement="top">
              <el-button
                :icon="activeNote.official ? Hide : Promotion"
                circle
                plain
                :type="activeNote.official ? 'success' : 'info'"
                @click="toggleOfficial(activeNote)"
              />
            </el-tooltip>
            <el-button :icon="Edit" circle plain @click="openEdit(activeNote)" />
            <el-button :icon="Delete" circle plain type="danger" @click="handleDelete(activeNote)" />
          </div>
        </header>

        <div class="reader-paper">
          <MarkdownContent :content="activeNote.content" />
        </div>
      </article>

      <div v-else class="reader-empty">
        <el-icon><Document /></el-icon>
        <strong>把笔记放进来</strong>
        <span>支持 .md、.markdown、.txt，上传后会按 Things 风格渲染阅读。</span>
      </div>
    </div>

    <el-dialog v-model="editorVisible" :title="editingNote ? '编辑笔记' : '写一篇笔记'" width="760px">
      <el-form ref="editorRef" :model="editorForm" :rules="editorRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="editorForm.title" maxlength="160" show-word-limit />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editorForm.tags" placeholder="学习, 项目, 生活" />
        </el-form-item>
        <el-form-item v-if="userStore.isAdmin" label="官网发布">
          <el-switch v-model="editorForm.official" active-text="官方笔记" inactive-text="仅自己可见" />
        </el-form-item>
        <el-form-item label="Markdown 正文" prop="content">
          <el-input v-model="editorForm.content" type="textarea" :rows="12" resize="vertical" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitEditor">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Document, Edit, Hide, Notebook, Plus, Promotion, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { createNote, deleteNote, fetchNote, fetchNotes, updateNote, uploadNote } from '@/api/note'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { useUserStore } from '@/stores/user'
import type { Note } from '@/types/note'

const notes = ref<Note[]>([])
const activeNote = ref<Note | null>(null)
const loading = ref(false)
const activeLoading = ref(false)
const uploading = ref(false)
const saving = ref(false)
const keyword = ref('')
const fileInput = ref<HTMLInputElement>()
const editorVisible = ref(false)
const editingNote = ref<Note | null>(null)
const editorRef = ref<FormInstance>()
const editorForm = reactive({
  title: '',
  tags: '',
  content: '',
  official: false
})
const userStore = useUserStore()

const editorRules: FormRules = {
  title: [{ required: true, message: '请输入笔记标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入笔记正文', trigger: 'blur' }]
}

const activeTags = computed(() => {
  return activeNote.value?.tags
    ?.split(',')
    .map((tag) => tag.trim())
    .filter(Boolean) ?? []
})

onMounted(() => {
  loadNotes()
})

async function loadNotes() {
  loading.value = true
  try {
    notes.value = await fetchNotes({ keyword: keyword.value || undefined, limit: 12 })
    if (!notes.value.length) {
      activeNote.value = null
      return
    }
    const current = activeNote.value
      ? notes.value.find((note) => note.id === activeNote.value?.id)
      : notes.value[0]
    if (current) {
      await selectNote(current)
    }
  } finally {
    loading.value = false
  }
}

async function selectNote(note: Note) {
  activeLoading.value = true
  try {
    activeNote.value = await fetchNote(note.id)
  } finally {
    activeLoading.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  uploading.value = true
  try {
    const note = await uploadNote(file)
    ElMessage.success('笔记已上传')
    await loadNotes()
    await selectNote(note)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function openCreate() {
  editingNote.value = null
  Object.assign(editorForm, {
    title: '',
    tags: '',
    content: '# 新笔记\n\n',
    official: false
  })
  editorVisible.value = true
}

function openEdit(note: Note) {
  editingNote.value = note
  Object.assign(editorForm, {
    title: note.title,
    tags: note.tags ?? '',
    content: note.content,
    official: note.official
  })
  editorVisible.value = true
}

async function submitEditor() {
  const valid = await editorRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = {
      title: editorForm.title.trim(),
      tags: editorForm.tags.trim(),
      content: editorForm.content.trim(),
      ...(userStore.isAdmin ? { official: editorForm.official } : {})
    }
    const saved = editingNote.value
      ? await updateNote(editingNote.value.id, payload)
      : await createNote(payload)
    ElMessage.success('笔记已保存')
    editorVisible.value = false
    await loadNotes()
    await selectNote(saved)
  } finally {
    saving.value = false
  }
}

async function toggleOfficial(note: Note) {
  const saved = await updateNote(note.id, { official: !note.official })
  activeNote.value = saved
  const item = notes.value.find((candidate) => candidate.id === note.id)
  if (item) {
    item.official = saved.official
    item.published = saved.published
  }
  ElMessage.success(saved.official ? '已发布到官网官方笔记' : '已从官网撤下')
}

async function handleDelete(note: Note) {
  await ElMessageBox.confirm(`确认删除「${note.title}」吗？`, '删除笔记', { type: 'warning' })
  await deleteNote(note.id)
  ElMessage.success('笔记已删除')
  activeNote.value = null
  await loadNotes()
}

function formatDate(value?: string) {
  if (!value) {
    return '刚刚'
  }
  return new Date(value).toLocaleDateString('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<style scoped>
.blog-panel {
  border-color: #dfe5ed;
  background: #fbfcfd;
}

.blog-header {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96)),
    #ffffff;
}

.blog-kicker {
  margin-bottom: 5px;
  color: #086ddd;
  font-size: 12px;
  font-weight: 800;
}

.blog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.hidden-file {
  display: none;
}

.blog-body {
  display: grid;
  grid-template-columns: minmax(270px, 0.72fr) minmax(0, 1.6fr);
  min-height: 500px;
}

.note-rail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 13px;
  padding: 16px;
  border-right: 1px solid #e5e9f0;
  background: #f8fafc;
}

.note-stat-strip {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: #667085;
  font-size: 12px;
  font-weight: 700;
}

.note-list-scroll {
  height: 392px;
}

.note-list-item {
  display: grid;
  width: 100%;
  grid-template-columns: 9px minmax(0, 1fr);
  gap: 10px;
  padding: 12px 10px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: #25313f;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.note-list-item:hover,
.note-list-item.active {
  border-color: #d9e6f6;
  background: #ffffff;
}

.note-list-item.active {
  box-shadow: 0 10px 24px rgba(27, 39, 51, 0.06);
}

.note-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: #2e80f2;
}

.note-list-copy {
  min-width: 0;
}

.note-list-copy strong,
.note-list-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-list-copy strong {
  color: #17202a;
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
}

.note-list-copy small {
  display: -webkit-box;
  margin-top: 5px;
  color: #667085;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.things-reader {
  min-width: 0;
  padding: 18px;
  background:
    linear-gradient(180deg, #ffffff 0%, #fbfcff 100%),
    #ffffff;
}

.reader-titlebar {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 13px;
  align-items: start;
  margin-bottom: 16px;
}

.reader-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border: 1px solid #dce8f8;
  border-radius: 8px;
  color: #086ddd;
  background: #eef6ff;
}

.reader-heading {
  min-width: 0;
}

.reader-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #707a89;
  font-size: 12px;
  font-weight: 700;
}

.reader-heading h3 {
  margin: 5px 0 0;
  color: #222222;
  font-size: 22px;
  line-height: 1.28;
}

.reader-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 9px;
}

.reader-actions {
  display: flex;
  gap: 8px;
}

.reader-paper {
  max-height: 620px;
  overflow: auto;
  padding: 22px 24px;
  border: 1px solid #e4e9f1;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.things-reader :deep(.markdown-content) {
  max-width: 760px;
  color: #222222;
  font-size: 15px;
  line-height: 1.72;
}

.things-reader :deep(.markdown-content h1) {
  color: #222222;
  font-size: 1.7rem;
}

.things-reader :deep(.markdown-content h2) {
  color: #222222;
  font-size: 1.45rem;
}

.things-reader :deep(.markdown-content h3) {
  color: #2e80f2;
}

.things-reader :deep(.markdown-content h4) {
  color: #bd8e37;
}

.things-reader :deep(.markdown-content strong),
.things-reader :deep(.markdown-content em) {
  color: #c32b74;
}

.things-reader :deep(.markdown-content blockquote) {
  border-left-color: #3eb4bf;
  color: #3f5860;
  background: transparent;
}

.things-reader :deep(.markdown-content code) {
  color: #c32b74;
  background: #eceef1;
}

.things-reader :deep(.markdown-content pre) {
  background: #f0f2f5;
}

.things-reader :deep(.markdown-content pre code) {
  color: #303540;
}

.reader-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 460px;
  padding: 24px;
  color: #667085;
  text-align: center;
}

.reader-empty .el-icon {
  color: #2e80f2;
  font-size: 32px;
}

.reader-empty strong {
  color: #17202a;
  font-size: 15px;
}

.reader-empty span {
  max-width: 320px;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 980px) {
  .blog-body {
    grid-template-columns: 1fr;
  }

  .note-rail {
    border-right: 0;
    border-bottom: 1px solid #e5e9f0;
  }

  .note-list-scroll {
    height: 260px;
  }

  .reader-titlebar {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .reader-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
