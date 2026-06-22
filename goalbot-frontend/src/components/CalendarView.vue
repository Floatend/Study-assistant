<template>
  <div class="panel calendar-panel">
    <FullCalendar :options="calendarOptions" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import listPlugin from '@fullcalendar/list'
import type { Task } from '@/types/task'
import { labelOf, taskStatusOptions } from '@/utils/enums'

const props = defineProps<{
  tasks: Task[]
}>()

const emit = defineEmits<{
  select: [tasks: Task[]]
}>()

const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin],
  initialView: 'timeGridWeek',
  locale: 'zh-cn',
  height: 'auto',
  firstDay: 1,
  nowIndicator: true,
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay,listWeek'
  },
  slotEventOverlap: false,
  eventMaxStack: 2,
  dayMaxEvents: 3,
  moreLinkText: '更多',
  events: calendarEvents.value,
  eventClick: (info: { event: { id: string; extendedProps: { taskIds?: number[] } } }) => {
    const taskIds = info.event.extendedProps.taskIds ?? [Number(info.event.id)]
    const selected = taskIds
      .map((id) => taskById.value.get(id))
      .filter((task): task is Task => Boolean(task))
    if (selected.length) emit('select', selected)
  }
}))

const taskById = computed(() => new Map(props.tasks.map((task) => [task.id, task])))

const calendarEvents = computed(() => {
  const normalizedTasks = props.tasks.filter((task) => task.status === 0 || task.status === 2)
  const timedTasks = normalizedTasks
    .filter((task) => Boolean(task.startTime))
    .map((task) => ({
      task,
      startMinute: toMinute(task.startTime),
      endMinute: resolveEndMinute(task)
    }))
    .sort((a, b) => a.task.planDate.localeCompare(b.task.planDate) || a.startMinute - b.startMinute)

  const untimedEvents = normalizedTasks
    .filter((task) => !task.startTime)
    .map((task) => taskEvent(task))

  const groups: Array<Array<(typeof timedTasks)[number]>> = []
  const byDate = new Map<string, typeof timedTasks>()
  timedTasks.forEach((item) => {
    const list = byDate.get(item.task.planDate) ?? []
    list.push(item)
    byDate.set(item.task.planDate, list)
  })

  byDate.forEach((items) => {
    let current: typeof items = []
    let currentEnd = -1
    items.forEach((item) => {
      if (!current.length || item.startMinute < currentEnd) {
        current.push(item)
        currentEnd = Math.max(currentEnd, item.endMinute)
        return
      }
      groups.push(current)
      current = [item]
      currentEnd = item.endMinute
    })
    if (current.length) {
      groups.push(current)
    }
  })

  return [
    ...untimedEvents,
    ...groups.map((group) => (group.length > 1 ? conflictEvent(group) : taskEvent(group[0].task)))
  ]
})

function taskEvent(task: Task) {
  const completed = task.status === 2
  return {
    id: String(task.id),
    title: `${task.title} · ${labelOf(taskStatusOptions, task.status)}`,
    start: task.startTime ? `${task.planDate}T${task.startTime}` : task.planDate,
    end: task.endTime ? `${task.planDate}T${task.endTime}` : undefined,
    backgroundColor: completed ? '#2f7d68' : '#3b82f6',
    borderColor: completed ? '#2f7d68' : '#3b82f6',
    classNames: [completed ? 'calendar-event-completed' : 'calendar-event-pending'],
    extendedProps: {
      taskIds: [task.id]
    }
  }
}

function conflictEvent(group: Array<{ task: Task; startMinute: number; endMinute: number }>) {
  const first = group[0]
  const startMinute = Math.min(...group.map((item) => item.startMinute))
  const endMinute = Math.max(...group.map((item) => item.endMinute))
  const previewTitles = group.slice(0, 2).map((item) => item.task.title).join('、')
  const pendingCount = group.filter((item) => item.task.status !== 2).length
  const completedCount = group.length - pendingCount
  return {
    id: `conflict:${group.map((item) => item.task.id).join(',')}`,
    title: `时间冲突 ${group.length} 项 · ${previewTitles}`,
    start: `${first.task.planDate}T${fromMinute(startMinute)}`,
    end: `${first.task.planDate}T${fromMinute(endMinute)}`,
    backgroundColor: '#b45309',
    borderColor: '#b45309',
    classNames: ['calendar-event-conflict'],
    extendedProps: {
      taskIds: group.map((item) => item.task.id),
      pendingCount,
      completedCount
    }
  }
}

function resolveEndMinute(task: Task) {
  const start = toMinute(task.startTime)
  const explicitEnd = task.endTime ? toMinute(task.endTime) : null
  if (explicitEnd !== null && explicitEnd > start) {
    return explicitEnd
  }
  return start + Math.max(task.plannedMinutes || 30, 15)
}

function toMinute(time?: string) {
  if (!time) return 0
  const [hour = '0', minute = '0'] = time.split(':')
  return Number(hour) * 60 + Number(minute)
}

function fromMinute(value: number) {
  const normalized = Math.max(0, Math.min(value, 23 * 60 + 59))
  const hour = Math.floor(normalized / 60).toString().padStart(2, '0')
  const minute = (normalized % 60).toString().padStart(2, '0')
  return `${hour}:${minute}:00`
}
</script>

<style scoped>
.calendar-panel {
  padding: 14px;
  background:
    linear-gradient(180deg, #ffffff, #fbfcfd);
}

.calendar-panel :deep(.fc) {
  color: var(--gb-text);
  font-size: 13px;
}

.calendar-panel :deep(.fc-toolbar) {
  gap: 12px;
  margin-bottom: 14px;
  padding: 4px 2px 12px;
  border-bottom: 1px solid #edf0f5;
}

.calendar-panel :deep(.fc-toolbar-title) {
  color: var(--gb-text);
  font-size: 18px;
  font-weight: 850;
}

.calendar-panel :deep(.fc-button) {
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  color: var(--gb-text);
  font-weight: 750;
  background: #ffffff;
  box-shadow: none;
}

.calendar-panel :deep(.fc-button:hover),
.calendar-panel :deep(.fc-button-primary:not(:disabled).fc-button-active) {
  border-color: rgba(47, 125, 104, 0.28);
  color: var(--gb-primary-dark);
  background: var(--gb-primary-soft);
}

.calendar-panel :deep(.fc-scrollgrid),
.calendar-panel :deep(.fc-theme-standard td),
.calendar-panel :deep(.fc-theme-standard th) {
  border-color: #e9edf3;
}

.calendar-panel :deep(.fc-col-header-cell) {
  background: #f8fafb;
}

.calendar-panel :deep(.fc-col-header-cell-cushion) {
  padding: 9px 6px;
  color: var(--gb-muted);
  font-weight: 800;
}

.calendar-panel :deep(.fc-timegrid-slot-label) {
  color: var(--gb-subtle);
  font-size: 12px;
}

.calendar-panel :deep(.fc-event) {
  overflow: hidden;
  border: 0;
  border-radius: 7px;
  box-shadow: 0 6px 14px rgba(25, 35, 45, 0.12);
}

.calendar-panel :deep(.fc-event-main) {
  padding: 2px 4px;
  font-weight: 750;
}

.calendar-panel :deep(.calendar-event-completed) {
  opacity: 0.78;
}

.calendar-panel :deep(.calendar-event-completed .fc-event-title) {
  text-decoration: line-through;
}

.calendar-panel :deep(.calendar-event-pending) {
  box-shadow: 0 6px 14px rgba(59, 130, 246, 0.18);
}

.calendar-panel :deep(.calendar-event-conflict) {
  box-shadow: 0 7px 18px rgba(180, 83, 9, 0.24);
}

.calendar-panel :deep(.fc-day-today) {
  background: rgba(47, 125, 104, 0.06);
}
</style>
