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
.public-home{overflow:hidden;color:#0b3340;background:#fff7e7}.home-inner{width:min(1320px,calc(100% - 8vw));margin:0 auto}.home-hero{position:relative;min-height:calc(100svh - 56px);overflow:hidden;background:#075866}.hero-image{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center;transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.055);transition:transform 1.35s cubic-bezier(.16,1,.3,1)}.ready .hero-image{transform:translate3d(var(--hero-x),var(--hero-y),0) scale(1.012)}.hero-shade{position:absolute;inset:0;background:rgba(0,47,59,.38)}.hero-content{position:relative;display:flex;min-height:calc(100svh - 56px);flex-direction:column;justify-content:space-between;padding:28px 0 34px;color:#fffdf5;transform:translate3d(var(--hero-copy-x),var(--hero-copy-y),0);transition:transform .55s ease-out}.hero-meta{display:flex;justify-content:space-between;color:#d7fff0;font-size:11px;font-weight:700;letter-spacing:.1em}.hero-copy{max-width:780px;margin:auto 0}.hero-copy>p{margin:0 0 8px;color:#f2ffb6;font-size:clamp(17px,2vw,26px);font-weight:600;line-height:1.45}.hero-copy h1{margin:0;color:#fffdf5;font-size:clamp(64px,12vw,174px);font-weight:800;letter-spacing:0;line-height:.84}.hero-lines{display:grid;gap:4px;margin-top:31px;color:#fffdf5;font-size:clamp(17px,2vw,25px);line-height:1.48}.hero-actions{display:flex;flex-wrap:wrap;gap:24px;margin-top:32px}.hero-actions a{display:inline-flex;align-items:center;gap:7px;border-bottom:2px solid #f4ff95;padding-bottom:5px;color:#fffdf5;font-size:14px;font-weight:700;text-decoration:none}.hero-actions .el-icon{transition:transform .3s ease}.hero-actions a:hover .el-icon{transform:translateX(5px)}.hero-footer-row{display:flex;align-items:center;justify-content:space-between;gap:16px}.hero-footer-row p{margin:0;color:#d7fff0;font-size:13px}.hero-scroll{display:inline-flex;align-items:center;gap:7px;padding:0;border:0;color:#f4ff95;background:transparent;font-family:inherit;font-size:13px;font-weight:700;cursor:pointer}.hero-scroll .el-icon{font-size:16px;animation:scroll-bounce 1.8s ease-in-out infinite}@keyframes scroll-bounce{0%,to{transform:translateY(0)}50%{transform:translateY(4px)}}.manifesto-band{padding:clamp(78px,13vw,168px) 0;background:#fff7e7}.manifesto-grid{display:grid;grid-template-columns:.5fr 1.35fr .8fr;gap:clamp(24px,6vw,86px);align-items:start}.section-index{margin:0;color:#f15843;font-size:11px;font-weight:800;letter-spacing:.12em}.manifesto-grid h2{margin:0;color:#0b3340;font-size:clamp(34px,4vw,60px);font-weight:800;line-height:1.2}.manifesto-grid div>p{margin:0;color:#42636a;font-size:15px;line-height:1.9}.manifesto-grid a{display:inline-flex;gap:7px;margin-top:25px;color:#007d7a;font-size:14px;font-weight:800;text-decoration:none}.manifesto-grid a span{transition:transform .3s ease}.manifesto-grid a:hover span{transform:translateX(4px)}.directions-band{padding:0 0 clamp(76px,10vw,142px);background:#fff7e7}.directions-head{display:flex;align-items:end;justify-content:space-between;gap:20px;padding-bottom:20px;border-bottom:1px solid #f0cda5}.directions-head>span{color:#557078;font-size:14px}.directions-grid{display:grid;grid-template-columns:repeat(3,1fr)}.direction{display:flex;min-height:360px;flex-direction:column;padding:31px clamp(20px,3vw,46px) 35px;border-right:1px solid rgba(11,51,64,.16);transition:transform .34s cubic-bezier(.16,1,.3,1),filter .34s ease}.direction:first-child{border-left:1px solid rgba(11,51,64,.16)}.direction:hover{position:relative;z-index:1;transform:translateY(-12px);filter:saturate(1.08)}.direction>p{margin:0;color:#f15843;font-size:18px;font-weight:800}.direction h2{margin:64px 0 13px;color:#0b3340;font-size:clamp(25px,2.7vw,39px);font-weight:800}.direction>span{max-width:270px;color:#285563;font-size:14px;line-height:1.82}.direction a{display:flex;align-items:center;justify-content:space-between;margin-top:auto;color:#006b72;font-size:14px;font-weight:800;text-decoration:none}.direction a b{font-size:18px;transition:transform .3s ease}.direction a:hover b{transform:translateX(6px)}.direction-notes{background:#ffe980}.direction-system{background:#8ee8dc}.direction-lab{background:#ffb59d}.pause-band{padding:clamp(82px,15vw,205px) 0;background:#ff604d;color:#fff9eb}.pause-copy{display:flex;gap:clamp(22px,5vw,75px);align-items:start}.pause-copy span{font-size:clamp(70px,12vw,150px);font-weight:800;line-height:.7}.pause-copy p{margin:0;font-size:clamp(28px,4.3vw,62px);font-weight:700;line-height:1.32}.home-cta{padding:clamp(74px,11vw,145px) 0;background:#064a63;color:#fffdf5}.cta-grid{display:grid;grid-template-columns:.5fr 1.3fr .65fr;gap:clamp(24px,6vw,86px);align-items:end}.home-cta .section-index{color:#f5ff9a}.cta-grid h2{margin:0;font-size:clamp(34px,4.4vw,64px);font-weight:800;line-height:1.18}.cta-grid>div{display:grid;gap:14px}.cta-link{display:inline-flex;align-items:center;justify-content:space-between;gap:18px;padding-bottom:10px;border-bottom:2px solid rgba(255,253,245,.48);color:#fffdf5;font-size:15px;font-weight:800;text-decoration:none}.cta-link span{font-size:20px;transition:transform .3s ease}.cta-link:hover span{transform:translateX(5px)}.cta-link.quiet{color:#f5ff9a}.home-footer{padding:37px 0;background:#fff7e7}.home-footer>.home-inner{display:grid;grid-template-columns:1fr auto auto;gap:22px;color:#567078;font-size:12px}.footer-brand{color:#064a63;font-size:22px;font-weight:800}.reveal{opacity:0;transform:translateY(24px);transition:opacity .72s ease,transform .72s cubic-bezier(.16,1,.3,1)}.reveal.is-visible{opacity:1;transform:translateY(0)}@media(max-width:820px){.home-inner{width:min(100% - 32px,1320px)}.home-hero,.hero-content{min-height:calc(100svh - 44px)}.hero-copy h1{font-size:clamp(60px,18vw,110px)}.manifesto-grid,.cta-grid{grid-template-columns:1fr;gap:22px}.directions-grid{grid-template-columns:1fr}.direction,.direction:first-child{min-height:260px;border-right:0;border-left:0;border-bottom:1px solid rgba(11,51,64,.18)}.directions-grid .direction:first-child{border-top:1px solid rgba(11,51,64,.18)}.direction:hover{transform:none}.direction h2{margin:34px 0 12px}.pause-copy{gap:12px}.home-footer>.home-inner{grid-template-columns:1fr;gap:10px}.hero-meta{font-size:10px}.hero-footer-row{align-items:flex-start;flex-direction:column}.hero-content{transform:none}}@media(prefers-reduced-motion:reduce){.hero-image,.hero-content,.reveal,.hero-actions .el-icon,.direction,.direction a b,.cta-link span{transition:none}.hero-image,.ready .hero-image{transform:scale(1)}.reveal{opacity:1;transform:none}.hero-scroll .el-icon{animation:none}}
/* Cool blue editorial theme, inspired by modern AI products without copying a brand. */
.public-home,.manifesto-band,.directions-band,.home-footer{color:#1f2a44;background:#f7f9ff}.home-hero{background:#4d6bfe}.hero-shade{background:rgba(18,38,128,.34)}.hero-content,.hero-copy h1,.hero-lines,.hero-actions a,.pause-band,.home-cta,.cta-link{color:#fff}.hero-meta,.hero-footer-row p{color:#dce4ff}.hero-copy>p,.hero-scroll{color:#baf4e9}.hero-actions a{border-bottom-color:#baf4e9}.hero-scroll{transition:color .2s ease}.hero-scroll:hover{color:#fff}.section-index,.direction>p{color:#4d6bfe}.manifesto-grid h2,.direction h2{color:#1f2a44}.manifesto-grid div>p,.directions-head>span{color:#65708c}.manifesto-grid a,.direction a{color:#3559e8}.directions-head{border-bottom-color:#dfe5f5}.direction,.direction:first-child{border-color:rgba(44,67,126,.16)}.direction>span{color:#50617e}.direction-notes{background:#eef2ff}.direction-system{background:#e6f8f5}.direction-lab{background:#fff0e5}.pause-band{background:#4d6bfe}.home-cta{background:#1c318e}.home-cta .section-index,.cta-link.quiet{color:#baf4e9}.cta-link{border-bottom-color:rgba(255,255,255,.45)}.home-footer>.home-inner{color:#71809b}.footer-brand{color:#3559e8}@media(max-width:820px){.direction,.direction:first-child{border-color:rgba(44,67,126,.16)}}

.hero-shade {
  z-index: 0;
  background: linear-gradient(90deg, rgba(18, 35, 112, .79) 0%, rgba(28, 49, 142, .58) 44%, rgba(27, 48, 117, .26) 75%, rgba(15, 29, 77, .38) 100%);
}

.hero-content { z-index: 2; }

.sakura-petals {
  position: absolute;
  z-index: 1;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.sakura-petal {
  position: absolute;
  width: 14px;
  height: 10px;
  opacity: 0;
  border-radius: 90% 10% 85% 15%;
  background: linear-gradient(135deg, #fffafc 0%, #ffd5e1 54%, #f3a4bc 100%);
  box-shadow: 0 3px 9px rgba(108, 30, 65, .18);
  transform: translate3d(0, -28px, 0) rotate(-20deg);
}

.sakura-petal:nth-child(1) { top: 15%; left: 59%; }
.sakura-petal:nth-child(2) { top: 29%; left: 74%; width: 11px; height: 8px; }
.sakura-petal:nth-child(3) { top: 13%; left: 86%; width: 16px; height: 11px; }
.sakura-petal:nth-child(4) { top: 45%; left: 69%; width: 10px; height: 7px; }
.sakura-petal:nth-child(5) { top: 57%; left: 88%; }
.sakura-petal:nth-child(6) { top: 21%; left: 94%; width: 9px; height: 7px; }
.sakura-petal:nth-child(7) { top: 36%; left: 81%; width: 12px; height: 9px; }
.sakura-petal:nth-child(8) { top: 62%; left: 77%; width: 9px; height: 7px; }

.ready .sakura-petal { animation: sakura-arrive 1.15s cubic-bezier(.16, 1, .3, 1) both; }
.ready .sakura-petal:nth-child(2) { animation-delay: .08s; }
.ready .sakura-petal:nth-child(3) { animation-delay: .16s; }
.ready .sakura-petal:nth-child(4) { animation-delay: .24s; }
.ready .sakura-petal:nth-child(5) { animation-delay: .32s; }
.ready .sakura-petal:nth-child(6) { animation-delay: .4s; }
.ready .sakura-petal:nth-child(7) { animation-delay: .48s; }
.ready .sakura-petal:nth-child(8) { animation-delay: .56s; }

@keyframes sakura-arrive {
  from { opacity: 0; transform: translate3d(-24px, -42px, 0) rotate(-44deg) scale(.72); }
  72% { opacity: .82; }
  to { opacity: .58; transform: translate3d(0, 0, 0) rotate(18deg) scale(1); }
}

@media (max-width: 820px) {
  .sakura-petal:nth-child(n + 6) { display: none; }
  .sakura-petal:nth-child(1) { left: 63%; }
  .sakura-petal:nth-child(2) { left: 78%; }
  .sakura-petal:nth-child(4) { left: 74%; }
}

@media (prefers-reduced-motion: reduce) {
  .ready .sakura-petal { animation: none; opacity: .5; transform: rotate(18deg); }
}
</style>
