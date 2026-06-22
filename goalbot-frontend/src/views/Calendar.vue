<template>
  <section class="page-section">
    <div class="calendar-workbench">
      <div class="calendar-heading">
        <div class="calendar-kicker">Timetable</div>
        <h2>课程表工作台</h2>
        <p>把课程、任务和未来日程放进同一张时间表。</p>
      </div>
      <div class="calendar-filter">
        <el-date-picker
          v-model="range"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
        <el-button :icon="Refresh" plain @click="load">刷新</el-button>
      </div>
    </div>

    <div class="panel schedule-import-panel">
      <div class="import-shell">
        <div class="import-title-block">
          <div class="import-badge">ICS</div>
          <div>
            <h2 class="panel-title">导入课表</h2>
            <p>{{ selectedFile?.name || '支持 .ics 课程表和系统日历文件' }}</p>
          </div>
        </div>
        <div class="import-actions">
          <el-upload
            action="#"
            accept=".ics,text/calendar"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleFileChange"
          >
            <el-button :icon="Upload" plain>选择 ICS</el-button>
          </el-upload>
          <el-button :disabled="!selectedFile" :loading="importLoading" plain @click="previewImport">预览</el-button>
          <el-button
            type="primary"
            :disabled="!selectedFile || importableCount === 0"
            :loading="importLoading"
            @click="confirmImport"
          >
            导入 {{ importableCount || '' }}
          </el-button>
        </div>
      </div>

      <div class="panel-body import-body">
        <div class="import-controls">
          <el-date-picker
            v-model="importRange"
            type="daterange"
            start-placeholder="导入开始"
            end-placeholder="导入结束"
            value-format="YYYY-MM-DD"
          />
          <el-switch v-model="skipExisting" active-text="跳过重复任务" />
        </div>

        <template v-if="importResult">
          <div class="import-stats">
            <el-tag>源事件 {{ importResult.sourceEventCount }}</el-tag>
            <el-tag type="success">可导入 {{ importableCount }}</el-tag>
            <el-tag type="warning">已跳过 {{ importResult.skippedCount }}</el-tag>
            <el-tag v-if="!importResult.dryRun" type="success">已导入 {{ importResult.importedCount }}</el-tag>
          </div>

          <el-alert
            v-for="warning in importResult.warnings"
            :key="warning"
            class="import-warning"
            type="warning"
            :closable="false"
            :title="warning"
          />

          <el-table :data="importPreviewRows" max-height="320" size="small">
            <el-table-column prop="planDate" label="日期" width="112" />
            <el-table-column label="时间" width="150">
              <template #default="{ row }">
                {{ formatImportTime(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="title" label="日程" min-width="180" show-overflow-tooltip />
            <el-table-column prop="location" label="地点" min-width="140" show-overflow-tooltip />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="row.skipped" type="warning">跳过</el-tag>
                <el-tag v-else type="success">可导入</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="skipReason" label="说明" min-width="180" show-overflow-tooltip />
          </el-table>
        </template>
      </div>
    </div>

    <CalendarView :tasks="tasks" @select="selectedTasks = $event" />

    <el-drawer v-model="drawerVisible" title="任务详情" size="360px">
      <template v-if="selectedTasks.length">
        <template v-if="selectedTasks.length === 1 && selectedTask">
          <h3>{{ selectedTask.title }}</h3>
          <p class="empty-text">{{ selectedTask.description || '暂无描述' }}</p>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="日期">{{ selectedTask.planDate }}</el-descriptions-item>
            <el-descriptions-item label="时间">{{ formatTaskTime(selectedTask) }}</el-descriptions-item>
            <el-descriptions-item label="目标">{{ selectedTask.goalTitle || '-' }}</el-descriptions-item>
            <el-descriptions-item label="计划用时">{{ selectedTask.plannedMinutes }} 分钟</el-descriptions-item>
            <el-descriptions-item label="状态">{{ labelOf(taskStatusOptions, selectedTask.status) }}</el-descriptions-item>
          </el-descriptions>
        </template>

        <template v-else>
          <h3>时间冲突 {{ selectedTasks.length }} 项</h3>
          <el-alert
            class="conflict-alert"
            type="warning"
            :closable="false"
            title="这些任务存在时间重叠，可以考虑调整其中一个任务的开始或结束时间。"
          />
          <el-table :data="selectedTasks" size="small">
            <el-table-column prop="title" label="任务" min-width="120" show-overflow-tooltip />
            <el-table-column label="时间" width="118">
              <template #default="{ row }">{{ formatTaskTime(row) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }">
                <el-tag :type="row.status === 2 ? 'success' : 'warning'" effect="plain">
                  {{ labelOf(taskStatusOptions, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { fetchCalendarTasks, importIcs } from '@/api/task'
import CalendarView from '@/components/CalendarView.vue'
import type { IcsImportEvent, IcsImportResult, Task } from '@/types/task'
import { labelOf, taskStatusOptions } from '@/utils/enums'

const tasks = ref<Task[]>([])
const range = ref<[string, string] | []>([])
const importRange = ref<[string, string] | [] | null>([todayString(), addMonthsString(6)])
const selectedTasks = ref<Task[]>([])
const selectedTask = computed(() => selectedTasks.value[0] ?? null)
const selectedFile = ref<File | null>(null)
const importResult = ref<IcsImportResult | null>(null)
const importLoading = ref(false)
const skipExisting = ref(true)
const drawerVisible = computed({
  get: () => selectedTasks.value.length > 0,
  set: (value) => {
    if (!value) selectedTasks.value = []
  }
})
const importableCount = computed(() => importResult.value?.events.filter((event) => !event.skipped).length ?? 0)
const importPreviewRows = computed(() => importResult.value?.events.slice(0, 200) ?? [])

onMounted(load)
watch(range, load)

async function load() {
  tasks.value = await fetchCalendarTasks({
    startDate: range.value[0],
    endDate: range.value[1]
  })
}

function handleFileChange(uploadFile: UploadFile) {
  const rawFile = uploadFile.raw
  if (!rawFile) return
  if (!rawFile.name.toLowerCase().endsWith('.ics')) {
    ElMessage.warning('请选择 .ics 文件')
    return
  }
  selectedFile.value = rawFile
  importResult.value = null
}

async function previewImport() {
  if (!selectedFile.value) return
  importLoading.value = true
  try {
    importResult.value = await importIcs(selectedFile.value, {
      ...currentImportOptions(true)
    })
  } finally {
    importLoading.value = false
  }
}

async function confirmImport() {
  if (!selectedFile.value) return
  importLoading.value = true
  try {
    importResult.value = await importIcs(selectedFile.value, {
      ...currentImportOptions(false)
    })
    ElMessage.success(`已导入 ${importResult.value.importedCount} 条日程`)
    await load()
  } finally {
    importLoading.value = false
  }
}

function formatImportTime(event: IcsImportEvent) {
  if (event.allDay) return '全天'
  if (event.startTime && event.endTime) return `${event.startTime.slice(0, 5)} - ${event.endTime.slice(0, 5)}`
  if (event.startTime) return event.startTime.slice(0, 5)
  return '未设置'
}

function formatTaskTime(task: Task) {
  if (!task.startTime && !task.endTime) return '未设置'
  const start = task.startTime?.slice(0, 5) || '未设置'
  const end = task.endTime?.slice(0, 5) || '未设置'
  return `${start} - ${end}`
}

function currentImportOptions(dryRun: boolean) {
  const selectedRange = Array.isArray(importRange.value) ? importRange.value : []
  return {
    dryRun,
    startDate: selectedRange[0],
    endDate: selectedRange[1],
    skipExisting: skipExisting.value
  }
}

function todayString() {
  return new Date().toISOString().slice(0, 10)
}

function addMonthsString(months: number) {
  const date = new Date()
  date.setMonth(date.getMonth() + months)
  return date.toISOString().slice(0, 10)
}
</script>

<style scoped>
.calendar-workbench {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(47, 125, 104, 0.08), rgba(82, 103, 216, 0.08)),
    #ffffff;
  box-shadow: 0 1px 2px rgba(27, 39, 51, 0.03);
}

.calendar-kicker {
  color: var(--gb-primary);
  font-size: 12px;
  font-weight: 820;
  text-transform: uppercase;
}

.calendar-heading h2 {
  margin: 6px 0 0;
  color: var(--gb-text);
  font-size: 24px;
  line-height: 1.15;
}

.calendar-heading p {
  margin: 7px 0 0;
  color: var(--gb-muted);
  font-size: 13px;
}

.calendar-filter {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.schedule-import-panel {
  background:
    linear-gradient(180deg, #ffffff, #fbfcfd);
}

.import-shell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 16px 18px;
  border-bottom: 1px solid #edf0f5;
}

.import-title-block {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.import-title-block p {
  margin: 5px 0 0;
  color: var(--gb-muted);
  font-size: 13px;
}

.import-badge {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid rgba(82, 103, 216, 0.22);
  border-radius: 8px;
  color: #3344a5;
  font-size: 12px;
  font-weight: 850;
  background: #eef1ff;
}

.import-actions,
.import-controls,
.import-stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.import-body {
  display: grid;
  gap: 14px;
}

.import-warning {
  margin: 0;
}

.conflict-alert {
  margin: 10px 0 14px;
}

@media (max-width: 860px) {
  .calendar-workbench,
  .import-shell {
    align-items: flex-start;
    flex-direction: column;
  }

  .calendar-filter,
  .import-actions {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
