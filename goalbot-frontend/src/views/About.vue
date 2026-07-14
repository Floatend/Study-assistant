<template>
  <main class="about-page">
    <div class="about-shell">
      <PublicSiteHeader />

      <section class="about-intro">
        <p>ABOUT</p>
        <h1>关于 linge.xin</h1>
        <span>一处公开记录的地方，也是一套持续在真实生活里运行的个人系统。</span>
      </section>

      <section class="about-lines">
        <article>
          <p>记录</p>
          <h2>把值得回看的过程留下。</h2>
          <span>官方笔记用于沉淀学习、项目和阶段性的想法。它们经过筛选后才会对外展示。</span>
          <RouterLink to="/notes">进入官方笔记</RouterLink>
        </article>
        <article>
          <p>系统</p>
          <h2>让对话落到一张真实的日程表上。</h2>
          <span>GoalBot 将任务、日历、复盘、飞书和 AI 建议放在同一个私人工作区，服务于实际安排而不是展示。</span>
          <RouterLink to="/login">登录 GoalBot</RouterLink>
        </article>
        <article>
          <p>原则</p>
          <h2>公开的归公开，私人的归私人。</h2>
          <span>个人任务、对话、复盘和未发布笔记不属于这个网站的公开内容。每篇官方笔记都需要管理员主动确认。</span>
        </article>
      </section>
    </div>
  </main>
</template>

<script setup lang="ts">
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  color: #28372f;
  background: #f3f4ee;
}

.about-shell {
  width: min(1080px, calc(100% - 9vw));
  margin: 0 auto;
}

.about-intro {
  padding: 88px 0 52px;
  border-bottom: 1px solid #dce1da;
  animation: about-arrival 0.68s ease-out both;
}

.about-intro p,
.about-lines article > p {
  margin: 0;
  color: #4d7b67;
  font-size: 12px;
  font-weight: 700;
}

.about-intro h1 {
  margin: 14px 0 10px;
  font-size: 42px;
  font-weight: 650;
}

.about-intro span {
  color: #6b766e;
  font-size: 16px;
  line-height: 1.8;
}

.about-lines article {
  display: grid;
  grid-template-columns: 144px minmax(0, 1fr) minmax(220px, 0.66fr);
  align-items: start;
  gap: 32px;
  padding: 38px 0;
  border-bottom: 1px solid #dce1da;
  animation: about-arrival 0.68s 0.12s ease-out both;
}

.about-lines h2,
.about-lines span {
  margin: 0;
}

.about-lines h2 {
  color: #29382f;
  font-size: 23px;
  font-weight: 650;
  line-height: 1.48;
}

.about-lines span {
  color: #6b766e;
  font-size: 14px;
  line-height: 1.85;
}

.about-lines a {
  grid-column: 2;
  color: #2e6c58;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}

.about-lines a:hover {
  text-decoration: underline;
}

@keyframes about-arrival {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 760px) {
  .about-shell { width: min(100% - 32px, 1080px); }
  .about-intro { padding: 52px 0 34px; }
  .about-intro h1 { font-size: 33px; }
  .about-lines article { grid-template-columns: 1fr; gap: 15px; padding: 31px 0; }
  .about-lines a { grid-column: auto; }
}

@media (prefers-reduced-motion: reduce) {
  .about-intro,
  .about-lines article { animation: none; }
}
</style>
