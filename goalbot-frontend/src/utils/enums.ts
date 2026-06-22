import type { OptionItem } from '@/types/common'

export const goalStatusOptions: OptionItem[] = [
  { label: '未开始', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已暂停', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已归档', value: 4 }
]

export const taskStatusOptions: OptionItem[] = [
  { label: '待完成', value: 0 },
  { label: '已完成', value: 2 }
]

export const priorityOptions: OptionItem[] = [
  { label: '低', value: 1 },
  { label: '中', value: 2 },
  { label: '高', value: 3 },
  { label: '紧急', value: 4 }
]

export const reviewTypeOptions: OptionItem[] = [
  { label: '每日复盘', value: 1 },
  { label: '每周复盘', value: 2 },
  { label: '月度复盘', value: 3 },
  { label: 'AI 建议', value: 4 }
]

export function labelOf(options: OptionItem[], value?: number) {
  return options.find((item) => item.value === value)?.label ?? '-'
}
