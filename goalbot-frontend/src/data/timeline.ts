export type TimelineCategory = 'education' | 'project'
export type JourneyFilter = 'all' | TimelineCategory | 'achievement'
export type AchievementScope = 'national' | 'provincial' | 'campus'

export interface TimelineLink {
  label: string
  to: string
}

export interface TimelineItem {
  id: string
  code: string
  category: TimelineCategory
  start: string
  end?: string
  period: string
  title: string
  role?: string
  description: string
  details: string[]
  tags: string[]
  link?: TimelineLink
  current?: boolean
}

export interface AchievementItem {
  id: string
  scope: AchievementScope
  title: string
  result: string
}

// Public journey data is taken from the owner's resume. Keep undated awards out
// of the chronological scale so the page never implies dates that were not given.
export const timelineItems: TimelineItem[] = [
  {
    id: 'zhengzhou-high-school',
    code: 'E1',
    category: 'education',
    start: '2022-09',
    end: '2025-06',
    period: '2022.09 - 2025.06',
    title: '郑州中学',
    role: '高中阶段',
    description: '在郑州中学完成高中阶段学习。',
    details: [],
    tags: ['郑州中学']
  },
  {
    id: 'henan-university',
    code: 'E2',
    category: 'education',
    start: '2025-09',
    period: '2025.09 - 至今',
    title: '河南大学 · 网络工程',
    role: '网络工程专业九班',
    description: '进入河南大学学习网络工程，大一上学期平均加权成绩位于专业前 5%。',
    details: ['大一上学期平均加权成绩位于专业前 5%。'],
    tags: ['网络工程', '专业前 5%'],
    current: true
  },
  {
    id: 'cloud-edge-capture',
    code: 'P1',
    category: 'project',
    start: '2026-04',
    end: '2026-05',
    period: '2026.04 - 2026.05',
    title: '云边端智能识别抓取平台',
    role: '后端负责人',
    description: '面向移动端、后端服务与边缘设备协同场景，完成接口、通信联调、部署和 AI 对话模块。',
    details: [
      '基于 Spring Boot 设计并开发移动端数据交互接口。',
      '参与移动端、后端服务与边缘设备的通信联调，优化软硬件协同场景下的响应延迟。',
      '完成后端服务部署与云端联调，打通本地服务与云服务器。',
      '集成 AI 对话交互模块。'
    ],
    tags: ['Spring Boot', '云边端协同', 'AI 对话'],
    link: { label: '查看项目详情', to: '/projects/cloud-edge-capture' }
  },
  {
    id: 'wechat-llm-agent',
    code: 'P2',
    category: 'project',
    start: '2026-04',
    period: '2026.04 - 至今',
    title: '基于 LLM 的微信智能体机器人',
    role: '个人开发',
    description: '搭建面向微信群聊场景的 LLM 智能体机器人，支持自动回复、定时提醒、持续对话与多角色切换。',
    details: [
      '实现关键词自动回复、定时提醒与持续对话。',
      '设计多角色人设切换机制，支持不同对话风格下的日常聊天与群聊交互。',
      '针对并发请求和接口调用频率限制进行优化，提升运行稳定性。'
    ],
    tags: ['LLM', '微信机器人', '多角色对话'],
    link: { label: '查看项目详情', to: '/projects/wechat-llm-agent' },
    current: true
  },
  {
    id: 'ceramic-commerce',
    code: 'P3',
    category: 'project',
    start: '2026-05',
    period: '2026.05 - 至今',
    title: '3D 定制陶瓷电商平台',
    role: '后端负责人',
    description: '围绕用户定制需求，完成后端核心业务、3D 模型生成展示流程与数据持久化设计。',
    details: [
      '基于 Spring Boot 与 MyBatis 完成核心业务接口开发。',
      '接入混元 3D 模型，设计 3D 陶瓷模型生成与展示流程。',
      '设计 MySQL 表结构，持久化管理用户、商品和订单等核心数据。'
    ],
    tags: ['Spring Boot', 'MyBatis', 'MySQL', '混元 3D'],
    link: { label: '查看项目详情', to: '/projects/ceramic-commerce' },
    current: true
  },
  {
    id: 'linge-site',
    code: 'P4',
    category: 'project',
    start: '2026-05',
    period: '2026.05 - 至今',
    title: '个人网站 linge.xin',
    role: '个人开发',
    description: '搭建个人技术博客，记录学习笔记、项目实践与个人履历，负责网站部署维护。',
    details: [
      '建设个人技术博客；早期探索过目标监督、咨询推送与项目复盘工具，当前站点聚焦公开笔记和个人作品展示。',
      '负责 Linux 环境、Nginx 反向代理、域名解析以及网站部署维护。'
    ],
    tags: ['个人技术博客', 'Linux', 'Nginx'],
    link: { label: '查看学习笔记', to: '/notes' },
    current: true
  }
]

export const achievements: AchievementItem[] = [
  { id: 'lanqiao', scope: 'provincial', title: '蓝桥杯程序设计竞赛', result: '省级二等奖' },
  { id: 'huawei-ict', scope: 'provincial', title: '华为 ICT 大赛', result: '省级三等奖' },
  { id: 'computer-design', scope: 'provincial', title: '中国大学生计算机设计大赛', result: '省级三等奖' },
  { id: 'matiji', scope: 'national', title: '码蹄杯程序设计竞赛', result: '国赛铜奖' },
  { id: 'innovation', scope: 'campus', title: '中国国际大学生创新竞赛', result: '校级二等奖' },
  { id: 'baidu-star', scope: 'national', title: '百度之星程序设计大赛', result: '国赛铜奖' },
  { id: 'robot-ai', scope: 'campus', title: '中国机器人及人工智能大赛', result: '校级三等奖' },
  { id: 'innovation-training', scope: 'campus', title: '大学生创新创业训练计划项目', result: '校级立项' }
]
