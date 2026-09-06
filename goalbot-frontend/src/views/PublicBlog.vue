<template>
  <main class="public-home" :class="{ ready }">
    <div class="home-inner header-wrap"><PublicSiteHeader /></div>

    <section class="home-hero" :style="heroStyle" @pointermove="handleHeroPointerMove" @pointerleave="resetHeroPointer">
      <img class="hero-image" :src="sakuraHero" alt="盛开的樱花与林间步道" fetchpriority="high" />
      <div class="hero-shade" aria-hidden="true" />
      <div class="home-inner hero-content">
        <div class="hero-meta"><span>LINGE.XIN / 郭麟阁的个人网站</span></div>
        <div class="hero-copy">
          <p>河南大学 · 网络工程</p>
          <h1>郭麟阁</h1>
          <p class="hero-description">关注 Java 后端、AI 应用与服务部署。<br />这里记录我的学习笔记，以及从接口到上线的项目实践。</p>
          <div class="hero-actions">
            <a href="#selected-work" @click.prevent="scrollToSection('selected-work')">查看项目 <el-icon><ArrowDown /></el-icon></a>
            <RouterLink to="/notes">阅读笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
          </div>
        </div>
        <div class="hero-footer-row">
          <span>Java / Spring Boot / AI Applications</span>
          <button type="button" @click="scrollToSection('profile')">认识我 <el-icon><ArrowDown /></el-icon></button>
        </div>
      </div>
    </section>

    <section id="profile" class="profile-band">
      <div class="home-inner profile-layout reveal">
        <div class="section-heading"><p class="section-index">01 / PROFILE</p><h2>从课程学习，<br />走进实际开发。</h2></div>
        <div class="profile-copy">
          <p>我是郭麟阁，2025 年进入河南大学学习网络工程。课程之外，我参与云边端协同和 3D 定制电商项目，也独立开发 LLM 微信机器人与这个个人网站。</p>
          <p>我主要负责后端接口、数据持久化与部署联调，尝试把 AI 能力接入具体的业务流程。</p>
          <dl class="profile-facts">
            <div><dt>学习</dt><dd>河南大学 · 网络工程<br /><span>2025.09 起</span></dd></div>
            <div><dt>实践</dt><dd>Java / Spring Boot / MySQL<br /><span>Linux / Nginx / Vue 3</span></dd></div>
          </dl>
          <RouterLink class="text-link" to="/journey">教育经历与竞赛荣誉 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
      </div>
    </section>

    <section id="selected-work" class="work-band">
      <div class="home-inner">
        <header class="work-heading reveal">
          <div class="section-heading"><p class="section-index">02 / SELECTED WORK</p><h2>做过的项目</h2></div>
          <RouterLink class="text-link" to="/journey">完整项目时间线 <el-icon><ArrowRight /></el-icon></RouterLink>
        </header>
        <div class="project-list">
          <article v-for="project in selectedProjects" :key="project.id" class="project-row reveal">
            <div class="project-meta"><span>{{ project.role }}</span><time>{{ project.period }}</time></div>
            <div class="project-content">
              <h3>{{ project.title }}</h3>
              <p>{{ project.description }}</p>
              <div class="project-tags"><span v-for="tag in project.tags" :key="tag">{{ tag }}</span></div>
              <details class="project-details">
                <summary>具体工作 <el-icon class="detail-chevron"><ArrowDown /></el-icon></summary>
                <ul><li v-for="detail in project.details" :key="detail">{{ detail }}</li></ul>
              </details>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="archive-band">
      <div class="home-inner archive-layout reveal">
        <div class="section-heading"><p class="section-index">03 / LEARNING NOTES</p><h2>学习的另一面，<br />留在笔记里。</h2></div>
        <div class="archive-copy">
          <p>课程知识、代码片段和项目中的问题，整理成可以再次查阅的笔记。</p>
          <RouterLink class="text-link" to="/notes">进入学习笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
        </div>
      </div>
    </section>

    <footer class="home-footer"><div class="home-inner"><span class="footer-brand">linge.xin</span><nav aria-label="页脚导航"><RouterLink to="/about">关于本站</RouterLink><RouterLink to="/login">站长入口</RouterLink></nav><span>© {{ currentYear }} 郭麟阁</span></div></footer>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import { timelineItems } from '@/data/timeline'
import sakuraHero from '@/assets/linge-sakura-hero.png'

const ready = ref(false)
const heroOffset = ref({ x: 0, y: 0 })
const currentYear = new Date().getFullYear()
const selectedProjects = timelineItems.filter((item) => ['cloud-edge-capture', 'ceramic-commerce', 'wechat-llm-agent'].includes(item.id))
const heroStyle = computed(() => ({
  '--hero-x': heroOffset.value.x + 'px',
  '--hero-y': heroOffset.value.y + 'px',
  '--hero-copy-x': heroOffset.value.x * -0.18 + 'px',
  '--hero-copy-y': heroOffset.value.y * -0.18 + 'px'
}))

onMounted(() => { ready.value = true })

function handleHeroPointerMove(event: PointerEvent) {
  if (window.matchMedia('(prefers-reduced-motion: reduce), (pointer: coarse)').matches) return
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  heroOffset.value = {
    x: Math.max(-9, Math.min(9, (event.clientX - rect.left - rect.width / 2) / rect.width * 18)),
    y: Math.max(-7, Math.min(7, (event.clientY - rect.top - rect.height / 2) / rect.height * 14))
  }
}
function resetHeroPointer() { heroOffset.value = { x: 0, y: 0 } }
function scrollToSection(id: string) {
  const section = document.getElementById(id)
  if (!section) return
  section.tabIndex = -1
  section.focus({ preventScroll: true })
  section.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' })
}
</script>

<style scoped>
.public-home { --hero-overlay:color-mix(in srgb, var(--text) 62%, transparent); --hero-muted:color-mix(in srgb, var(--surface) 88%, transparent); overflow:hidden; color:var(--text); background:transparent; }
.home-inner { width:min(100% - 32px, 1240px); margin-inline:auto; }
.header-wrap { padding-block:var(--space-3); }
.home-hero { position:relative; overflow:hidden; color:var(--surface); background:var(--brand); }
.hero-image { position:absolute; inset:0; width:100%; height:100%; object-fit:cover; object-position:center 34%; transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.055); transition:transform 1.2s cubic-bezier(.16,1,.3,1); }
.ready .hero-image { transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.025); }
.hero-shade { position:absolute; inset:0; background:var(--hero-overlay); }
.hero-content { position:relative; z-index:2; display:flex; min-height:min(620px, 68svh); flex-direction:column; justify-content:space-between; gap:var(--space-5); padding-block:var(--space-5); transform:translate3d(var(--hero-copy-x),var(--hero-copy-y),0); transition:transform .5s ease-out; }
.hero-meta, .hero-footer-row { display:flex; flex-wrap:wrap; gap:var(--space-3); color:var(--hero-muted); font-size:14px; }
.hero-copy { display:flex; max-width:760px; flex-direction:column; gap:var(--space-4); }
.hero-copy>p, .hero-copy h1 { margin:0; }
.hero-copy>p { font-size:16px; line-height:1.6; }
.hero-copy h1 { color:var(--surface); font-size:56px; line-height:1.1; }
.hero-copy .hero-description { max-width:38em; color:var(--surface); }
.hero-actions { display:flex; flex-wrap:wrap; gap:var(--space-5); padding-top:var(--space-2); }
.hero-actions a { display:inline-flex; min-height:44px; align-items:center; gap:var(--space-2); border-bottom:1px solid var(--hero-muted); color:var(--surface); font-size:15px; font-weight:700; text-decoration:none; }
.hero-footer-row { align-items:center; justify-content:space-between; }
.hero-footer-row button { display:inline-flex; align-items:center; gap:var(--space-2); min-height:44px; padding:0; border:0; color:var(--surface); background:transparent; font-size:14px; cursor:pointer; }
.profile-band, .work-band, .archive-band { padding-block:var(--space-7); scroll-margin-top:var(--space-5); }
.profile-layout, .archive-layout { display:flex; flex-direction:column; gap:var(--space-5); }
.section-heading { display:flex; flex-direction:column; gap:var(--space-3); }
.section-index { margin:0; color:var(--brand); font-size:14px; font-weight:700; }
.section-heading h2 { margin:0; color:var(--text); font-size:30px; line-height:1.3; }
.profile-copy, .archive-copy { display:flex; flex-direction:column; align-items:flex-start; gap:var(--space-5); }
.profile-copy>p, .archive-copy>p { margin:0; max-width:54ch; color:var(--muted); font-size:16px; line-height:1.6; }
.profile-facts { display:flex; flex-direction:column; gap:var(--space-5); margin:0; padding-block:var(--space-5); width:100%; border-block:1px solid var(--line); }
.profile-facts>div { display:flex; gap:var(--space-5); font-size:14px; }
.profile-facts dt { flex:none; color:var(--brand); }
.profile-facts dd { margin:0; color:var(--text); }
.profile-facts dd span { color:var(--muted); }
.text-link { display:inline-flex; align-items:center; min-height:44px; gap:var(--space-2); color:var(--brand-strong); font-size:15px; font-weight:650; text-decoration:none; }
.text-link:hover { text-decoration:underline; text-underline-offset:5px; }
.text-link .el-icon, .hero-actions .el-icon { transition:transform .2s ease; }
.text-link:hover .el-icon, .hero-actions a:hover .el-icon { transform:translateX(4px); }
.work-band { padding-top:var(--space-5); }
.work-heading { display:flex; flex-direction:column; align-items:flex-start; gap:var(--space-4); padding-bottom:var(--space-5); }
.project-list { display:flex; flex-direction:column; }
.project-row { display:flex; flex-direction:column; gap:var(--space-4); padding-block:var(--space-6); border-top:1px solid var(--line); }
.project-meta { display:flex; flex-wrap:wrap; gap:var(--space-2) var(--space-4); color:var(--muted); font-size:14px; }
.project-meta>span { color:var(--brand-strong); font-weight:650; }
.project-content { display:flex; min-width:0; flex-direction:column; gap:var(--space-4); }
.project-content h3 { margin:0; color:var(--text); font-size:24px; line-height:1.4; overflow-wrap:anywhere; }
.project-content>p { margin:0; max-width:58ch; color:var(--muted); font-size:16px; line-height:1.6; }
.project-tags { display:flex; flex-wrap:wrap; gap:var(--space-2) var(--space-4); color:var(--brand-strong); font-size:14px; }
.project-details summary { display:flex; align-items:center; gap:var(--space-2); min-height:44px; width:fit-content; color:var(--brand-strong); font-size:14px; cursor:pointer; list-style:none; }
.project-details summary::-webkit-details-marker { display:none; }
.detail-chevron { transition:transform .2s ease; }
.project-details[open] .detail-chevron { transform:rotate(180deg); }
.project-details ul { display:flex; flex-direction:column; gap:var(--space-2); margin:0; padding:var(--space-3) 0 var(--space-2) var(--space-5); color:var(--muted); font-size:15px; line-height:1.6; }
.archive-band { color:var(--text); background:var(--brand-soft); }
.home-footer { padding-block:var(--space-6); border-top:1px solid var(--line); }
.home-footer>.home-inner { display:flex; flex-wrap:wrap; gap:var(--space-4); align-items:center; justify-content:space-between; color:var(--muted); font-size:14px; }
.home-footer nav { display:flex; gap:var(--space-4); }
.home-footer nav a { padding-block:var(--space-3); text-decoration:none; }
.home-footer nav a:hover { color:var(--brand-strong); text-decoration:underline; }
.footer-brand { color:var(--brand-strong); font-family:var(--font-display); font-size:22px; font-weight:700; }
.reveal { opacity:1; transform:none; }
@media(prefers-reduced-motion:no-preference) {
  @supports(animation-timeline:view()) {
    .reveal { animation:section-enter linear both; animation-timeline:view(); animation-range:entry 0% entry 18%; }
  }
}
@keyframes section-enter { from { opacity:.35; transform:translateY(14px); } to { opacity:1; transform:none; } }
@media(min-width:760px) {
  .home-inner { width:min(100% - 64px, 1240px); }
  .hero-copy h1 { font-size:88px; }
  .hero-copy>p { font-size:18px; }
  .profile-band, .archive-band { padding-block:var(--space-8); }
  .profile-layout, .archive-layout { flex-direction:row; justify-content:space-between; gap:var(--space-7); }
  .profile-layout>.section-heading, .archive-layout>.section-heading { flex:0 0 36%; }
  .profile-copy, .archive-copy { flex:1; max-width:660px; }
  .section-heading h2 { font-size:40px; }
  .profile-facts { flex-direction:row; flex-wrap:wrap; }
  .work-heading { flex-direction:row; align-items:flex-end; justify-content:space-between; }
  .project-row { flex-direction:row; gap:var(--space-7); padding-block:var(--space-7); }
  .project-meta { flex:0 0 24%; flex-direction:column; align-items:flex-start; }
  .project-content { flex:1; }
  .project-content h3 { font-size:30px; }
}
@media(prefers-reduced-motion:reduce) {
  .hero-image, .ready .hero-image, .hero-content, .reveal { transform:none; animation:none; opacity:1; }
}
</style>
