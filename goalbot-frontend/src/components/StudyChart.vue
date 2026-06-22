<template>
  <div ref="chartRef" class="chart-box"></div>
</template>

<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { TrendPoint } from '@/types/dashboard'

const props = withDefaults(
  defineProps<{
    points: TrendPoint[]
    chartType?: 'line' | 'bar'
  }>(),
  {
    chartType: 'line'
  }
)

const chartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    color: ['#2f7d68', '#3b82f6'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 28, bottom: 32 },
    xAxis: {
      type: 'category',
      data: props.points.map((item) => item.date.slice(5)),
      axisTick: { show: false }
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf1f4' } } },
    series: [
      {
        name: '实际用时',
        type: props.chartType,
        smooth: true,
        data: props.points.map((item) => item.minutes)
      },
      {
        name: '完成任务',
        type: 'bar',
        data: props.points.map((item) => item.completedTasks)
      }
    ]
  })
}

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

watch(() => props.points, renderChart, { deep: true })
</script>
