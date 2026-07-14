<template>
  <main class="official-notes-page">
    <div class="notes-shell">
      <PublicSiteHeader />

      <section class="notes-intro">
        <p>OFFICIAL NOTES</p>
        <h1>官方笔记</h1>
        <span>来自本站管理员的公开记录。私人草稿、任务和个人笔记不会出现在这里。</span>
      </section>

      <section class="notes-layout" v-loading="loading">
        <aside class="notes-index">
          <el-input
            v-model="keyword"
            :prefix-icon="Search"
            clearable
            placeholder="搜索官方笔记"
            @clear="loadNotes"
            @keyup.enter="loadNotes"
          />
          <p>{{ notes.length }} 篇内容</p>
          <div class="note-cards">
            <button
              v-for="note in notes"
              :key="note.id"
              class="note-card"
              :class="{ active: activeNote?.id === note.id }"
              type="button"
              @click="selectNote(note)"
            >
              <span>{{ formatDate(note.updatedAt) }}</span>
              <strong>{{ note.title }}</strong>
              <small>{{ note.summary || '阅读这篇笔记。' }}</small>
            </button>
            <el-empty v-if="!notes.length && !loading" description="暂时还没有官方笔记" :image-size="86" />
          </div>
        </aside>

        <article v-if="activeNote" class="official-article" v-loading="activeLoading">
          <header>
            <p>{{ activeNote.authorName || 'linge.xin' }} · {{ formatLongDate(activeNote.updatedAt) }} · {{ activeNote.wordCount }} 字</p>
            <h2>{{ activeNote.title }}</h2>
            <div v-if="activeTags.length" class="article-tags">
              <span v-for="tag in activeTags" :key="tag"># {{ tag }}</span>
            </div>
          </header>
          <div class="article-content">
            <MarkdownContent :content="activeNote.content" />
          </div>
        </article>

        <section v-else class="empty-article">
          <h2>还没有正式发布的笔记。</h2>
          <p>登录 GoalBot 后，以管理员身份把笔记标记为「官网发布」，它才会出现在这里。</p>
          <RouterLink to="/login">进入 GoalBot</RouterLink>
        </section>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNotes } from '@/api/note'
import MarkdownContent from '@/components/MarkdownContent.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import type { Note } from '@/types/note'

const notes = ref<Note[]>([])
const activeNote = ref<Note | null>(null)
const keyword = ref('')
const loading = ref(false)
const activeLoading = ref(false)

const activeTags = computed(() => activeNote.value?.tags
  ?.split(',')
  .map((tag) => tag.trim())
  .filter(Boolean) ?? [])

onMounted(loadNotes)

async function loadNotes() {
  loading.value = true
  try {
    notes.value = await fetchOfficialNotes({ keyword: keyword.value.trim() || undefined, limit: 24 })
    const selected = activeNote.value
      ? notes.value.find((note) => note.id === activeNote.value?.id)
      : notes.value[0]
    if (selected) {
      await selectNote(selected)
    } else {
      activeNote.value = null
    }
  } finally {
    loading.value = false
  }
}

async function selectNote(note: Note) {
  activeLoading.value = true
  try {
    activeNote.value = await fetchOfficialNote(note.id)
  } finally {
    activeLoading.value = false
  }
}

function formatDate(value?: string) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function formatLongDate(value?: string) {
  if (!value) return '刚刚发布'
  return new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>

<style scoped>
.official-notes-page {
  min-height: 100vh;
  color: #26342d;
  background: #f7f7f2;
}

.notes-shell {
  width: min(1180px, calc(100% - 9vw));
  margin: 0 auto;
}

.notes-intro {
  padding: 74px 0 44px;
  border-bottom: 1px solid #dfe3dc;
  animation: intro-arrival 0.65s ease-out both;
}

.notes-intro p {
  margin: 0;
  color: #4a7863;
  font-size: 12px;
  font-weight: 700;
}

.notes-intro h1 {
  margin: 13px 0 9px;
  color: #26342d;
  font-size: 40px;
  font-weight: 650;
}

.notes-intro span {
  color: #727d75;
  font-size: 15px;
  line-height: 1.75;
}

.notes-layout {
  display: grid;
  grid-template-columns: minmax(242px, 0.58fr) minmax(0, 1.42fr);
  min-height: 620px;
}

.notes-index {
  padding: 29px 28px 40px 0;
  border-right: 1px solid #dfe3dc;
}

.notes-index > p {
  margin: 18px 0 12px;
  color: #879087;
  font-size: 12px;
}

.note-cards {
  display: grid;
  gap: 4px;
}

.note-card {
  display: grid;
  width: 100%;
  gap: 5px;
  padding: 14px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: inherit;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.note-card:hover,
.note-card.active {
  border-color: #d7e4dc;
  background: #eef4ef;
}

.note-card span {
  color: #8b938d;
  font-size: 11px;
}

.note-card strong {
  overflow: hidden;
  color: #2b3a32;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-card small {
  display: -webkit-box;
  overflow: hidden;
  color: #747d76;
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.official-article {
  min-width: 0;
  padding: 42px 0 58px 56px;
  animation: intro-arrival 0.65s 0.12s ease-out both;
}

.official-article header {
  max-width: 770px;
  padding-bottom: 28px;
  border-bottom: 1px solid #dfe3dc;
}

.official-article header > p {
  margin: 0;
  color: #7e887f;
  font-size: 12px;
}

.official-article h2 {
  margin: 13px 0 0;
  color: #24322b;
  font-size: 31px;
  font-weight: 650;
  line-height: 1.34;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 16px;
  color: #4d7865;
  font-size: 13px;
}

.article-content {
  max-width: 770px;
  padding-top: 30px;
}

.official-article :deep(.markdown-content) {
  color: #354139;
  font-size: 16px;
  line-height: 1.9;
}

.official-article :deep(.markdown-content h1),
.official-article :deep(.markdown-content h2),
.official-article :deep(.markdown-content h3),
.official-article :deep(.markdown-content h4) {
  color: #27362e;
  font-weight: 650;
}

.official-article :deep(.markdown-content blockquote) {
  border-left-color: #5d8d76;
  color: #637168;
  background: #edf3ee;
}

.empty-article {
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 11px;
  padding: 40px 56px;
}

.empty-article h2,
.empty-article p {
  margin: 0;
}

.empty-article h2 {
  color: #2b3932;
  font-size: 22px;
  font-weight: 650;
}

.empty-article p {
  max-width: 430px;
  color: #737d76;
  font-size: 14px;
  line-height: 1.75;
}

.empty-article a {
  margin-top: 9px;
  color: #2d6b58;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}

.empty-article a:hover {
  text-decoration: underline;
}

@keyframes intro-arrival {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 760px) {
  .notes-shell { width: min(100% - 32px, 1180px); }
  .notes-intro { padding: 48px 0 30px; }
  .notes-intro h1 { font-size: 32px; }
  .notes-layout { grid-template-columns: 1fr; }
  .notes-index { padding: 24px 0; border-right: 0; border-bottom: 1px solid #dfe3dc; }
  .note-cards { max-height: 280px; overflow-y: auto; }
  .official-article, .empty-article { padding: 34px 0 46px; }
  .official-article h2 { font-size: 26px; }
}

@media (prefers-reduced-motion: reduce) {
  .notes-intro,
  .official-article { animation: none; }
}
</style>
