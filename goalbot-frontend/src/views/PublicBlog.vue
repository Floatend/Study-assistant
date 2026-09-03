<template>
  <main class="public-home" :class="{ ready }">
    <div class="home-inner header-wrap"><PublicSiteHeader /></div>

    <section class="home-hero" :style="heroStyle" @pointermove="handleHeroPointerMove" @pointerleave="resetHeroPointer">
      <img class="hero-image" :src="sakuraHero" alt="樱花盛开的校园学习园景" />
      <div class="hero-shade" aria-hidden="true" />
      <div class="home-inner hero-content">
        <div class="hero-meta"><span>PERSONAL KNOWLEDGE ARCHIVE</span><span>EST. 2026</span></div>
        <div class="hero-copy">
          <p>在学习、记录与构建之间。</p>
          <h1>linge.xin</h1>
          <div class="hero-lines"><span>留出时间，完成重要的事。</span><span>留下过程，成为更清楚的自己。</span></div>
          <div class="hero-actions">
            <RouterLink to="/notes">进入学习笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
            <RouterLink to="/journey">查看履历时间线 <el-icon><ArrowRight /></el-icon></RouterLink>
          </div>
        </div>
        <div class="hero-footer-row">
          <p>A personal place for notes, systems, and deliberate days.</p>
          <button type="button" class="hero-scroll" @click="scrollToDirections">继续探索 <el-icon><ArrowDown /></el-icon></button>
        </div>
      </div>
    </section>

    <section class="manifesto-band">
      <div class="home-inner manifesto-grid reveal">
        <p class="section-index">01 / WHY THIS EXISTS</p>
        <h2>不急着填满每一天，<br />先让每一件事有自己的位置。</h2>
        <div><p>这里展示站长经过整理的学习成果、项目记录和阶段思考。它不是任务清单的镜像，而是那些值得被重新阅读的过程。</p><RouterLink to="/about">关于 linge.xin <span>→</span></RouterLink></div>
      </div>
    </section>

    <section id="directions" class="directions-band">
      <div class="home-inner directions-head"><p class="section-index">02 / EXPLORE</p><span>沿着真实内容继续浏览。</span></div>
      <div class="home-inner directions-list">
        <article class="direction reveal"><p>01</p><div><h2>学习笔记</h2><span>把课程、技术与思考从一次性输入整理成可以回访的知识页。</span></div><RouterLink to="/notes">浏览知识库 <b>→</b></RouterLink></article>
        <article class="direction reveal"><p>02</p><div><h2>履历时间线</h2><span>沿着时间查看真实推进过的课程、项目与阶段学习路线。</span></div><RouterLink to="/journey">查看时间线 <b>→</b></RouterLink></article>
        <article class="direction reveal"><p>03</p><div><h2>关于本站</h2><span>了解内容如何从私人草稿，经过整理后成为公开知识页。</span></div><RouterLink to="/about">了解发布原则 <b>→</b></RouterLink></article>
      </div>
    </section>

    <section class="pause-band"><div class="home-inner pause-copy reveal"><span>“</span><p>真正的系统不是把生活压缩得更满，<br />而是让重要的部分有机会发生。</p></div></section>

    <section class="home-cta"><div class="home-inner cta-grid reveal"><p class="section-index">03 / START HERE</p><h2>从一篇笔记，<br />或一段真实经历开始。</h2><div><RouterLink to="/notes" class="cta-link">阅读站长笔记 <span>→</span></RouterLink><RouterLink to="/login" class="cta-link quiet">站长入口 <span>↗</span></RouterLink></div></div></section>

    <footer class="home-footer"><div class="home-inner"><span class="footer-brand">linge.xin</span><span>Write slowly. Build deliberately.</span><span>© 2026</span></div></footer>
  </main>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowDown, ArrowRight } from '@element-plus/icons-vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import sakuraHero from '@/assets/linge-sakura-hero.png'

const ready = ref(false)
const heroOffset = ref({ x: 0, y: 0 })
let observer: IntersectionObserver | undefined

const heroStyle = computed(() => ({
  '--hero-x': `${heroOffset.value.x}px`,
  '--hero-y': `${heroOffset.value.y}px`,
  '--hero-copy-x': `${heroOffset.value.x * -0.18}px`,
  '--hero-copy-y': `${heroOffset.value.y * -0.18}px`
}))

onMounted(async () => {
  await nextTick()
  ready.value = true
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return
      entry.target.classList.add('is-visible')
      observer?.unobserve(entry.target)
    })
  }, { threshold: .14 })
  document.querySelectorAll('.reveal').forEach((element) => observer?.observe(element))
})

onBeforeUnmount(() => observer?.disconnect())

function handleHeroPointerMove(event: PointerEvent) {
  if (window.matchMedia('(prefers-reduced-motion: reduce), (pointer: coarse)').matches) return
  const element = event.currentTarget as HTMLElement
  const rect = element.getBoundingClientRect()
  heroOffset.value = {
    x: Math.max(-9, Math.min(9, (event.clientX - rect.left - rect.width / 2) / rect.width * 18)),
    y: Math.max(-7, Math.min(7, (event.clientY - rect.top - rect.height / 2) / rect.height * 14))
  }
}

function resetHeroPointer() { heroOffset.value = { x: 0, y: 0 } }
function scrollToDirections() { document.getElementById('directions')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
</script>

<style scoped>
.public-home { overflow: hidden; color: var(--text); background: transparent; }
.home-inner { width: min(100% - 32px, 1240px); margin-inline: auto; }
.header-wrap { padding-block: var(--space-3); }

.home-hero {
  position: relative;
  min-height: min(680px, 72svh);
  overflow: hidden;
  color: var(--surface);
  background: var(--brand);
}
.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 34%;
  opacity: .9;
  transform: translate3d(var(--hero-x), var(--hero-y), 0) scale(1.055);
  transition: transform 1.2s cubic-bezier(.16,1,.3,1), opacity .8s ease;
}
.ready .hero-image { transform: translate3d(var(--hero-x), var(--hero-y), 0) scale(1.012); }
.hero-shade { position: absolute; inset: 0; background: color-mix(in srgb, var(--text) 72%, transparent); }
.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  min-height: min(680px, 72svh);
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-6);
  padding-block: var(--space-5);
  transform: translate3d(var(--hero-copy-x), var(--hero-copy-y), 0);
  transition: transform .5s ease-out;
}
.hero-meta, .hero-footer-row { display: flex; justify-content: space-between; gap: var(--space-4); color: color-mix(in srgb, var(--surface) 72%, transparent); font-size: 11px; font-weight: 700; letter-spacing: .12em; }
.hero-copy { display: flex; max-width: 820px; flex-direction: column; gap: var(--space-4); }
.hero-copy>p, .hero-copy h1, .hero-footer-row p { margin: 0; }
.hero-copy>p { color: color-mix(in srgb, var(--surface) 82%, var(--accent)); font-size: 17px; font-weight: 650; }
.hero-copy h1 { color: var(--surface); font-size: 58px; line-height: .92; }
.hero-lines { display: flex; flex-direction: column; gap: var(--space-1); color: var(--surface); font-size: 17px; line-height: 1.5; }
.hero-actions { display: flex; flex-wrap: wrap; gap: var(--space-5); padding-top: var(--space-2); }
.hero-actions a { display: inline-flex; align-items: center; gap: var(--space-2); padding-bottom: var(--space-1); border-bottom: 1px solid color-mix(in srgb, var(--surface) 68%, transparent); color: var(--surface); font-size: 14px; font-weight: 750; text-decoration: none; }
.hero-actions .el-icon, .direction b, .cta-link span { transition: transform .24s ease; }
.hero-actions a:hover .el-icon, .direction a:hover b, .cta-link:hover span { transform: translateX(5px); }
.hero-footer-row { align-items: flex-start; flex-direction: column; letter-spacing: 0; }
.hero-footer-row p { font-size: 13px; }
.hero-scroll { display: inline-flex; align-items: center; gap: var(--space-2); padding: 0; border: 0; color: var(--surface); background: transparent; font-size: 14px; font-weight: 700; cursor: pointer; }
.hero-scroll .el-icon { animation: scroll-bounce 1.8s ease-in-out infinite; }
@keyframes scroll-bounce { 0%,to{transform:translateY(0)} 50%{transform:translateY(4px)} }

.manifesto-band, .directions-band, .home-cta { padding-block: var(--space-8); }
.manifesto-grid, .cta-grid { display: flex; flex-direction: column; gap: var(--space-5); }
.section-index { color: var(--brand); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.manifesto-grid h2, .cta-grid h2 { color: var(--text); font-size: 36px; line-height: 1.24; }
.manifesto-grid div { display: flex; flex-direction: column; gap: var(--space-5); }
.manifesto-grid div>p { max-width: 52ch; color: var(--muted); font-size: 15px; line-height: 1.6; }
.manifesto-grid a, .direction a { display: inline-flex; align-items: center; gap: var(--space-2); color: var(--brand-strong); font-size: 14px; font-weight: 750; text-decoration: none; }

.directions-band { padding-top: 0; }
.directions-head { display: flex; flex-direction: column; gap: var(--space-2); padding-bottom: var(--space-5); border-bottom: 1px solid var(--line); }
.directions-head>span { color: var(--muted); font-size: 14px; }
.directions-list { display: flex; flex-direction: column; }
.direction {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: var(--space-4);
  padding-block: var(--space-6);
  border-bottom: 1px solid var(--line);
  transition: padding .28s ease, background-color .28s ease;
}
.direction:hover { padding-inline: var(--space-4); background: var(--surface-soft); }
.direction>p { color: var(--accent); font-size: 14px; font-weight: 800; }
.direction>div { display: flex; flex-direction: column; gap: var(--space-2); }
.direction h2 { color: var(--text); font-size: 29px; line-height: 1.2; }
.direction>div>span { max-width: 46ch; color: var(--muted); font-size: 14px; line-height: 1.6; }
.direction>a { grid-column: 2; justify-self: start; }

.pause-band { padding-block: var(--space-8); color: var(--on-brand); background: var(--brand); }
.pause-copy { display: flex; gap: var(--space-4); align-items: flex-start; }
.pause-copy span { color: color-mix(in srgb, var(--surface) 46%, transparent); font-family: var(--font-display); font-size: 72px; line-height: .7; }
.pause-copy p { max-width: 22ch; font-family: var(--font-display); font-size: 28px; line-height: 1.35; text-wrap: balance; }

.cta-grid>div { display: flex; flex-wrap: wrap; gap: var(--space-3); }
.cta-link { display: inline-flex; min-height: 46px; align-items: center; justify-content: center; gap: var(--space-2); padding-inline: var(--space-5); border: 1px solid var(--brand); border-radius: 999px; color: var(--on-brand); background: var(--brand); font-size: 14px; font-weight: 750; text-decoration: none; transition: transform .22s ease, background-color .22s ease; }
.cta-link:hover { background: var(--brand-strong); transform: translateY(-2px); }
.cta-link.quiet { color: var(--brand-strong); background: transparent; }
.cta-link.quiet:hover { background: var(--brand-soft); }

.home-footer { padding-block: var(--space-6); border-top: 1px solid var(--line); }
.home-footer>.home-inner { display: flex; flex-direction: column; gap: var(--space-2); color: var(--subtle); font-size: 12px; }
.footer-brand { color: var(--brand-strong); font-family: var(--font-display); font-size: 22px; font-weight: 700; }
.reveal { opacity: 0; transform: translateY(18px); transition: opacity .65s ease, transform .65s cubic-bezier(.16,1,.3,1); }
.reveal.is-visible { opacity: 1; transform: none; }

@media (min-width: 760px) {
  .home-inner { width: min(100% - 64px, 1240px); }
  .hero-copy h1 { font-size: 104px; }
  .hero-copy>p, .hero-lines { font-size: 20px; }
  .hero-footer-row { align-items: center; flex-direction: row; }
  .manifesto-grid, .cta-grid { display: grid; grid-template-columns: .48fr 1.24fr .72fr; gap: var(--space-7); align-items: start; }
  .manifesto-grid h2, .cta-grid h2 { font-size: 49px; }
  .directions-head { flex-direction: row; align-items: flex-end; justify-content: space-between; }
  .direction { grid-template-columns: 70px minmax(0,1fr) auto; align-items: center; gap: var(--space-6); padding-block: var(--space-7); }
  .direction h2 { font-size: 38px; }
  .direction>a { grid-column: 3; justify-self: end; }
  .pause-copy { gap: var(--space-7); }
  .pause-copy span { font-size: 112px; }
  .pause-copy p { font-size: 45px; }
  .home-footer>.home-inner { flex-direction: row; align-items: center; justify-content: space-between; }
}
</style>
