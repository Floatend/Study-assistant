<template>
  <RouterView v-if="route.meta.public" />

  <div v-else class="authenticated-app">
    <el-container class="app-shell">
      <el-aside class="app-aside" width="248px">
        <div class="brand">
          <div class="brand-copy">
            <div class="brand-name">GoalBot</div>
            <div class="brand-subtitle">AI 日程与目标助手</div>
          </div>
        </div>

        <div class="aside-label">Workspace</div>
        <el-menu :default-active="route.path" router class="nav-menu">
          <el-menu-item index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            <span>任务</span>
          </el-menu-item>
          <el-menu-item index="/calendar">
            <el-icon><Calendar /></el-icon>
            <span>时间表</span>
          </el-menu-item>
          <el-menu-item index="/review">
            <el-icon><Notebook /></el-icon>
            <span>复盘</span>
          </el-menu-item>
          <el-menu-item index="/analytics">
            <el-icon><TrendCharts /></el-icon>
            <span>统计</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/notebook">
            <el-icon><Document /></el-icon>
            <span>站长笔记</span>
          </el-menu-item>
          <el-menu-item index="/settings">
            <el-icon><Setting /></el-icon>
            <span>配置</span>
          </el-menu-item>
          <el-menu-item v-if="userStore.isAdmin" index="/users">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-menu>

        <div class="aside-status">
          <span class="status-dot" />
          <div>
            <div class="aside-status-title">飞书助手在线</div>
            <div class="aside-status-text">自然语言指令已接入</div>
          </div>
        </div>
      </el-aside>

      <el-container>
        <el-header class="app-header">
          <div>
            <div class="page-kicker">GoalBot Workspace</div>
            <h1>{{ pageTitle }}</h1>
            <p>{{ pageSubtitle }}</p>
          </div>
          <div class="header-actions">
            <el-dropdown trigger="click" @command="handleAccountCommand">
              <button class="account-button" type="button">
                <span class="account-avatar">{{ accountInitial }}</span>
                <span class="account-copy">
                  <strong>{{ userStore.displayName }}</strong>
                  <small>{{ userStore.profile?.role === 'ADMIN' ? '管理员' : '个人用户' }}</small>
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
          </div>
        </el-header>
        <el-main class="app-main">
          <RouterView />
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-model="passwordVisible" title="修改密码" width="440px">
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowDown,
  Calendar,
  DataBoard,
  Document,
  Key,
  List,
  Notebook,
  Setting,
  SwitchButton,
  TrendCharts,
  UserFilled
} from '@element-plus/icons-vue'
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

const titles: Record<string, string> = {
  '/dashboard': '今日工作台',
  '/tasks': '任务管理',
  '/calendar': '时间表',
  '/review': '复盘',
  '/analytics': '统计',
  '/notebook': '站长笔记',
  '/settings': '配置',
  '/users': '用户管理'
}

const subtitles: Record<string, string> = {
  '/dashboard': '目标、任务、建议和打卡集中处理',
  '/tasks': '维护未来日程和每日行动项',
  '/calendar': '按日、周、月查看任务安排',
  '/review': '沉淀每日复盘与阶段总结',
  '/analytics': '查看投入、完成和趋势',
  '/notebook': '撰写、整理并发布官网学习成果',
  '/settings': '配置主动消息、飞书和 Dify 状态',
  '/users': '管理账号、权限、状态和飞书绑定'
}

const pageTitle = computed(() => titles[route.path] ?? 'GoalBot')
const pageSubtitle = computed(() => subtitles[route.path] ?? '个人目标管理助手')
const accountInitial = computed(() => userStore.displayName.slice(0, 1).toUpperCase())

onMounted(async () => {
  if (userStore.token) await userStore.loadCurrentUser()
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
    await changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    })
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
.authenticated-app {
  min-height: 100vh;
}

.account-button {
  display: flex;
  align-items: center;
  min-width: 172px;
  gap: 9px;
  padding: 6px 8px;
  border: 1px solid transparent;
  border-radius: 8px;
  color: var(--gb-text);
  background: transparent;
  cursor: pointer;
}

.account-button:hover {
  border-color: var(--gb-border);
  background: #f8faf9;
}

.account-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 50%;
  color: #236552;
  font-size: 13px;
  font-weight: 800;
  background: #dff1ea;
}

.account-copy {
  min-width: 0;
  flex: 1;
  text-align: left;
}

.account-copy strong,
.account-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-copy strong {
  font-size: 13px;
}

.account-copy small {
  margin-top: 2px;
  color: var(--gb-muted);
  font-size: 11px;
}

@media (max-width: 760px) {
  .account-copy {
    display: none;
  }

  .account-button {
    min-width: 0;
  }
}
</style>
