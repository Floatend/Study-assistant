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
          <button class="category-link" :class="{ active: !selectedCategory }" type="button" @click="selectCategory('')"><span>全部文章</span><small>{{ totalCount }}</small></button>
          <el-tree
            v-if="categoryTree.length"
            class="category-tree"
            :data="categoryTree"
            node-key="key"
            :props="{ label: 'label', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            highlight-current
            @node-click="selectCategoryNode"
          >
            <template #default="{ data }">
              <div class="category-tree-node" :class="{ active: selectedCategory === data.value, group: data.children.length }">
                <el-icon class="category-tree-icon"><FolderOpened v-if="data.children.length" /><Document v-else /></el-icon>
                <span class="category-tree-label">{{ data.label }}</span>
                <small class="category-tree-count">{{ data.count }}</small>
              </div>
            </template>
          </el-tree>
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
import { Document, FolderOpened, Search } from '@element-plus/icons-vue'
import { fetchOfficialNote, fetchOfficialNoteCategories, fetchOfficialNotes } from '@/api/note'
import BackToTopButton from '@/components/BackToTopButton.vue'
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
const readingProgress = ref(0)
const activeHeadingId = ref('')
const selectedCategoryDescendants = ref<string[] | null>(null)
let scrollFrame: number | undefined

interface CategoryTreeNode {
  key: string
  label: string
  value: string
  count: number
  leafValues: string[]
  children: CategoryTreeNode[]
}

const totalCount = computed(() => categories.value.reduce((total, category) => total + category.count, 0))
const categoryTree = computed(() => buildCategoryTree(categories.value))
const activeTags = computed(() => activeNote.value?.tags?.split(',').map((tag) => tag.trim()).filter(Boolean) ?? [])
const headings = computed(() => extractMarkdownHeadings(activeNote.value?.content))
const activeIndex = computed(() => notes.value.findIndex((note) => note.id === activeNote.value?.id))
const previousNote = computed(() => activeIndex.value > 0 ? notes.value[activeIndex.value - 1] : null)
const nextNote = computed(() => activeIndex.value >= 0 && activeIndex.value < notes.value.length - 1 ? notes.value[activeIndex.value + 1] : null)

onMounted(async () => {
  const requestedCategory = typeof route.query.category === 'string' ? route.query.category : ''
  categories.value = await fetchOfficialNoteCategories()
  selectedCategory.value = requestedCategory
  const initialCategoryNode = findCategoryNode(categoryTree.value, requestedCategory)
  selectedCategoryDescendants.value = initialCategoryNode?.children.length ? initialCategoryNode.leafValues : null
  await loadNotes(false)
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
  const node = findCategoryNode(categoryTree.value, category)
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
    const requestedId = Number(route.query.note)
    const target = notes.value.find((note) => note.id === requestedId) ?? notes.value[0]
    if (target) await selectNote(target, syncRoute)
    else activeNote.value = null
  } finally { loading.value = false }
}

async function selectCategory(category: string) {
  selectedCategory.value = category
  selectedCategoryDescendants.value = null
  await router.replace({ query: category ? { category } : {} })
  await loadNotes()
}

async function selectCategoryNode(node: CategoryTreeNode) {
  selectedCategory.value = node.value
  selectedCategoryDescendants.value = node.children.length ? [...node.leafValues] : null
  await router.replace({ query: { category: node.value } })
  await loadNotes()
}

function splitCategoryPath(category: string) {
  return category.split(/\s*(?:\/|>|::|\\)\s*/).map((part) => part.trim()).filter(Boolean)
}

function buildCategoryTree(items: NoteCategory[]): CategoryTreeNode[] {
  const roots: CategoryTreeNode[] = []
  for (const item of items) {
    const parts = splitCategoryPath(item.name)
    if (!parts.length) continue
    let current = roots
    const path: string[] = []
    parts.forEach((part, index) => {
      path.push(part)
      let node = current.find((candidate) => candidate.label === part)
      if (!node) {
        node = { key: path.join('/'), label: part, value: index === parts.length - 1 ? item.name : path.join('/'), count: 0, leafValues: [], children: [] }
        current.push(node)
      }
      node.count += item.count
      if (!node.leafValues.includes(item.name)) node.leafValues.push(item.name)
      node.value = index === parts.length - 1 ? item.name : path.join('/')
      current = node.children
    })
  }
  return roots
}

function findCategoryNode(nodes: CategoryTreeNode[], value: string): CategoryTreeNode | null {
  if (!value) return null
  for (const node of nodes) {
    if (node.value === value) return node
    const match = findCategoryNode(node.children, value)
    if (match) return match
  }
  return null
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
.knowledge-page{min-height:100vh;color:#0b3340;background:#fff7e7}.knowledge-shell{width:min(1320px,calc(100% - 8vw));margin:0 auto}.knowledge-intro{display:grid;grid-template-columns:1fr minmax(260px,.52fr);gap:28px;align-items:end;padding:clamp(56px,9vw,120px) 0 40px;border-bottom:1px solid #f0cda5}.knowledge-intro p,.library-label,.article-outline>p{margin:0;color:#f15843;font-size:11px;font-weight:800;letter-spacing:.12em}.knowledge-intro h1{margin:11px 0 0;color:#075866;font-size:clamp(48px,7vw,90px);font-weight:800;line-height:.9}.knowledge-intro>span{color:#4d717a;font-size:15px;line-height:1.85}.knowledge-layout{display:grid;grid-template-columns:minmax(245px,.55fr) minmax(0,1.55fr) minmax(175px,.35fr);min-height:700px}.knowledge-library{padding:26px 22px 40px 0;border-right:1px solid #f0cda5}.library-label{margin:26px 0 8px;color:#4d717a;letter-spacing:.09em}.notes-label{margin-top:30px}.category-link{display:flex;width:100%;align-items:center;justify-content:space-between;padding:9px 10px;border:0;border-radius:4px;color:#245863;background:transparent;font-size:13px;font-weight:600;text-align:left;cursor:pointer;transition:background-color .22s ease,color .22s ease}.category-link:hover,.category-link.active{color:#064a63;background:#91e8dd}.category-link small{color:#46747b;font-size:11px}.public-note-scroll{height:360px;margin-right:-10px;padding-right:10px}.public-note-link{display:grid;width:100%;gap:4px;padding:13px 10px;border:0;border-bottom:1px solid #f2d9bb;color:inherit;background:transparent;text-align:left;cursor:pointer;transition:background-color .22s ease,transform .22s ease}.public-note-link:hover,.public-note-link.active{background:#ffe980}.public-note-link:hover{transform:translateX(4px)}.public-note-link small{color:#f15843;font-size:11px;font-weight:700}.public-note-link strong{overflow:hidden;color:#0b4051;font-size:14px;font-weight:800;text-overflow:ellipsis;white-space:nowrap}.knowledge-article{position:relative;min-width:0;padding:42px clamp(28px,4.2vw,76px) 66px}.reading-progress{position:sticky;top:0;z-index:3;width:100%;height:4px;margin-bottom:27px;background:#f6d6b0}.reading-progress span{display:block;height:100%;background:#ff604d;transition:width .12s linear}.article-head{max-width:820px;padding-bottom:28px;border-bottom:1px solid #f0d4b2}.article-path{display:flex;gap:8px;color:#f15843;font-size:12px;font-weight:700}.article-path b{font-weight:500}.article-head h2{margin:17px 0 0;color:#0a4053;font-size:clamp(29px,3.6vw,50px);font-weight:800;line-height:1.22}.article-head>p{margin:17px 0 0;color:#4e7179;font-size:16px;line-height:1.82}.article-meta{display:flex;flex-wrap:wrap;gap:10px;margin-top:19px;color:#6f9095;font-size:12px}.article-meta span+span::before{margin-right:10px;color:#f0b988;content:'•'}.article-tags{display:flex;flex-wrap:wrap;gap:8px;margin-top:15px}.article-tags span{color:#087a78;font-size:12px;font-weight:700}.article-body{max-width:820px;padding-top:34px}.knowledge-article :deep(.markdown-content){color:#164b57;font-size:16px;line-height:1.95}.knowledge-article :deep(.markdown-content h1),.knowledge-article :deep(.markdown-content h2),.knowledge-article :deep(.markdown-content h3),.knowledge-article :deep(.markdown-content h4){scroll-margin-top:20px;color:#084759}.knowledge-article :deep(.markdown-content h2){margin-top:50px;padding-bottom:10px;border-bottom:1px solid #f1d5b7}.article-pagination{display:grid;grid-template-columns:1fr 1fr;gap:18px;max-width:820px;margin-top:62px;padding-top:23px;border-top:1px solid #f0d4b2}.article-pagination button{display:grid;gap:5px;padding:0;border:0;color:#007879;background:transparent;text-align:left;cursor:pointer}.article-pagination button:last-child{text-align:right}.article-pagination button:disabled{color:#8c9f9d;cursor:default}.article-pagination small{font-size:11px;font-weight:700}.article-pagination strong{font-size:13px}.article-outline{padding:46px 0 40px 16px;border-left:1px solid #f0cda5}.article-outline>p{color:#437079;letter-spacing:.09em}.article-outline>span{display:block;margin-top:16px;color:#8aa0a0;font-size:12px}.article-outline button{display:block;width:100%;overflow:hidden;margin-top:11px;border:0;color:#4d7075;background:transparent;font-size:12px;line-height:1.45;text-align:left;text-overflow:ellipsis;white-space:nowrap;cursor:pointer;transition:color .2s ease}.article-outline button:hover,.article-outline button.active{color:#f15843;font-weight:800}.knowledge-empty{display:grid;grid-column:2 / 4;align-content:center;justify-items:start;gap:11px;padding:50px}.knowledge-empty p{margin:0;color:#285b66;font-size:17px}.knowledge-empty a{color:#007b78;font-size:14px;font-weight:800;text-decoration:none}.knowledge-empty a:hover{text-decoration:underline}@media(max-width:1000px){.knowledge-layout{grid-template-columns:minmax(230px,.55fr) minmax(0,1.45fr)}.article-outline{display:none}.knowledge-empty{grid-column:2}}@media(max-width:760px){.knowledge-shell{width:min(100% - 32px,1320px)}.knowledge-intro{grid-template-columns:1fr;padding:52px 0 32px}.knowledge-layout{grid-template-columns:1fr}.knowledge-library{padding:22px 0;border-right:0;border-bottom:1px solid #f0cda5}.public-note-scroll{height:240px}.knowledge-article,.knowledge-empty{grid-column:auto;padding:32px 0 48px}.reading-progress{margin-bottom:22px}.article-pagination{grid-template-columns:1fr}.article-pagination button:last-child{text-align:left}.knowledge-intro h1{font-size:52px}}
/* Cool blue editorial theme, inspired by modern AI products without copying a brand. */
.knowledge-page{color:#1f2a44;background:#f7f9ff}.knowledge-intro,.knowledge-library,.article-outline{border-color:#e3e8f5}.knowledge-intro{grid-template-columns:1fr}.knowledge-intro p,.article-path{color:#4d6bfe}.knowledge-intro h1,.article-head h2{color:#1f2a44}.knowledge-intro>span{color:#65708c}.library-label,.article-outline>p{color:#65708c}.category-link{color:#41516f}.category-link small{color:#7582a0}.category-link:hover,.category-link.active{color:#3559e8;background:#e7edff}.public-note-link{border-bottom-color:#e8ecf6}.public-note-link:hover,.public-note-link.active{background:#f0f4ff}.public-note-link small{color:#4d6bfe}.public-note-link strong{color:#263755}.reading-progress{background:#e8edfb}.reading-progress span{background:#4d6bfe}.article-head,.article-pagination{border-color:#e3e8f5}.article-head>p{color:#65708c}.article-meta{color:#7d89a2}.article-meta span+span::before{color:#b1bcce}.article-tags span{color:#1b9f9c}.knowledge-article :deep(.markdown-content){color:#30415f}.knowledge-article :deep(.markdown-content h1),.knowledge-article :deep(.markdown-content h2),.knowledge-article :deep(.markdown-content h3),.knowledge-article :deep(.markdown-content h4){color:#1f2a44}.knowledge-article :deep(.markdown-content h2){border-bottom-color:#e3e8f5}.article-pagination button,.knowledge-empty a{color:#3559e8}.article-pagination button:disabled{color:#a0aabc}.article-outline>span{color:#9ba5b8}.article-outline button{color:#65708c}.article-outline button:hover,.article-outline button.active{color:#3559e8}.knowledge-empty p{color:#50617e}@media(max-width:760px){.knowledge-library{border-bottom-color:#e3e8f5}}
.category-tree{margin:2px -8px 0 0;padding-right:8px;background:transparent}.category-tree :deep(.el-tree-node__content){height:38px;margin:2px 0;border-radius:9px;transition:background-color .22s ease,transform .22s ease}.category-tree :deep(.el-tree-node__content:hover){background:#edf2ff;transform:translateX(3px)}.category-tree :deep(.el-tree-node.is-current > .el-tree-node__content){background:#e7edff}.category-tree :deep(.el-tree-node__expand-icon){color:#8995b0;transition:color .22s ease,transform .22s ease}.category-tree :deep(.el-tree-node__expand-icon.expanded){color:#4d6bfe}.category-tree :deep(.el-tree-node__children){position:relative}.category-tree :deep(.el-tree-node__children)::before{position:absolute;top:0;bottom:0;left:13px;border-left:1px dashed #d9e0f1;content:''}.category-tree-node{display:flex;min-width:0;width:100%;align-items:center;gap:7px;color:#52617c;font-size:13px;transition:color .22s ease}.category-tree-node.active{color:#3559e8;font-weight:750}.category-tree-node.group .category-tree-icon{color:#4d6bfe}.category-tree-icon{flex:0 0 auto;color:#8390aa;font-size:15px}.category-tree-label{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.category-tree-count{margin-left:auto;padding-left:8px;color:#9ba7bc;font-size:11px;font-weight:600}.category-tree-node.active .category-tree-count{color:#6c82e9}@media(max-width:760px){.category-tree{margin-right:0;padding-right:0}.category-tree :deep(.el-tree-node__content){height:40px}.category-tree :deep(.el-tree-node__children)::before{left:13px}}
</style>
