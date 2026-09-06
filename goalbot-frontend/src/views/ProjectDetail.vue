<template>
  <main class="portfolio-page">
    <div class="portfolio-shell">
      <PublicSiteHeader />
      <template v-if="project">
        <header class="portfolio-heading detail-heading">
          <RouterLink class="portfolio-link" to="/projects"><el-icon><ArrowLeft /></el-icon>全部项目</RouterLink>
          <p class="portfolio-eyebrow">{{ project.direction }}</p>
          <h1 :key="project.id" ref="pageTitle" tabindex="-1">{{ project.title }}</h1>
          <p class="detail-lead">{{ project.description }}</p>
          <dl class="project-facts">
            <div><dt>我的角色</dt><dd>{{ project.role }}</dd></div>
            <div><dt>项目时间</dt><dd>{{ project.period }}</dd></div>
            <div><dt>技术方向</dt><dd>{{ project.tags.join(' / ') }}</dd></div>
          </dl>
        </header>

        <div class="detail-layout">
          <nav class="project-outline" aria-label="项目章节">
            <a href="#background" @click.prevent="jumpTo('background')">项目背景</a>
            <a href="#work" @click.prevent="jumpTo('work')">我负责的工作</a>
            <a href="#focus" @click.prevent="jumpTo('focus')">实现重点</a>
            <a href="#reading" @click.prevent="jumpTo('reading')">延伸阅读</a>
            <RouterLink :to="{ path: '/journey', query: { project: project.id } }">时间线定位 <el-icon><ArrowRight /></el-icon></RouterLink>
          </nav>
          <div class="detail-content" :key="project.id">
            <section id="background" class="detail-section" tabindex="-1">
              <h2>项目背景</h2>
              <p>{{ project.background }}</p>
              <ProjectFlow :title="project.flowTitle" :steps="project.flow" />
            </section>
            <section id="work" class="detail-section" tabindex="-1">
              <h2>我负责的工作</h2>
              <ol class="deliverable-list"><li v-for="detail in project.details" :key="detail">{{ detail }}</li></ol>
            </section>
            <section id="focus" class="detail-section" tabindex="-1">
              <h2>实现重点</h2>
              <div v-for="focus in project.focus" :key="focus.title" class="project-focus"><h3>{{ focus.title }}</h3><p>{{ focus.description }}</p></div>
            </section>
            <div id="reading" class="reading-anchor" tabindex="-1"><ProjectNotes :topics="project.noteTopics" /></div>
          </div>
        </div>

        <nav class="project-neighbors" aria-label="相邻项目">
          <RouterLink v-if="neighbors.previous" :to="`/projects/${neighbors.previous.id}`"><span><el-icon><ArrowLeft /></el-icon>上一个项目</span><strong>{{ neighbors.previous.title }}</strong></RouterLink>
          <RouterLink v-if="neighbors.next" :to="`/projects/${neighbors.next.id}`"><span>下一个项目 <el-icon><ArrowRight /></el-icon></span><strong>{{ neighbors.next.title }}</strong></RouterLink>
        </nav>
      </template>
      <section v-else class="portfolio-heading project-missing">
        <p class="portfolio-eyebrow">项目未找到</p><h1>这个项目还没有公开详情</h1>
        <p>链接可能有误，已公开的项目可以在总览中查看。</p>
        <RouterLink class="portfolio-link" to="/projects"><el-icon><ArrowLeft /></el-icon>返回项目总览</RouterLink>
      </section>
      <footer class="portfolio-footer"><span>linge.xin / 郭麟阁</span><RouterLink class="portfolio-link" to="/projects">全部项目 <el-icon><ArrowRight /></el-icon></RouterLink></footer>
    </div>
    <BackToTopButton />
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import BackToTopButton from '@/components/BackToTopButton.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import ProjectFlow from '@/components/ProjectFlow.vue'
import ProjectNotes from '@/components/ProjectNotes.vue'
import { findProject, adjacentProjects } from '@/data/projects'
import '@/styles/portfolio.css'

const route = useRoute()
const project = computed(() => findProject(route.params.slug))
const neighbors = computed(() => adjacentProjects(project.value?.id ?? ''))
const pageTitle = ref<HTMLElement>()

watch(() => route.params.slug, async () => {
  await nextTick()
  pageTitle.value?.focus({ preventScroll: true })
})

function jumpTo(id: string) {
  const target = document.getElementById(id)
  target?.focus({ preventScroll: true })
  target?.scrollIntoView({ block: 'start', behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' })
}
</script>
