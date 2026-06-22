<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑任务' : '新增任务'" width="620px" @close="close">
    <el-form label-width="90px" :model="form">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="128" />
      </el-form-item>
      <el-form-item label="目标">
        <el-select v-model="form.goalId" clearable filterable placeholder="不绑定目标">
          <el-option v-for="goal in goals" :key="goal.id" :label="goal.title" :value="goal.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期" required>
        <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="时间">
        <div class="time-row">
          <el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm:ss" placeholder="开始" />
          <el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm:ss" placeholder="结束" />
        </div>
      </el-form-item>
      <el-form-item label="计划用时">
        <el-input-number v-model="form.plannedMinutes" :min="0" :step="10" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status">
          <el-option v-for="item in taskStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createTask, updateTask } from '@/api/task'
import type { Goal } from '@/types/goal'
import type { Task, TaskPayload } from '@/types/task'
import { taskStatusOptions } from '@/utils/enums'
import { todayString } from '@/utils/date'

const props = defineProps<{
  modelValue: boolean
  task?: Task | null
  goals: Goal[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const form = reactive<Partial<TaskPayload> & { id?: number }>({
  title: '',
  description: '',
  planDate: todayString(),
  plannedMinutes: 30,
  status: 0
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    Object.assign(form, {
      id: props.task?.id,
      goalId: props.task?.goalId,
      title: props.task?.title ?? '',
      description: props.task?.description ?? '',
      planDate: props.task?.planDate ?? todayString(),
      startTime: props.task?.startTime,
      endTime: props.task?.endTime,
      plannedMinutes: props.task?.plannedMinutes ?? 30,
      status: props.task?.status ?? 0
    })
  }
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (!form.title?.trim()) {
    ElMessage.warning('请输入任务标题')
    return
  }
  if (!form.planDate) {
    ElMessage.warning('请选择计划日期')
    return
  }
  saving.value = true
  try {
    const payload: TaskPayload = {
      goalId: form.goalId,
      title: form.title,
      description: form.description,
      planDate: form.planDate,
      startTime: form.startTime,
      endTime: form.endTime,
      plannedMinutes: form.plannedMinutes ?? 0,
      status: form.status ?? 0
    }
    if (form.id) {
      await updateTask(form.id, payload)
    } else {
      await createTask(payload)
    }
    ElMessage.success('已保存')
    emit('saved')
    close()
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.time-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
</style>
