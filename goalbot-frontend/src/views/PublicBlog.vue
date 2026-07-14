<template>
  <main class="personal-home">
    <div class="site-shell">
      <PublicSiteHeader />

      <section class="home-hero">
        <img class="hero-image" :src="workspaceHero" alt="窗边的个人学习与创作工作台" />
        <div class="hero-copy">
          <p class="hero-kicker">PERSONAL WEBSITE / 2026</p>
          <h1>linge.xin</h1>
          <p class="hero-statement">记录学习、构建与正在慢慢成为的自己。</p>
          <p class="hero-description">这里保留公开的文字，也连接一套真正参与日常安排的个人智能助手。</p>
          <div class="hero-links">
            <RouterLink to="/notes">阅读官方笔记 <el-icon><ArrowRight /></el-icon></RouterLink>
            <RouterLink to="/login">进入 GoalBot <el-icon><ArrowRight /></el-icon></RouterLink>
          </div>
        </div>
        <p class="hero-caption">A quiet place for ideas, systems, and the days in between.</p>
      </section>

      <section class="home-direction">
        <article class="direction-block writing-block">
          <p>01 / WRITING</p>
          <h2>把过程留下来。</h2>
          <span>公开的官方笔记收纳学习、项目与持续思考，不追逐更新频率，只留下值得回看的内容。</span>
          <RouterLink to="/notes">查看官方笔记</RouterLink>
        </article>
        <article class="direction-block system-block">
          <p>02 / SYSTEM</p>
          <h2>让计划真的发生。</h2>
          <span>GoalBot 是一套私人的目标、日程与飞书对话系统，让自然语言最终落到可执行的时间表里。</span>
          <RouterLink to="/login">登录个人工作台</RouterLink>
        </article>
      </section>

      <footer class="site-footer">
        <span>linge.xin</span>
        <span>Write slowly. Build deliberately.</span>
      </footer>
    </div>
  </main>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import workspaceHero from '@/assets/linge-workspace-hero.png'
</script>

<style scoped>
.personal-home {
  min-height: 100vh;
  color: #223029;
  background: #f3f4ee;
}

.site-shell {
  width: min(1420px, calc(100% - 9vw));
  margin: 0 auto;
}

.home-hero {
  position: relative;
  display: flex;
  align-items: center;
  min-height: min(680px, calc(100vh - 76px));
  overflow: hidden;
  border-bottom: 1px solid rgba(36, 47, 41, 0.16);
  background: #d7d5cb;
}

.hero-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  animation: image-arrival 1.2s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-copy {
  position: relative;
  z-index: 1;
  max-width: 510px;
  padding: 58px 7%;
  animation: copy-arrival 0.82s 0.18s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.hero-kicker,
.direction-block > p {
  margin: 0;
  color: #46715d;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 18px 0 14px;
  color: #203028;
  font-size: clamp(46px, 6.2vw, 84px);
  font-weight: 700;
  line-height: 0.98;
}

.hero-statement {
  margin: 0;
  color: #273a30;
  font-size: clamp(20px, 2.2vw, 30px);
  font-weight: 600;
  line-height: 1.45;
}

.hero-description {
  max-width: 400px;
  margin: 18px 0 0;
  color: #56645b;
  font-size: 15px;
  line-height: 1.85;
}

.hero-links {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 30px;
}

.hero-links a,
.direction-block a {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #285f4e;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}

.hero-links a:hover,
.direction-block a:hover {
  text-decoration: underline;
}

.hero-links .el-icon {
  font-size: 15px;
}

.hero-caption {
  position: absolute;
  right: 6%;
  bottom: 28px;
  z-index: 1;
  margin: 0;
  color: rgba(39, 52, 44, 0.68);
  font-size: 12px;
}

.home-direction {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.direction-block {
  display: grid;
  align-content: start;
  gap: 17px;
  min-height: 326px;
  padding: 46px 7%;
  animation: section-arrival 0.8s 0.34s ease-out both;
}

.writing-block {
  border-right: 1px solid rgba(36, 47, 41, 0.16);
}

.direction-block h2 {
  margin: 0;
  color: #26362e;
  font-size: 30px;
  font-weight: 650;
  line-height: 1.3;
}

.direction-block span {
  max-width: 410px;
  color: #68736c;
  font-size: 15px;
  line-height: 1.8;
}

.direction-block a {
  margin-top: auto;
}

.site-footer {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 0 32px;
  border-top: 1px solid rgba(36, 47, 41, 0.16);
  color: #7c857f;
  font-size: 12px;
}

@keyframes image-arrival {
  from { opacity: 0; transform: scale(1.025); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes copy-arrival {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes section-arrival {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 760px) {
  .site-shell {
    width: min(100% - 32px, 1420px);
  }

  .home-hero {
    align-items: flex-start;
    min-height: 610px;
  }

  .hero-image {
    object-position: 63% center;
  }

  .hero-copy {
    padding: 68px 8%;
  }

  .hero-copy h1 {
    font-size: 48px;
  }

  .hero-statement {
    max-width: 260px;
    font-size: 21px;
  }

  .hero-description {
    max-width: 270px;
    font-size: 14px;
  }

  .hero-caption {
    right: 8%;
    bottom: 20px;
    max-width: 240px;
    text-align: right;
  }

  .home-direction {
    grid-template-columns: 1fr;
  }

  .direction-block {
    min-height: 280px;
    padding: 36px 8%;
  }

  .writing-block {
    border-right: 0;
    border-bottom: 1px solid rgba(36, 47, 41, 0.16);
  }

  .direction-block h2 {
    font-size: 26px;
  }

  .site-footer {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-image,
  .hero-copy,
  .direction-block {
    animation: none;
  }
}
</style>
