<template>
  <section class="page-section dashboard-page" v-loading="dashboardStore.loading">
    <div class="dashboard-focus">
      <div>
        <div class="focus-kicker">Today</div>
        <h2>今日安排</h2>
        <p>任务、目标和 AI 建议集中在这里处理。</p>
      </div>
      <div class="focus-actions">
        <el-button :icon="Refresh" plain @click="loadHome">刷新</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreateGoal">新增目标</el-button>
      </div>
    </div>

    <div class="metric-grid">
      <div class="metric-panel">
        <div class="metric-label">今日投入</div>
        <div class="metric-value">{{ minutesText(data?.todayActualMinutes) }}</div>
        <div class="metric-note">来自打卡记录</div>
      </div>
      <div class="metric-panel">
        <div class="metric-label">今日任务</div>
        <div class="metric-value">{{ data?.todayTaskCount ?? 0 }}</div>
        <div class="metric-note">待完成和已完成</div>
      </div>
      <div class="metric-panel">
        <div class="metric-label">已完成</div>
        <div class="metric-value">{{ data?.completedTaskCount ?? 0 }}</div>
        <div class="metric-note">完成后自动更新任务状态</div>
      </div>
    </div>

    <div class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <div>
            <h2 class="panel-title">今日任务</h2>
            <p class="panel-subtitle">直接打卡，保持页面和建议同步</p>
          </div>
          <el-button :icon="Refresh" plain @click="loadHome">刷新</el-button>
        </div>
        <div class="panel-body task-list-body">
          <TaskCard
            v-for="task in data?.todayTasks"
            :key="task.id"
            :task="task"
            @checkin="handleQuickCheckin"
          />
          <p v-if="!data?.todayTasks?.length" class="empty-text">今天还没有任务。可以在任务页新增，也可以通过飞书自然语言创建。</p>
        </div>
      </div>

      <AiAdviceCard
        :content="data?.latestAiAdvice"
        :loading="dashboardStore.adviceLoading"
        :stale="data?.aiAdviceStale"
        :days="dashboardStore.adviceDays"
        :start-date="data?.adviceStartDate"
        :end-date="data?.adviceEndDate"
        @days-change="handleAdviceDaysChange"
      />
    </div>

    <div class="panel">
      <div class="panel-header">
        <div>
          <h2 class="panel-title">目标管理</h2>
          <p class="panel-subtitle">目标只做方向约束，具体行动落到任务和时间表。</p>
        </div>
        <el-button :icon="Plus" type="primary" @click="openCreateGoal">新增目标</el-button>
      </div>
      <div class="panel-body">
        <div class="toolbar dashboard-goal-toolbar">
          <div class="toolbar-left">
            <el-input
              v-model="goalFilters.keyword"
              clearable
              placeholder="搜索目标"
              style="width: 220px"
              @keyup.enter="loadGoals"
            />
            <el-select v-model="goalFilters.status" clearable placeholder="状态" style="width: 140px">
              <el-option v-for="item in goalStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-select v-model="goalFilters.priority" clearable placeholder="优先级" style="width: 140px">
              <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button :icon="Search" plain @click="loadGoals">查询</el-button>
          </div>
        </div>

        <el-table :data="goalStore.goals" v-loading="goalStore.loading" row-key="id">
          <el-table-column prop="title" label="目标" min-width="180" />
          <el-table-column label="优先级" width="100">
            <template #default="{ row }">{{ labelOf(priorityOptions, row.priority) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag effect="plain">{{ labelOf(goalStatusOptions, row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务" width="120">
            <template #default="{ row }">{{ row.completedTaskCount ?? 0 }} / {{ row.totalTaskCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="endDate" label="截止日期" width="120" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button :icon="Edit" circle plain @click="openEditGoal(row)" />
              <el-button :icon="Delete" circle plain type="danger" @click="handleDeleteGoal(row.id)" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <GoalFormDialog v-model="goalDialogVisible" :goal="editingGoal" @saved="handleGoalSaved" />
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createCheckin } from '@/api/checkin'
import { deleteGoal } from '@/api/goal'
import AiAdviceCard from '@/components/AiAdviceCard.vue'
import GoalFormDialog from '@/components/GoalFormDialog.vue'
import TaskCard from '@/components/TaskCard.vue'
import { useDashboardStore } from '@/stores/dashboard'
import { useGoalStore } from '@/stores/goal'
import { minutesText } from '@/utils/date'
import { goalStatusOptions, labelOf, priorityOptions } from '@/utils/enums'
import type { Goal } from '@/types/goal'
import type { Task } from '@/types/task'

const dashboardStore = useDashboardStore()
const goalStore = useGoalStore()
const data = computed(() => dashboardStore.data)
const goalFilters = reactive<{ keyword?: string; status?: number; priority?: number }>({})
const goalDialogVisible = ref(false)
const editingGoal = ref<Goal | null>(null)
let refreshTimer: number | undefined

onMounted(() => {
  loadHome()
  refreshTimer = window.setInterval(() => {
    refreshDashboardSilently()
  }, 60_000)
  window.addEventListener('focus', refreshDashboardSilently)
})

onBeforeUnmount(() => {
  if (refreshTimer !== undefined) {
    window.clearInterval(refreshTimer)
  }
  window.removeEventListener('focus', refreshDashboardSilently)
})

async function loadHome() {
  await Promise.all([dashboardStore.loadDashboard(), goalStore.loadGoals(goalFilters)])
  refreshAdviceIfNeeded()
}

async function loadGoals() {
  await goalStore.loadGoals(goalFilters)
}

async function handleQuickCheckin(task: Task) {
  await createCheckin({
    taskId: task.id,
    actualMinutes: task.plannedMinutes ?? 0,
    content: '首页快速打卡'
  })
  ElMessage.success('已打卡')
  await Promise.all([dashboardStore.loadDashboard(), goalStore.loadGoals(goalFilters)])
  refreshAdviceIfNeeded()
}

function openCreateGoal() {
  editingGoal.value = null
  goalDialogVisible.value = true
}

function openEditGoal(goal: Goal) {
  editingGoal.value = goal
  goalDialogVisible.value = true
}

async function handleGoalSaved() {
  await Promise.all([dashboardStore.loadDashboard(), goalStore.loadGoals(goalFilters)])
  refreshAdviceIfNeeded()
}

async function handleDeleteGoal(id: number) {
  await ElMessageBox.confirm('确认删除这个目标吗？', '删除目标', { type: 'warning' })
  await deleteGoal(id)
  ElMessage.success('已删除')
  await Promise.all([dashboardStore.loadDashboard(), goalStore.loadGoals(goalFilters)])
  refreshAdviceIfNeeded()
}

async function refreshDashboardSilently() {
  await dashboardStore.loadDashboard({ silent: true })
  refreshAdviceIfNeeded()
}

async function handleAdviceDaysChange(days: number) {
  if (days === dashboardStore.adviceDays) {
    return
  }
  dashboardStore.setAdviceDays(days)
  await dashboardStore.loadDashboard({ silent: true })
  refreshAdviceIfNeeded()
}

function refreshAdviceIfNeeded() {
  if (dashboardStore.data?.aiAdviceStale && !dashboardStore.adviceLoading) {
    dashboardStore.refreshAdvice()
  }
}
</script>

<style scoped>
.dashboard-focus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--gb-shadow);
}

.focus-kicker {
  color: var(--gb-primary);
  font-size: 12px;
  font-weight: 800;
}

.dashboard-focus h2 {
  margin: 7px 0 0;
  color: var(--gb-text);
  font-size: 24px;
}

.dashboard-focus p {
  margin: 7px 0 0;
  color: var(--gb-muted);
  font-size: 14px;
}

.focus-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.panel-subtitle {
  margin: 5px 0 0;
  color: var(--gb-muted);
  font-size: 12px;
}

.task-list-body {
  padding-top: 8px;
  padding-bottom: 8px;
}

.dashboard-goal-toolbar {
  margin-bottom: 14px;
}

@media (max-width: 760px) {
  .dashboard-focus {
    align-items: flex-start;
    flex-direction: column;
  }

  .focus-actions {
    justify-content: flex-start;
  }
}
</style>
