<template>
  <main class="knowledge-page">
    <div class="knowledge-shell">
      <PublicSiteHeader />

      <section class="knowledge-intro">
        <div><p>LEARNING ARCHIVE</p><h1>学习笔记</h1></div>
      </section>

      <section class="knowledge-layout" v-loading="loading">
        <aside class="knowledge-library">
          <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="搜索学习笔记" @clear="loadNotes" @keyup.enter="loadNotes" />
          <div class="library-label">分类索引</div>
          <NoteCategoryTree :categories="categories" :model-value="selectedCategory" :all-count="totalCount" all-label="全部文章" @select="selectCategoryNode" />
          <div class="library-label notes-label">文章列表</div>
          <el-scrollbar class="public-note-scroll">
            <button v-for="note in notes" :key="note.id" class="public-note-link" :class="{ active: activeNote?.id === note.id }" type="button" @click="selectNote(note)">
              <small>{{ note.category || '未分类' }} · {{ formatShortDate(note.updatedAt) }}</small><strong>{{ note.title }}</strong>
            </button>
            <el-empty v-if="!notes.length && !loading" description="暂时没有对应笔记" :image-size="72" />
          </el-scrollbar>
        </aside>

        <article v-if="activeNote" class="knowledge-article" v-loading="articleLoading">
          <div class="reading-progress" aria-hidden="true"><span :style="{ width: `${readingProgress}%` }" /></div>
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
          <button v-for="heading in headings" :key="heading.id" :class="{ active: activeHeadingId === heading.id }" :style="{ paddingLeft: `${(heading.level - 1) * 12}px` }" type="button" @click="scrollToHeading(heading.id)">{{ heading.text }}</button>
          <span v-if="!headings.length">正文没有标题</span>
        </aside>
      </section>
    </div>
    <BackToTopButton />
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNoteCategories, fetchOfficialNotes } from '@/api/note'
import BackToTopButton from '@/components/BackToTopButton.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import NoteCategoryTree from '@/components/NoteCategoryTree.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import type { Note, NoteCategory } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'
import { buildNoteCategoryTree, findNoteCategoryNode, summarizeNoteCategories } from '@/utils/noteCategories'

const route = useRoute()
const router = useRouter()
const notes = ref<Note[]>([])
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const keyword = ref('')
const selectedCategory = ref('')
const loading = ref(false)
const articleLoading = ref(false)
const readingProgress = ref(0)
const activeHeadingId = ref('')
const selectedCategoryDescendants = ref<string[] | null>(null)
let scrollFrame: number | undefined

const totalCount = computed(() => categories.value.reduce((total, category) => total + category.count, 0))
const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const headings = computed(() => extractMarkdownHeadings(activeNote.value?.content))
const activeIndex = computed(() => notes.value.findIndex((note) => note.id === activeNote.value?.id))
const previousNote = computed(() => activeIndex.value > 0 ? notes.value[activeIndex.value - 1] : null)
const nextNote = computed(() => activeIndex.value >= 0 && activeIndex.value < notes.value.length - 1 ? notes.value[activeIndex.value + 1] : null)

onMounted(async () => {
  const requestedCategory = typeof route.query.category === 'string' ? route.query.category : ''
  try { categories.value = await fetchOfficialNoteCategories() } catch { categories.value = [] }
  selectedCategory.value = requestedCategory
  const initialCategoryNode = findNoteCategoryNode(buildNoteCategoryTree(categories.value), requestedCategory)
  selectedCategoryDescendants.value = initialCategoryNode?.children.length ? initialCategoryNode.leafValues : null
  try { await loadNotes(false) } catch { notes.value = []; activeNote.value = null }
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  if (scrollFrame !== undefined) window.cancelAnimationFrame(scrollFrame)
})

watch(() => route.query.note, (value) => {
  const id = Number(value)
  const target = notes.value.find((note) => note.id === id)
  if (target && target.id !== activeNote.value?.id) selectNote(target, false)
})

watch(() => route.query.category, async (value) => {
  const category = typeof value === 'string' ? value : ''
  if (category === selectedCategory.value) return
  selectedCategory.value = category
  const node = findNoteCategoryNode(buildNoteCategoryTree(categories.value), category)
  selectedCategoryDescendants.value = node?.children.length ? node.leafValues : null
  await loadNotes(false)
})

watch(activeNote, () => nextTick(updateReadingState))

async function loadNotes(syncRoute = true) {
  loading.value = true
  try {
    const descendants = selectedCategoryDescendants.value
    const items = await fetchOfficialNotes({
      keyword: keyword.value.trim() || undefined,
      category: descendants ? undefined : selectedCategory.value || undefined,
      limit: 100,
    })
    notes.value = descendants ? items.filter((note) => descendants.includes(note.category || '未分类')) : items
    if (!categories.value.length) categories.value = summarizeNoteCategories(items)
    const requestedId = Number(route.query.note)
    const target = notes.value.find((note) => note.id === requestedId) ?? notes.value[0]
    if (target) await selectNote(target, syncRoute)
    else activeNote.value = null
  } finally { loading.value = false }
}

async function selectCategoryNode(category: string, descendants: string[] | null) {
  selectedCategory.value = category
  selectedCategoryDescendants.value = descendants
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

function scrollToHeading(id: string) {
  activeHeadingId.value = id
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleScroll() {
  if (scrollFrame !== undefined) return
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = undefined
    updateReadingState()
  })
}

function updateReadingState() {
  const article = document.querySelector('.knowledge-article') as HTMLElement | null
  if (!article || !activeNote.value) {
    readingProgress.value = 0
    return
  }
  const start = article.getBoundingClientRect().top + window.scrollY
  const range = Math.max(1, article.offsetHeight - window.innerHeight * .62)
  readingProgress.value = Math.max(0, Math.min(100, ((window.scrollY - start + window.innerHeight * .3) / range) * 100))
  const passedHeadings = headings.value
    .filter((heading) => {
      const target = document.getElementById(heading.id)
      return target ? target.getBoundingClientRect().top <= 160 : false
    })
  activeHeadingId.value = passedHeadings[passedHeadings.length - 1]?.id ?? headings.value[0]?.id ?? ''
}
function formatShortDate(value?: string) { return value ? new Date(value).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) : '刚刚' }
function formatLongDate(value?: string) { return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) : '刚刚发布' }
</script>

<style scoped>
.knowledge-page { min-height:100vh; color:var(--gb-text); background:var(--gb-bg); }
.knowledge-shell { width:min(1320px,calc(100% - 8vw)); margin:0 auto; padding-bottom:64px; }

/* ============ Intro ============ */
.knowledge-intro { display:grid; grid-template-columns:1fr; gap:14px; padding:clamp(52px,8vw,108px) 0 38px; }
.knowledge-intro p { margin:0; color:var(--gb-primary); font-size:11px; font-weight:800; letter-spacing:.14em; }
.knowledge-intro h1 { margin:0; color:var(--gb-text); font-size:clamp(46px,6.4vw,84px); font-weight:800; letter-spacing:-.02em; line-height:.92; }

/* ============ Layout ============ */
.knowledge-layout { display:grid; grid-template-columns:minmax(250px,.62fr) minmax(0,1.7fr) minmax(180px,.4fr); gap:22px; min-height:700px; align-items:start; }

/* ============ Library ============ */
.knowledge-library { padding:20px; border:1px solid var(--gb-border); border-radius:var(--gb-radius); background:var(--gb-surface); box-shadow:var(--gb-shadow); }
.library-label { margin:22px 0 10px; color:var(--gb-muted); font-size:11px; font-weight:800; letter-spacing:.12em; }
.notes-label { margin-top:26px; }
.public-note-scroll { height:380px; margin-right:-8px; padding-right:8px; }
.public-note-link {
  display:grid; width:100%; gap:4px; margin-bottom:6px;
  padding:13px 14px;
  border:0; border-radius:var(--gb-radius-sm);
  color:inherit; background:transparent; text-align:left; cursor:pointer;
  transition:background-color .22s ease,transform .22s ease;
}
.public-note-link:hover,.public-note-link.active { background:var(--gb-primary-soft); }
.public-note-link:hover { transform:translateX(3px); }
.public-note-link small { color:var(--gb-primary); font-size:11px; font-weight:700; }
.public-note-link strong { overflow:hidden; color:var(--gb-text); font-size:14px; font-weight:750; text-overflow:ellipsis; white-space:nowrap; }

/* ============ Article ============ */
.knowledge-article {
  position:relative; min-width:0;
  padding:44px clamp(30px,4.4vw,78px) 60px;
  border:1px solid var(--gb-border); border-radius:var(--gb-radius);
  background:var(--gb-surface); box-shadow:var(--gb-shadow);
}
.reading-progress { position:sticky; top:0; z-index:3; width:100%; height:4px; margin:0 -1px 27px -1px; border-radius:4px; background:var(--gb-primary-soft); }
.reading-progress span { display:block; height:100%; border-radius:4px; background:var(--gb-primary); transition:width .12s linear; }
.article-head { max-width:820px; padding-bottom:28px; border-bottom:1px solid var(--gb-border); }
.article-path { display:flex; gap:8px; color:var(--gb-primary); font-size:12px; font-weight:700; }
.article-path b { color:var(--gb-subtle); font-weight:500; }
.article-head h2 { margin:17px 0 0; color:var(--gb-text); font-size:clamp(28px,3.4vw,46px); font-weight:800; letter-spacing:-.01em; line-height:1.24; }
.article-head>p { margin:17px 0 0; color:var(--gb-muted); font-size:15px; line-height:1.82; }
.article-meta { display:flex; flex-wrap:wrap; gap:10px; margin-top:19px; color:var(--gb-subtle); font-size:12px; }
.article-meta span+span::before { margin-right:10px; color:var(--gb-border-strong); content:'•'; }
.article-tags { display:flex; flex-wrap:wrap; gap:8px; margin-top:15px; }
.article-tags span { padding:4px 11px; border-radius:999px; color:var(--gb-primary-dark); background:var(--gb-primary-soft); font-size:12px; font-weight:650; }
.article-body { max-width:820px; padding-top:32px; }
.knowledge-article :deep(.markdown-content) { color:#2c3a55; font-size:16px; line-height:1.95; }
.knowledge-article :deep(.markdown-content h1),
.knowledge-article :deep(.markdown-content h2),
.knowledge-article :deep(.markdown-content h3),
.knowledge-article :deep(.markdown-content h4) { scroll-margin-top:24px; color:var(--gb-text); }
.knowledge-article :deep(.markdown-content h2) { margin-top:48px; padding-bottom:10px; border-bottom:1px solid var(--gb-border); }

/* ============ Pagination ============ */
.article-pagination { display:grid; grid-template-columns:1fr 1fr; gap:14px; max-width:820px; margin-top:56px; padding-top:26px; border-top:1px solid var(--gb-border); }
.article-pagination button {
  display:grid; gap:5px; padding:16px 18px;
  border:1px solid var(--gb-border); border-radius:14px;
  color:var(--gb-primary-dark); background:var(--gb-surface);
  text-align:left; cursor:pointer;
  transition:border-color .22s ease,background-color .22s ease,transform .22s ease;
}
.article-pagination button:last-child { text-align:right; }
.article-pagination button:hover:not(:disabled) { border-color:var(--gb-primary); background:var(--gb-primary-soft); transform:translateY(-2px); }
.article-pagination button:disabled { color:var(--gb-subtle); background:var(--gb-surface-soft); cursor:default; }
.article-pagination small { font-size:11px; font-weight:700; }
.article-pagination strong { font-size:13px; font-weight:750; }

/* ============ Outline ============ */
.article-outline { position:sticky; top:24px; padding:22px 20px; border:1px solid var(--gb-border); border-radius:var(--gb-radius); background:var(--gb-surface); box-shadow:var(--gb-shadow); }
.article-outline>p { margin:0; color:var(--gb-muted); font-size:11px; font-weight:800; letter-spacing:.1em; }
.article-outline>span { display:block; margin-top:16px; color:var(--gb-subtle); font-size:12px; }
.article-outline button {
  display:block; width:100%; overflow:hidden; margin-top:11px;
  padding:6px 10px;
  border:0; border-radius:8px;
  color:var(--gb-muted); background:transparent;
  font-size:12px; line-height:1.45; text-align:left; text-overflow:ellipsis; white-space:nowrap; cursor:pointer;
  transition:color .2s ease,background-color .2s ease;
}
.article-outline button:hover,.article-outline button.active { color:var(--gb-primary-dark); background:var(--gb-primary-soft); font-weight:700; }

/* ============ Empty ============ */
.knowledge-empty { display:grid; grid-column:2 / -1; min-height:560px; align-content:center; justify-items:center; gap:14px; padding:50px; border:1px dashed var(--gb-border-strong); border-radius:var(--gb-radius); background:var(--gb-surface); }
.knowledge-empty p { margin:0; color:var(--gb-muted); font-size:16px; }
.knowledge-empty a { display:inline-flex; margin-top:6px; padding:9px 24px; border-radius:999px; color:#fff; background:var(--gb-primary); font-size:13px; font-weight:750; text-decoration:none; transition:background-color .2s ease,transform .2s ease; }
.knowledge-empty a:hover { background:var(--gb-primary-dark); transform:translateY(-1px); }

@media(max-width:1100px){
  .knowledge-layout { grid-template-columns:minmax(240px,.62fr) minmax(0,1.7fr); }
  .article-outline { display:none; }
  .knowledge-empty { grid-column:2; }
}
@media(max-width:760px){
  .knowledge-shell { width:min(100% - 32px,1320px); }
  .knowledge-intro { grid-template-columns:1fr; padding:52px 0 30px; }
  .knowledge-layout { grid-template-columns:1fr; }
  .knowledge-library { padding:18px; }
  .public-note-scroll { height:240px; }
  .knowledge-article,.knowledge-empty { grid-column:auto; padding:32px 22px 48px; }
  .reading-progress { margin-bottom:22px; }
  .article-pagination { grid-template-columns:1fr; }
  .article-pagination button:last-child { text-align:left; }
  .knowledge-intro h1 { font-size:52px; }
}
</style>
