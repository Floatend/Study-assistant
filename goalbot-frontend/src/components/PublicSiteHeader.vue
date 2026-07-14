<template>
  <header class="site-header">
    <RouterLink class="site-wordmark" to="/">linge.xin</RouterLink>
    <nav class="site-nav" aria-label="网站导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/notes">官方笔记</RouterLink>
      <RouterLink to="/about">关于</RouterLink>
      <RouterLink class="site-workspace-link" to="/login">GoalBot</RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 76px;
  border-bottom: 1px solid rgba(36, 47, 41, 0.16);
}

.site-wordmark {
  color: #213029;
  font-size: 18px;
  font-weight: 700;
  text-decoration: none;
}

.site-nav {
  display: flex;
  align-items: center;
  gap: 22px;
}

.site-nav a {
  color: #6a756d;
  font-size: 14px;
  text-decoration: none;
}

.site-nav a:hover,
.site-nav a.router-link-exact-active {
  color: #26372e;
}

.site-nav .site-workspace-link {
  color: #2e6d59;
  font-weight: 650;
}

@media (max-width: 620px) {
  .site-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 14px;
    padding: 18px 0;
  }

  .site-nav {
    width: 100%;
    justify-content: space-between;
    gap: 12px;
  }
}
</style>
