<template>
  <main class="knowledge-page">
    <div class="knowledge-shell">
      <PublicSiteHeader />
      <section class="knowledge-intro"><p>LEARNING ARCHIVE</p><h1>学习笔记</h1></section>
      <section class="knowledge-layout">
        <aside class="knowledge-library liquid-glass">
          <PublicNoteLibrary v-model:keyword="keyword" :category="selectedCategory" :categories="categories" :notes="notes"
            :active-id="activeNote?.id" :loading="loading" :error="listError" @search="searchNotes" @category="selectCategory"
            @select="selectNote" @retry="syncRoute(true)" />
        </aside>

        <section v-if="articleLoading" class="knowledge-empty" aria-live="polite" aria-busy="true">
          <el-icon class="is-loading"><Loading /></el-icon><p>正在打开文章…</p>
        </section>
        <section v-else-if="loadError || !activeNote" class="knowledge-empty" aria-live="polite">
          <p>{{ loadError || (loading ? '正在查找笔记…' : '没有找到对应笔记。') }}</p>
          <button v-if="loadError || listError" type="button" @click="syncRoute(true)">重新加载</button>
          <button v-else-if="!loading" type="button" @click="openDrawer('library')">选择其他笔记</button>
        </section>
        <article v-else ref="articleElement" class="knowledge-article">
          <div class="reading-progress" aria-hidden="true"><span :style="{ width: readingProgress + '%' }" /></div>
          <header class="article-head">
            <div class="article-path"><span>学习笔记</span><span aria-hidden="true">/</span><span>{{ activeNote.category || '未分类' }}</span></div>
            <h2 id="reader-article-title" tabindex="-1">{{ activeNote.title }}</h2>
            <p v-if="activeNote.summary">{{ activeNote.summary }}</p>
            <div class="article-meta"><span>{{ activeNote.authorName || 'linge.xin' }}</span><time>{{ formatLongDate(activeNote.updatedAt) }}</time><span>{{ activeNote.wordCount }} 字</span></div>
            <div v-if="activeTags.length" class="article-tags"><span v-for="tag in activeTags" :key="tag"># {{ tag }}</span></div>
          </header>
          <div class="article-body"><MarkdownContent :content="activeNote.content" /></div>
          <nav class="article-pagination" aria-label="相邻文章">
            <button type="button" :disabled="!previousNote" @click="previousNote && selectNote(previousNote)"><small>上一篇</small><strong>{{ previousNote?.title || '已经是第一篇' }}</strong></button>
            <button type="button" :disabled="!nextNote" @click="nextNote && selectNote(nextNote)"><small>下一篇</small><strong>{{ nextNote?.title || '已经是最后一篇' }}</strong></button>
          </nav>
        </article>

        <aside v-if="activeNote && !articleLoading" class="article-outline liquid-glass">
          <h2>文章目录</h2><PublicNoteOutline :headings="headings" :active-id="activeHeadingId" @select="scrollToHeading" />
        </aside>
      </section>
    </div>

    <nav class="reader-tools liquid-glass liquid-glass-strong" aria-label="阅读工具">
      <button type="button" :aria-expanded="drawerOpen && drawerMode === 'library'" aria-haspopup="dialog" @click="openDrawer('library')"><el-icon><Collection /></el-icon><span>笔记</span></button>
      <button type="button" :disabled="!activeNote || articleLoading" :aria-expanded="drawerOpen && drawerMode === 'outline'" aria-haspopup="dialog" @click="openDrawer('outline')"><el-icon><List /></el-icon><span>目录</span></button>
    </nav>
    <el-drawer v-model="drawerOpen" :title="drawerMode === 'library' ? '查找笔记' : '文章目录'" :direction="drawerMode === 'library' ? 'ltr' : 'rtl'"
      size="min(92vw, 420px)" append-to-body @close="drawerClosing = true" @closed="finishDrawerNavigation">
      <PublicNoteLibrary v-if="drawerMode === 'library'" v-model:keyword="keyword" :category="selectedCategory" :categories="categories" :notes="notes"
        :active-id="activeNote?.id" :loading="loading" :error="listError" @search="searchNotes" @category="selectCategory"
        @select="selectNote" @retry="syncRoute(true)" />
      <PublicNoteOutline v-else :headings="headings" :active-id="activeHeadingId" @select="scrollToHeading" />
    </el-drawer>
    <BackToTopButton class="reader-back-top" />
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Collection, List, Loading } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNoteCategories, fetchOfficialNotes } from '@/api/note'
import BackToTopButton from '@/components/BackToTopButton.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import PublicNoteLibrary from '@/components/PublicNoteLibrary.vue'
import PublicNoteOutline from '@/components/PublicNoteOutline.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import type { Note, NoteCategory } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'
import { buildNoteCategoryTree, findNoteCategoryNode, summarizeNoteCategories } from '@/utils/noteCategories'

const route = useRoute()
const router = useRouter()
const notes = ref<Note[]>([])
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const articleElement = ref<HTMLElement>()
const keyword = ref('')
const selectedCategory = ref('')
const loading = ref(false)
const articleLoading = ref(true)
const loadError = ref('')
const listError = ref('')
const readingProgress = ref(0)
const activeHeadingId = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'library' | 'outline'>('library')
const drawerClosing = ref(false)
let pendingScroll = ''
let scrollFrame: number | undefined
let requestVersion = 0
let listKey: string | undefined
let initialized = false
let disposed = false
let hasOpenedArticle = false

const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const headings = computed(() => extractMarkdownHeadings(activeNote.value?.content))
const activeIndex = computed(() => notes.value.findIndex((note) => note.id === activeNote.value?.id))
const previousNote = computed(() => activeIndex.value > 0 ? notes.value[activeIndex.value - 1] : null)
const nextNote = computed(() => activeIndex.value >= 0 && activeIndex.value < notes.value.length - 1 ? notes.value[activeIndex.value + 1] : null)
const queryString = (value: unknown) => typeof value === 'string' ? value : ''

onMounted(async () => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  try { categories.value = await fetchOfficialNoteCategories() } catch { categories.value = [] }
  if (disposed) return
  initialized = true
  await syncRoute()
})

watch(() => [route.query.note, route.query.category, route.query.q], () => {
  if (initialized) void syncRoute()
})

onBeforeUnmount(() => {
  disposed = true
  requestVersion++
  window.removeEventListener('scroll', handleScroll)
  if (scrollFrame !== undefined) window.cancelAnimationFrame(scrollFrame)
})

// The URL is the navigation source. Ignore stale responses after rapid selections;
// direct article links must not depend on the first page of search results.
async function syncRoute(force = false) {
  const version = ++requestVersion
  const category = queryString(route.query.category)
  const search = queryString(route.query.q)
  const requestedNote = queryString(route.query.note)
  const requestedId = Number(requestedNote)
  const key = JSON.stringify([category, search])
  selectedCategory.value = category
  keyword.value = search
  loadError.value = ''
  articleLoading.value = force || !activeNote.value || activeNote.value.id !== requestedId
  if (articleLoading.value) pendingScroll = ''

  if (force || listKey !== key) {
    loading.value = true
    listError.value = ''
    listKey = undefined
    notes.value = []
    const node = findNoteCategoryNode(buildNoteCategoryTree(categories.value), category)
    const descendants = node?.children.length ? node.leafValues : null
    try {
      const items = await fetchOfficialNotes({ keyword: search || undefined, category: descendants ? undefined : category || undefined, limit: 100 })
      if (version !== requestVersion) return
      notes.value = descendants ? items.filter((note) => descendants.includes(note.category || '未分类')) : items
      if (!categories.value.length) categories.value = summarizeNoteCategories(items)
      listKey = key
    } catch {
      if (version !== requestVersion) return
      listKey = undefined
      listError.value = '文章列表暂时不可用，请重试。'
    } finally {
      if (version === requestVersion) loading.value = false
    }
  } else loading.value = false

  if (requestedNote && (!Number.isSafeInteger(requestedId) || requestedId <= 0)) {
    activeNote.value = null
    articleLoading.value = false
    loadError.value = '文章链接无效，请从笔记列表重新选择。'
    return
  }
  const targetId = requestedNote ? requestedId : notes.value[0]?.id
  if (!targetId) {
    activeNote.value = null
    articleLoading.value = false
    loadError.value = listError.value
    return
  }
  if (!requestedNote) {
    await router.replace({ query: { ...route.query, note: String(targetId) } })
    return
  }
  if (!force && activeNote.value?.id === targetId) { articleLoading.value = false; return }
  activeNote.value = null
  articleLoading.value = true
  try {
    const note = await fetchOfficialNote(targetId)
    if (version !== requestVersion) return
    activeNote.value = note
    articleLoading.value = false
    await nextTick()
    if (version !== requestVersion) return
    if (hasOpenedArticle) requestScroll('reader-article-title')
    hasOpenedArticle = true
    updateReadingState()
  } catch {
    if (version !== requestVersion) return
    loadError.value = '文章暂时无法打开，可能已取消公开。请重试或选择其他笔记。'
  } finally {
    if (version === requestVersion) articleLoading.value = false
  }
}

function searchNotes() {
  const q = keyword.value.trim()
  if (q === queryString(route.query.q)) return syncRoute(true)
  return router.push({ query: { ...route.query, q: q || undefined, note: undefined } })
}
function selectCategory(category: string) {
  if (category === selectedCategory.value) return
  return router.push({ query: { ...route.query, category: category || undefined, note: undefined } })
}
function selectNote(note: Note) {
  closeDrawer()
  if (Number(route.query.note) === note.id) {
    if (loadError.value) return syncRoute(true)
    if (!articleLoading.value && activeNote.value?.id === note.id) requestScroll('reader-article-title')
    return
  }
  return router.push({ query: { ...route.query, note: String(note.id) } })
}
function openDrawer(mode: 'library' | 'outline') {
  drawerMode.value = mode
  drawerOpen.value = true
}
function closeDrawer() {
  if (!drawerOpen.value) return
  drawerClosing.value = true
  drawerOpen.value = false
}
function scrollToHeading(id: string) {
  closeDrawer()
  requestScroll(id)
}
function finishDrawerNavigation() {
  drawerClosing.value = false
  if (pendingScroll) requestScroll(pendingScroll)
}
function headingElement(id: string) {
  return articleElement.value?.querySelector<HTMLElement>('[id="' + CSS.escape(id) + '"]')
}
function requestScroll(id: string) {
  pendingScroll = id
  if (drawerClosing.value || drawerOpen.value) return
  const target = headingElement(id)
  if (target) {
    target.tabIndex = -1
    target.focus({ preventScroll: true })
    target.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' })
    activeHeadingId.value = id
  }
  pendingScroll = ''
}
function handleScroll() {
  if (scrollFrame !== undefined) return
  scrollFrame = window.requestAnimationFrame(() => { scrollFrame = undefined; updateReadingState() })
}
function updateReadingState() {
  const article = articleElement.value
  if (!article || !activeNote.value) { readingProgress.value = 0; return }
  const start = article.getBoundingClientRect().top + window.scrollY
  const range = Math.max(1, article.offsetHeight - window.innerHeight * .62)
  readingProgress.value = Math.max(0, Math.min(100, ((window.scrollY - start + window.innerHeight * .3) / range) * 100))
  const passed = headings.value.filter((heading) => {
    const target = headingElement(heading.id)
    return target ? target.getBoundingClientRect().top <= 180 : false
  })
  activeHeadingId.value = passed[passed.length - 1]?.id ?? headings.value[0]?.id ?? ''
}
function formatLongDate(value: string) { return new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }
</script>

<style scoped>
.knowledge-page { --reader-offset:160px; min-height:100vh; color:var(--text); background:transparent; }
.knowledge-shell { width:min(1320px, calc(100% - 32px)); margin-inline:auto; padding:var(--space-3) 0 calc(var(--space-8) + env(safe-area-inset-bottom)); }
.knowledge-intro { display:flex; flex-direction:column; gap:var(--space-2); padding-block:var(--space-6); }
.knowledge-intro p { margin:0; color:var(--brand); font-size:14px; font-weight:700; }
.knowledge-intro h1 { margin:0; font-size:32px; line-height:1.2; }
.knowledge-layout { display:flex; flex-direction:column; align-items:stretch; gap:var(--space-5); }
.knowledge-library, .article-outline { display:none; }
.knowledge-article { position:relative; min-width:0; padding:var(--space-5) var(--space-4) var(--space-7); color:var(--text); background:var(--surface); border-block:1px solid var(--line); }
.reading-progress { position:sticky; top:var(--reader-offset); z-index:3; height:3px; background:var(--brand-soft); }
.reading-progress span { display:block; height:100%; background:var(--brand); transition:width .12s linear; }
.article-head { display:flex; flex-direction:column; gap:var(--space-4); padding-block:var(--space-5); border-bottom:1px solid var(--line); }
.article-path, .article-meta, .article-tags { display:flex; flex-wrap:wrap; gap:var(--space-2) var(--space-3); color:var(--muted); font-size:14px; }
.article-path { color:var(--brand-strong); overflow-wrap:anywhere; }
.article-head h2 { margin:0; color:var(--text); font-size:28px; line-height:1.35; overflow-wrap:anywhere; scroll-margin-top:var(--reader-offset); }
.article-head>p { margin:0; color:var(--muted); font-size:15px; line-height:1.6; overflow-wrap:anywhere; }
.article-tags { color:var(--brand-strong); }
.article-body { padding-top:var(--space-5); }
.knowledge-article :deep(.markdown-content) { min-width:0; color:var(--text); font-size:16px; line-height:1.6; overflow-wrap:anywhere; }
.knowledge-article :deep(.markdown-content :is(h1,h2,h3,h4,h5,h6)) { scroll-margin-top:var(--reader-offset); color:var(--text); }
.article-pagination { display:flex; flex-direction:column; gap:var(--space-3); padding-top:var(--space-5); margin-top:var(--space-7); border-top:1px solid var(--line); }
.article-pagination button { display:flex; min-width:0; flex:1; flex-direction:column; gap:var(--space-2); padding:var(--space-4); border:1px solid var(--line); border-radius:var(--radius-sm); color:var(--brand-strong); background:var(--surface); text-align:left; cursor:pointer; transition:background-color .2s ease; }
.article-pagination button:hover:not(:disabled) { background:var(--brand-soft); }
.article-pagination button:disabled { color:var(--muted); cursor:default; }
.article-pagination small, .article-pagination strong { font-size:14px; line-height:1.5; overflow-wrap:anywhere; }
.knowledge-empty { display:flex; min-height:360px; min-width:0; flex:1; flex-direction:column; align-items:center; justify-content:center; gap:var(--space-4); padding:var(--space-6); color:var(--muted); background:var(--surface); }
.knowledge-empty p { margin:0; font-size:15px; }
.knowledge-empty button { padding:var(--space-3) var(--space-4); border:0; border-radius:var(--radius-sm); color:var(--on-brand); background:var(--brand); cursor:pointer; }
.reader-tools { position:fixed; z-index:25; left:16px; bottom:calc(16px + env(safe-area-inset-bottom)); display:flex; max-width:calc(100% - 88px); gap:var(--space-1); padding:var(--space-1); border-radius:999px; }
.reader-tools button { display:flex; min-height:44px; align-items:center; gap:var(--space-2); padding-inline:var(--space-3); border:0; border-radius:999px; color:var(--brand-strong); background:transparent; font-size:14px; cursor:pointer; }
.reader-tools button:hover:not(:disabled) { background:var(--brand-soft); }
.reader-tools button:disabled { color:var(--muted); opacity:.6; cursor:default; }
.reader-back-top { right:16px; bottom:calc(20px + env(safe-area-inset-bottom)); width:44px; min-height:44px; justify-content:center; padding:0; }
.reader-back-top :deep(span:not(.el-icon)) { display:none; }
@media(min-width:760px) {
  .knowledge-page { --reader-offset:104px; }
  .knowledge-intro { padding-block:var(--space-7) var(--space-6); }
  .knowledge-intro h1 { font-size:42px; }
  .knowledge-article { padding:var(--space-6) var(--space-7) var(--space-7); }
  .article-head h2 { font-size:36px; }
  .article-pagination { flex-direction:row; }
  .article-pagination button:last-child { text-align:right; align-items:flex-end; }
}
@media(min-width:1101px) {
  .knowledge-shell { width:min(1440px, calc(100% - 64px)); }
  .knowledge-layout { display:grid; grid-template-columns:250px minmax(0,1fr) 190px; align-items:start; gap:var(--space-4); }
  .knowledge-library, .article-outline { display:flex; min-width:0; flex-direction:column; gap:var(--space-4); position:sticky; top:104px; max-height:calc(100svh - 128px); overflow-y:auto; padding:var(--space-4); border-radius:var(--radius-sm); }
  .article-outline h2 { margin:0; color:var(--muted); font:700 14px/1.5 var(--font-body); }
  .knowledge-article { padding-inline:var(--space-6); }
  .reader-tools { display:none; }
  .reader-back-top { right:24px; bottom:24px; }
}
</style>
