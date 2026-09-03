<template>
  <main class="journey-page">
    <div class="journey-shell">
      <PublicSiteHeader />

      <section class="journey-hero">
        <p>JOURNEY / 履历时间线</p>
        <h1>把走过的路，<br />排成一条时间线。</h1>
        <div class="journey-context">
          <span>学习路线、课程、项目、证书放在同一个坐标里。它不证明什么都做过，只记录认真做过什么。</span>
          <div class="journey-stats"><span v-for="stat in stats" :key="stat.label"><b>{{ stat.count }}</b>{{ stat.label }}</span></div>
        </div>
      </section>

      <section class="journey-filters" aria-label="时间线筛选">
        <button v-for="filter in filters" :key="filter.key" class="journey-filter" :class="{ 'is-active': activeFilter === filter.key }" type="button" @click="activeFilter = filter.key">
          {{ filter.label }} <small>{{ filter.count }}</small>
        </button>
      </section>

      <section class="journey-timeline">
        <article v-for="(item, index) in visibleItems" :key="item.id" class="timeline-item" :class="[`cat-${item.category}`, { 'is-highlight': item.highlight }]">
          <div class="timeline-index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="timeline-date">{{ item.date }}</div>
          <div class="timeline-body">
            <div class="timeline-meta"><span class="category-pill">{{ categoryLabel(item.category) }}</span><span v-if="item.highlight" class="now-pill">NOW</span></div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.description }}</p>
            <div v-if="item.tags?.length" class="timeline-tags"><span v-for="tag in item.tags" :key="tag"># {{ tag }}</span></div>
            <RouterLink v-if="item.link" :to="item.link.to" class="timeline-link">{{ item.link.label }} <b>→</b></RouterLink>
          </div>
        </article>

        <div v-if="!visibleItems.length" class="journey-empty">
          <p>证书档案还在整理中，之后会把完成的学习认证补进来。</p>
          <button type="button" @click="activeFilter = 'all'">查看全部经历</button>
        </div>
      </section>

      <section class="journey-closing">
        <span>KEEP<br />MAKING.</span>
        <div class="journey-closing-copy">
          <p>把时间留给重要的事。</p>
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
import BackToTopButton from '@/components/BackToTopButton.vue'
import PublicSiteHeader from '@/components/PublicSiteHeader.vue'
import { timelineItems } from '@/data/timeline'
import type { TimelineCategory } from '@/data/timeline'

const activeFilter = ref<'all' | TimelineCategory>('all')
const categoryLabels: Record<TimelineCategory, string> = { project: '项目', learning: '学习路线', course: '课程', certificate: '证书' }
const filters = computed(() => [
  { key: 'all' as const, label: '全部', count: timelineItems.length },
  ...(Object.keys(categoryLabels) as TimelineCategory[]).map((key) => ({
    key,
    label: categoryLabels[key],
    count: timelineItems.filter((item) => item.category === key).length
  }))
])
const stats = computed(() => filters.value.slice(1).map((filter) => ({ label: filter.label, count: filter.count })))
const visibleItems = computed(() => activeFilter.value === 'all' ? timelineItems : timelineItems.filter((item) => item.category === activeFilter.value))
function categoryLabel(category: TimelineCategory) { return categoryLabels[category] }
</script>

<style scoped>
.journey-page { min-height: 100vh; color: var(--text); background: var(--bg); }
.journey-shell { width: min(100% - 32px, 1180px); margin-inline: auto; padding-block: var(--space-3) 0; }
.journey-hero { display: flex; flex-direction: column; gap: var(--space-5); padding-block: var(--space-8); border-bottom: 1px solid var(--line); }
.journey-hero>p { color: var(--brand); font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.journey-hero h1 { color: var(--text); font-size: 46px; line-height: 1.12; }
.journey-context { display: flex; flex-direction: column; gap: var(--space-5); }
.journey-context>span { max-width: 36ch; color: var(--muted); font-size: 15px; line-height: 1.6; }
.journey-stats { display: flex; flex-wrap: wrap; gap: var(--space-3) var(--space-5); }
.journey-stats span { display: inline-flex; align-items: baseline; gap: var(--space-2); color: var(--muted); font-size: 12px; }
.journey-stats b { color: var(--brand-strong); font-family: var(--font-display); font-size: 20px; }
.journey-filters {
  position: sticky;
  top: 96px;
  z-index: 12;
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding: var(--space-3);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  background: var(--glass);
  box-shadow: inset 0 1px 0 var(--glass-highlight), var(--shadow-soft);
  backdrop-filter: blur(22px) saturate(1.2);
}
.journey-filter { display: inline-flex; align-items: center; gap: var(--space-2); padding: 8px 13px; border: 0; border-radius: 999px; color: var(--muted); background: transparent; font-size: 14px; font-weight: 700; cursor: pointer; transition: color .22s ease, background-color .22s ease; }
.journey-filter small { color: var(--subtle); font-size: 11px; }
.journey-filter:hover, .journey-filter.is-active { color: var(--brand-strong); background: var(--brand-soft); }
.journey-timeline { display: flex; flex-direction: column; padding-block: var(--space-5) var(--space-8); }
.timeline-item { display: grid; grid-template-columns: 44px 1fr; gap: var(--space-4); padding-block: var(--space-6); border-bottom: 1px solid var(--line); transition: padding .26s ease, background-color .26s ease; }
.timeline-item:hover { padding-inline: var(--space-3); background: var(--surface-soft); }
.timeline-index { color: var(--accent); font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.timeline-date { grid-column: 2; color: var(--brand-strong); font-size: 14px; font-weight: 800; }
.timeline-body { grid-column: 2; display: flex; max-width: 720px; flex-direction: column; gap: var(--space-3); }
.timeline-meta { display: flex; align-items: center; gap: var(--space-2); }
.category-pill, .now-pill { display: inline-flex; padding: 4px 9px; border-radius: 999px; color: var(--brand-strong); background: var(--brand-soft); font-size: 11px; font-weight: 800; letter-spacing: .06em; }
.now-pill { color: var(--text); background: var(--accent-soft); }
.timeline-body h2 { color: var(--text); font-size: 28px; line-height: 1.25; }
.timeline-body>p { color: var(--muted); font-size: 15px; line-height: 1.6; }
.timeline-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.timeline-tags span { color: var(--subtle); font-size: 12px; font-weight: 700; }
.timeline-link { display: inline-flex; align-items: center; gap: var(--space-2); align-self: flex-start; color: var(--brand-strong); font-size: 14px; font-weight: 800; text-decoration: none; }
.timeline-link b, .journey-closing-copy a b { transition: transform .24s ease; }
.timeline-link:hover b, .journey-closing-copy a:hover b { transform: translateX(5px); }
.journey-empty { display: flex; flex-direction: column; align-items: flex-start; gap: var(--space-4); padding-block: var(--space-8); color: var(--muted); font-size: 15px; }
.journey-empty button { padding: 9px 15px; border: 1px solid var(--line-strong); border-radius: 999px; color: var(--brand-strong); background: var(--surface); font-size: 14px; font-weight: 750; cursor: pointer; }
.journey-closing { display: flex; flex-direction: column; gap: var(--space-7); padding: var(--space-8) var(--space-5) var(--space-6); color: var(--on-brand); background: var(--brand); }
.journey-closing>span { font-family: var(--font-display); font-size: 58px; font-weight: 700; line-height: .86; }
.journey-closing-copy { display: flex; flex-direction: column; gap: var(--space-5); }
.journey-closing-copy>p { color: color-mix(in srgb, var(--surface) 76%, transparent); font-size: 15px; font-weight: 700; }
.journey-closing-copy nav { display: flex; flex-direction: column; gap: var(--space-2); }
.journey-closing-copy a { display: flex; align-items: center; justify-content: space-between; gap: var(--space-5); padding-bottom: var(--space-2); border-bottom: 1px solid color-mix(in srgb, var(--surface) 26%, transparent); color: var(--surface); font-size: 14px; font-weight: 750; text-decoration: none; }

@media (min-width: 760px) {
  .journey-shell { width: min(100% - 64px, 1180px); }
  .journey-hero { display: grid; grid-template-columns: .5fr 1.25fr .65fr; gap: var(--space-7); align-items: start; padding-block: 112px; }
  .journey-hero h1 { font-size: 68px; }
  .journey-filters { top: 84px; width: max-content; margin-inline: auto; transform: translateY(-18px); }
  .timeline-item { grid-template-columns: 58px 96px 1fr; gap: var(--space-5); padding-block: var(--space-7); }
  .timeline-date { grid-column: 2; }
  .timeline-body { grid-column: 3; }
  .timeline-body h2 { font-size: 38px; }
  .journey-closing { flex-direction: row; align-items: flex-end; justify-content: space-between; padding: 104px var(--space-7) var(--space-6); }
  .journey-closing>span { font-size: 104px; }
  .journey-closing-copy { min-width: 300px; }
}
</style>
