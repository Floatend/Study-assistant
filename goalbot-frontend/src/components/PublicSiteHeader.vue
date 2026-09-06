<template>
  <header class="site-header liquid-glass liquid-glass-strong">
    <RouterLink class="site-wordmark" to="/">linge.xin</RouterLink>
    <nav class="site-nav" aria-label="网站导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/notes">学习笔记</RouterLink>
      <RouterLink to="/projects" :class="{ 'is-current': route.path.startsWith('/projects/') }" :aria-current="route.path.startsWith('/projects/') ? 'location' : undefined">项目</RouterLink>
      <RouterLink to="/journey">时间线</RouterLink>
      <RouterLink to="/about">关于</RouterLink>
    </nav>
    <RouterLink class="site-workspace-link liquid-glass liquid-glass-control" to="/login">站长入口 <span aria-hidden="true">↗</span></RouterLink>
  </header>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<style scoped>
.site-header {
  position: sticky;
  top: var(--space-3);
  z-index: 30;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2) var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  color: var(--text);
}
.site-wordmark {
  color: var(--brand-strong);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  text-decoration: none;
}
.site-nav {
  display: flex;
  order: 3;
  width: 100%;
  justify-content: space-between;
  gap: 0;
  overflow: visible;
  scrollbar-width: none;
}
.site-nav::-webkit-scrollbar { display: none; }
.site-nav a {
  flex: 0 0 auto;
  padding: 8px 7px;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  border-radius: 0;
  color: var(--muted);
  box-shadow: inset 0 -2px 0 transparent;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
  transition: color .22s ease, box-shadow .22s ease;
}
.site-nav a:not(.site-workspace-link):hover,
.site-nav a.is-current,
.site-nav a:not(.site-workspace-link).router-link-exact-active {
  color: var(--brand-strong);
  background: transparent;
  box-shadow: inset 0 -2px 0 var(--brand);
}
.site-workspace-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border-radius: 999px;
  color: var(--brand-strong);
  padding: 8px 12px;
  min-height: 44px;
  font-size: 14px;
  font-weight: 650;
  text-decoration: none;
}
.site-workspace-link:hover { border-color: var(--brand); }
.site-workspace-link span { transition: transform .25s ease; }
.site-workspace-link:hover span { transform: translate(2px,-2px); }

@media (max-width: 359px) {
  .site-nav a { padding-inline: 4px; }
  .site-workspace-link span { display: none; }
}

@media (min-width: 760px) {
  .site-header { min-height: 64px; flex-wrap: nowrap; padding: 10px 14px 10px 20px; }
  .site-nav { order: 0; width: auto; align-items: center; justify-content: flex-start; gap: var(--space-1); }
  .site-nav a { padding: 7px 10px; font-size: 14px; }
}
</style>
