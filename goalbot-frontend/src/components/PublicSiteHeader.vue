<template>
  <header class="site-header liquid-glass liquid-glass-strong">
    <RouterLink class="site-wordmark" to="/">linge.xin</RouterLink>
    <nav class="site-nav" aria-label="网站导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/notes">学习笔记</RouterLink>
      <RouterLink to="/journey">时间线</RouterLink>
      <RouterLink to="/about">关于</RouterLink>
      <RouterLink class="site-workspace-link liquid-glass liquid-glass-control" to="/login">站长入口 <span aria-hidden="true">↗</span></RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: var(--space-3);
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-lg);
  color: var(--text);
}
.site-wordmark {
  align-self: flex-start;
  color: var(--brand-strong);
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  text-decoration: none;
}
.site-nav {
  display: flex;
  width: 100%;
  justify-content: space-between;
  gap: 0;
  overflow: visible;
  scrollbar-width: none;
}
.site-nav::-webkit-scrollbar { display: none; }
.site-nav a {
  flex: 0 0 auto;
  padding: 7px;
  border-radius: 0;
  color: var(--muted);
  box-shadow: inset 0 -2px 0 transparent;
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
  transition: color .22s ease, box-shadow .22s ease;
}
.site-nav a:not(.site-workspace-link):hover,
.site-nav a:not(.site-workspace-link).router-link-exact-active {
  color: var(--brand-strong);
  background: transparent;
  box-shadow: inset 0 -2px 0 var(--brand);
}
.site-nav .site-workspace-link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  border-radius: 999px;
  color: var(--brand-strong);
}
.site-nav .site-workspace-link:hover { border-color: var(--brand); }
.site-workspace-link span { transition: transform .25s ease; }
.site-workspace-link:hover span { transform: translate(2px,-2px); }

@media (max-width: 359px) {
  .site-nav a { padding-inline: 4px; font-size: 12px; }
  .site-workspace-link span { display: none; }
}

@media (min-width: 760px) {
  .site-header { min-height: 64px; flex-direction: row; align-items: center; justify-content: space-between; padding: 10px 14px 10px 20px; }
  .site-nav { width: auto; align-items: center; justify-content: flex-start; gap: var(--space-1); }
  .site-nav a { padding: 7px 10px; font-size: 14px; }
}
</style>
