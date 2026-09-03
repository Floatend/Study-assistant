<template>
  <main class="login-page">
    <section class="login-context">
      <img class="context-image" :src="workspaceHero" alt="安静的书桌与学习空间" />
      <div class="context-shade" aria-hidden="true" />
      <RouterLink class="login-wordmark" to="/">linge.xin</RouterLink>
      <div class="context-copy">
        <span>EDITORIAL ACCESS</span>
        <h1>整理、编辑，<br />然后决定公开什么。</h1>
        <p>这里是站长专用的内容工作台。草稿只保留在后台，只有主动发布的文章才会进入公开知识库。</p>
      </div>
      <div class="context-index" aria-hidden="true">
        <span>01</span><strong>WRITE</strong>
        <span>02</span><strong>REVIEW</strong>
        <span>03</span><strong>PUBLISH</strong>
      </div>
    </section>

    <section class="login-form-area">
      <div class="login-panel">
        <RouterLink class="back-to-blog" to="/">← 返回个人主页</RouterLink>
        <div class="login-heading">
          <span>OWNER SIGN IN</span>
          <h2>站长登录</h2>
          <p>使用管理员账号进入笔记工作台</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" :prefix-icon="Lock" type="password" autocomplete="current-password" show-password placeholder="请输入密码" @keyup.enter="submit" />
          </el-form-item>
          <el-button class="login-button" type="primary" native-type="submit" :loading="userStore.loading">进入工作台</el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import workspaceHero from '@/assets/linge-workspace-hero.png'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  const user = await userStore.login(form)
  if (user.role !== 'ADMIN') {
    userStore.clearSession()
    ElMessage.error('仅站长管理员账号可以进入此工作台')
    return
  }
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/notebook'
  await router.replace(redirect === '/notebook' ? redirect : '/notebook')
}
</script>

<style scoped>
.login-page { display: flex; min-height: 100vh; flex-direction: column; color: var(--text); background: var(--bg); }
.login-context { position: relative; display: flex; min-height: 360px; flex-direction: column; justify-content: space-between; gap: var(--space-6); overflow: hidden; padding: var(--space-5); color: var(--surface); background: var(--brand); }
.context-image { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.context-shade { position: absolute; inset: 0; background: color-mix(in srgb, var(--text) 76%, transparent); }
.login-wordmark, .context-copy, .context-index { position: relative; z-index: 1; }
.login-wordmark { align-self: flex-start; color: var(--surface); font-family: var(--font-display); font-size: 22px; font-weight: 700; text-decoration: none; }
.context-copy { display: flex; max-width: 680px; flex-direction: column; gap: var(--space-4); }
.context-copy>span, .login-heading>span { color: color-mix(in srgb, var(--surface) 78%, var(--accent)); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.context-copy h1 { color: var(--surface); font-size: 38px; line-height: 1.12; }
.context-copy p { max-width: 54ch; color: color-mix(in srgb, var(--surface) 78%, transparent); font-size: 15px; line-height: 1.6; }
.context-index { display: none; grid-template-columns: auto 1fr; gap: var(--space-2) var(--space-4); color: color-mix(in srgb, var(--surface) 62%, transparent); font-size: 11px; }
.context-index strong { color: var(--surface); font-size: 12px; }
.login-form-area { display: grid; flex: 1; place-items: center; padding: var(--space-5) var(--space-4); }
.login-panel { display: flex; width: min(420px,100%); flex-direction: column; gap: var(--space-6); padding: var(--space-6); border: 1px solid var(--glass-border); border-radius: var(--radius-lg); background: var(--glass-strong); box-shadow: inset 0 1px 0 var(--glass-highlight), var(--shadow-float); backdrop-filter: blur(24px) saturate(1.2); }
.back-to-blog { align-self: flex-start; color: var(--brand-strong); font-size: 14px; font-weight: 700; text-decoration: none; }
.login-heading { display: flex; flex-direction: column; gap: var(--space-2); }
.login-heading>span { color: var(--brand); }
.login-heading h2 { color: var(--text); font-size: 30px; }
.login-heading p { color: var(--muted); font-size: 14px; }
.login-button { width: 100%; }

@media (min-width: 840px) {
  .login-page { display: grid; grid-template-columns: minmax(460px,1.08fr) minmax(390px,.92fr); }
  .login-context { min-height: 100vh; padding: var(--space-6) clamp(40px,5vw,76px) var(--space-7); }
  .context-copy h1 { font-size: 54px; }
  .context-index { display: grid; }
  .login-form-area { padding: var(--space-7); }
}
</style>
