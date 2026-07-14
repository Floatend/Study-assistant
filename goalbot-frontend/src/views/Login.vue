<template>
  <main class="login-page">
    <section class="login-context">
      <div class="login-brand">
        <div>
          <div class="login-brand-name">GoalBot</div>
          <div class="login-brand-subtitle">AI 日程与目标助手</div>
        </div>
      </div>

      <div class="context-copy">
        <span class="context-label">PERSONAL WORKSPACE</span>
        <h1>把对话变成你的时间安排</h1>
        <p>任务、课表、复盘和飞书助手都归属于当前登录用户。</p>
      </div>

      <div class="schedule-preview" aria-hidden="true">
        <div class="schedule-time">08:30</div>
        <div class="schedule-line schedule-line-green">
          <strong>高等数学</strong>
          <span>08:30 - 10:10</span>
        </div>
        <div class="schedule-time">14:00</div>
        <div class="schedule-line schedule-line-blue">
          <strong>项目推进</strong>
          <span>14:00 - 15:30</span>
        </div>
        <div class="schedule-time">19:30</div>
        <div class="schedule-line schedule-line-coral">
          <strong>晚间安排</strong>
          <span>19:30 - 20:30</span>
        </div>
      </div>
    </section>

    <section class="login-form-area">
      <div class="login-panel">
        <RouterLink class="back-to-blog" to="/">返回个人主页</RouterLink>
        <div class="login-heading">
          <span class="login-kicker">WELCOME BACK</span>
          <h2>登录工作区</h2>
          <p>使用管理员创建的 GoalBot 账号</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" autocomplete="username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              type="password"
              autocomplete="current-password"
              show-password
              placeholder="请输入密码"
              @keyup.enter="submit"
            />
          </el-form-item>
          <el-button class="login-button" type="primary" native-type="submit" :loading="userStore.loading">
            登录
          </el-button>
        </el-form>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
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
  await userStore.login(form)
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
  await router.replace(redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/dashboard')
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(420px, 1.08fr) minmax(420px, 0.92fr);
  background: #fff;
}

.login-context {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  overflow: hidden;
  padding: 38px 54px 44px;
  color: #f8fbfa;
  background: #173e36;
}

.login-brand {
  display: flex;
  z-index: 1;
  align-items: center;
}

.login-brand-name {
  font-size: 18px;
  font-weight: 800;
}

.login-brand-subtitle {
  margin-top: 2px;
  color: rgba(248, 251, 250, 0.66);
  font-size: 12px;
}

.context-copy {
  z-index: 1;
  max-width: 620px;
  margin-top: clamp(70px, 13vh, 130px);
}

.context-label,
.login-kicker {
  color: #9fd7c5;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.context-copy h1 {
  max-width: 560px;
  margin: 18px 0 16px;
  font-size: 44px;
  line-height: 1.18;
  letter-spacing: 0;
}

.context-copy p {
  max-width: 540px;
  margin: 0;
  color: rgba(248, 251, 250, 0.72);
  font-size: 16px;
  line-height: 1.8;
}

.schedule-preview {
  z-index: 1;
  display: grid;
  align-items: center;
  max-width: 640px;
  margin-top: auto;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 9px 14px;
}

.schedule-time {
  color: rgba(248, 251, 250, 0.55);
  font-size: 12px;
}

.schedule-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 54px;
  padding: 11px 16px;
  border-left: 4px solid;
  border-radius: 4px;
  color: #17202a;
  background: rgba(255, 255, 255, 0.92);
}

.schedule-line span {
  color: #657083;
  font-size: 12px;
}

.schedule-line-green { border-color: #4ba588; }
.schedule-line-blue { border-color: #6279df; }
.schedule-line-coral { border-color: #dc7868; }

.login-form-area {
  display: grid;
  place-items: center;
  padding: 42px;
  background: #f7f8fb;
}

.login-panel {
  width: min(410px, 100%);
  padding: 34px;
  border: 1px solid #e2e7ee;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 55px rgba(24, 36, 48, 0.08);
}

.back-to-blog {
  display: inline-block;
  margin-bottom: 24px;
  color: #2f7d68;
  font-size: 13px;
  text-decoration: none;
}

.back-to-blog:hover {
  text-decoration: underline;
}

.login-heading {
  margin-bottom: 28px;
}

.login-kicker {
  color: #2f7d68;
}

.login-heading h2 {
  margin: 10px 0 7px;
  color: #17202a;
  font-size: 27px;
  letter-spacing: 0;
}

.login-heading p {
  margin: 0;
  color: #6d7787;
  font-size: 14px;
}

.login-button {
  width: 100%;
  margin-top: 6px;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-context {
    min-height: 310px;
    padding: 28px;
  }

  .context-copy {
    margin-top: 46px;
  }

  .context-copy h1 {
    font-size: 32px;
  }

  .schedule-preview {
    display: none;
  }

  .login-form-area {
    min-height: calc(100vh - 310px);
    padding: 28px 18px;
  }

  .login-panel {
    padding: 26px;
  }
}
</style>
