<template>
  <section class="page-section users-page">
    <div class="users-toolbar">
      <div class="users-filters">
        <el-input
          v-model="filters.keyword"
          :prefix-icon="Search"
          clearable
          placeholder="搜索用户名或昵称"
          @keyup.enter="load"
          @clear="load"
        />
        <el-select v-model="filters.status" clearable placeholder="全部状态" @change="load">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
    </div>

    <div class="users-table-wrap" v-loading="loading">
      <el-table :data="users" row-key="id">
        <el-table-column label="用户" min-width="190">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">{{ avatarText(row) }}</div>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <span>@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'info'" effect="plain">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status-label" :class="row.status === 1 ? 'status-active' : 'status-disabled'">
              <i />{{ row.status === 1 ? '启用' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="飞书用户 ID" min-width="210">
          <template #default="{ row }">
            <span class="mono-value">{{ row.feishuUserId || '未绑定' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="最近登录" min-width="165">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="136" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="编辑用户" placement="top">
              <el-button circle :icon="Edit" @click="openEdit(row)" />
            </el-tooltip>
            <el-tooltip content="重置密码" placement="top">
              <el-button circle :icon="Key" @click="openReset(row)" />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && users.length === 0" description="没有匹配的用户" />
    </div>

    <el-dialog v-model="editorVisible" :title="editingUser ? '编辑用户' : '新增用户'" width="520px">
      <el-form ref="editorRef" :model="editor" :rules="editorRules" label-position="top">
        <el-form-item v-if="!editingUser" label="用户名" prop="username">
          <el-input v-model="editor.username" autocomplete="off" placeholder="例如 kylin-19" />
        </el-form-item>
        <el-form-item v-if="!editingUser" label="初始密码" prop="password">
          <el-input v-model="editor.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editor.nickname" placeholder="显示名称" />
        </el-form-item>
        <el-form-item label="飞书用户 ID" prop="feishuUserId">
          <el-input v-model="editor.feishuUserId" clearable placeholder="open_id、user_id 或 union_id" />
        </el-form-item>
        <div class="form-split">
          <el-form-item label="角色" prop="role">
            <el-segmented v-model="editor.role" :options="roleOptions" :disabled="isEditingSelf" />
          </el-form-item>
          <el-form-item v-if="editingUser" label="状态" prop="status">
            <el-switch
              v-model="editor.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
              :disabled="isEditingSelf"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="440px">
      <div class="reset-target">{{ resetTarget?.nickname || resetTarget?.username }}</div>
      <el-form ref="resetRef" :model="resetForm" :rules="resetRules" label-position="top">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReset">确认重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Edit, Key, Plus, Search } from '@element-plus/icons-vue'
import { createUser, fetchUsers, resetUserPassword, updateUser } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { UserProfile, UserRole } from '@/types/auth'

const userStore = useUserStore()
const users = ref<UserProfile[]>([])
const loading = ref(false)
const saving = ref(false)
const filters = reactive<{ keyword: string; status?: number }>({ keyword: '', status: undefined })
const editorVisible = ref(false)
const editingUser = ref<UserProfile | null>(null)
const editorRef = ref<FormInstance>()
const editor = reactive({ username: '', password: '', nickname: '', feishuUserId: '', role: 'USER' as UserRole, status: 1 as 0 | 1 })
const roleOptions = [
  { label: '普通用户', value: 'USER' },
  { label: '管理员', value: 'ADMIN' }
]
const editorRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 64, message: '用户名长度为 3 到 64 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.-]+$/, message: '只能包含字母、数字、点、下划线和短横线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 8, max: 128, message: '密码至少 8 个字符', trigger: 'blur' }
  ]
}

const resetVisible = ref(false)
const resetTarget = ref<UserProfile | null>(null)
const resetRef = ref<FormInstance>()
const resetForm = reactive({ newPassword: '', confirmPassword: '' })
const resetRules: FormRules = {
  newPassword: [{ required: true, min: 8, max: 128, message: '密码至少 8 个字符', trigger: 'blur' }],
  confirmPassword: [{ validator: (_rule, value, callback) => {
    if (!value) callback(new Error('请再次输入新密码'))
    else if (value !== resetForm.newPassword) callback(new Error('两次输入的密码不一致'))
    else callback()
  }, trigger: 'blur' }]
}
const isEditingSelf = computed(() => editingUser.value?.id === userStore.profile?.id)

onMounted(load)

async function load() {
  loading.value = true
  try {
    users.value = await fetchUsers({ keyword: filters.keyword || undefined, status: filters.status })
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingUser.value = null
  Object.assign(editor, { username: '', password: '', nickname: '', feishuUserId: '', role: 'USER', status: 1 })
  editorVisible.value = true
}

function openEdit(user: UserProfile) {
  editingUser.value = user
  Object.assign(editor, {
    username: user.username,
    password: '',
    nickname: user.nickname || '',
    feishuUserId: user.feishuUserId || '',
    role: user.role,
    status: user.status
  })
  editorVisible.value = true
}

async function saveUser() {
  const valid = await editorRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingUser.value) {
      await updateUser(editingUser.value.id, {
        nickname: editor.nickname,
        feishuUserId: editor.feishuUserId,
        role: editor.role,
        status: editor.status
      })
      if (editingUser.value.id === userStore.profile?.id) await userStore.loadCurrentUser()
      ElMessage.success('用户已更新')
    } else {
      await createUser({
        username: editor.username,
        password: editor.password,
        nickname: editor.nickname,
        feishuUserId: editor.feishuUserId,
        role: editor.role
      })
      ElMessage.success('用户已创建')
    }
    editorVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

function openReset(user: UserProfile) {
  resetTarget.value = user
  Object.assign(resetForm, { newPassword: '', confirmPassword: '' })
  resetVisible.value = true
}

async function submitReset() {
  const valid = await resetRef.value?.validate().catch(() => false)
  if (!valid || !resetTarget.value) return
  saving.value = true
  try {
    await resetUserPassword(resetTarget.value.id, resetForm.newPassword)
    ElMessage.success('密码已重置，原有登录已失效')
    resetVisible.value = false
  } finally {
    saving.value = false
  }
}

function avatarText(user: UserProfile) {
  return (user.nickname || user.username).slice(0, 1).toUpperCase()
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '从未登录'
}
</script>

<style scoped>
.users-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.users-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.users-filters {
  display: flex;
  width: min(520px, 100%);
  gap: 10px;
}

.users-filters .el-input {
  flex: 1;
}

.users-filters .el-select {
  width: 140px;
}

.users-table-wrap {
  min-height: 420px;
  overflow: hidden;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: var(--gb-surface);
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 11px;
}

.user-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  place-items: center;
  border-radius: 50%;
  color: #236552;
  font-size: 13px;
  font-weight: 800;
  background: #e7f4ef;
}

.user-cell strong,
.user-cell span {
  display: block;
}

.user-cell span {
  margin-top: 2px;
  color: var(--gb-muted);
  font-size: 12px;
}

.status-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--gb-muted);
  font-size: 13px;
}

.status-label i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9aa3af;
}

.status-active i { background: #2f9d78; }
.status-disabled i { background: #c16b5d; }

.mono-value {
  color: #4e5969;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.form-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.reset-target {
  margin-bottom: 18px;
  color: var(--gb-text);
  font-size: 15px;
  font-weight: 700;
}

@media (max-width: 700px) {
  .users-toolbar,
  .users-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .users-filters .el-select {
    width: 100%;
  }

  .form-split {
    grid-template-columns: 1fr;
  }
}
</style>
