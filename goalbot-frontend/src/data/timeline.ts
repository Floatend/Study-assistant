export type TimelineCategory = 'project' | 'learning' | 'course' | 'certificate'

export interface TimelineLink {
  label: string
  to: string
}

export interface TimelineItem {
  id: string
  category: TimelineCategory
  date: string
  title: string
  description: string
  tags?: string[]
  link?: TimelineLink
  highlight?: boolean
}

// 公开时间线的数据入口：直接增删下面的条目即可，页面会自动更新筛选和统计。
// 证书（certificate）为空时会显示“待整理”空状态，填一条数据后会自动出现在时间线里。
export const timelineItems: TimelineItem[] = [
  {
    id: 'linge-site',
    category: 'project',
    date: '2026.08',
    title: 'linge.xin 个人知识站',
    description: '把公开主页、学习笔记与站长工作台整理成同一个站点：Vue 3 前端、Spring Boot 后端与 MySQL 数据模型，并持续打磨阅读体验。',
    tags: ['Vue 3', 'Spring Boot', 'MySQL'],
    link: { label: '查看学习笔记', to: '/notes' },
    highlight: true
  },
  {
    id: 'study-assistant',
    category: 'project',
    date: '2026.06',
    title: 'Study-assistant 全栈实践',
    description: '从目标、任务与打卡管理出发，完成前后端分离的全栈项目，并尝试接入 AI 建议与飞书机器人交互。',
    tags: ['全栈', 'AI 集成', '飞书']
  },
  {
    id: 'algorithm-training',
    category: 'learning',
    date: '2026.02',
    title: '算法与竞赛训练',
    description: '围绕 C++ 与数据结构持续训练，把常用算法模板和解题过程沉淀成可复用的笔记。',
    tags: ['C++', '数据结构', 'ACM']
  },
  {
    id: 'web-development',
    category: 'course',
    date: '2025.09',
    title: 'Web 开发基础',
    description: '学习 HTML、CSS、JavaScript 与前端工程化，并开始用真实项目检验课堂知识。',
    tags: ['HTML', 'CSS', 'JavaScript']
  },
  {
    id: 'discrete-math',
    category: 'course',
    date: '2025.09',
    title: '离散数学',
    description: '集合、关系、图论与逻辑是理解算法和程序结构的重要基础。',
    tags: ['集合论', '图论', '逻辑']
  },
  {
    id: 'advanced-math',
    category: 'course',
    date: '2025.03',
    title: '高等数学',
    description: '从极限、导数到积分，建立持续推导和把问题形式化的能力。',
    tags: ['微积分', '极限', '级数']
  },
  {
    id: 'note-archive',
    category: 'learning',
    date: '2024.10',
    title: '建立个人知识库',
    description: '开始用 Markdown 记录课程、项目与思考，逐渐形成分类、整理和公开写作的习惯。',
    tags: ['Markdown', 'Obsidian', '知识管理']
  }
]
