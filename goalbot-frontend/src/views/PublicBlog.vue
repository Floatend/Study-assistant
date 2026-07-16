<template>
  <main class="public-home" :class="{ ready: ready }">
    <div class="home-inner"><PublicSiteHeader /></div>

    <section class="home-hero">
      <img class="hero-image" :src="workspaceHero" alt="窗边的个人学习与创作工作台" />
      <div class="hero-shade" aria-hidden="true" />
      <div class="home-inner hero-content">
        <div class="hero-meta"><span>PERSONAL STUDY SYSTEM</span><span>EST. 2026</span></div>
        <div class="hero-copy">
          <p>在学习、记录与构建之间。</p>
          <h1>linge.xin</h1>
          <div class="hero-lines"><span>留出时间，完成重要的事。</span><span>留下过程，成为更清楚的自己。</span></div>
          <div class="hero-actions">
            <RouterLink to="/notes">进入学习笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
            <RouterLink to="/about">认识这个站点 <el-icon><ArrowRight /></el-icon></RouterLink>
          </div>
        </div>
        <p class="hero-foot">A personal place for notes, systems, and deliberate days.</p>
      </div>
    </section>

    <section class="manifesto-band" ref="manifestoRef">
      <div class="home-inner manifesto-grid reveal">
        <p class="section-index">01 / WHY THIS EXISTS</p>
        <h2>不急着填满每一天，<br />先让每一件事有自己的位置。</h2>
        <div><p>这里展示站长经过整理的学习成果、项目记录和阶段思考。它不是任务清单的镜像，而是那些值得被重新阅读的过程。</p><RouterLink to="/about">关于 linge.xin <span>→</span></RouterLink></div>
      </div>
    </section>

    <section class="directions-band">
      <div class="home-inner directions-head"><p class="section-index">02 / THREE DIRECTIONS</p><span>把输入、行动与复盘连接起来。</span></div>
      <div class="home-inner directions-grid">
        <article class="direction direction-notes reveal"><p>01</p><h2>学习笔记</h2><span>把课程、技术与思考从一次性输入整理成可以回访的知识页。</span><RouterLink to="/notes">浏览知识库 <b>→</b></RouterLink></article>
        <article class="direction direction-system reveal"><p>02</p><h2>GoalBot</h2><span>让自然语言对话、日程、任务和复盘最终落到真实可执行的时间表。</span><RouterLink to="/login">进入工作台 <b>→</b></RouterLink></article>
        <article class="direction direction-lab reveal"><p>03</p><h2>持续构建</h2><span>给每一次试错保留痕迹，在小系统和长期项目中练习判断与创造。</span><RouterLink to="/about">了解方法 <b>→</b></RouterLink></article>
      </div>
    </section>

    <section class="pause-band"><div class="home-inner pause-copy reveal"><span>“</span><p>真正的系统不是把生活压缩得更满，<br />而是让重要的部分有机会发生。</p></div></section>

    <section class="home-cta"><div class="home-inner cta-grid reveal"><p class="section-index">03 / START HERE</p><h2>从一篇笔记，<br />或一段更好的安排开始。</h2><div><RouterLink to="/notes" class="cta-link">阅读站长笔记 <span>→</span></RouterLink><RouterLink to="/login" class="cta-link quiet">登录 GoalBot <span>↗</span></RouterLink></div></div></section>

    <footer class="home-footer"><div class="home-inner"><span class="footer-brand">linge.xin</span><span>Write slowly. Build deliberately.</span><span>© 2026</span></div></footer>
  </main>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import workspaceHero from '@/assets/linge-workspace-hero.png'

const ready = ref(false)
const manifestoRef = ref<HTMLElement>()
let observer: IntersectionObserver | undefined

onMounted(async () => {
  await nextTick()
  ready.value = true
  observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => { if (entry.isIntersecting) entry.target.classList.add('is-visible') })
  }, { threshold: .14 })
  document.querySelectorAll('.reveal').forEach((element) => observer?.observe(element))
})
onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.public-home{overflow:hidden;color:#1f2721;background:#f4f0e7}.home-inner{width:min(1320px,calc(100% - 8vw));margin:0 auto}.home-hero{position:relative;min-height:calc(100svh - 56px);overflow:hidden;background:#203d33}.hero-image{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center;transform:scale(1.035);transition:transform 1.8s cubic-bezier(.16,1,.3,1)}.ready .hero-image{transform:scale(1)}.hero-shade{position:absolute;inset:0;background:rgba(17,28,22,.46)}.hero-content{position:relative;display:flex;min-height:calc(100svh - 56px);flex-direction:column;justify-content:space-between;padding:28px 0 34px;color:#f7f3e9}.hero-meta{display:flex;justify-content:space-between;color:rgba(247,243,233,.78);font-size:11px;font-weight:700;letter-spacing:.12em}.hero-copy{max-width:780px;margin:auto 0}.hero-copy>p{margin:0 0 8px;font-size:clamp(17px,2vw,26px);line-height:1.45}.hero-copy h1{margin:0;color:#fffdf7;font-family:Georgia,"Times New Roman","PingFang SC",serif;font-size:clamp(64px,12vw,174px);font-weight:600;letter-spacing:0;line-height:.84}.hero-lines{display:grid;gap:4px;margin-top:31px;color:#f7f3e9;font-size:clamp(17px,2vw,25px);line-height:1.48}.hero-actions{display:flex;flex-wrap:wrap;gap:24px;margin-top:32px}.hero-actions a{display:inline-flex;align-items:center;gap:7px;border-bottom:1px solid rgba(255,255,255,.74);padding-bottom:5px;color:#fffdf7;font-size:14px;font-weight:700;text-decoration:none}.hero-actions .el-icon{transition:transform .3s ease}.hero-actions a:hover .el-icon{transform:translateX(4px)}.hero-foot{margin:0;color:rgba(247,243,233,.78);font-family:Georgia,"Times New Roman",serif;font-size:13px;font-style:italic}.manifesto-band{padding:clamp(78px,13vw,168px) 0;background:#f4f0e7}.manifesto-grid{display:grid;grid-template-columns:.5fr 1.35fr .8fr;gap:clamp(24px,6vw,86px);align-items:start}.section-index{margin:0;color:#a84836;font-size:11px;font-weight:800;letter-spacing:.13em}.manifesto-grid h2{margin:0;color:#1b2720;font-size:clamp(34px,4vw,60px);font-weight:600;line-height:1.2}.manifesto-grid div>p{margin:0;color:#667168;font-size:15px;line-height:1.9}.manifesto-grid a{display:inline-flex;gap:7px;margin-top:25px;color:#275d50;font-size:14px;font-weight:700;text-decoration:none}.manifesto-grid a span{transition:transform .3s ease}.manifesto-grid a:hover span{transform:translateX(4px)}.directions-band{padding:0 0 clamp(76px,10vw,142px);background:#f4f0e7}.directions-head{display:flex;align-items:end;justify-content:space-between;gap:20px;padding-bottom:20px;border-bottom:1px solid #d8d3c9}.directions-head>span{color:#738077;font-size:14px}.directions-grid{display:grid;grid-template-columns:repeat(3,1fr)}.direction{display:flex;min-height:360px;flex-direction:column;padding:31px clamp(20px,3vw,46px) 35px;border-right:1px solid #d8d3c9}.direction:first-child{border-left:1px solid #d8d3c9}.direction>p{margin:0;color:#9b4233;font-family:Georgia,"Times New Roman",serif;font-size:18px}.direction h2{margin:64px 0 13px;color:#1d2922;font-size:clamp(25px,2.7vw,39px);font-weight:600}.direction>span{max-width:270px;color:#6a756d;font-size:14px;line-height:1.82}.direction a{display:flex;align-items:center;justify-content:space-between;margin-top:auto;color:#1e5f50;font-size:14px;font-weight:700;text-decoration:none}.direction a b{font-size:18px;transition:transform .3s ease}.direction a:hover b{transform:translateX(5px)}.direction-system{background:#e5ece5}.direction-lab{background:#dce8e7}.pause-band{padding:clamp(82px,15vw,205px) 0;background:#b64d3c;color:#fff9ef}.pause-copy{display:flex;gap:clamp(22px,5vw,75px);align-items:start}.pause-copy span{font-family:Georgia,"Times New Roman",serif;font-size:clamp(70px,12vw,150px);line-height:.7}.pause-copy p{margin:0;font-size:clamp(28px,4.3vw,62px);font-weight:500;line-height:1.32}.home-cta{padding:clamp(74px,11vw,145px) 0;background:#18382f;color:#f8f4e9}.cta-grid{display:grid;grid-template-columns:.5fr 1.3fr .65fr;gap:clamp(24px,6vw,86px);align-items:end}.home-cta .section-index{color:#d9a292}.cta-grid h2{margin:0;font-size:clamp(34px,4.4vw,64px);font-weight:500;line-height:1.18}.cta-grid>div{display:grid;gap:14px}.cta-link{display:inline-flex;align-items:center;justify-content:space-between;gap:18px;padding-bottom:10px;border-bottom:1px solid rgba(248,244,233,.36);color:#fffaf0;font-size:15px;font-weight:700;text-decoration:none}.cta-link span{font-size:20px;transition:transform .3s ease}.cta-link:hover span{transform:translateX(5px)}.cta-link.quiet{color:#c9d7cf}.home-footer{padding:37px 0;background:#f4f0e7}.home-footer>.home-inner{display:grid;grid-template-columns:1fr auto auto;gap:22px;color:#6d786f;font-size:12px}.footer-brand{color:#23352a;font-family:Georgia,"Times New Roman",serif;font-size:22px;font-weight:700}.reveal{opacity:0;transform:translateY(24px);transition:opacity .72s ease,transform .72s cubic-bezier(.16,1,.3,1)}.reveal.is-visible{opacity:1;transform:translateY(0)}@media(max-width:820px){.home-inner{width:min(100% - 32px,1320px)}.home-hero,.hero-content{min-height:calc(100svh - 44px)}.hero-copy h1{font-size:clamp(60px,18vw,110px)}.manifesto-grid,.cta-grid{grid-template-columns:1fr;gap:22px}.directions-grid{grid-template-columns:1fr}.direction,.direction:first-child{min-height:260px;border-right:0;border-left:0;border-bottom:1px solid #d8d3c9}.directions-grid .direction:first-child{border-top:1px solid #d8d3c9}.direction h2{margin:34px 0 12px}.pause-copy{gap:12px}.home-footer>.home-inner{grid-template-columns:1fr;gap:10px}.hero-meta{font-size:10px}.hero-foot{max-width:240px;line-height:1.45}}@media(prefers-reduced-motion:reduce){.hero-image,.reveal,.hero-actions .el-icon,.direction a b,.cta-link span{transition:none}.reveal{opacity:1;transform:none}}
</style>
