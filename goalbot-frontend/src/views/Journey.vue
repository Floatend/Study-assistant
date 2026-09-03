<template>
  <main class="journey-page">
    <div class="journey-shell">
      <PublicSiteHeader />

      <section class="journey-hero">
        <p>JOURNEY / 履历坐标</p>
        <h1>学习、项目与结果，<br />在同一个坐标里相遇。</h1>
        <div class="journey-context">
          <p>内容依据个人简历整理。时间轴保留真实的开始与结束月份，并把 2026 年同时推进的项目放进并行轨道。</p>
          <div class="journey-summary" aria-label="履历内容概览">
            <span><b>{{ educationItems.length }}</b> 段教育经历</span>
            <span><b>{{ projectItems.length }}</b> 个项目</span>
            <span><b>{{ achievements.length }}</b> 项竞赛与荣誉</span>
          </div>
        </div>
      </section>

      <nav class="journey-filters liquid-glass liquid-glass-strong" aria-label="履历视图筛选">
        <button
          v-for="filter in filters"
          :key="filter.key"
          class="journey-filter"
          :class="{ 'is-active': activeFilter === filter.key }"
          :aria-pressed="activeFilter === filter.key"
          type="button"
          @click="setFilter(filter.key)"
        >
          {{ filter.label }} <small>{{ filter.count }}</small>
        </button>
      </nav>

      <section v-if="activeFilter === 'all' || activeFilter === 'education'" class="journey-section overview-section" aria-labelledby="overview-title">
        <header class="section-heading">
          <div>
            <p>01 / OVERVIEW</p>
            <h2 id="overview-title">成长全程坐标</h2>
          </div>
          <p>从高中到大学，项目实践在 2026 年开始密集展开。轨道长度对应真实月份。</p>
        </header>

        <div class="timeline-scroll" tabindex="0" aria-label="横向成长时间轴">
          <div class="overview-canvas">
            <div class="map-axis" aria-hidden="true">
              <span class="axis-caption">TIME</span>
              <div class="axis-track">
                <span
                  v-for="(tick, index) in overviewTicks"
                  :key="tick.key"
                  class="axis-tick"
                  :class="{ 'is-start': index === 0, 'is-end': index === overviewTicks.length - 1 }"
                  :style="{ left: tick.left }"
                >{{ tick.label }}</span>
              </div>
            </div>

            <div class="map-lane">
              <span class="lane-name">教育</span>
              <div class="lane-track">
                <span v-for="tick in overviewTicks" :key="tick.key" class="map-guide" :style="{ left: tick.left }" aria-hidden="true" />
                <button
                  v-for="item in educationItems"
                  :key="item.id"
                  class="map-node education-node"
                  :class="{ 'is-selected': selectedId === item.id, 'is-current': item.current }"
                  :style="overviewItemStyle(item)"
                  :aria-label="`${item.period}，${item.title}`"
                  :aria-pressed="selectedId === item.id"
                  type="button"
                  @click="selectTimelineItem(item.id)"
                >
                  <small>{{ item.code }}</small>
                  <strong>{{ item.title }}</strong>
                  <i v-if="item.current" aria-hidden="true" />
                </button>
              </div>
            </div>

            <div v-if="activeFilter === 'all'" class="map-lane project-overview-lane">
              <span class="lane-name">项目</span>
              <div class="lane-track">
                <span v-for="tick in overviewTicks" :key="tick.key" class="map-guide" :style="{ left: tick.left }" aria-hidden="true" />
                <button
                  class="map-node project-cluster"
                  :class="{ 'is-selected': selectedItem?.category === 'project' }"
                  :style="projectClusterStyle"
                  type="button"
                  aria-label="查看 2026 年并行项目"
                  @click="selectTimelineItem('linge-site')"
                >
                  <small>P1-P4</small>
                  <strong>4 项并行</strong>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="activeFilter === 'all' || activeFilter === 'project'" class="journey-section project-section" aria-labelledby="project-title">
        <header class="section-heading">
          <div>
            <p>{{ activeFilter === 'all' ? '02' : '01' }} / PROJECT FOCUS</p>
            <h2 id="project-title">2026 项目并行轨道</h2>
          </div>
          <p>同一月份内并行推进的工作分轨展示，避免在普通列表里失去时间关系。</p>
        </header>

        <div class="project-scroll" tabindex="0" aria-label="2026 年项目月份图">
          <div class="project-grid">
            <span class="project-grid-year">2026</span>
            <div class="project-months" aria-hidden="true">
              <span v-for="month in months" :key="month">{{ month }}月</span>
            </div>

            <template v-for="(item, index) in projectItems" :key="item.id">
              <div class="project-row-label">
                <strong>{{ item.code }}</strong>
                <span>{{ item.role }}</span>
              </div>
              <div class="project-track">
                <span
                  v-for="month in months"
                  :key="month"
                  class="project-month-cell"
                  :class="{ 'is-current': isCurrentMonth(month), 'is-future': isFutureMonth(month) }"
                  aria-hidden="true"
                />
                <button
                  class="project-bar"
                  :class="{ 'is-selected': selectedId === item.id, 'is-current': item.current }"
                  :style="{ ...projectBarStyle(item), '--row-index': index }"
                  :title="`${item.title} · ${item.period}`"
                  :aria-label="`${item.title}，${item.period}，${item.role}`"
                  :aria-pressed="selectedId === item.id"
                  type="button"
                  @click="selectTimelineItem(item.id)"
                >
                  <span>{{ item.title }}</span>
                  <small>{{ item.current ? '至今' : '完成' }}</small>
                </button>
              </div>
            </template>
          </div>
        </div>
      </section>

      <section v-if="selectedItem && activeFilter !== 'achievement'" class="journey-inspector" aria-live="polite" aria-label="所选履历详情">
        <div class="inspector-meta">
          <span>{{ selectedItem.code }}</span>
          <p>{{ categoryLabel(selectedItem.category) }}</p>
          <time>{{ selectedItem.period }}</time>
        </div>
        <div class="inspector-summary">
          <p>{{ selectedItem.role }}</p>
          <h2>{{ selectedItem.title }}</h2>
          <span>{{ selectedItem.description }}</span>
          <div class="inspector-tags">
            <small v-for="tag in selectedItem.tags" :key="tag"># {{ tag }}</small>
          </div>
          <RouterLink v-if="selectedItem.link" :to="selectedItem.link.to">{{ selectedItem.link.label }} <b>→</b></RouterLink>
        </div>
        <ul v-if="selectedItem.details.length" class="inspector-details">
          <li v-for="detail in selectedItem.details" :key="detail">{{ detail }}</li>
        </ul>
      </section>

      <section v-if="activeFilter === 'all' || activeFilter === 'achievement'" class="journey-section achievement-section" aria-labelledby="achievement-title">
        <header class="section-heading">
          <div>
            <p>{{ activeFilter === 'all' ? '03' : '01' }} / OUTCOMES</p>
            <h2 id="achievement-title">竞赛与荣誉成果图谱</h2>
          </div>
          <p>简历未注明获奖月份，因此这里按赛事级别归类，不把未知日期强行映射到时间轴。</p>
        </header>

        <div class="achievement-map">
          <article v-for="group in achievementGroups" :key="group.scope" class="achievement-zone" :class="`scope-${group.scope}`">
            <header>
              <div><span>{{ group.code }}</span><h3>{{ group.label }}</h3></div>
              <strong>{{ group.items.length }}</strong>
            </header>
            <ol>
              <li v-for="item in group.items" :key="item.id">
                <i aria-hidden="true" />
                <div><h4>{{ item.title }}</h4><p>{{ item.result }}</p></div>
              </li>
            </ol>
          </article>
        </div>
      </section>

      <section class="journey-closing">
        <span>KEEP<br />MAKING.</span>
        <div class="journey-closing-copy">
          <p>把结果放回过程里，才看得见下一步。</p>
          <nav aria-label="继续浏览">
            <RouterLink to="/notes">学习笔记 <b>→</b></RouterLink>
            <RouterLink to="/about">关于这个站点 <b>→</b></RouterLink>
            <RouterLink to="/">回到首页 <b>→</b></RouterLink>
          </nav>
        </div>
      </section>
    </div>
    <BackToTopButton />
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { CSSProperties } from 'vue'
import BackToTopButton from '@/components/BackToTopButton.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import { achievements, timelineItems } from '@/data/timeline'
import type { AchievementScope, JourneyFilter, TimelineCategory, TimelineItem } from '@/data/timeline'

const activeFilter = ref<JourneyFilter>('all')
const selectedId = ref('linge-site')
const months = Array.from({ length: 12 }, (_, index) => index + 1)
const now = new Date()
const currentYear = now.getFullYear()
const currentMonth = now.getMonth() + 1
const timelineStart = monthSerial('2022-09')
const timelineEnd = Math.max(monthSerial('2026-09'), currentYear * 12 + currentMonth - 1)
const timelineSpan = timelineEnd - timelineStart + 1

const educationItems = timelineItems.filter((item) => item.category === 'education')
const projectItems = timelineItems.filter((item) => item.category === 'project')
const selectedItem = computed(() => timelineItems.find((item) => item.id === selectedId.value))
const filters = computed(() => [
  { key: 'all' as const, label: '全部', count: timelineItems.length + achievements.length },
  { key: 'education' as const, label: '教育', count: educationItems.length },
  { key: 'project' as const, label: '项目', count: projectItems.length },
  { key: 'achievement' as const, label: '荣誉', count: achievements.length }
])

const overviewTicks = computed(() => {
  const lastYear = Math.max(2026, currentYear)
  const ticks = [{ key: '2022-09', label: '2022.09' }]
  for (let year = 2023; year <= lastYear; year += 1) ticks.push({ key: `${year}-01`, label: String(year) })
  const currentKey = `${currentYear}-${String(currentMonth).padStart(2, '0')}`
  if (currentKey !== ticks[ticks.length - 1]?.key) ticks.push({ key: currentKey, label: '现在' })
  return ticks.map((tick) => ({ ...tick, left: `${overviewPercent(tick.key)}%` }))
})

const projectClusterStyle = computed<CSSProperties>(() => {
  const left = overviewPercent('2026-04')
  return { left: `${left}%`, width: `${Math.max(9, 100 - left)}%` }
})

const scopeMeta: Array<{ scope: AchievementScope; code: string; label: string }> = [
  { scope: 'national', code: 'A', label: '国赛成果' },
  { scope: 'provincial', code: 'B', label: '省级成果' },
  { scope: 'campus', code: 'C', label: '校级成果' }
]
const achievementGroups = computed(() => scopeMeta.map((group) => ({
  ...group,
  items: achievements.filter((item) => item.scope === group.scope)
})))

function monthSerial(value: string) {
  const [year, month] = value.split('-').map(Number)
  return year * 12 + month - 1
}

function overviewPercent(value: string) {
  return Math.max(0, Math.min(100, ((monthSerial(value) - timelineStart) / Math.max(1, timelineSpan - 1)) * 100))
}

function overviewItemStyle(item: TimelineItem): CSSProperties {
  const start = monthSerial(item.start)
  const end = item.current ? timelineEnd : monthSerial(item.end ?? item.start)
  return {
    left: `${((start - timelineStart) / timelineSpan) * 100}%`,
    width: `${Math.max(3, ((end - start + 1) / timelineSpan) * 100)}%`
  }
}

function projectBarStyle(item: TimelineItem): CSSProperties {
  const start = Number(item.start.slice(5, 7))
  const explicitEnd = item.end ? Number(item.end.slice(5, 7)) : undefined
  const presentEnd = currentYear < 2026 ? start : currentYear === 2026 ? currentMonth : 12
  const end = Math.max(start, Math.min(12, explicitEnd ?? presentEnd))
  return { gridColumn: `${start} / ${end + 1}` }
}

function isCurrentMonth(month: number) {
  return currentYear === 2026 && month === currentMonth
}

function isFutureMonth(month: number) {
  return currentYear === 2026 && month > currentMonth
}

function categoryLabel(category: TimelineCategory) {
  return category === 'education' ? '教育经历' : '项目实践'
}

function selectTimelineItem(id: string) {
  selectedId.value = id
}

function setFilter(filter: JourneyFilter) {
  activeFilter.value = filter
  if (filter === 'education') selectedId.value = educationItems[educationItems.length - 1]?.id ?? selectedId.value
  if (filter === 'project') selectedId.value = projectItems.find((item) => item.current)?.id ?? projectItems[0]?.id ?? selectedId.value
}
</script>

<style scoped>
.journey-page { min-height: 100vh; color: var(--text); background: transparent; }
.journey-shell { width: min(100% - 32px, 1180px); margin-inline: auto; padding-block: var(--space-3) 0; }
.journey-hero { display: flex; flex-direction: column; gap: var(--space-5); padding-block: var(--space-7); border-bottom: 1px solid var(--line); }
.journey-hero > p, .section-heading > div > p { color: var(--brand); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.journey-hero h1 { max-width: 15ch; color: var(--text); font-size: 40px; line-height: 1.12; }
.journey-context { display: flex; flex-direction: column; gap: var(--space-5); }
.journey-context > p { max-width: 44ch; color: var(--muted); font-size: 15px; line-height: 1.6; }
.journey-summary { display: flex; flex-wrap: wrap; gap: var(--space-3) var(--space-5); }
.journey-summary span { color: var(--muted); font-size: 13px; }
.journey-summary b { color: var(--brand-strong); font-family: var(--font-display); font-size: 20px; }
.journey-filters { display: flex; width: max-content; max-width: 100%; flex-wrap: wrap; gap: var(--space-1); padding: var(--space-2); border-radius: var(--radius-lg); }
.journey-filter { display: inline-flex; align-items: center; gap: var(--space-2); padding: 8px 12px; border: 0; border-radius: 999px; color: var(--muted); background: transparent; box-shadow: inset 0 -2px 0 transparent; font-size: 14px; font-weight: 700; cursor: pointer; transition: color .22s ease, box-shadow .22s ease, background-color .22s ease; }
.journey-filter small { color: var(--subtle); font-size: 11px; }
.journey-filter:hover { color: var(--brand-strong); box-shadow: inset 0 -2px 0 var(--brand); }
.journey-filter.is-active { color: var(--brand-strong); background: var(--brand-soft); box-shadow: none; }
.journey-section { display: grid; gap: var(--space-6); padding-block: var(--space-8); border-bottom: 1px solid var(--line); }
.section-heading { display: grid; gap: var(--space-4); }
.section-heading > div { display: grid; gap: var(--space-2); }
.section-heading h2 { max-width: 15ch; color: var(--text); font-size: 34px; line-height: 1.16; }
.section-heading > p { max-width: 42ch; color: var(--muted); font-size: 14px; line-height: 1.6; }
.timeline-scroll, .project-scroll { width: 100%; overflow-x: auto; overscroll-behavior-inline: contain; border-block: 1px solid var(--line); }
.overview-canvas { display: grid; min-width: 880px; gap: var(--space-3); padding-block: var(--space-5); }
.map-axis, .map-lane { display: grid; grid-template-columns: 84px minmax(0, 1fr); gap: var(--space-4); }
.axis-caption, .lane-name { align-self: center; color: var(--subtle); font-size: 11px; font-weight: 800; letter-spacing: .1em; }
.axis-track, .lane-track { position: relative; }
.axis-track { height: 34px; border-bottom: 1px solid var(--line-strong); }
.axis-tick { position: absolute; bottom: 8px; color: var(--subtle); font-size: 11px; font-variant-numeric: tabular-nums; white-space: nowrap; transform: translateX(-50%); }
.axis-tick::after { position: absolute; bottom: -13px; left: 50%; width: 1px; height: 7px; content: ''; background: var(--line-strong); }
.axis-tick.is-start { transform: none; }
.axis-tick.is-start::after { left: 0; }
.axis-tick.is-end { transform: translateX(-100%); }
.axis-tick.is-end::after { right: 0; left: auto; }
.lane-track { height: 68px; background: var(--surface-soft); }
.map-guide { position: absolute; inset-block: 0; width: 1px; background: var(--line); }
.map-node { position: absolute; top: 13px; z-index: 1; display: flex; height: 42px; min-width: 68px; align-items: center; gap: var(--space-2); overflow: hidden; padding-inline: var(--space-3); border: 1px solid var(--brand); border-radius: var(--radius-sm); color: var(--brand-strong); background: var(--brand-soft); cursor: pointer; animation: reveal-track .72s both; transition: color .2s ease, background-color .2s ease, box-shadow .2s ease; }
.map-node small { flex: 0 0 auto; font-size: 10px; font-weight: 900; }
.map-node strong { overflow: hidden; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.map-node i { width: 6px; height: 6px; flex: 0 0 auto; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 4px var(--accent-soft); }
.map-node:hover, .map-node.is-selected { color: var(--surface); background: var(--brand); box-shadow: 0 8px 24px var(--glass-shadow-color); }
.project-cluster { border-color: var(--accent); color: var(--text); background: var(--accent-soft); }
.project-cluster:hover, .project-cluster.is-selected { color: var(--text); background: var(--accent); }
.project-grid { display: grid; min-width: 920px; grid-template-columns: 112px minmax(0, 1fr); padding-block: var(--space-4); }
.project-grid-year { display: flex; align-items: flex-end; padding: 0 var(--space-3) var(--space-2) 0; color: var(--brand-strong); font-family: var(--font-display); font-size: 20px; font-weight: 700; }
.project-months { display: grid; grid-template-columns: repeat(12, minmax(58px, 1fr)); }
.project-months span { padding-bottom: var(--space-2); color: var(--subtle); font-size: 11px; text-align: center; font-variant-numeric: tabular-nums; }
.project-row-label { display: flex; min-height: 58px; flex-direction: column; justify-content: center; gap: var(--space-1); padding-right: var(--space-3); border-top: 1px solid var(--line); }
.project-row-label strong { color: var(--accent); font-size: 13px; }
.project-row-label span { color: var(--muted); font-size: 11px; }
.project-track { display: grid; min-height: 58px; grid-template-columns: repeat(12, minmax(58px, 1fr)); border-top: 1px solid var(--line); }
.project-month-cell { grid-row: 1; min-width: 0; border-left: 1px solid var(--line); }
.project-month-cell:last-of-type { border-right: 1px solid var(--line); }
.project-month-cell.is-current { background: var(--accent-soft); }
.project-month-cell.is-future { background: var(--surface-soft); }
.project-bar { z-index: 1; grid-row: 1; align-self: center; display: flex; min-width: 0; height: 36px; align-items: center; justify-content: space-between; gap: var(--space-2); margin-inline: 4px; overflow: hidden; padding-inline: var(--space-3); border: 1px solid var(--accent); border-radius: var(--radius-sm); color: var(--text); background: var(--accent-soft); cursor: pointer; animation: reveal-track .72s calc(var(--row-index) * 90ms) both; transition: color .2s ease, background-color .2s ease, box-shadow .2s ease; }
.project-bar span { overflow: hidden; font-size: 12px; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }
.project-bar small { flex: 0 0 auto; font-size: 10px; font-weight: 800; }
.project-bar:hover, .project-bar.is-selected { background: var(--accent); box-shadow: 0 8px 24px var(--glass-shadow-color); }
.journey-inspector { display: grid; gap: var(--space-6); padding-block: var(--space-7); border-bottom: 1px solid var(--line); }
.inspector-meta { display: flex; align-items: baseline; gap: var(--space-3); }
.inspector-meta > span { color: var(--accent); font-size: 12px; font-weight: 900; }
.inspector-meta p { color: var(--brand-strong); font-size: 12px; font-weight: 800; }
.inspector-meta time { color: var(--subtle); font-size: 12px; font-variant-numeric: tabular-nums; }
.inspector-summary { display: grid; gap: var(--space-3); }
.inspector-summary > p { color: var(--accent); font-size: 12px; font-weight: 800; }
.inspector-summary h2 { max-width: 18ch; color: var(--text); font-size: 30px; line-height: 1.18; }
.inspector-summary > span { max-width: 52ch; color: var(--muted); font-size: 15px; line-height: 1.65; }
.inspector-tags { display: flex; flex-wrap: wrap; gap: var(--space-2) var(--space-3); }
.inspector-tags small { color: var(--subtle); font-size: 11px; font-weight: 700; }
.inspector-summary a { display: inline-flex; align-items: center; gap: var(--space-2); width: max-content; color: var(--brand-strong); font-size: 13px; font-weight: 800; text-decoration: none; }
.inspector-summary a b, .journey-closing-copy a b { transition: transform .24s ease; }
.inspector-summary a:hover b, .journey-closing-copy a:hover b { transform: translateX(5px); }
.inspector-details { display: grid; gap: var(--space-3); padding: 0; list-style: none; }
.inspector-details li { position: relative; padding-left: var(--space-5); color: var(--muted); font-size: 14px; line-height: 1.6; }
.inspector-details li::before { position: absolute; top: .7em; left: 0; width: 12px; height: 1px; content: ''; background: var(--accent); }
.achievement-map { display: grid; }
.achievement-zone { display: grid; gap: var(--space-5); padding-block: var(--space-6); border-top: 1px solid var(--line); }
.achievement-zone > header { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-4); }
.achievement-zone > header > div { display: flex; align-items: baseline; gap: var(--space-3); }
.achievement-zone > header span { color: var(--accent); font-size: 11px; font-weight: 900; }
.achievement-zone > header h3 { color: var(--text); font-size: 24px; }
.achievement-zone > header > strong { color: var(--subtle); font-family: var(--font-display); font-size: 36px; }
.achievement-zone ol { position: relative; display: grid; gap: var(--space-4); padding: 0; list-style: none; }
.achievement-zone ol::before { position: absolute; inset-block: 7px; left: 5px; width: 1px; content: ''; background: var(--line-strong); }
.achievement-zone li { position: relative; z-index: 1; display: grid; grid-template-columns: 12px 1fr; gap: var(--space-3); align-items: start; }
.achievement-zone li > i { width: 11px; height: 11px; margin-top: 5px; border: 2px solid var(--surface); border-radius: 50%; background: var(--brand); box-shadow: 0 0 0 1px var(--brand); }
.achievement-zone li > div { display: grid; gap: var(--space-1); }
.achievement-zone h4 { color: var(--text); font-family: var(--font-body); font-size: 14px; line-height: 1.45; }
.achievement-zone li p { color: var(--muted); font-size: 12px; font-weight: 700; }
.scope-national li > i { background: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.scope-campus li > i { background: var(--subtle); box-shadow: 0 0 0 1px var(--subtle); }
.journey-closing { display: flex; flex-direction: column; gap: var(--space-7); padding: var(--space-8) var(--space-5) var(--space-6); color: var(--on-brand); background: var(--brand); }
.journey-closing > span { font-family: var(--font-display); font-size: 58px; font-weight: 700; line-height: .86; }
.journey-closing-copy { display: flex; flex-direction: column; gap: var(--space-5); }
.journey-closing-copy > p { color: color-mix(in srgb, var(--surface) 76%, transparent); font-size: 15px; font-weight: 700; }
.journey-closing-copy nav { display: flex; flex-direction: column; gap: var(--space-2); }
.journey-closing-copy a { display: flex; align-items: center; justify-content: space-between; gap: var(--space-5); padding-bottom: var(--space-2); border-bottom: 1px solid color-mix(in srgb, var(--surface) 26%, transparent); color: var(--surface); font-size: 14px; font-weight: 750; text-decoration: none; }

@keyframes reveal-track {
  from { clip-path: inset(0 100% 0 0); opacity: .45; }
  to { clip-path: inset(0); opacity: 1; }
}

@media (min-width: 760px) {
  .journey-shell { width: min(100% - 64px, 1180px); }
  .journey-hero { display: grid; grid-template-columns: .45fr 1.25fr .7fr; gap: var(--space-7); align-items: start; padding-block: 104px; }
  .journey-hero h1 { font-size: 62px; }
  .journey-filters { margin-inline: auto; }
  .section-heading { grid-template-columns: 1fr minmax(260px, .55fr); align-items: end; }
  .section-heading h2 { font-size: 44px; }
  .journey-inspector { grid-template-columns: 150px minmax(0, .95fr) minmax(0, 1fr); align-items: start; }
  .inspector-meta { flex-direction: column; gap: var(--space-2); }
  .inspector-summary h2 { font-size: 36px; }
  .achievement-map { grid-template-columns: 1.05fr 1.2fr 1fr; }
  .achievement-zone { padding: var(--space-6); border-top: 0; border-left: 1px solid var(--line); }
  .achievement-zone:first-child { padding-left: 0; border-left: 0; }
  .achievement-zone:last-child { padding-right: 0; }
  .journey-closing { flex-direction: row; align-items: flex-end; justify-content: space-between; padding: 104px var(--space-7) var(--space-6); }
  .journey-closing > span { font-size: 104px; }
  .journey-closing-copy { min-width: 300px; }
}

@media (prefers-reduced-motion: reduce) {
  .map-node, .project-bar { animation: none; }
}
</style>
