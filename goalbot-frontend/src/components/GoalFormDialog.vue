<template>
  <el-dialog :model-value="modelValue" :title="form.id ? '编辑目标' : '新增目标'" width="560px" @close="close">
    <el-form label-width="84px" :model="form">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" maxlength="128" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="周期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item label="优先级">
        <el-select v-model="form.priority">
          <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status">
          <el-option v-for="item in goalStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
import { createGoal, updateGoal } from '@/api/goal'
import type { Goal, GoalPayload } from '@/types/goal'
import { goalStatusOptions, priorityOptions } from '@/utils/enums'

const props = defineProps<{
  modelValue: boolean
  goal?: Goal | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const saving = ref(false)
const dateRange = ref<[string, string] | []>([])
const form = reactive<Partial<GoalPayload> & { id?: number }>({
  title: '',
  description: '',
  priority: 2,
  status: 0
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) return
    Object.assign(form, {
      id: props.goal?.id,
      title: props.goal?.title ?? '',
      description: props.goal?.description ?? '',
      priority: props.goal?.priority ?? 2,
      status: props.goal?.status ?? 0
    })
    dateRange.value = props.goal?.startDate && props.goal?.endDate ? [props.goal.startDate, props.goal.endDate] : []
  }
)

function close() {
  emit('update:modelValue', false)
}

async function submit() {
  if (!form.title?.trim()) {
    ElMessage.warning('请输入目标标题')
    return
  }
  saving.value = true
  try {
    const payload: GoalPayload = {
      title: form.title,
      description: form.description,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      priority: form.priority ?? 2,
      status: form.status ?? 0
    }
    if (form.id) {
      await updateGoal(form.id, payload)
    } else {
      await createGoal(payload)
    }
    ElMessage.success('已保存')
    emit('saved')
    close()
  } finally {
    saving.value = false
  }
}
</script>
