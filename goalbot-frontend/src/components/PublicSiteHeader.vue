<template>
  <header class="site-header">
    <RouterLink class="site-wordmark" to="/">linge.xin</RouterLink>
    <nav class="site-nav" aria-label="网站导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/notes">学习笔记</RouterLink>
      <RouterLink to="/about">关于</RouterLink>
      <RouterLink class="site-workspace-link" to="/login">站长入口 <span aria-hidden="true">↗</span></RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.site-header { display:flex; align-items:center; justify-content:space-between; min-height:76px; border-bottom:1px solid var(--gb-border); color:var(--gb-text); }
.site-wordmark { color:inherit; font-size:20px; font-weight:800; letter-spacing:-.01em; text-decoration:none; }
.site-nav { display:flex; align-items:center; gap:4px; }
.site-nav a { position:relative; padding:8px 13px; border-radius:999px; color:var(--gb-muted); font-size:13px; font-weight:650; text-decoration:none; transition:color .22s ease,background-color .22s ease; }
.site-nav a:hover { color:var(--gb-primary-dark); background:var(--gb-primary-soft); }
.site-nav a.router-link-exact-active { color:var(--gb-primary-dark); background:var(--gb-primary-soft); }
.site-nav .site-workspace-link { display:inline-flex; align-items:center; gap:3px; margin-left:8px; padding:8px 15px; border:1px solid var(--gb-border-strong); color:var(--gb-primary); background:var(--gb-surface); font-weight:750; }
.site-nav .site-workspace-link:hover { color:var(--gb-primary-dark); border-color:var(--gb-primary); background:var(--gb-primary-soft); }
.site-workspace-link span { display:inline-block; margin-left:2px; transition:transform .25s ease; }
.site-workspace-link:hover span { transform:translate(2px,-2px); }
@media(max-width:620px){.site-header{align-items:flex-start;flex-direction:column;gap:13px;padding:18px 0}.site-nav{width:100%;justify-content:space-between;gap:2px}.site-nav a{font-size:12px;padding:7px 10px}.site-nav .site-workspace-link{margin-left:0}}
</style>
