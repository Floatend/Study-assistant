<template>
  <main class="portfolio-page">
    <div class="portfolio-shell">
      <PublicSiteHeader />
      <header class="portfolio-heading">
        <p class="portfolio-eyebrow">郭麟阁 / 项目实践</p>
        <h1>项目</h1>
        <div class="portfolio-intro">
          <p>从后端接口到 AI 应用，记录我在不同项目里承担的工作。</p>
          <RouterLink class="portfolio-link" to="/journey">在时间线中查看 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
      </header>

      <div class="portfolio-index">
        <article v-for="project in projects" :id="project.id" :key="project.id" class="portfolio-entry">
          <div class="entry-meta"><span>{{ project.direction }}</span><time>{{ project.period }}</time><span>{{ project.role }}</span></div>
          <div class="entry-body">
            <h2><RouterLink :to="`/projects/${project.id}`">{{ project.title }}</RouterLink></h2>
            <p class="entry-description">{{ project.description }}</p>
            <ul class="portfolio-tags" aria-label="技术方向"><li v-for="tag in project.tags" :key="tag">{{ tag }}</li></ul>
            <ProjectFlow :title="project.flowTitle" :steps="project.flow" />
            <RouterLink class="portfolio-link" :to="`/projects/${project.id}`" :aria-label="`查看项目详情：${project.title}`">查看项目详情 <el-icon><ArrowRight /></el-icon></RouterLink>
          </div>
        </article>
      </div>

      <footer class="portfolio-footer"><span>linge.xin / 郭麟阁</span><RouterLink class="portfolio-link" to="/notes">继续阅读学习笔记 <el-icon><ArrowRight /></el-icon></RouterLink></footer>
    </div>
    <BackToTopButton />
  </main>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import BackToTopButton from '@/components/BackToTopButton.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import ProjectFlow from '@/components/ProjectFlow.vue'
import { projects } from '@/data/projects'
import '@/styles/portfolio.css'
</script>
