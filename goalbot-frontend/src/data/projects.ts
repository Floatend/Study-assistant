import { timelineItems, type TimelineItem } from './timeline'

export const projectIds = ['cloud-edge-capture', 'wechat-llm-agent', 'ceramic-commerce'] as const
export type ProjectId = typeof projectIds[number]

export interface ProjectStory {
  direction: string
  background: string
  flowTitle: string
  flow: Array<{ title: string; description: string }>
  focus: Array<{ title: string; description: string }>
  noteTopics: string[]
}

// Expand only resume-supported work. Dates, roles and deliverables stay in timeline.ts.
const stories: Record<ProjectId, ProjectStory> = {
  'cloud-edge-capture': {
    direction: '云边端协同 / 后端开发',
    background: '这个平台面向移动端、后端服务与边缘设备共同参与的智能识别抓取场景。我的工作集中在后端接口、跨端通信联调、服务部署，以及 AI 对话能力的接入。',
    flowTitle: '三端协作关系',
    flow: [
      { title: '移动端', description: '数据交互与应用入口' },
      { title: '后端服务', description: 'Spring Boot 接口与通信联调' },
      { title: '边缘设备', description: '参与识别抓取场景的软硬件协同' }
    ],
    focus: [
      { title: '接口之外，还有设备', description: '联调涉及移动端、后端和边缘设备。除了完成接口开发，我也参与软硬件协同下的响应延迟优化。' },
      { title: '从本地走到云端', description: '负责后端服务部署与云端联调，打通本地服务和云服务器，让跨端协作在部署环境中贯通。' },
      { title: '加入 AI 对话入口', description: '在平台中集成 AI 对话交互模块，将对话能力接入具体的应用场景。' }
    ],
    noteTopics: ['Spring Boot', '部署', 'AI']
  },
  'wechat-llm-agent': {
    direction: 'AI 应用 / 对话交互',
    background: '围绕微信群聊和日常聊天，我独立开发了基于 LLM 的智能体机器人。它把关键词回复、定时提醒、持续对话和多角色人设放在同一个交互入口里。',
    flowTitle: '对话处理概览',
    flow: [
      { title: '微信消息', description: '日常聊天与群聊交互' },
      { title: 'LLM 对话', description: '持续对话与多角色人设' },
      { title: '消息回复', description: '不同对话风格下的聊天回复' }
    ],
    focus: [
      { title: '对话不止一轮', description: '实现持续对话，并设计多角色人设切换机制，使机器人可以用不同的对话风格参与日常聊天。' },
      { title: '聊天之外的日常功能', description: '实现关键词自动回复和定时提醒，让消息入口也能承接日常提醒需求。' },
      { title: '面对调用频率限制', description: '针对并发请求与接口调用频率限制进行优化，关注机器人连续运行时的稳定性。' }
    ],
    noteTopics: ['LLM', 'AI', '并发']
  },
  'ceramic-commerce': {
    direction: '业务系统 / 3D 生成',
    background: '这个电商平台围绕陶瓷定制需求展开，将 3D 模型生成和展示接入业务流程。我担任后端负责人，负责核心业务接口、模型能力接入及核心数据的持久化设计。',
    flowTitle: '定制模型流程',
    flow: [
      { title: '用户定制', description: '陶瓷定制需求' },
      { title: '模型生成', description: '接入混元 3D 模型' },
      { title: '模型展示', description: '定制模型的展示流程' }
    ],
    focus: [
      { title: '让模型生成进入业务', description: '接入混元 3D 模型，设计陶瓷模型从生成到展示的流程，使模型能力服务于用户的定制需求。' },
      { title: '核心接口与数据持久化', description: '基于 Spring Boot 和 MyBatis 开发核心业务接口，并使用 MySQL 持久化管理用户、商品、订单等数据。' }
    ],
    noteTopics: ['MySQL', 'Spring Boot', 'MyBatis']
  }
}

export type PortfolioProject = TimelineItem & ProjectStory

export const projects: PortfolioProject[] = projectIds.map((id) => {
  const item = timelineItems.find((entry) => entry.id === id && entry.category === 'project')
  if (!item) throw new Error(`Missing project timeline record: ${id}`)
  return { ...item, ...stories[id] }
})

export function findProject(slug: unknown) {
  return typeof slug === 'string' ? projects.find((project) => project.id === slug) : undefined
}

export function adjacentProjects(slug: string) {
  const index = projects.findIndex((project) => project.id === slug)
  return {
    previous: index > 0 ? projects[index - 1] : undefined,
    next: index >= 0 ? projects[index + 1] : undefined
  }
}
