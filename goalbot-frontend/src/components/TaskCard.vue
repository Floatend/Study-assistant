<template>
  <div class="task-card" :class="{ completed: task.status === 2 }">
    <div class="task-marker">
      <span />
    </div>
    <div class="task-main">
      <div class="task-title-row">
        <div class="task-title">{{ task.title }}</div>
        <el-tag :type="task.status === 2 ? 'success' : 'warning'" effect="plain">
          {{ labelOf(taskStatusOptions, task.status) }}
        </el-tag>
      </div>
      <div class="task-meta">
        <span>{{ task.planDate }}</span>
        <span v-if="task.startTime || task.endTime">{{ timeRange }}</span>
        <span>{{ minutesText(task.plannedMinutes) }}</span>
        <span v-if="task.goalTitle">{{ task.goalTitle }}</span>
      </div>
    </div>
    <el-button
      v-if="task.status !== 2"
      :icon="Check"
      type="success"
      plain
      @click="$emit('checkin', task)"
    >
      打卡
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'
import type { Task } from '@/types/task'
import { labelOf, taskStatusOptions } from '@/utils/enums'
import { minutesText } from '@/utils/date'

const props = defineProps<{
  task: Task
}>()

defineEmits<{
  checkin: [task: Task]
}>()

const timeRange = computed(() => {
  const start = props.task.startTime?.slice(0, 5) ?? '未设置'
  const end = props.task.endTime?.slice(0, 5) ?? '未设置'
  return `${start} - ${end}`
})
</script>

<style scoped>
.task-card {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 78px;
  padding: 14px 0;
  border-bottom: 1px solid #edf1f4;
}

.task-card:last-child {
  border-bottom: 0;
}

.task-marker {
  display: flex;
  justify-content: center;
}

.task-marker span {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--gb-primary);
  box-shadow: 0 0 0 5px rgba(47, 125, 104, 0.1);
}

.task-card.completed .task-marker span {
  background: #91a0b5;
  box-shadow: 0 0 0 5px rgba(145, 160, 181, 0.12);
}

.task-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.task-title {
  overflow: hidden;
  color: var(--gb-text);
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
  margin-top: 7px;
  color: var(--gb-muted);
  font-size: 12px;
}

.task-meta span {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 8px;
  background: #f6f8fa;
}

@media (max-width: 640px) {
  .task-card {
    grid-template-columns: 14px minmax(0, 1fr);
  }

  .task-card > .el-button {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
