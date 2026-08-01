<template>
  <main class="about-page">
    <div class="about-shell">
      <PublicSiteHeader />
      <section class="about-hero">
        <p>ABOUT THIS PLACE</p>
        <h1>记录并不只是<br />为了回忆。</h1>
        <span>它让模糊的想法变得可以讨论，让正在发生的日子有迹可循。</span>
      </section>
      <section class="about-principles">
        <article><p>01 / RECORD</p><h2>把值得回看的过程留下来。</h2><span>公开笔记只收录站长完成整理的学习、技术和项目实践。它不追逐更新频率，而是追求能在以后继续提供帮助。</span><RouterLink to="/notes">阅读学习笔记 <b>→</b></RouterLink></article>
        <article><p>02 / EDIT</p><h2>先写下来，再把它整理成可以阅读的文章。</h2><span>站长工作台负责导入、分类、编辑和预览 Markdown。内容在成为公开文章以前，始终只是私人草稿。</span><RouterLink to="/login">进入站长工作台 <b>↗</b></RouterLink></article>
        <article><p>03 / PUBLISH</p><h2>只有主动发布的内容，才属于公开网站。</h2><span>公开知识库只读取同时标记为“已整理”和“官网发布”的文章，后台草稿与账户信息不会成为公开素材。</span></article>
      </section>
      <section class="about-closing"><span>linge.xin</span><p>Make room for the work that matters.</p></section>
    </div>
  </main>
</template>

<script setup lang="ts">
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
</script>

<style scoped>
.about-page { min-height:100vh; color:var(--gb-text); background:var(--gb-bg); }
.about-shell { width:min(1180px,calc(100% - 8vw)); margin:0 auto; }

.about-hero {
  display:grid; grid-template-columns:.52fr 1.3fr .6fr; gap:30px; align-items:start;
  padding:clamp(72px,12vw,160px) 0 clamp(64px,9vw,120px);
  border-bottom:1px solid var(--gb-border);
}
.about-hero>p,.about-principles article>p { margin:0; color:var(--gb-primary); font-size:11px; font-weight:800; letter-spacing:.14em; }
.about-hero h1 { margin:0; color:var(--gb-text); font-size:clamp(44px,5.8vw,78px); font-weight:800; letter-spacing:-.02em; line-height:1.1; }
.about-hero>span { padding-top:12px; color:var(--gb-muted); font-size:15px; line-height:1.9; }

.about-principles { display:grid; gap:20px; padding:clamp(44px,7vw,84px) 0; }
.about-principles article {
  display:grid; grid-template-columns:.52fr 1.3fr .6fr; gap:30px;
  padding:40px 36px;
  border:1px solid var(--gb-border); border-radius:var(--gb-radius);
  transition:background-color .3s ease,transform .3s cubic-bezier(.16,1,.3,1),box-shadow .3s ease;
}
.about-principles article:nth-child(1) { background:linear-gradient(180deg,#eef2ff 0%,#e4eaff 100%); }
.about-principles article:nth-child(2) { background:linear-gradient(180deg,#e2f6f1 0%,#d2efe8 100%); }
.about-principles article:nth-child(3) { background:linear-gradient(180deg,#fdf0e7 0%,#fbe4d3 100%); }
.about-principles article:hover { position:relative; z-index:1; transform:translateX(10px); box-shadow:var(--gb-shadow-lg); }
.about-principles h2,.about-principles span { margin:0; }
.about-principles h2 { color:var(--gb-text); font-size:clamp(22px,2.2vw,30px); font-weight:800; letter-spacing:-.01em; line-height:1.42; }
.about-principles span { color:var(--gb-muted); font-size:14px; line-height:1.86; }
.about-principles a { grid-column:2; display:inline-flex; align-items:center; gap:8px; margin-top:4px; color:var(--gb-primary); font-size:14px; font-weight:800; text-decoration:none; }
.about-principles a b { font-size:18px; transition:transform .25s ease; }
.about-principles a:hover b { transform:translateX(5px); }

.about-closing {
  display:flex; align-items:end; justify-content:space-between; gap:20px;
  margin:0 -4vw; padding:clamp(66px,10vw,128px) 4vw 44px;
  background:linear-gradient(118deg,#131c4a 0%,#1b2a68 55%,#243580 100%);
}
.about-closing span { color:#fff; font-size:clamp(52px,10.5vw,132px); font-weight:800; letter-spacing:-.02em; line-height:.82; }
.about-closing p { margin:0; color:var(--gb-mint); font-size:14px; font-weight:700; }

@media(max-width:760px){
  .about-shell { width:min(100% - 32px,1180px); }
  .about-hero,.about-principles article { grid-template-columns:1fr; gap:19px; }
  .about-principles article { padding:31px 24px; }
  .about-principles article:hover { transform:none; }
  .about-principles a { grid-column:auto; }
  .about-closing { align-items:start; flex-direction:column; margin:0 -16px; padding-right:16px; padding-left:16px; }
  .about-closing span { font-size:64px; }
}
</style>
