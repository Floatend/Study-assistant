<template>
  <main class="login-page">
    <section class="login-context">
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
.login-page { display:grid; min-height:100vh; grid-template-columns:minmax(420px,1.08fr) minmax(380px,.92fr); color:var(--gb-text); background:var(--gb-bg); }
.login-context {
  position:relative; display:flex; min-height:100vh; flex-direction:column; overflow:hidden;
  padding:38px clamp(34px,5vw,76px) 48px; color:#fff;
  background:linear-gradient(128deg,#131c4a 0%,#1b2a68 52%,#27409c 100%);
}
.login-context::before {
  content:''; position:absolute; inset:0;
  background:
    radial-gradient(720px 420px at 84% 10%, rgba(111,139,255,.26), transparent 60%),
    radial-gradient(560px 400px at 6% 90%, rgba(169,243,228,.13), transparent 60%);
}
.login-wordmark { position:relative; color:#fff; font-size:21px; font-weight:800; letter-spacing:-.01em; text-decoration:none; }
.context-copy { position:relative; max-width:680px; margin:auto 0; }
.context-copy>span,.login-heading>span { color:var(--gb-mint); font-size:11px; font-weight:800; letter-spacing:.14em; }
.context-copy h1 { margin:18px 0; color:#fff; font-size:clamp(40px,4.4vw,62px); font-weight:800; letter-spacing:-.02em; line-height:1.1; }
.context-copy p { max-width:580px; margin:0; color:rgba(226,232,255,.82); font-size:15px; line-height:1.9; }
.context-index { position:relative; display:grid; grid-template-columns:auto 1fr; gap:8px 18px; color:rgba(226,232,255,.7); font-size:11px; }
.context-index strong { color:#fff; font-size:12px; }
.login-form-area { display:grid; place-items:center; padding:42px; }
.login-panel { width:min(410px,100%); padding:36px; border:1px solid var(--gb-border); border-radius:var(--gb-radius); background:var(--gb-surface); box-shadow:var(--gb-shadow-lg); }
.back-to-blog { display:inline-block; margin-bottom:34px; color:var(--gb-primary); font-size:13px; font-weight:700; text-decoration:none; }
.login-heading { margin-bottom:28px; }
.login-heading>span { color:var(--gb-primary); }
.login-heading h2 { margin:10px 0 7px; font-size:28px; letter-spacing:-.01em; }
.login-heading p { margin:0; color:var(--gb-muted); font-size:14px; }
.login-button { width:100%; margin-top:6px; }
@media(max-width:820px){
  .login-page { grid-template-columns:1fr; }
  .login-context { min-height:330px; padding:28px 22px; }
  .context-copy { margin:56px 0; }
  .context-copy h1 { font-size:34px; }
  .context-index { display:none; }
  .login-form-area { min-height:calc(100vh - 330px); padding:28px 18px; }
  .login-panel { padding:26px; }
}
</style>
