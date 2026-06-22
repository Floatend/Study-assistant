<template>
  <section class="page-section">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker v-model="filters.date" type="date" value-format="YYYY-MM-DD" placeholder="日期" />
        <el-select v-model="filters.goalId" clearable filterable placeholder="目标" style="width: 180px">
          <el-option v-for="goal in goalStore.goals" :key="goal.id" :label="goal.title" :value="goal.id" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="状态" style="width: 140px">
          <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button :icon="Search" plain @click="load">查询</el-button>
        <el-button :icon="Plus" type="primary" @click="openCreate">新增任务</el-button>
      </div>
    </div>

    <div class="panel">
      <el-table :data="taskStore.tasks" v-loading="taskStore.loading" row-key="id">
        <el-table-column prop="title" label="任务" min-width="180" />
        <el-table-column prop="goalTitle" label="目标" min-width="140" />
        <el-table-column prop="planDate" label="日期" width="120" />
        <el-table-column label="时间" width="130">
          <template #default="{ row }">{{ row.startTime?.slice(0, 5) || '-' }} - {{ row.endTime?.slice(0, 5) || '-' }}</template>
        </el-table-column>
        <el-table-column prop="plannedMinutes" label="计划分钟" width="100" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : 'warning'" effect="plain">
              {{ labelOf(taskStatusOptions, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 2" :icon="Check" circle plain type="success" @click="handleCheckin(row)" />
            <el-button :icon="Edit" circle plain @click="openEdit(row)" />
            <el-button :icon="Delete" circle plain type="danger" @click="handleDelete(row.id)" />
          </template>
        </el-table-column>
      </el-table>
    </div>

    <TaskFormDialog v-model="dialogVisible" :task="editingTask" :goals="goalStore.goals" @saved="load" />
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Delete, Edit, Plus, Search } from '@element-plus/icons-vue'
import { createCheckin } from '@/api/checkin'
import { deleteTask } from '@/api/task'
import TaskFormDialog from '@/components/TaskFormDialog.vue'
import { useGoalStore } from '@/stores/goal'
import { useTaskStore } from '@/stores/task'
import type { Task } from '@/types/task'
import { labelOf, taskStatusOptions } from '@/utils/enums'
import { todayString } from '@/utils/date'

const taskStore = useTaskStore()
const goalStore = useGoalStore()
const filters = reactive<{ date?: string; goalId?: number; status?: number }>({ date: todayString() })
const dialogVisible = ref(false)
const editingTask = ref<Task | null>(null)

onMounted(async () => {
  await goalStore.loadGoals()
  await load()
})

function load() {
  return taskStore.loadTasks(filters)
}

function openCreate() {
  editingTask.value = null
  dialogVisible.value = true
}

function openEdit(task: Task) {
  editingTask.value = task
  dialogVisible.value = true
}

async function handleCheckin(task: Task) {
  await createCheckin({
    taskId: task.id,
    actualMinutes: task.plannedMinutes ?? 0,
    content: '任务页快速打卡'
  })
  ElMessage.success('已打卡')
  await load()
  await goalStore.loadGoals()
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除这个任务吗？', '删除任务', { type: 'warning' })
  await deleteTask(id)
  ElMessage.success('已删除')
  await load()
  await goalStore.loadGoals()
}
</script>
