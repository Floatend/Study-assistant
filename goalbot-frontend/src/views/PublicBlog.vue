<template>
  <main class="public-blog-page">
    <header class="public-header">
      <RouterLink class="public-wordmark" to="/">Linge Notes</RouterLink>
      <nav class="public-nav" aria-label="网站导航">
        <span>公开笔记</span>
        <RouterLink to="/login">进入 GoalBot</RouterLink>
      </nav>
    </header>

    <section class="public-intro">
      <div>
        <p class="intro-kicker">PERSONAL BLOG</p>
        <h1>笔记、学习与正在发生的日常。</h1>
      </div>
      <p>这里展示从个人工作台发布出来的 Markdown 笔记。计划与私人任务留在 GoalBot 里，公开内容才会出现在这里。</p>
    </section>

    <section class="public-reader-layout" v-loading="loading">
      <aside class="public-note-index">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索公开笔记"
          @clear="loadNotes"
          @keyup.enter="loadNotes"
        />
        <div class="index-caption">{{ notes.length }} 篇已发布</div>

        <div class="public-note-list">
          <button
            v-for="note in notes"
            :key="note.id"
            class="public-note-item"
            :class="{ active: activeNote?.id === note.id }"
            type="button"
            @click="selectNote(note)"
          >
            <span>{{ formatDate(note.updatedAt) }}</span>
            <strong>{{ note.title }}</strong>
            <small>{{ note.summary || '打开查看笔记正文。' }}</small>
          </button>
          <el-empty v-if="!notes.length && !loading" description="还没有公开笔记" :image-size="82" />
        </div>

        <section class="goalbot-entry">
          <p>GOALBOT WORKSPACE</p>
          <strong>个人日程与 AI 助手</strong>
          <span>任务、时间表、复盘和飞书对话只在登录后的个人工作台中可见。</span>
          <RouterLink to="/login">登录 GoalBot</RouterLink>
        </section>
      </aside>

      <article v-if="activeNote" class="public-article" v-loading="activeLoading">
        <header class="article-header">
          <p class="article-meta">
            <span>{{ activeNote.authorName || '匿名作者' }}</span>
            <span>{{ formatLongDate(activeNote.updatedAt) }}</span>
            <span>{{ activeNote.wordCount }} 字</span>
          </p>
          <h2>{{ activeNote.title }}</h2>
          <div v-if="activeTags.length" class="article-tags">
            <span v-for="tag in activeTags" :key="tag"># {{ tag }}</span>
          </div>
        </header>
        <div class="article-paper">
          <MarkdownContent :content="activeNote.content" />
        </div>
      </article>

      <section v-else class="public-empty">
        <p>公开笔记会在这里安静地展开。</p>
        <span>登录 GoalBot 后，可以在个人笔记面板选择发布。</span>
        <RouterLink to="/login">前往个人工作台</RouterLink>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { fetchPublishedNote, fetchPublishedNotes } from '@/api/note'
import MarkdownContent from '@/components/MarkdownContent.vue'
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
    notes.value = await fetchPublishedNotes({ keyword: keyword.value.trim() || undefined, limit: 24 })
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
    activeNote.value = await fetchPublishedNote(note.id)
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
  return new Date(value).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<style scoped>
.public-blog-page {
  min-height: 100vh;
  padding: 0 5vw 56px;
  color: #27302d;
  background: #fafaf8;
}

.public-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1180px;
  min-height: 78px;
  margin: 0 auto;
  border-bottom: 1px solid #e5e7e2;
}

.public-wordmark {
  color: #1e2925;
  font-size: 18px;
  font-weight: 700;
  text-decoration: none;
}

.public-nav {
  display: flex;
  align-items: center;
  gap: 24px;
  color: #737b75;
  font-size: 14px;
}

.public-nav a,
.public-empty a {
  color: #2d6a58;
  font-weight: 650;
  text-decoration: none;
}

.public-nav a:hover,
.public-empty a:hover {
  text-decoration: underline;
}

.public-intro {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(280px, 0.7fr);
  gap: 40px;
  max-width: 1180px;
  margin: 0 auto;
  padding: 70px 0 46px;
  border-bottom: 1px solid #e5e7e2;
}

.intro-kicker {
  margin: 0 0 12px;
  color: #58836f;
  font-size: 12px;
  font-weight: 700;
}

.public-intro h1 {
  max-width: 720px;
  margin: 0;
  color: #202a26;
  font-size: 38px;
  font-weight: 650;
  line-height: 1.28;
}

.public-intro > p {
  align-self: end;
  max-width: 360px;
  margin: 0;
  color: #66716b;
  font-size: 15px;
  line-height: 1.85;
}

.public-reader-layout {
  display: grid;
  grid-template-columns: minmax(240px, 0.62fr) minmax(0, 1.38fr);
  max-width: 1180px;
  min-height: 620px;
  margin: 0 auto;
}

.public-note-index {
  padding: 28px 28px 28px 0;
  border-right: 1px solid #e5e7e2;
}

.index-caption {
  margin: 18px 0 10px;
  color: #8a928d;
  font-size: 12px;
}

.public-note-list {
  display: grid;
  gap: 3px;
}

.goalbot-entry {
  display: grid;
  gap: 8px;
  margin-top: 28px;
  padding: 16px;
  border: 1px solid #dce8e0;
  border-radius: 6px;
  background: #f2f7f3;
}

.goalbot-entry p {
  margin: 0;
  color: #5e8b76;
  font-size: 11px;
  font-weight: 700;
}

.goalbot-entry strong {
  color: #2b3932;
  font-size: 14px;
  font-weight: 650;
}

.goalbot-entry span {
  color: #68756d;
  font-size: 12px;
  line-height: 1.65;
}

.goalbot-entry a {
  margin-top: 3px;
  color: #2d6a58;
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
}

.goalbot-entry a:hover {
  text-decoration: underline;
}

.public-note-item {
  display: grid;
  width: 100%;
  gap: 5px;
  padding: 13px 12px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: #2a3430;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.public-note-item:hover,
.public-note-item.active {
  border-color: #d8e4dc;
  background: #f0f6f2;
}

.public-note-item span {
  color: #8a928d;
  font-size: 11px;
}

.public-note-item strong {
  overflow: hidden;
  color: #27302d;
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.public-note-item small {
  display: -webkit-box;
  overflow: hidden;
  color: #727b75;
  font-size: 12px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.public-article {
  min-width: 0;
  padding: 40px 0 48px 54px;
}

.article-header {
  max-width: 780px;
  padding-bottom: 27px;
  border-bottom: 1px solid #e7e8e4;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin: 0;
  color: #7b847e;
  font-size: 12px;
}

.article-meta span + span::before {
  margin-right: 9px;
  color: #b5bbb6;
  content: "·";
}

.article-header h2 {
  margin: 12px 0 0;
  color: #202a26;
  font-size: 31px;
  font-weight: 650;
  line-height: 1.32;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 16px;
  color: #59816f;
  font-size: 13px;
}

.article-paper {
  max-width: 780px;
  padding-top: 30px;
}

.public-article :deep(.markdown-content) {
  color: #35403a;
  font-size: 16px;
  line-height: 1.9;
}

.public-article :deep(.markdown-content h1),
.public-article :deep(.markdown-content h2),
.public-article :deep(.markdown-content h3),
.public-article :deep(.markdown-content h4) {
  color: #25312b;
  font-weight: 650;
}

.public-article :deep(.markdown-content h1) {
  font-size: 25px;
}

.public-article :deep(.markdown-content h2) {
  font-size: 21px;
}

.public-article :deep(.markdown-content blockquote) {
  border-left-color: #5d8d76;
  color: #627168;
  background: #f2f6f2;
}

.public-empty {
  display: grid;
  align-content: center;
  justify-items: start;
  gap: 9px;
  padding: 40px 54px;
}

.public-empty p {
  margin: 0;
  color: #2a3430;
  font-size: 20px;
}

.public-empty span {
  color: #737b75;
  font-size: 14px;
  line-height: 1.75;
}

.public-empty a {
  margin-top: 10px;
  font-size: 14px;
}

@media (max-width: 820px) {
  .public-blog-page {
    padding: 0 22px 40px;
  }

  .public-intro,
  .public-reader-layout {
    grid-template-columns: 1fr;
  }

  .public-intro {
    gap: 18px;
    padding: 46px 0 32px;
  }

  .public-intro h1 {
    font-size: 30px;
  }

  .public-note-index {
    padding: 24px 0;
    border-right: 0;
    border-bottom: 1px solid #e5e7e2;
  }

  .public-note-list {
    max-height: 260px;
    overflow-y: auto;
  }

  .public-article,
  .public-empty {
    padding: 32px 0;
  }

  .article-header h2 {
    font-size: 26px;
  }
}
</style>
