<template>
  <div class="panel ai-advice-card">
    <div class="panel-header advice-header">
      <div>
        <div class="advice-kicker">AI Planner</div>
        <h2 class="panel-title">日程建议</h2>
        <p class="advice-range">{{ rangeText }}</p>
      </div>
      <div class="advice-tools">
        <div class="range-switch" :style="{ '--active-index': activeIndex }">
          <button
            v-for="option in rangeOptions"
            :key="option.value"
            type="button"
            :class="{ active: option.value === days }"
            @click="emit('days-change', option.value)"
          >
            {{ option.label }}
          </button>
        </div>
        <el-tag v-if="stale" effect="plain" type="warning">更新中</el-tag>
        <el-tag v-else effect="plain" type="success">已同步</el-tag>
      </div>
    </div>
    <div class="panel-body advice-content" v-loading="loading">
      <MarkdownContent v-if="content" :content="content" />
      <p v-else class="empty-text">{{ loading ? '正在生成日程建议...' : '暂无建议' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const props = defineProps<{
  content?: string
  loading?: boolean
  stale?: boolean
  days?: number
  startDate?: string
  endDate?: string
}>()

const emit = defineEmits<{
  'days-change': [days: number]
}>()

const rangeOptions = [
  { label: '今天', value: 1 },
  { label: '今明', value: 2 },
  { label: '近三天', value: 3 }
]

const days = computed(() => props.days ?? 2)
const activeIndex = computed(() => Math.max(0, rangeOptions.findIndex((option) => option.value === days.value)))
const rangeText = computed(() => {
  if (props.startDate && props.endDate) {
    return props.startDate === props.endDate ? props.startDate : `${props.startDate} 至 ${props.endDate}`
  }
  return days.value === 1 ? '只看今天' : `未来 ${days.value} 天`
})
</script>

<style scoped>
.ai-advice-card {
  min-height: 100%;
}

.advice-header {
  align-items: flex-start;
  background: linear-gradient(180deg, #fbfcff, #ffffff);
}

.advice-kicker {
  margin-bottom: 5px;
  color: var(--gb-accent);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.advice-range {
  margin: 5px 0 0;
  color: var(--gb-muted);
  font-size: 12px;
}

.advice-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.range-switch {
  position: relative;
  display: grid;
  grid-template-columns: repeat(3, minmax(54px, 1fr));
  min-width: 198px;
  padding: 3px;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: #f4f7f8;
}

.range-switch::before {
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 3px;
  width: calc((100% - 6px) / 3);
  border-radius: 6px;
  background: #ffffff;
  box-shadow: 0 4px 14px rgba(25, 35, 45, 0.1);
  transform: translateX(calc(var(--active-index) * 100%));
  transition: transform 0.18s ease;
  content: "";
}

.range-switch button {
  position: relative;
  z-index: 1;
  height: 28px;
  border: 0;
  border-radius: 6px;
  color: var(--gb-muted);
  font: inherit;
  font-size: 12px;
  font-weight: 750;
  background: transparent;
  cursor: pointer;
}

.range-switch button.active {
  color: var(--gb-primary-dark);
}

.advice-content {
  min-height: 220px;
  line-height: 1.72;
}

.advice-content p {
  margin: 0;
}

@media (max-width: 760px) {
  .advice-tools {
    justify-content: flex-start;
    width: 100%;
  }

  .range-switch {
    width: 100%;
  }
}
</style>
