<template>
  <div class="admin-shell">
    <header class="admin-header liquid-glass liquid-glass-strong">
      <RouterLink class="admin-wordmark" to="/">linge.xin</RouterLink>
      <nav class="admin-nav" aria-label="站长后台导航">
        <RouterLink to="/notes">公开笔记</RouterLink>
        <RouterLink to="/notebook">站长工作台</RouterLink>
      </nav>
      <el-dropdown trigger="click" @command="handleAccountCommand">
        <button class="account-button liquid-glass liquid-glass-control" type="button">
          <span>
            <small>已登录</small>
            <strong>{{ userStore.displayName }}</strong>
          </span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="password" :icon="Key">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </header>

    <main class="admin-main">
      <RouterView />
    </main>

    <el-dialog v-model="passwordVisible" title="修改密码" width="min(440px, calc(100vw - 32px))">
      <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="submitPassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowDown, Key, SwitchButton } from '@element-plus/icons-vue'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordRef = ref<FormInstance>()
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules: FormRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 8, max: 128, message: '新密码至少 8 个字符', trigger: 'blur' }],
  confirmPassword: [{ validator: (_rule, value, callback) => {
    if (!value) callback(new Error('请再次输入新密码'))
    else if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
    else callback()
  }, trigger: 'blur' }]
}

onMounted(async () => {
  if (!route.meta.public && userStore.token) await userStore.loadCurrentUser()
})

async function handleAccountCommand(command: string) {
  if (command === 'password') {
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    passwordVisible.value = true
    return
  }
  if (command === 'logout') {
    await userStore.logout()
    await router.replace('/login')
  }
}

async function submitPassword() {
  const valid = await passwordRef.value?.validate().catch(() => false)
  if (!valid) return
  passwordSaving.value = true
  try {
    await changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    userStore.clearSession()
    passwordVisible.value = false
    ElMessage.success('密码已修改，请重新登录')
    await router.replace('/login')
  } finally {
    passwordSaving.value = false
  }
}
</script>

<style scoped>
.admin-shell { min-height: 100vh; color: var(--text); background: transparent; }
.admin-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: var(--space-3);
  align-items: center;
  padding: var(--space-3) var(--space-4);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}
.admin-wordmark { color: var(--brand-strong); font-family: var(--font-display); font-size: 21px; font-weight: 700; text-decoration: none; }
.admin-nav { grid-row: 2; grid-column: 1 / -1; display: flex; gap: var(--space-2); padding-top: var(--space-2); border-top: 1px solid var(--line); }
.admin-nav a { padding: 7px 11px; border-radius: 999px; color: var(--muted); font-size: 14px; font-weight: 700; text-decoration: none; transition: color .2s ease, background-color .2s ease; }
.admin-nav a:hover, .admin-nav a.router-link-exact-active { color: var(--brand-strong); background: var(--brand-soft); }
.account-button { display: flex; align-items: center; justify-self: end; gap: var(--space-2); padding: 7px 11px; border-radius: 999px; color: var(--text); cursor: pointer; transition: transform .2s ease, border-color .2s ease, background-color .2s ease; }
.account-button:hover { border-color: var(--brand); transform: translateY(-1px); }
.account-button span { display: grid; text-align: right; }
.account-button small { color: var(--subtle); font-size: 11px; }
.account-button strong { font-size: 14px; }
.admin-main { padding: var(--space-4) var(--space-3) var(--space-7); }
@media (min-width: 720px) {
  .admin-header { min-height: 70px; grid-template-columns: 1fr auto 1fr; padding-inline: clamp(24px,4vw,58px); }
  .admin-nav { grid-row: auto; grid-column: auto; padding-top: 0; border-top: 0; }
  .account-button { grid-column: auto; }
  .admin-main { padding: var(--space-6) clamp(20px,3vw,42px) var(--space-7); }
}
</style>
