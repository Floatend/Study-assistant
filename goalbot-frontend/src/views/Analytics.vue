<template>
  <section class="page-section">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker
          v-model="range"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </div>
      <div class="toolbar-right">
        <el-button :icon="Refresh" plain @click="load">刷新</el-button>
      </div>
    </div>

    <div class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">每日投入</h2>
        </div>
        <div class="panel-body">
          <StudyChart :points="duration" />
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">任务状态</h2>
        </div>
        <div class="panel-body">
          <div ref="statusChartRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <div class="panel">
        <div class="panel-header">
          <h2 class="panel-title">最近打卡</h2>
        </div>
        <div class="panel-body">
          <el-table :data="recent" size="small">
            <el-table-column prop="taskTitle" label="任务" min-width="120" />
            <el-table-column prop="actualMinutes" label="分钟" width="80" />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import {
  fetchAnalyticsRecentCheckins,
  fetchStudyDuration,
  fetchTaskStatus
} from '@/api/dashboard'
import StudyChart from '@/components/StudyChart.vue'
import type { Checkin } from '@/types/checkin'
import type { TaskStatusCount, TrendPoint } from '@/types/dashboard'
import { labelOf, taskStatusOptions } from '@/utils/enums'

const range = ref<[string, string] | []>([])
const duration = ref<TrendPoint[]>([])
const statuses = ref<TaskStatusCount[]>([])
const recent = ref<Checkin[]>([])
const statusChartRef = ref<HTMLDivElement>()
let statusChart: echarts.ECharts | null = null

onMounted(load)
onBeforeUnmount(() => {
  statusChart?.dispose()
})
watch(range, load)

async function load() {
  const params = { startDate: range.value[0], endDate: range.value[1] }
  const [durationData, statusData, recentData] = await Promise.all([
    fetchStudyDuration(params),
    fetchTaskStatus(params),
    fetchAnalyticsRecentCheckins(10)
  ])
  duration.value = durationData
  statuses.value = statusData
  recent.value = recentData
  await nextTick()
  renderCharts()
}

function renderCharts() {
  if (statusChartRef.value) {
    statusChart = statusChart || echarts.init(statusChartRef.value)
    statusChart.setOption({
      color: ['#3b82f6', '#2f7d68'],
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['48%', '72%'],
          data: statuses.value.map((item) => ({
            name: labelOf(taskStatusOptions, item.status),
            value: item.count
          }))
        }
      ]
    })
  }
}
</script>
