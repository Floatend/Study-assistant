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
        <article><p>02 / TRACE</p><h2>让课程、项目和阶段学习互相连接。</h2><span>时间线按照真实发生的顺序整理经历，让结果之外的路径也能被看见。</span><RouterLink to="/journey">查看履历时间线 <b>→</b></RouterLink></article>
        <article><p>03 / PUBLISH</p><h2>只有主动发布的内容，才属于公开网站。</h2><span>公开知识库只读取同时标记为“已整理”和“官网发布”的文章，后台草稿与账户信息不会成为公开素材。</span><RouterLink to="/login">进入站长工作台 <b>↗</b></RouterLink></article>
      </section>
      <section class="about-closing"><span>linge.xin</span><p>Make room for the work that matters.</p></section>
    </div>
  </main>
</template>

<script setup lang="ts">
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
</script>

<style scoped>
.about-page { min-height: 100vh; color: var(--text); background: transparent; }
.about-shell { width: min(100% - 32px, 1180px); margin-inline: auto; padding-block: var(--space-3) 0; }
.about-hero { display: flex; flex-direction: column; gap: var(--space-5); padding-block: var(--space-8); border-bottom: 1px solid var(--line); }
.about-hero>p, .about-principles article>p { color: var(--brand); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.about-hero h1 { color: var(--text); font-size: 46px; line-height: 1.12; }
.about-hero>span { max-width: 34ch; color: var(--muted); font-size: 15px; line-height: 1.6; }
.about-principles { display: flex; flex-direction: column; padding-block: var(--space-6) var(--space-8); }
.about-principles article {
  display: grid;
  grid-template-columns: 44px 1fr;
  gap: var(--space-4);
  padding-block: var(--space-6);
  border-bottom: 1px solid var(--line);
  transition: padding .26s ease, background-color .26s ease;
}
.about-principles article:hover { padding-inline: var(--space-4); background: var(--surface-soft); }
.about-principles article>p { grid-row: 1 / span 3; }
.about-principles h2 { color: var(--text); font-size: 27px; line-height: 1.35; }
.about-principles article>span { color: var(--muted); font-size: 14px; line-height: 1.6; }
.about-principles a { display: inline-flex; align-items: center; gap: var(--space-2); justify-self: start; color: var(--brand-strong); font-size: 14px; font-weight: 750; text-decoration: none; }
.about-principles a b { transition: transform .24s ease; }
.about-principles a:hover b { transform: translateX(5px); }
.about-closing {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding: var(--space-8) var(--space-5) var(--space-6);
  color: var(--on-brand);
  background: var(--brand);
}
.about-closing span { font-family: var(--font-display); font-size: 58px; font-weight: 700; line-height: .9; }
.about-closing p { color: color-mix(in srgb, var(--surface) 74%, transparent); font-size: 14px; font-weight: 700; }

@media (min-width: 760px) {
  .about-shell { width: min(100% - 64px, 1180px); }
  .about-hero { display: grid; grid-template-columns: .5fr 1.25fr .65fr; gap: var(--space-7); align-items: start; padding-block: 112px; }
  .about-hero h1 { font-size: 68px; }
  .about-principles article { grid-template-columns: .45fr 1.25fr .65fr; gap: var(--space-7); align-items: start; padding-block: var(--space-7); }
  .about-principles article>p { grid-row: auto; }
  .about-principles a { grid-column: 2; }
  .about-closing { flex-direction: row; align-items: flex-end; justify-content: space-between; padding: 104px var(--space-7) var(--space-6); }
  .about-closing span { font-size: 104px; }
}
</style>
