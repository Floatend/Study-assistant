<template>
  <main class="knowledge-page">
    <div class="knowledge-shell">
      <PublicSiteHeader />

      <section class="knowledge-intro">
        <div><p>LEARNING ARCHIVE</p><h1>学习笔记</h1></div>
        <span>站长将课程、技术与项目实践整理在这里。每一篇只在经过复盘和编辑后公开。</span>
      </section>

      <section class="knowledge-layout" v-loading="loading">
        <aside class="knowledge-library">
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索学习笔记" @clear="loadNotes" @keyup.enter="loadNotes" />
          <div class="library-label">分类索引</div>
          <button class="category-link" :class="{ active: !selectedCategory }" type="button" @click="selectCategory('')"><span>全部文章</span><small>{{ totalCount }}</small></button>
          <button v-for="category in categories" :key="category.name" class="category-link" :class="{ active: selectedCategory === category.name }" type="button" @click="selectCategory(category.name)"><span>{{ category.name }}</span><small>{{ category.count }}</small></button>
          <div class="library-label notes-label">文章列表</div>
          <el-scrollbar class="public-note-scroll">
            <button v-for="note in notes" :key="note.id" class="public-note-link" :class="{ active: activeNote?.id === note.id }" type="button" @click="selectNote(note)">
              <small>{{ note.category || '未分类' }} · {{ formatShortDate(note.updatedAt) }}</small><strong>{{ note.title }}</strong>
            </button>
            <el-empty v-if="!notes.length && !loading" description="暂时没有对应笔记" :image-size="72" />
          </el-scrollbar>
        </aside>

        <article v-if="activeNote" class="knowledge-article" v-loading="articleLoading">
          <header class="article-head">
            <div class="article-path"><span>学习笔记</span><b>/</b><span>{{ activeNote.category || '未分类' }}</span></div>
            <h2>{{ activeNote.title }}</h2>
            <p v-if="activeNote.summary">{{ activeNote.summary }}</p>
            <div class="article-meta"><span>{{ activeNote.authorName || 'linge.xin' }}</span><span>{{ formatLongDate(activeNote.updatedAt) }}</span><span>{{ activeNote.wordCount }} 字</span></div>
            <div v-if="activeTags.length" class="article-tags"><span v-for="tag in activeTags" :key="tag"># {{ tag }}</span></div>
          </header>
          <div class="article-body"><MarkdownContent :content="activeNote.content" /></div>
          <nav class="article-pagination" aria-label="相邻文章">
            <button type="button" :disabled="!previousNote" @click="previousNote && selectNote(previousNote)"><small>上一篇</small><strong>{{ previousNote?.title || '已经是第一篇' }}</strong></button>
            <button type="button" :disabled="!nextNote" @click="nextNote && selectNote(nextNote)"><small>下一篇</small><strong>{{ nextNote?.title || '已经是最后一篇' }}</strong></button>
          </nav>
        </article>

        <section v-else class="knowledge-empty">
          <p>还没有可以阅读的内容。</p>
          <RouterLink to="/">返回首页</RouterLink>
        </section>

        <aside v-if="activeNote" class="article-outline">
          <p>文章目录</p>
          <button v-for="heading in headings" :key="heading.id" :style="{ paddingLeft: `${(heading.level - 1) * 12}px` }" type="button" @click="scrollToHeading(heading.id)">{{ heading.text }}</button>
          <span v-if="!headings.length">正文没有标题</span>
        </aside>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNoteCategories, fetchOfficialNotes } from '@/api/note'
import MarkdownContent from '@/components/MarkdownContent.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import type { Note, NoteCategory } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'

const route = useRoute()
const router = useRouter()
const notes = ref<Note[]>([])
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const keyword = ref('')
const selectedCategory = ref('')
const loading = ref(false)
const articleLoading = ref(false)

const totalCount = computed(() => categories.value.reduce((total, category) => total + category.count, 0))
const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const headings = computed(() => extractMarkdownHeadings(activeNote.value?.content))
const activeIndex = computed(() => notes.value.findIndex((note) => note.id === activeNote.value?.id))
const previousNote = computed(() => activeIndex.value > 0 ? notes.value[activeIndex.value - 1] : null)
const nextNote = computed(() => activeIndex.value >= 0 && activeIndex.value < notes.value.length - 1 ? notes.value[activeIndex.value + 1] : null)

onMounted(async () => {
  const requestedCategory = typeof route.query.category === 'string' ? route.query.category : ''
  selectedCategory.value = requestedCategory
  const [categoryItems] = await Promise.all([fetchOfficialNoteCategories(), loadNotes(false)])
  categories.value = categoryItems
})

watch(() => route.query.note, (value) => {
  const id = Number(value)
  const target = notes.value.find((note) => note.id === id)
  if (target && target.id !== activeNote.value?.id) selectNote(target, false)
})

async function loadNotes(syncRoute = true) {
  loading.value = true
  try {
    const items = await fetchOfficialNotes({ keyword: keyword.value.trim() || undefined, category: selectedCategory.value || undefined, limit: 100 })
    notes.value = items
    const requestedId = Number(route.query.note)
    const target = items.find((note) => note.id === requestedId) ?? items[0]
    if (target) await selectNote(target, syncRoute)
    else activeNote.value = null
  } finally { loading.value = false }
}

async function selectCategory(category: string) {
  selectedCategory.value = category
  await router.replace({ query: category ? { category } : {} })
  await loadNotes()
}

async function selectNote(note: Note, syncRoute = true) {
  articleLoading.value = true
  try {
    activeNote.value = await fetchOfficialNote(note.id)
    if (syncRoute && Number(route.query.note) !== note.id) {
      await router.replace({ query: { ...(selectedCategory.value ? { category: selectedCategory.value } : {}), note: String(note.id) } })
    }
  } finally { articleLoading.value = false }
}

function scrollToHeading(id: string) { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
function formatShortDate(value?: string) { return value ? new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) : '刚刚' }
function formatLongDate(value?: string) { return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : '刚刚发布' }
</script>

<style scoped>
.knowledge-page{min-height:100vh;color:#20271f;background:#f4f0e7}.knowledge-shell{width:min(1320px,calc(100% - 8vw));margin:0 auto}.knowledge-intro{display:grid;grid-template-columns:1fr minmax(260px,.52fr);gap:28px;align-items:end;padding:clamp(56px,9vw,120px) 0 40px;border-bottom:1px solid #d6d1c7}.knowledge-intro p,.library-label,.article-outline>p{margin:0;color:#a54837;font-size:11px;font-weight:800;letter-spacing:.13em}.knowledge-intro h1{margin:11px 0 0;color:#1e2a21;font-family:Georgia,"Times New Roman","PingFang SC",serif;font-size:clamp(48px,7vw,90px);font-weight:600;line-height:.9}.knowledge-intro>span{color:#69746a;font-size:15px;line-height:1.85}.knowledge-layout{display:grid;grid-template-columns:minmax(245px,.55fr) minmax(0,1.55fr) minmax(175px,.35fr);min-height:700px}.knowledge-library{padding:26px 22px 40px 0;border-right:1px solid #d6d1c7}.library-label{margin:26px 0 8px;color:#727c74;letter-spacing:.09em}.notes-label{margin-top:30px}.category-link{display:flex;width:100%;align-items:center;justify-content:space-between;padding:9px 10px;border:0;border-radius:4px;color:#58645c;background:transparent;font-size:13px;text-align:left;cursor:pointer}.category-link:hover,.category-link.active{color:#1f4e42;background:#e6ece4}.category-link small{color:#969e97;font-size:11px}.public-note-scroll{height:360px;margin-right:-10px;padding-right:10px}.public-note-link{display:grid;width:100%;gap:4px;padding:13px 10px;border:0;border-bottom:1px solid #e1ddd3;color:inherit;background:transparent;text-align:left;cursor:pointer}.public-note-link:hover,.public-note-link.active{background:#ebe7dc}.public-note-link small{color:#a55a4b;font-size:11px}.public-note-link strong{overflow:hidden;color:#273229;font-size:14px;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.knowledge-article{min-width:0;padding:42px clamp(28px,4.2vw,76px) 66px}.article-head{max-width:820px;padding-bottom:28px;border-bottom:1px solid #dfddd6}.article-path{display:flex;gap:8px;color:#a44c3c;font-size:12px}.article-path b{font-weight:500}.article-head h2{margin:17px 0 0;color:#1e2921;font-size:clamp(29px,3.6vw,50px);font-weight:600;line-height:1.22}.article-head>p{margin:17px 0 0;color:#68746b;font-size:16px;line-height:1.82}.article-meta{display:flex;flex-wrap:wrap;gap:10px;margin-top:19px;color:#89928b;font-size:12px}.article-meta span+span::before{margin-right:10px;color:#c5c9c4;content:'•'}.article-tags{display:flex;flex-wrap:wrap;gap:8px;margin-top:15px}.article-tags span{color:#5d695f;font-size:12px}.article-body{max-width:820px;padding-top:34px}.knowledge-article :deep(.markdown-content){color:#29342c;font-size:16px;line-height:1.95}.knowledge-article :deep(.markdown-content h1),.knowledge-article :deep(.markdown-content h2),.knowledge-article :deep(.markdown-content h3),.knowledge-article :deep(.markdown-content h4){scroll-margin-top:20px;color:#1d2920}.knowledge-article :deep(.markdown-content h2){margin-top:50px;padding-bottom:10px;border-bottom:1px solid #deded8}.article-pagination{display:grid;grid-template-columns:1fr 1fr;gap:18px;max-width:820px;margin-top:62px;padding-top:23px;border-top:1px solid #dfddd6}.article-pagination button{display:grid;gap:5px;padding:0;border:0;color:#275a4c;background:transparent;text-align:left;cursor:pointer}.article-pagination button:last-child{text-align:right}.article-pagination button:disabled{color:#9ba39d;cursor:default}.article-pagination small{font-size:11px}.article-pagination strong{font-size:13px}.article-outline{padding:46px 0 40px 16px;border-left:1px solid #d6d1c7}.article-outline>p{color:#69736b;letter-spacing:.09em}.article-outline>span{display:block;margin-top:16px;color:#969e96;font-size:12px}.article-outline button{display:block;width:100%;overflow:hidden;margin-top:11px;border:0;color:#667269;background:transparent;font-size:12px;line-height:1.45;text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer}.article-outline button:hover{color:#a54837}.knowledge-empty{display:grid;grid-column:2 / 4;align-content:center;justify-items:start;gap:11px;padding:50px}.knowledge-empty p{margin:0;color:#59655c;font-size:17px}.knowledge-empty a{color:#24604f;font-size:14px;font-weight:700;text-decoration:none}.knowledge-empty a:hover{text-decoration:underline}@media(max-width:1000px){.knowledge-layout{grid-template-columns:minmax(230px,.55fr) minmax(0,1.45fr)}.article-outline{display:none}.knowledge-empty{grid-column:2}}@media(max-width:760px){.knowledge-shell{width:min(100% - 32px,1320px)}.knowledge-intro{grid-template-columns:1fr;padding:52px 0 32px}.knowledge-layout{grid-template-columns:1fr}.knowledge-library{padding:22px 0;border-right:0;border-bottom:1px solid #d6d1c7}.public-note-scroll{height:240px}.knowledge-article,.knowledge-empty{grid-column:auto;padding:32px 0 48px}.article-pagination{grid-template-columns:1fr}.article-pagination button:last-child{text-align:left}.knowledge-intro h1{font-size:52px}}
</style>
