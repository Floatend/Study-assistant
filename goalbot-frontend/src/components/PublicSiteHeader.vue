<template>
  <header class="site-header">
    <RouterLink class="site-wordmark" to="/">linge.xin</RouterLink>
    <nav class="site-nav" aria-label="网站导航">
      <RouterLink to="/">首页</RouterLink>
      <RouterLink to="/notes">学习笔记</RouterLink>
      <RouterLink to="/about">关于</RouterLink>
      <RouterLink class="site-workspace-link" to="/login">GoalBot <span aria-hidden="true">↗</span></RouterLink>
    </nav>
  </header>
</template>

<style scoped>
.site-header { display:flex; align-items:center; justify-content:space-between; min-height:76px; border-bottom:1px solid #f2cfa7; color:#093b4c; }
.site-wordmark { color:inherit; font-size:21px; font-weight:800; letter-spacing:0; text-decoration:none; }
.site-nav { display:flex; align-items:center; gap:23px; }
.site-nav a { color:#456a73; font-size:13px; font-weight:600; text-decoration:none; transition:color .24s ease,transform .24s ease; }
.site-nav a:hover,.site-nav a.router-link-exact-active { color:#008c88; transform:translateY(-1px); }
.site-nav .site-workspace-link { color:#f15843; font-weight:800; }
.site-workspace-link span { display:inline-block; margin-left:2px; transition:transform .25s ease; }
.site-workspace-link:hover span { transform:translate(2px,-2px); }
@media(max-width:620px){.site-header{align-items:flex-start;flex-direction:column;gap:13px;padding:18px 0}.site-nav{width:100%;justify-content:space-between;gap:11px}.site-nav a{font-size:12px}}
/* Shared public-site theme. */
.site-header{border-bottom-color:#e3e8f5;color:#1f2a44}.site-nav a{color:#65708c}.site-nav a:hover,.site-nav a.router-link-exact-active{color:#4d6bfe}.site-nav .site-workspace-link{color:#3559e8}
</style>
