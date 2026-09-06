<template>
  <main class="knowledge-page">
    <div class="knowledge-shell">
      <PublicSiteHeader />
      <section class="knowledge-intro"><p>LEARNING ARCHIVE</p><h1>学习笔记</h1></section>
      <section class="knowledge-layout" :class="{ 'results-layout': !requestedNote }">
        <aside class="knowledge-library liquid-glass">
          <PublicNoteLibrary v-model:keyword="keyword" v-bind="libraryProps" :show-notes="!!requestedNote" :show-search="!!requestedNote"
            @search="searchNotes" @category="selectCategory" @select="selectNote" @page="changePage" @retry="loadSearch(true)" />
        </aside>

        <section v-if="!requestedNote" class="note-results">
          <header class="results-heading">
            <h2 id="reader-results-title" tabindex="-1">{{ matchedKeyword ? '搜索结果' : selectedCategory || '全部笔记' }}</h2>
            <p v-if="matchedKeyword">“{{ matchedKeyword }}”<span v-if="selectedCategory"> · {{ selectedCategory }}</span></p>
            <button v-if="selectedCategory" type="button" @click="selectCategory('')">清除分类 <el-icon><Close /></el-icon></button>
          </header>
          <PublicNoteLibrary v-model:keyword="keyword" v-bind="libraryProps" results-view
            @search="searchNotes" @select="selectNote" @page="changePage" @retry="loadSearch(true)" />
        </section>
        <section v-else-if="articleLoading" class="knowledge-empty" aria-live="polite" aria-busy="true">
          <el-icon class="is-loading"><Loading /></el-icon><p>正在打开文章…</p>
        </section>
        <section v-else-if="loadError || !activeNote" class="knowledge-empty" aria-live="polite">
          <p>{{ loadError || '没有找到这篇文章。' }}</p>
          <button type="button" @click="loadArticle(true)">重新加载</button>
          <button type="button" @click="backToResults">返回列表</button>
        </section>
        <article v-else ref="articleElement" class="knowledge-article">
          <div class="reading-progress" aria-hidden="true"><span :style="{ width: readingProgress + '%' }" /></div>
          <header class="article-head">
            <div class="article-path"><button type="button" @click="backToResults"><el-icon><Back /></el-icon>返回列表</button><span aria-hidden="true">/</span><span>{{ activeNote.category || '未分类' }}</span></div>
            <h2 id="reader-article-title" tabindex="-1">{{ activeNote.title }}</h2>
            <p v-if="activeNote.summary">{{ activeNote.summary }}</p>
            <div class="article-meta"><span>{{ activeNote.authorName || 'linge.xin' }}</span><time>{{ formatLongDate(activeNote.updatedAt) }}</time><span>{{ activeNote.wordCount }} 字</span></div>
            <div v-if="activeTags.length" class="article-tags"><span v-for="tag in activeTags" :key="tag"># {{ tag }}</span></div>
            <button v-if="resumePosition" class="resume-reading" type="button" @click="resumeReading"><el-icon><Position /></el-icon>继续阅读 · 上次 {{ Math.round(resumePosition.progress * 100) }}%</button>
          </header>
          <div ref="bodyElement" class="article-body" tabindex="-1"><MarkdownContent :content="activeNote.content" /></div>
          <div v-if="linksError" class="reading-links-error" role="status"><span>{{ linksError }}</span><button type="button" @click="loadReadingLinks">重试</button></div>
          <nav class="article-pagination" aria-label="相邻文章" :aria-busy="linksLoading">
            <button type="button" :disabled="!navigation.previous || linksLoading" @click="navigateNeighbor('previous')"><small>上一篇</small><strong>{{ navigation.previous?.title || '已经是第一篇' }}</strong></button>
            <button type="button" :disabled="!navigation.next || linksLoading" @click="navigateNeighbor('next')"><small>下一篇</small><strong>{{ navigation.next?.title || '已经是最后一篇' }}</strong></button>
          </nav>
          <section class="related-notes" aria-labelledby="related-title">
            <header><h3 id="related-title">同类文章</h3><button type="button" @click="selectCategory(activeNote.category || '未分类')">查看分类 <el-icon><ArrowRight /></el-icon></button></header>
            <p v-if="linksLoading" role="status">正在查找相关文章…</p>
            <p v-else-if="!relatedNotes.length && !linksError">这个分类暂时没有其他公开文章。</p>
            <button v-for="note in relatedNotes" :key="note.id" class="related-note" type="button" @click="selectRelated(note)">
              <span>{{ note.title }}</span><small>{{ formatLongDate(note.updatedAt) }}</small><el-icon><ArrowRight /></el-icon>
            </button>
          </section>
          <button class="article-return" type="button" @click="backToResults"><el-icon><Back /></el-icon>返回笔记列表</button>
        </article>

        <aside v-if="activeNote && !articleLoading && requestedNote" class="article-outline liquid-glass">
          <h2>文章目录</h2><PublicNoteOutline :headings="headings" :active-id="activeHeadingId" @select="scrollToHeading" />
        </aside>
      </section>
    </div>

    <nav class="reader-tools liquid-glass liquid-glass-strong" aria-label="阅读工具">
      <button type="button" :aria-expanded="drawerOpen && drawerMode === 'library'" aria-haspopup="dialog" @click="openDrawer('library')"><el-icon><Collection /></el-icon><span>笔记</span></button>
      <button v-if="requestedNote" type="button" :disabled="!activeNote || articleLoading" :aria-expanded="drawerOpen && drawerMode === 'outline'" aria-haspopup="dialog" @click="openDrawer('outline')"><el-icon><List /></el-icon><span>目录</span></button>
    </nav>
    <el-drawer v-model="drawerOpen" :title="drawerMode === 'library' ? '查找笔记' : '文章目录'" :direction="drawerMode === 'library' ? 'ltr' : 'rtl'"
      size="min(92vw, 420px)" append-to-body @close="drawerClosing = true" @closed="finishDrawerNavigation">
      <PublicNoteLibrary v-if="drawerMode === 'library'" v-model:keyword="keyword" v-bind="libraryProps"
        @search="searchNotes" @category="selectCategory" @select="selectNote" @page="changePage" @retry="loadSearch(true)" />
      <PublicNoteOutline v-else :headings="headings" :active-id="activeHeadingId" @select="scrollToHeading" />
    </el-drawer>
    <BackToTopButton class="reader-back-top" />
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Back, Close, Collection, List, Loading, Position } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNoteCategories, searchOfficialNotes, fetchRelatedNotes, fetchNoteNavigation } from '@/api/note'
import BackToTopButton from '@/components/BackToTopButton.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import PublicNoteLibrary from '@/components/PublicNoteLibrary.vue'
import PublicNoteOutline from '@/components/PublicNoteOutline.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import type { Note, NoteCategory, PublicNoteItem, PublicNoteNavigation } from '@/types/note'
import { extractMarkdownHeadings } from '@/utils/markdown'
import { loadReadingPosition, saveReadingPosition, type ReadingPosition } from '@/utils/noteReading'

const route = useRoute()
const router = useRouter()
const pageSize = 12
const queryString = (value: unknown) => typeof value === 'string' ? value : ''
const requestedNote = computed(() => queryString(route.query.note))
const matchedKeyword = computed(() => queryString(route.query.q).trim())
const selectedCategory = computed(() => queryString(route.query.category))
const currentPage = computed(() => {
  const value = Number(route.query.page)
  return Number.isSafeInteger(value) && value > 0 && value <= 2147483647 ? value : 1
})
const notes = ref<PublicNoteItem[]>([])
const total = ref(0)
const categories = ref<NoteCategory[]>([])
const activeNote = ref<Note | null>(null)
const articleElement = ref<HTMLElement>()
const bodyElement = ref<HTMLElement>()
const keyword = ref(matchedKeyword.value)
const loading = ref(false)
const articleLoading = ref(false)
const loadError = ref('')
const listError = ref('')
const readingProgress = ref(0)
const activeHeadingId = ref('')
const drawerOpen = ref(false)
const drawerMode = ref<'library' | 'outline'>('library')
const drawerClosing = ref(false)
const navigation = ref<PublicNoteNavigation>({ previous: null, next: null, position: 0 })
const relatedNotes = ref<PublicNoteItem[]>([])
const linksError = ref('')
const linksLoading = ref(false)
const resumePosition = ref<ReadingPosition | null>(null)
let pendingAction: (() => void) | null = null
let scrollFrame: number | undefined
let saveTimer: ReturnType<typeof setTimeout> | undefined
let searchVersion = 0
let articleVersion = 0
let linksVersion = 0
let loadedSearchKey = ''
let disposed = false
let readingEngaged = false

const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const headings = computed(() => extractMarkdownHeadings(activeNote.value?.content))
const libraryProps = computed(() => ({
  category: selectedCategory.value, categories: categories.value, notes: notes.value, total: total.value,
  page: currentPage.value, pageSize, matchedKeyword: matchedKeyword.value, activeId: activeNote.value?.id,
  loading: loading.value, error: listError.value
}))
const searchParams = () => ({ keyword: matchedKeyword.value || undefined, category: selectedCategory.value || undefined, descendants: true })
const searchKey = (page: number) => JSON.stringify([matchedKeyword.value, selectedCategory.value, page])

watch(matchedKeyword, (value) => { keyword.value = value })
watch(() => [matchedKeyword.value, selectedCategory.value, currentPage.value], () => { void loadSearch() }, { immediate: true })
watch(() => [requestedNote.value, matchedKeyword.value, selectedCategory.value], () => { void loadArticle() }, { immediate: true })

onMounted(() => {
  void fetchOfficialNoteCategories().then((items) => { if (!disposed) categories.value = items }).catch(() => {})
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('wheel', engageReading, { passive: true })
  window.addEventListener('touchmove', engageReading, { passive: true })
  window.addEventListener('keydown', engageReading)
  window.addEventListener('pagehide', persistPosition)
  document.addEventListener('visibilitychange', saveWhenHidden)
})
onBeforeUnmount(() => {
  persistPosition()
  disposed = true
  searchVersion++
  articleVersion++
  linksVersion++
  pendingAction = null
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('wheel', engageReading)
  window.removeEventListener('touchmove', engageReading)
  window.removeEventListener('keydown', engageReading)
  window.removeEventListener('pagehide', persistPosition)
  document.removeEventListener('visibilitychange', saveWhenHidden)
  if (scrollFrame !== undefined) window.cancelAnimationFrame(scrollFrame)
  if (saveTimer !== undefined) clearTimeout(saveTimer)
})

// Lists and article bodies load independently; a slow sidebar must not block reading.
async function loadSearch(force = false) {
  const key = searchKey(currentPage.value)
  if (!force && loadedSearchKey === key) return
  const version = ++searchVersion
  loadedSearchKey = ''
  loading.value = true
  listError.value = ''
  notes.value = []
  try {
    const result = await searchOfficialNotes({ ...searchParams(), page: currentPage.value, pageSize })
    if (version !== searchVersion || disposed) return
    notes.value = result.items
    total.value = result.total
    loadedSearchKey = searchKey(result.page)
    if (result.page !== currentPage.value) await router.replace({ query: { ...route.query, page: String(result.page) } })
    await nextTick()
    if (version !== searchVersion || disposed) return
    if (!requestedNote.value) runAfterDrawer(() => scrollToId('reader-results-title'))
  } catch {
    if (version === searchVersion && !disposed) { total.value = 0; listError.value = '文章列表暂时不可用，请重试。' }
  } finally {
    if (version === searchVersion && !disposed) loading.value = false
  }
}

async function loadArticle(force = false) {
  const version = ++articleVersion
  const id = Number(requestedNote.value)
  const sameArticle = activeNote.value?.id === id
  if (!sameArticle) {
    persistPosition()
    readingEngaged = false
    if (saveTimer !== undefined) clearTimeout(saveTimer)
    activeNote.value = null
    resumePosition.value = null
  }
  pendingAction = null
  loadError.value = ''
  linksVersion++
  linksError.value = ''
  navigation.value = { previous: null, next: null, position: 0 }
  relatedNotes.value = []
  if (!requestedNote.value) {
    articleLoading.value = false
    linksLoading.value = false
    await nextTick()
    if (!disposed && version === articleVersion) runAfterDrawer(() => scrollToId('reader-results-title'))
    return
  }
  if (!Number.isSafeInteger(id) || id <= 0) {
    articleLoading.value = false
    loadError.value = '文章链接无效，请从笔记列表重新选择。'
    return
  }
  if (!force && sameArticle) { articleLoading.value = false; void loadReadingLinks(); return }
  articleLoading.value = true
  try {
    const note = await fetchOfficialNote(id)
    if (version !== articleVersion || disposed) return
    activeNote.value = note
    resumePosition.value = loadReadingPosition(id, note.updatedAt)
    articleLoading.value = false
    await nextTick()
    if (version !== articleVersion || disposed) return
    runAfterDrawer(() => scrollToId('reader-article-title'))
    updateReadingState()
    void loadReadingLinks()
  } catch {
    if (version === articleVersion && !disposed) {
      activeNote.value = null
      loadError.value = '文章暂时无法打开，可能已取消公开。请重试或选择其他笔记。'
    }
  } finally {
    if (version === articleVersion && !disposed) articleLoading.value = false
  }
}

async function loadReadingLinks() {
  const id = activeNote.value?.id
  if (!id) return
  const version = ++linksVersion
  linksLoading.value = true
  linksError.value = ''
  const [related, neighbors] = await Promise.allSettled([fetchRelatedNotes(id), fetchNoteNavigation(id, searchParams())])
  if (version !== linksVersion || disposed || activeNote.value?.id !== id) return
  if (related.status === 'fulfilled') relatedNotes.value = related.value
  if (neighbors.status === 'fulfilled') {
    navigation.value = neighbors.value
    if (!route.query.page && neighbors.value.position > 0) {
      const page = Math.floor((neighbors.value.position - 1) / pageSize) + 1
      if (page > 1) await router.replace({ query: { ...route.query, page: String(page) } })
    }
  }
  if (version !== linksVersion || disposed || activeNote.value?.id !== id) return
  if (related.status === 'rejected' || neighbors.status === 'rejected') linksError.value = '延伸阅读暂时不可用。'
  linksLoading.value = false
}

function searchNotes() {
  const q = keyword.value.trim()
  closeDrawer()
  if (q === matchedKeyword.value && !requestedNote.value && currentPage.value === 1) return loadSearch(true)
  return router.push({ query: { ...route.query, q: q || undefined, page: '1', note: undefined } })
}
function selectCategory(category: string) {
  closeDrawer()
  if (category === selectedCategory.value && !requestedNote.value && currentPage.value === 1) return
  return router.push({ query: { ...route.query, category: category || undefined, page: '1', note: undefined } })
}
function changePage(page: number) {
  return router.push({ query: { ...route.query, page: String(page) } })
}
function selectNote(note: PublicNoteItem, page = currentPage.value) {
  closeDrawer()
  if (Number(requestedNote.value) === note.id) {
    if (loadError.value) return loadArticle(true)
    if (!articleLoading.value) runAfterDrawer(() => scrollToId('reader-article-title'))
    return
  }
  return router.push({ query: { ...route.query, page: String(page), note: String(note.id) } })
}
function navigateNeighbor(direction: 'previous' | 'next') {
  const note = navigation.value[direction]
  if (!note) return
  const index = navigation.value.position + (direction === 'previous' ? -2 : 0)
  return selectNote(note, Math.max(1, Math.floor(index / pageSize) + 1))
}
function selectRelated(note: PublicNoteItem) {
  return router.push({ query: { category: note.category || '未分类', page: '1', note: String(note.id) } })
}
function backToResults() {
  closeDrawer()
  return router.push({ query: { ...route.query, note: undefined } })
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
  readingEngaged = true
  resumePosition.value = null
  closeDrawer()
  runAfterDrawer(() => scrollToId(id))
}
function finishDrawerNavigation() {
  drawerClosing.value = false
  const action = pendingAction
  pendingAction = null
  if (!disposed) action?.()
}
function runAfterDrawer(action: () => void) {
  if (drawerClosing.value || drawerOpen.value) pendingAction = action
  else action()
}
function headingElement(id: string) {
  return articleElement.value?.querySelector<HTMLElement>('[id="' + CSS.escape(id) + '"]')
}
function scrollToId(id: string) {
  const target = id === 'reader-results-title' ? document.getElementById(id) : headingElement(id)
  if (!target) return
  target.tabIndex = -1
  target.focus({ preventScroll: true })
  const scrollTarget = id === 'reader-article-title' ? target.closest('.article-head') ?? target : target
  scrollTarget.scrollIntoView({ behavior: scrollBehavior(), block: 'start' })
  activeHeadingId.value = id
}
function scrollBehavior(): ScrollBehavior {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
}
function readerOffset() {
  return Number.parseFloat(getComputedStyle(document.querySelector('.knowledge-page')!).getPropertyValue('--reader-offset')) || 160
}
function readingGeometry() {
  const body = bodyElement.value
  if (!body) return null
  const offset = readerOffset()
  const available = Math.max(1, window.innerHeight - offset - 80)
  return { start: body.getBoundingClientRect().top + window.scrollY - offset, range: Math.max(1, body.offsetHeight - available) }
}
function resumeReading() {
  const saved = resumePosition.value
  if (!saved) return
  resumePosition.value = null
  readingEngaged = true
  runAfterDrawer(() => {
    const geometry = readingGeometry()
    if (!geometry) return
    bodyElement.value?.focus({ preventScroll: true })
    window.scrollTo({ top: geometry.start + saved.progress * geometry.range, behavior: scrollBehavior() })
  })
}
function engageReading(event: Event) {
  if (drawerOpen.value || drawerClosing.value || !activeNote.value || articleLoading.value) return
  if (event.target instanceof Element && event.target.closest('input, textarea, [contenteditable="true"]')) return
  if (event instanceof KeyboardEvent && !['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp', ' ', 'End', 'Home'].includes(event.key)) return
  readingEngaged = true
}
function persistPosition() {
  if (!readingEngaged || !activeNote.value || articleLoading.value) return
  const geometry = readingGeometry()
  if (!geometry) return
  saveReadingPosition(activeNote.value.id, activeNote.value.updatedAt, Math.max(0, Math.min(1, (window.scrollY - geometry.start) / geometry.range)))
}
function saveWhenHidden() {
  if (document.visibilityState === 'hidden') persistPosition()
}
function handleScroll() {
  if (scrollFrame !== undefined) return
  scrollFrame = window.requestAnimationFrame(() => {
    scrollFrame = undefined
    updateReadingState()
    if (readingEngaged) {
      if (saveTimer !== undefined) clearTimeout(saveTimer)
      saveTimer = setTimeout(persistPosition, 400)
    }
  })
}
function updateReadingState() {
  const geometry = readingGeometry()
  if (!geometry || !activeNote.value) { readingProgress.value = 0; return }
  readingProgress.value = Math.max(0, Math.min(100, (window.scrollY - geometry.start) / geometry.range * 100))
  const passed = headings.value.filter((heading) => {
    const target = headingElement(heading.id)
    return target ? target.getBoundingClientRect().top <= readerOffset() + 20 : false
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

.note-results { min-width:0; padding:var(--space-5) var(--space-4); color:var(--text); background:var(--surface); }
.results-heading { display:flex; flex-direction:column; align-items:flex-start; gap:var(--space-3); padding-bottom:var(--space-5); }
.results-heading h2 { margin:0; color:var(--text); font-size:28px; line-height:1.35; scroll-margin-top:var(--reader-offset); overflow-wrap:anywhere; }
.results-heading p { margin:0; color:var(--muted); overflow-wrap:anywhere; }
.results-heading button, .article-path button, .article-return, .related-notes header button, .reading-links-error button { display:inline-flex; align-items:center; gap:var(--space-2); min-height:44px; padding:0; border:0; color:var(--brand-strong); background:transparent; font-size:14px; cursor:pointer; }
.article-path { align-items:center; }
.article-head { scroll-margin-top:var(--reader-offset); }
#reader-results-title:focus, #reader-article-title:focus, .article-body:focus { outline:none; }
.resume-reading { display:inline-flex; align-items:center; align-self:flex-start; gap:var(--space-2); min-height:44px; padding:var(--space-2) var(--space-3); border:1px solid var(--line); border-radius:var(--radius-sm); color:var(--brand-strong); background:var(--brand-soft); font-size:14px; cursor:pointer; }
.reading-links-error { display:flex; flex-wrap:wrap; align-items:center; gap:var(--space-3); padding-top:var(--space-5); color:var(--muted); font-size:14px; }
.related-notes { display:flex; flex-direction:column; gap:var(--space-2); padding-block:var(--space-6); }
.related-notes header { display:flex; flex-wrap:wrap; align-items:center; justify-content:space-between; gap:var(--space-3); }
.related-notes h3 { margin:0; font-size:22px; color:var(--text); }
.related-notes p { margin:0; color:var(--muted); font-size:14px; }
.related-note { display:flex; align-items:center; flex-wrap:wrap; gap:var(--space-2) var(--space-3); width:100%; padding:var(--space-4) 0; border:0; border-bottom:1px solid var(--line); color:var(--text); background:transparent; text-align:left; cursor:pointer; }
.related-note span { flex:1 1 100%; font-size:16px; overflow-wrap:anywhere; }
.related-note small { flex:1; color:var(--muted); font-size:14px; }
.related-note:hover span { color:var(--brand-strong); text-decoration:underline; text-underline-offset:4px; }
@media(min-width:760px) { .note-results { padding:var(--space-6); } }
@media(min-width:1101px) { .results-layout { grid-template-columns:250px minmax(0,1fr); } .note-results { padding:var(--space-6) var(--space-7); } }

</style>
