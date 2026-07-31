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
.about-page{min-height:100vh;color:#0b3340;background:#fff7e7}.about-shell{width:min(1180px,calc(100% - 8vw));margin:0 auto}.about-hero{display:grid;grid-template-columns:.52fr 1.3fr .6fr;gap:30px;align-items:start;padding:clamp(72px,13vw,172px) 0 clamp(70px,10vw,132px);border-bottom:1px solid #f1cfa4}.about-hero>p,.about-principles article>p{margin:0;color:#f15843;font-size:11px;font-weight:800;letter-spacing:.12em}.about-hero h1{margin:0;color:#0a4053;font-size:clamp(46px,6.1vw,82px);font-weight:800;line-height:1.08}.about-hero>span{padding-top:11px;color:#496d76;font-size:15px;line-height:1.9}.about-principles article{display:grid;grid-template-columns:.52fr 1.3fr .6fr;gap:30px;padding:43px 34px;border-bottom:1px solid rgba(11,51,64,.16);transition:background-color .3s ease,transform .3s cubic-bezier(.16,1,.3,1)}.about-principles article:nth-child(1){background:#ffe980}.about-principles article:nth-child(2){background:#8ee8dc}.about-principles article:nth-child(3){background:#ffb59d}.about-principles article:hover{position:relative;z-index:1;transform:translateX(10px)}.about-principles h2,.about-principles span{margin:0}.about-principles h2{color:#0b4051;font-size:clamp(22px,2.2vw,31px);font-weight:800;line-height:1.42}.about-principles span{color:#245866;font-size:14px;line-height:1.86}.about-principles a{grid-column:2;display:inline-flex;align-items:center;gap:8px;margin-top:3px;color:#006e72;font-size:14px;font-weight:800;text-decoration:none}.about-principles a b{font-size:18px;transition:transform .25s ease}.about-principles a:hover b{transform:translateX(5px)}.about-closing{display:flex;align-items:end;justify-content:space-between;gap:20px;margin:0 -4vw;padding:clamp(70px,11vw,140px) 4vw 46px;background:#075866}.about-closing span{color:#fffdf5;font-size:clamp(54px,11vw,142px);font-weight:800;line-height:.8}.about-closing p{margin:0;color:#f4ff95;font-size:14px;font-weight:700}@media(max-width:760px){.about-shell{width:min(100% - 32px,1180px)}.about-hero,.about-principles article{grid-template-columns:1fr;gap:19px}.about-principles article{padding:31px 20px}.about-principles article:hover{transform:none}.about-principles a{grid-column:auto}.about-closing{align-items:start;flex-direction:column;margin:0 -16px;padding-right:16px;padding-left:16px}.about-closing span{font-size:64px}}
/* Cool blue editorial theme, inspired by modern AI products without copying a brand. */
.about-page{color:#1f2a44;background:#f7f9ff}.about-hero{border-bottom-color:#e3e8f5}.about-hero>p,.about-principles article>p{color:#4d6bfe}.about-hero h1,.about-principles h2{color:#1f2a44}.about-hero>span{color:#65708c}.about-principles article{border-bottom-color:rgba(44,67,126,.15)}.about-principles article:nth-child(1){background:#eef2ff}.about-principles article:nth-child(2){background:#e6f8f5}.about-principles article:nth-child(3){background:#fff0e5}.about-principles span{color:#50617e}.about-principles a{color:#3559e8}.about-closing{background:#1c318e}.about-closing span{color:#fff}.about-closing p{color:#baf4e9}
</style>
