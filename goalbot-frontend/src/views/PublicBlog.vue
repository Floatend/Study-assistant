<template>
  <main class="public-home" :class="{ ready }">
    <div class="home-inner"><PublicSiteHeader /></div>

    <section class="home-hero" :style="heroStyle" @pointermove="handleHeroPointerMove" @pointerleave="resetHeroPointer">
      <img class="hero-image" :src="sakuraHero" alt="樱花盛开的校园学习园景" />
      <div class="hero-shade" aria-hidden="true" />
      <div class="sakura-petals" aria-hidden="true"><span v-for="index in 8" :key="index" class="sakura-petal" /></div>
      <div class="home-inner hero-content">
        <div class="hero-meta"><span>PERSONAL KNOWLEDGE ARCHIVE</span><span>EST. 2026</span></div>
        <div class="hero-copy">
          <p>在学习、记录与构建之间。</p>
          <h1>linge.xin</h1>
          <div class="hero-lines"><span>留出时间，完成重要的事。</span><span>留下过程，成为更清楚的自己。</span></div>
          <div class="hero-actions">
            <RouterLink to="/notes">进入学习笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
            <RouterLink to="/about">认识这个站点 <el-icon><ArrowRight /></el-icon></RouterLink>
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
      <div class="home-inner directions-head"><p class="section-index">02 / THREE DIRECTIONS</p><span>把输入、实践与写作连接起来。</span></div>
      <div class="home-inner directions-grid">
        <article class="direction direction-notes reveal"><p>01</p><h2>学习笔记</h2><span>把课程、技术与思考从一次性输入整理成可以回访的知识页。</span><RouterLink to="/notes">浏览知识库 <b>→</b></RouterLink></article>
        <article class="direction direction-system reveal"><p>02</p><h2>项目记录</h2><span>记录真实做过的设计、开发与部署，把零散经验整理成下一次仍然能用的方法。</span><RouterLink to="/notes">查看整理成果 <b>→</b></RouterLink></article>
        <article class="direction direction-lab reveal"><p>03</p><h2>持续构建</h2><span>给每一次试错保留痕迹，在小系统和长期项目中练习判断与创造。</span><RouterLink to="/about">了解方法 <b>→</b></RouterLink></article>
      </div>
    </section>

    <section class="pause-band"><div class="home-inner pause-copy reveal"><span>“</span><p>真正的系统不是把生活压缩得更满，<br />而是让重要的部分有机会发生。</p></div></section>

    <section class="home-cta"><div class="home-inner cta-grid reveal"><p class="section-index">03 / START HERE</p><h2>从一篇笔记，<br />或一个正在推进的项目开始。</h2><div><RouterLink to="/notes" class="cta-link">阅读站长笔记 <span>→</span></RouterLink><RouterLink to="/login" class="cta-link quiet">站长入口 <span>↗</span></RouterLink></div></div></section>

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
  '--hero-copy-x': `${heroOffset.value.x * -0.22}px`,
  '--hero-copy-y': `${heroOffset.value.y * -0.22}px`
}))

onMounted(async () => {
  await nextTick()
  ready.value = true
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => { if (entry.isIntersecting) entry.target.classList.add('is-visible') })
  }, { threshold: .14 })
  document.querySelectorAll('.reveal').forEach((element) => observer?.observe(element))
})

onBeforeUnmount(() => observer?.disconnect())

function handleHeroPointerMove(event: PointerEvent) {
  if (window.matchMedia('(prefers-reduced-motion: reduce), (pointer: coarse)').matches) return
  const element = event.currentTarget as HTMLElement
  const rect = element.getBoundingClientRect()
  heroOffset.value = {
    x: Math.max(-11, Math.min(11, (event.clientX - rect.left - rect.width / 2) / rect.width * 22)),
    y: Math.max(-8, Math.min(8, (event.clientY - rect.top - rect.height / 2) / rect.height * 16))
  }
}

function resetHeroPointer() { heroOffset.value = { x: 0, y: 0 } }
function scrollToDirections() { document.getElementById('directions')?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
</script>

<style scoped>
.public-home { overflow:hidden; color:var(--gb-text); background:var(--gb-bg); }
.home-inner { width:min(1320px,calc(100% - 8vw)); margin:0 auto; }

/* ============ Hero ============ */
.home-hero {
  position:relative;
  min-height:calc(100svh - 76px);
  overflow:hidden;
  background:linear-gradient(118deg,#141e52 0%,#1b2c74 48%,#2a3f96 100%);
}
.hero-image {
  position:absolute; inset:0; width:100%; height:100%;
  object-fit:cover; object-position:center 30%; opacity:.88;
  transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.06);
  transition:transform 1.4s cubic-bezier(.16,1,.3,1);
}
.ready .hero-image { transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.015); }
.hero-shade {
  position:absolute; inset:0;
  background:
    linear-gradient(100deg,rgba(9,14,44,.94) 0%,rgba(13,20,58,.80) 34%,rgba(17,26,70,.56) 58%,rgba(17,26,70,.30) 76%,rgba(9,13,40,.55) 100%),
    linear-gradient(180deg,rgba(9,13,40,.28) 0%,rgba(9,13,40,0) 30%,rgba(9,13,40,0) 72%,rgba(9,13,40,.44) 100%);
}
.hero-content {
  position:relative; z-index:2;
  display:flex; min-height:calc(100svh - 76px); flex-direction:column; justify-content:space-between;
  padding:34px 0 38px; color:#f5f7ff;
  transform:translate3d(var(--hero-copy-x),var(--hero-copy-y),0);
  transition:transform .55s ease-out;
}
.hero-meta { display:flex; justify-content:space-between; color:rgba(214,225,255,.72); font-size:11px; font-weight:700; letter-spacing:.14em; }
.hero-copy { max-width:820px; margin:auto 0; }
.hero-copy>p { margin:0 0 10px; color:var(--gb-mint); font-size:clamp(17px,2vw,26px); font-weight:600; line-height:1.45; }
.hero-copy h1 { margin:0; color:#fff; font-size:clamp(64px,11.5vw,166px); font-weight:800; letter-spacing:-.02em; line-height:.86; }
.hero-lines { display:grid; gap:5px; margin-top:32px; color:rgba(245,247,255,.94); font-size:clamp(17px,2vw,25px); line-height:1.5; }
.hero-actions { display:flex; flex-wrap:wrap; gap:26px; margin-top:34px; }
.hero-actions a {
  display:inline-flex; align-items:center; gap:8px; padding-bottom:6px;
  border-bottom:2px solid rgba(169,243,228,.5); color:#f5f7ff;
  font-size:14px; font-weight:700; text-decoration:none;
  transition:border-color .22s ease,color .22s ease;
}
.hero-actions a:hover { border-bottom-color:var(--gb-mint); color:#fff; }
.hero-actions .el-icon { transition:transform .3s ease; }
.hero-actions a:hover .el-icon { transform:translateX(5px); }
.hero-footer-row { display:flex; align-items:center; justify-content:space-between; gap:16px; }
.hero-footer-row p { margin:0; color:rgba(214,225,255,.66); font-size:13px; }
.hero-scroll { display:inline-flex; align-items:center; gap:7px; padding:0; border:0; color:var(--gb-mint); background:transparent; font-family:inherit; font-size:13px; font-weight:700; cursor:pointer; transition:color .2s ease; }
.hero-scroll:hover { color:#fff; }
.hero-scroll .el-icon { font-size:16px; animation:scroll-bounce 1.8s ease-in-out infinite; }
@keyframes scroll-bounce { 0%,to{transform:translateY(0)} 50%{transform:translateY(4px)} }

/* Sakura petals — quiet accent over the navy hero */
.sakura-petals { position:absolute; z-index:1; inset:0; overflow:hidden; pointer-events:none; }
.sakura-petal {
  position:absolute; width:14px; height:10px; opacity:0;
  border-radius:90% 10% 85% 15%;
  background:linear-gradient(135deg,#ffffff 0%,#fde9f1 55%,#f5bcd4 100%);
  box-shadow:0 3px 9px rgba(10,14,44,.4);
  transform:translate3d(0,-28px,0) rotate(-20deg);
}
.sakura-petal:nth-child(1) { top:15%; left:59%; }
.sakura-petal:nth-child(2) { top:29%; left:74%; width:11px; height:8px; }
.sakura-petal:nth-child(3) { top:13%; left:86%; width:16px; height:11px; }
.sakura-petal:nth-child(4) { top:45%; left:69%; width:10px; height:7px; }
.sakura-petal:nth-child(5) { top:57%; left:88%; }
.sakura-petal:nth-child(6) { top:21%; left:94%; width:9px; height:7px; }
.sakura-petal:nth-child(7) { top:36%; left:81%; width:12px; height:9px; }
.sakura-petal:nth-child(8) { top:62%; left:77%; width:9px; height:7px; }
.ready .sakura-petal { animation:sakura-arrive 1.15s cubic-bezier(.16,1,.3,1) both; }
.ready .sakura-petal:nth-child(2) { animation-delay:.08s; }
.ready .sakura-petal:nth-child(3) { animation-delay:.16s; }
.ready .sakura-petal:nth-child(4) { animation-delay:.24s; }
.ready .sakura-petal:nth-child(5) { animation-delay:.32s; }
.ready .sakura-petal:nth-child(6) { animation-delay:.4s; }
.ready .sakura-petal:nth-child(7) { animation-delay:.48s; }
.ready .sakura-petal:nth-child(8) { animation-delay:.56s; }
@keyframes sakura-arrive {
  from { opacity:0; transform:translate3d(-24px,-42px,0) rotate(-44deg) scale(.72); }
  72% { opacity:.8; }
  to { opacity:.5; transform:translate3d(0,0,0) rotate(18deg) scale(1); }
}

/* ============ Manifesto ============ */
.manifesto-band { padding:clamp(80px,12vw,160px) 0; }
.manifesto-grid { display:grid; grid-template-columns:.5fr 1.35fr .8fr; gap:clamp(24px,6vw,86px); align-items:start; }
.section-index { margin:0; color:var(--gb-primary); font-size:11px; font-weight:800; letter-spacing:.14em; }
.manifesto-grid h2 { margin:0; color:var(--gb-text); font-size:clamp(34px,4vw,58px); font-weight:800; letter-spacing:-.02em; line-height:1.22; }
.manifesto-grid div>p { margin:0; color:var(--gb-muted); font-size:15px; line-height:1.9; }
.manifesto-grid a { display:inline-flex; gap:7px; margin-top:26px; color:var(--gb-primary); font-size:14px; font-weight:800; text-decoration:none; }
.manifesto-grid a span { transition:transform .3s ease; }
.manifesto-grid a:hover span { transform:translateX(4px); }

/* ============ Directions (cards) ============ */
.directions-band { padding:0 0 clamp(80px,10vw,140px); }
.directions-head { display:flex; align-items:end; justify-content:space-between; gap:20px; padding-bottom:22px; border-bottom:1px solid var(--gb-border); }
.directions-head>span { color:var(--gb-muted); font-size:14px; }
.directions-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:22px; margin-top:26px; }
.direction {
  position:relative; display:flex; min-height:330px; flex-direction:column;
  padding:32px 30px 34px;
  border:1px solid var(--gb-border); border-radius:var(--gb-radius);
  transition:transform .34s cubic-bezier(.16,1,.3,1),box-shadow .34s ease,border-color .34s ease;
}
.direction:hover { transform:translateY(-8px); border-color:transparent; box-shadow:var(--gb-shadow-lg); }
.direction>p { margin:0; color:var(--gb-primary); font-size:17px; font-weight:800; letter-spacing:.04em; }
.direction h2 { margin:56px 0 13px; color:var(--gb-text); font-size:clamp(24px,2.6vw,34px); font-weight:800; letter-spacing:-.01em; }
.direction>span { max-width:290px; color:var(--gb-muted); font-size:14px; line-height:1.82; }
.direction a { display:flex; align-items:center; justify-content:space-between; margin-top:auto; padding-top:22px; color:var(--gb-primary-dark); font-size:14px; font-weight:800; text-decoration:none; }
.direction a b { font-size:18px; transition:transform .3s ease; }
.direction a:hover b { transform:translateX(6px); }
.direction-notes { background:linear-gradient(180deg,#eef2ff 0%,#e3eaff 100%); }
.direction-system { background:linear-gradient(180deg,#e2f6f1 0%,#d1efe8 100%); }
.direction-lab { background:linear-gradient(180deg,#fdf0e7 0%,#fbe4d3 100%); }

/* ============ Pause ============ */
.pause-band { padding:clamp(84px,14vw,190px) 0; background:linear-gradient(118deg,#131c4a 0%,#1b2a68 55%,#243580 100%); color:#f5f7ff; }
.pause-copy { display:flex; max-width:920px; margin:0 auto; gap:clamp(22px,5vw,70px); align-items:start; }
.pause-copy span { color:rgba(169,243,228,.55); font-size:clamp(70px,11vw,140px); font-weight:800; line-height:.7; }
.pause-copy p { margin:0; font-size:clamp(28px,4.2vw,58px); font-weight:700; line-height:1.32; }

/* ============ CTA ============ */
.home-cta { padding:clamp(76px,11vw,140px) 0; }
.cta-grid { display:grid; grid-template-columns:.5fr 1.3fr .65fr; gap:clamp(24px,6vw,86px); align-items:end; }
.home-cta .section-index { color:var(--gb-primary); }
.cta-grid h2 { margin:0; color:var(--gb-text); font-size:clamp(34px,4.4vw,62px); font-weight:800; letter-spacing:-.02em; line-height:1.18; }
.cta-grid>div { display:grid; gap:12px; }
.cta-link {
  display:inline-flex; align-items:center; justify-content:center; gap:10px;
  min-height:50px; padding:0 28px; border-radius:999px;
  color:#fff; background:var(--gb-primary);
  font-size:15px; font-weight:750; text-decoration:none;
  box-shadow:0 14px 30px rgba(77,107,254,.28);
  transition:background-color .22s ease,transform .22s ease,box-shadow .22s ease;
}
.cta-link:hover { background:var(--gb-primary-dark); box-shadow:0 18px 38px rgba(77,107,254,.36); transform:translateY(-2px); }
.cta-link span { font-size:17px; transition:transform .3s ease; }
.cta-link:hover span { transform:translateX(4px); }
.cta-link.quiet { color:var(--gb-primary-dark); background:var(--gb-surface); border:1px solid var(--gb-border-strong); box-shadow:none; }
.cta-link.quiet:hover { color:var(--gb-primary-dark); background:var(--gb-primary-soft); box-shadow:none; }

/* ============ Footer ============ */
.home-footer { padding:38px 0; border-top:1px solid var(--gb-border); }
.home-footer>.home-inner { display:grid; grid-template-columns:1fr auto auto; gap:22px; align-items:center; color:var(--gb-subtle); font-size:12px; }
.footer-brand { color:var(--gb-text); font-size:21px; font-weight:800; letter-spacing:-.01em; }

/* ============ Scroll reveal ============ */
.reveal { opacity:0; transform:translateY(24px); transition:opacity .72s ease,transform .72s cubic-bezier(.16,1,.3,1); }
.reveal.is-visible { opacity:1; transform:translateY(0); }

@media(max-width:820px){
  .home-inner { width:min(100% - 32px,1320px); }
  .home-hero,.hero-content { min-height:calc(100svh - 44px); }
  .hero-copy h1 { font-size:clamp(60px,18vw,110px); }
  .manifesto-grid,.cta-grid { grid-template-columns:1fr; gap:22px; }
  .directions-grid { grid-template-columns:1fr; }
  .direction,.direction:first-child { min-height:250px; }
  .direction:hover { transform:none; }
  .direction h2 { margin:34px 0 12px; }
  .pause-copy { gap:12px; }
  .home-footer>.home-inner { grid-template-columns:1fr; gap:10px; }
  .hero-meta { font-size:10px; }
  .hero-footer-row { align-items:flex-start; flex-direction:column; }
  .hero-content { transform:none; }
  .sakura-petal:nth-child(n+6) { display:none; }
  .sakura-petal:nth-child(1) { left:63%; }
  .sakura-petal:nth-child(2) { left:78%; }
  .sakura-petal:nth-child(4) { left:74%; }
}
@media(prefers-reduced-motion:reduce){
  .hero-image,.hero-content,.reveal,.hero-actions .el-icon,.direction,.direction a b,.cta-link,.cta-link span { transition:none; }
  .hero-image,.ready .hero-image { transform:scale(1); }
  .reveal { opacity:1; transform:none; }
  .hero-scroll .el-icon { animation:none; }
  .ready .sakura-petal { animation:none; opacity:.5; transform:rotate(18deg); }
}
</style>
