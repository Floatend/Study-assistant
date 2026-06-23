<template>
  <section class="page-section settings-page" v-loading="loading">
    <div class="settings-hero">
      <div>
        <div class="settings-kicker">Assistant Config</div>
        <h2>主动消息与集成配置</h2>
        <p>把飞书连接、主动提醒节奏和 AI 内容源放在一个地方，方便按日常使用流调整。</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" plain @click="loadSettings">刷新</el-button>
        <el-button :icon="Promotion" type="primary" :loading="saving" @click="saveSettings">保存配置</el-button>
      </div>
    </div>

    <div class="settings-grid">
      <div class="settings-main">
        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">连接基础</h2>
              <p class="panel-subtitle">先确认机器人能不能主动找到你。这里不放 API Key，只保存当前用户的推送目标。</p>
            </div>
            <el-switch v-model="form.proactiveEnabled" size="large" />
          </div>
          <div class="panel-body settings-form">
            <div class="setting-row">
              <div>
                <div class="setting-title">启用主动消息</div>
                <p>关闭后，定时提醒和间隔规划提醒都会暂停。</p>
              </div>
              <el-tag :type="form.proactiveEnabled ? 'success' : 'info'" effect="plain">
                {{ form.proactiveEnabled ? '已开启' : '已暂停' }}
              </el-tag>
            </div>

            <div class="setting-row">
              <div>
                <div class="setting-title">飞书应用机器人推送</div>
                <p>用于每日提醒、AI 资讯和主动规划提醒；自然语言对话不受这个开关影响。</p>
              </div>
              <el-switch v-model="form.feishuEnabled" />
            </div>

            <div class="setting-block">
              <label>飞书 chat_id</label>
              <el-input
                v-model="form.feishuChatId"
                clearable
                placeholder="首次私聊机器人后自动填写，也可手动覆盖"
              />
              <p class="field-help">只自动绑定已关联用户的首次私聊；群聊不会覆盖。清空保存后，下次私聊会重新绑定。</p>
            </div>

            <div class="quiet-settings">
              <div class="setting-row">
                <div>
                  <div class="setting-title">静默时段</div>
                  <p>适合睡觉、上课或不希望被打扰的时间。静默时段内不会主动推送。</p>
                </div>
                <el-switch v-model="form.quietEnabled" />
              </div>
              <div class="quiet-range">
                <el-time-picker
                  v-model="form.quietStartTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  :clearable="false"
                  :disabled="!form.quietEnabled"
                />
                <span>至</span>
                <el-time-picker
                  v-model="form.quietEndTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  :clearable="false"
                  :disabled="!form.quietEnabled"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">主动节奏</h2>
              <p class="panel-subtitle">固定提醒负责一天的边界，间隔提醒负责在白天轻推你的计划。</p>
            </div>
            <el-icon class="panel-icon"><Bell /></el-icon>
          </div>
          <div class="panel-body reminder-list">
            <div class="reminder-item">
              <div class="reminder-copy">
                <el-icon><Sunrise /></el-icon>
                <div>
                  <div class="setting-title">早间任务提醒</div>
                  <p>到点发送今日任务；如果当天没有任务，会主动问你今天要安排什么。</p>
                </div>
              </div>
              <div class="reminder-controls">
                <el-switch v-model="form.morningEnabled" />
                <el-time-picker
                  v-model="form.morningTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  :clearable="false"
                  :disabled="!form.morningEnabled"
                />
              </div>
            </div>

            <div class="reminder-item highlight">
              <div class="reminder-copy">
                <el-icon><Timer /></el-icon>
                <div>
                  <div class="setting-title">主动规划提醒</div>
                  <p>按间隔根据今日任务轻推一次，适合让机器人像日程管家一样持续跟进。</p>
                </div>
              </div>
              <div class="reminder-controls nudge-controls">
                <el-switch v-model="form.periodicNudgeEnabled" />
                <el-select
                  v-model="form.periodicNudgeIntervalHours"
                  :disabled="!form.periodicNudgeEnabled"
                  style="width: 132px"
                >
                  <el-option
                    v-for="item in nudgeIntervalOptions"
                    :key="item"
                    :label="`${item} 小时一条`"
                    :value="item"
                  />
                </el-select>
              </div>
            </div>

            <div class="reminder-item">
              <div class="reminder-copy">
                <el-icon><Moon /></el-icon>
                <div>
                  <div class="setting-title">晚间复盘提醒</div>
                  <p>提醒你回顾当天投入、没完成的原因，以及明天最重要的一件事。</p>
                </div>
              </div>
              <div class="reminder-controls">
                <el-switch v-model="form.reviewEnabled" />
                <el-time-picker
                  v-model="form.reviewTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  :clearable="false"
                  :disabled="!form.reviewEnabled"
                />
              </div>
            </div>

            <div class="reminder-item">
              <div class="reminder-copy">
                <el-icon><Calendar /></el-icon>
                <div>
                  <div class="setting-title">每周周报提醒</div>
                  <p>周末整理完成率、计划投入和下周要调整的方向。</p>
                </div>
              </div>
              <div class="reminder-controls weekly-controls">
                <el-switch v-model="form.weeklyEnabled" />
                <el-select v-model="form.weeklyDay" :disabled="!form.weeklyEnabled" style="width: 118px">
                  <el-option v-for="item in weekDayOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
                <el-time-picker
                  v-model="form.weeklyTime"
                  format="HH:mm"
                  value-format="HH:mm:ss"
                  :clearable="false"
                  :disabled="!form.weeklyEnabled"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">AI 资讯源</h2>
              <p class="panel-subtitle">用于每日主动推送资讯解读，例如公众号文章的 RSS 或聚合源。</p>
            </div>
            <el-switch v-model="form.aiBriefingEnabled" />
          </div>
          <div class="panel-body ai-source-layout">
            <div class="setting-block">
              <label>推送时间</label>
              <el-time-picker
                v-model="form.aiBriefingTime"
                format="HH:mm"
                value-format="HH:mm:ss"
                :clearable="false"
                :disabled="!form.aiBriefingEnabled"
              />
            </div>
            <div class="setting-block">
              <label>来源名称</label>
              <el-input
                v-model="form.aiBriefingSourceName"
                clearable
                placeholder="例如：橘鸦Juya"
                :disabled="!form.aiBriefingEnabled"
              />
            </div>
            <div class="setting-block source-url">
              <label>资讯源 URL</label>
              <el-input
                v-model="form.aiBriefingSourceUrl"
                clearable
                placeholder="填写 RSS、聚合源或单篇文章 URL"
                :disabled="!form.aiBriefingEnabled"
              />
              <p class="field-help">微信公众号没有稳定公开 API，建议填可访问的 RSS 或聚合源；普通文章链接可用于手动测试。</p>
            </div>
          </div>
        </div>
      </div>

      <div class="settings-side">
        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">集成状态</h2>
              <p class="panel-subtitle">只展示可用性，敏感配置仍在后端环境变量中。</p>
            </div>
            <el-icon class="panel-icon"><Connection /></el-icon>
          </div>
          <div class="panel-body status-list">
            <div v-for="item in statusItems" :key="item.label" class="status-item">
              <div>
                <div class="status-title">{{ item.label }}</div>
                <p>{{ item.description }}</p>
              </div>
              <el-tag :type="item.ok ? 'success' : 'danger'" effect="plain">
                {{ item.ok ? '可用' : '未配置' }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">AI 建议范围</h2>
              <p class="panel-subtitle">控制首页建议默认参考今天、今明两天或近三天。</p>
            </div>
            <el-icon class="panel-icon"><SwitchButton /></el-icon>
          </div>
          <div class="panel-body">
            <el-radio-group v-model="form.adviceDays" class="advice-radio">
              <el-radio-button :label="1">今天</el-radio-button>
              <el-radio-button :label="2">今明</el-radio-button>
              <el-radio-button :label="3">近三天</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="panel">
          <div class="panel-header">
            <div>
              <h2 class="panel-title">立即推送</h2>
              <p class="panel-subtitle">保存后再发送，用来验证飞书和消息内容。</p>
            </div>
            <el-icon class="panel-icon"><Setting /></el-icon>
          </div>
          <div class="panel-body send-actions">
            <el-button :icon="Promotion" type="primary" plain :loading="testLoading" @click="sendTest">
              测试消息
            </el-button>
            <el-button plain :loading="sendingType === 'daily-task'" @click="sendNow('daily-task')">
              推送今日任务
            </el-button>
            <el-button plain :loading="sendingType === 'periodic-nudge'" @click="sendNow('periodic-nudge')">
              推送规划提醒
            </el-button>
            <el-button plain :loading="sendingType === 'daily-review'" @click="sendNow('daily-review')">
              推送复盘提醒
            </el-button>
            <el-button plain :loading="sendingType === 'weekly-review'" @click="sendNow('weekly-review')">
              推送周报提醒
            </el-button>
            <el-button plain :loading="sendingType === 'ai-briefing'" @click="sendNow('ai-briefing')">
              推送 AI 资讯
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  Calendar,
  Connection,
  Moon,
  Promotion,
  Refresh,
  Setting,
  Sunrise,
  SwitchButton,
  Timer
} from '@element-plus/icons-vue'
import {
  fetchAssistantSettings,
  sendAssistantMessageNow,
  sendAssistantTestMessage,
  updateAssistantSettings
} from '@/api/settings'
import { useDashboardStore } from '@/stores/dashboard'
import type { AssistantSettings, ProactiveMessageType } from '@/types/settings'

type SettingsForm = {
  proactiveEnabled: boolean
  feishuEnabled: boolean
  feishuChatId: string
  morningEnabled: boolean
  morningTime: string
  reviewEnabled: boolean
  reviewTime: string
  weeklyEnabled: boolean
  weeklyDay: number
  weeklyTime: string
  periodicNudgeEnabled: boolean
  periodicNudgeIntervalHours: number
  aiBriefingEnabled: boolean
  aiBriefingTime: string
  aiBriefingSourceName: string
  aiBriefingSourceUrl: string
  adviceDays: number
  quietEnabled: boolean
  quietStartTime: string
  quietEndTime: string
}

const loading = ref(false)
const saving = ref(false)
const testLoading = ref(false)
const sendingType = ref<ProactiveMessageType | ''>('')
const settings = ref<AssistantSettings | null>(null)
const dashboardStore = useDashboardStore()

const form = reactive<SettingsForm>({
  proactiveEnabled: true,
  feishuEnabled: true,
  feishuChatId: '',
  morningEnabled: true,
  morningTime: '08:00:00',
  reviewEnabled: true,
  reviewTime: '22:30:00',
  weeklyEnabled: true,
  weeklyDay: 7,
  weeklyTime: '21:00:00',
  periodicNudgeEnabled: false,
  periodicNudgeIntervalHours: 3,
  aiBriefingEnabled: false,
  aiBriefingTime: '09:30:00',
  aiBriefingSourceName: '橘鸦Juya',
  aiBriefingSourceUrl: '',
  adviceDays: 2,
  quietEnabled: false,
  quietStartTime: '23:30:00',
  quietEndTime: '07:30:00'
})

const weekDayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
]

const nudgeIntervalOptions = [1, 2, 3, 4, 6, 8, 12, 24]

const statusItems = computed(() => {
  const value = settings.value
  return [
    {
      label: '全局提醒开关',
      ok: value?.globalReminderEnabled ?? false,
      description: '由后端 GOALBOT_REMINDER_ENABLED 控制。'
    },
    {
      label: '飞书应用凭证',
      ok: value?.feishuAppConfigured ?? false,
      description: '需要 FEISHU_APP_ID 和 FEISHU_APP_SECRET。'
    },
    {
      label: '飞书目标会话',
      ok: value?.effectiveFeishuChatConfigured ?? false,
      description: '来自本页 chat_id 或 FEISHU_DEFAULT_CHAT_ID。'
    },
    {
      label: '长连接事件',
      ok: value?.feishuLongConnectionEnabled ?? false,
      description: '用于接收飞书消息并回复。'
    },
    {
      label: 'Dify Chat',
      ok: value?.difyChatConfigured ?? false,
      description: '用于自由对话、建议和复盘生成。'
    },
    {
      label: 'Dify Workflow',
      ok: value?.difyWorkflowConfigured ?? false,
      description: '用于结构化意图识别。'
    },
    {
      label: '每日 AI 资讯源',
      ok: value?.aiBriefingSourceConfigured ?? false,
      description: '来自本页填写的文章或 RSS 源 URL。'
    }
  ]
})

onMounted(loadSettings)

async function loadSettings() {
  loading.value = true
  try {
    const data = await fetchAssistantSettings()
    settings.value = data
    Object.assign(form, {
      proactiveEnabled: data.proactiveEnabled,
      feishuEnabled: data.feishuEnabled,
      feishuChatId: data.feishuChatId ?? '',
      morningEnabled: data.morningEnabled,
      morningTime: data.morningTime,
      reviewEnabled: data.reviewEnabled,
      reviewTime: data.reviewTime,
      weeklyEnabled: data.weeklyEnabled,
      weeklyDay: data.weeklyDay,
      weeklyTime: data.weeklyTime,
      periodicNudgeEnabled: data.periodicNudgeEnabled,
      periodicNudgeIntervalHours: data.periodicNudgeIntervalHours,
      aiBriefingEnabled: data.aiBriefingEnabled,
      aiBriefingTime: data.aiBriefingTime,
      aiBriefingSourceName: data.aiBriefingSourceName || '橘鸦Juya',
      aiBriefingSourceUrl: data.aiBriefingSourceUrl ?? '',
      adviceDays: data.adviceDays,
      quietEnabled: data.quietEnabled,
      quietStartTime: data.quietStartTime ?? '23:30:00',
      quietEndTime: data.quietEndTime ?? '07:30:00'
    })
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  saving.value = true
  try {
    settings.value = await updateAssistantSettings({
      ...form,
      feishuChatId: form.feishuChatId.trim(),
      aiBriefingSourceName: form.aiBriefingSourceName.trim() || '橘鸦Juya',
      aiBriefingSourceUrl: form.aiBriefingSourceUrl.trim() || null
    })
    dashboardStore.setAdviceDays(form.adviceDays)
    ElMessage.success('配置已保存')
  } finally {
    saving.value = false
  }
}

async function sendTest() {
  testLoading.value = true
  try {
    await saveSettings()
    await sendAssistantTestMessage()
    ElMessage.success('测试消息已发送')
  } finally {
    testLoading.value = false
  }
}

async function sendNow(type: ProactiveMessageType) {
  sendingType.value = type
  try {
    await saveSettings()
    await sendAssistantMessageNow(type)
    ElMessage.success('已触发推送')
  } finally {
    sendingType.value = ''
  }
}
</script>

<style scoped>
.settings-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--gb-shadow);
}

.settings-kicker {
  color: var(--gb-primary);
  font-size: 12px;
  font-weight: 800;
}

.settings-hero h2 {
  margin: 7px 0 0;
  color: var(--gb-text);
  font-size: 24px;
}

.settings-hero p,
.panel-subtitle,
.setting-row p,
.reminder-copy p,
.status-item p {
  margin: 6px 0 0;
  color: var(--gb-muted);
  font-size: 12px;
  line-height: 1.6;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.settings-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(330px, 0.75fr);
  gap: 18px;
  align-items: start;
}

.settings-main,
.settings-side {
  display: grid;
  gap: 18px;
}

.panel-icon {
  color: var(--gb-primary);
  font-size: 20px;
}

.settings-form {
  display: grid;
  gap: 18px;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.setting-title {
  color: var(--gb-text);
  font-size: 14px;
  font-weight: 800;
}

.setting-block {
  display: grid;
  gap: 8px;
}

.setting-block label {
  color: var(--gb-muted);
  font-size: 12px;
  font-weight: 750;
}

.field-help {
  margin: 4px 0 0;
  color: var(--gb-subtle);
  font-size: 12px;
  line-height: 1.55;
}

.quiet-settings {
  display: grid;
  gap: 12px;
}

.quiet-range {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.quiet-range span {
  color: var(--gb-muted);
  font-size: 13px;
}

.reminder-list {
  display: grid;
  gap: 12px;
}

.reminder-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px;
  border: 1px solid var(--gb-border);
  border-radius: 8px;
  background: #fbfcfc;
}

.reminder-item.highlight {
  border-color: rgba(32, 117, 107, 0.28);
  background: #f6fbfa;
}

.reminder-copy {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.reminder-copy .el-icon {
  margin-top: 2px;
  color: var(--gb-primary);
  font-size: 18px;
}

.reminder-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.weekly-controls {
  min-width: 380px;
}

.nudge-controls {
  min-width: 210px;
}

.ai-source-layout {
  display: grid;
  grid-template-columns: minmax(150px, 0.36fr) minmax(180px, 0.44fr);
  gap: 14px;
}

.source-url {
  grid-column: 1 / -1;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0f5;
}

.status-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.status-title {
  color: var(--gb-text);
  font-size: 13px;
  font-weight: 800;
}

.advice-radio {
  width: 100%;
}

.send-actions {
  display: grid;
  gap: 10px;
}

.send-actions .el-button {
  width: 100%;
  margin-left: 0;
}

@media (max-width: 980px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }

  .weekly-controls,
  .nudge-controls {
    min-width: 0;
  }

  .ai-source-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .settings-hero,
  .setting-row,
  .reminder-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions,
  .reminder-controls {
    justify-content: flex-start;
    width: 100%;
  }
}
</style>
