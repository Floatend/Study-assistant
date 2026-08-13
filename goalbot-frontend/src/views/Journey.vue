<template>
  <main class="journey-page">
    <div class="journey-shell">
      <PublicSiteHeader />

      <section class="journey-hero">
        <p>JOURNEY / 履历时间线</p>
        <h1>把走过的路，<br />排成一条时间线。</h1>
        <span>学习路线、课程、项目、证书放在同一个坐标里。它不证明什么都做过，只记录认真做过什么。</span>
        <div class="journey-stats">
          <div v-for="stat in stats" :key="stat.label" class="journey-stat"><strong>{{ stat.count }}</strong><span>{{ stat.label }}</span></div>
        </div>
      </section>

      <section class="journey-filters" aria-label="时间线筛选">
        <button
          v-for="filter in filters"
          :key="filter.key"
          class="journey-filter"
          :class="{ 'is-active': activeFilter === filter.key }"
          type="button"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }} <small>{{ filter.count }}</small>
        </button>
      </section>

      <section class="journey-timeline">
        <article
          v-for="(item, index) in visibleItems"
          :key="item.id"
          class="timeline-item"
          :class="[`cat-${item.category}`, { 'is-highlight': item.highlight }]"
        >
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

const categoryLabels: Record<TimelineCategory, string> = {
  project: '项目',
  learning: '学习路线',
  course: '课程',
  certificate: '证书'
}

const filters = computed(() => [
  { key: 'all' as const, label: '全部', count: timelineItems.length },
  ...(Object.keys(categoryLabels) as TimelineCategory[]).map((key) => ({
    key,
    label: categoryLabels[key],
    count: timelineItems.filter((item) => item.category === key).length
  }))
])

const stats = computed(() => filters.value.slice(1).map((filter) => ({
  label: filter.label,
  count: filter.count
})))

const visibleItems = computed(() => activeFilter.value === 'all'
  ? timelineItems
  : timelineItems.filter((item) => item.category === activeFilter.value))

function categoryLabel(category: TimelineCategory) {
  return categoryLabels[category]
}
</script>

<style scoped>
.journey-page { min-height:100vh; color:var(--gb-text); background:var(--gb-bg); }
.journey-shell { width:min(1180px,calc(100% - 8vw)); margin:0 auto; }

.journey-hero {
  display:grid; grid-template-columns:.52fr 1.3fr .6fr; gap:30px; align-items:start;
  padding:clamp(72px,12vw,160px) 0 clamp(42px,6vw,76px);
  border-bottom:1px solid var(--gb-border);
}
.journey-hero>p { grid-column:1; margin:0; color:var(--gb-primary); font-size:11px; font-weight:800; letter-spacing:.14em; }
.journey-hero h1 { grid-column:2; margin:0; color:var(--gb-text); font-size:clamp(44px,5.8vw,78px); font-weight:800; line-height:1.1; }
.journey-hero>span { grid-column:3; padding-top:12px; color:var(--gb-muted); font-size:15px; line-height:1.9; }

.journey-stats { grid-column:1 / -1; display:grid; grid-template-columns:repeat(4,1fr); margin-top:34px; border-top:1px solid var(--gb-border); }
.journey-stat { padding:24px 26px 4px 0; }
.journey-stat + .journey-stat { padding-left:26px; border-left:1px solid var(--gb-border); }
.journey-stat strong { display:block; color:var(--gb-text); font-size:clamp(34px,4vw,52px); font-weight:800; line-height:1; }
.journey-stat span { display:block; margin-top:8px; color:var(--gb-muted); font-size:12px; font-weight:700; }

.journey-filters { display:flex; flex-wrap:wrap; gap:10px; padding:clamp(30px,5vw,54px) 0 24px; }
.journey-filter {
  display:inline-flex; align-items:center; gap:8px; padding:9px 16px; border:1px solid var(--gb-border-strong);
  border-radius:999px; color:var(--gb-muted); background:var(--gb-surface); font-family:inherit; font-size:13px; font-weight:700;
  cursor:pointer; transition:color .22s ease,border-color .22s ease,background-color .22s ease,transform .22s cubic-bezier(.16,1,.3,1);
}
.journey-filter small { color:var(--gb-subtle); font-size:11px; font-weight:700; }
.journey-filter:hover { color:var(--gb-primary-dark); border-color:var(--gb-primary); transform:translateY(-2px); }
.journey-filter.is-active { color:var(--gb-primary-dark); border-color:var(--gb-primary); background:var(--gb-primary-soft); }
.journey-filter.is-active small { color:var(--gb-primary); }

.journey-timeline { position:relative; padding:18px 0 clamp(46px,7vw,92px); }
.journey-timeline::before { content:''; position:absolute; top:0; bottom:0; left:154px; width:1px; background:var(--gb-border-strong); }

.timeline-item {
  position:relative; display:grid; grid-template-columns:70px 84px 1fr; gap:24px; align-items:start;
  padding:36px 0 38px 0; border-bottom:1px solid var(--gb-border);
  transition:transform .34s cubic-bezier(.16,1,.3,1),background-color .34s ease,box-shadow .34s ease;
}
.timeline-item::before {
  content:''; position:absolute; top:46px; left:calc(154px - 5px); width:11px; height:11px; border-radius:50%;
  background:#4d6bfe; box-shadow:0 0 0 6px rgba(77,107,254,.12);
}
.timeline-item:hover { position:relative; z-index:1; transform:translateX(10px); }
.timeline-item.is-highlight { border-bottom-color:rgba(77,107,254,.32); }

.cat-learning::before { background:#0f8f7e; box-shadow:0 0 0 6px rgba(15,143,126,.12); }
.cat-course::before { background:#e67f45; box-shadow:0 0 0 6px rgba(230,127,69,.12); }
.cat-certificate::before { background:#c8952d; box-shadow:0 0 0 6px rgba(200,149,45,.12); }

.timeline-index { padding-top:4px; color:var(--gb-subtle); font-size:12px; font-weight:800; letter-spacing:.08em; }
.timeline-date { padding-top:4px; color:var(--gb-text); font-size:14px; font-weight:800; letter-spacing:.02em; }
.timeline-body { max-width:720px; }
.timeline-meta { display:flex; align-items:center; gap:9px; }
.category-pill { display:inline-flex; padding:4px 10px; border-radius:999px; color:#3559e8; background:#eef2ff; font-size:11px; font-weight:800; letter-spacing:.06em; }
.cat-learning .category-pill { color:#0b7568; background:#e2f6f1; }
.cat-course .category-pill { color:#b95f2d; background:#fdf0e7; }
.cat-certificate .category-pill { color:#9a6d16; background:#fff4d6; }
.now-pill { padding:4px 10px; border-radius:999px; color:#0b7568; background:#d9f4ec; font-size:11px; font-weight:800; letter-spacing:.08em; }
.timeline-body h2 { margin:13px 0 9px; color:var(--gb-text); font-size:clamp(24px,3vw,38px); font-weight:800; line-height:1.25; }
.timeline-body>p { margin:0; color:var(--gb-muted); font-size:15px; line-height:1.86; }
.timeline-tags { display:flex; flex-wrap:wrap; gap:7px; margin-top:17px; }
.timeline-tags span { color:var(--gb-subtle); font-size:12px; font-weight:700; }
.timeline-link { display:inline-flex; align-items:center; gap:8px; margin-top:18px; color:var(--gb-primary); font-size:13px; font-weight:800; text-decoration:none; }
.timeline-link b { font-size:17px; transition:transform .25s ease; }
.timeline-link:hover b { transform:translateX(5px); }

.journey-empty { padding:64px 0; text-align:center; color:var(--gb-muted); font-size:15px; line-height:1.9; }
.journey-empty p { margin:0 0 18px; }
.journey-empty button { padding:9px 17px; border:1px solid var(--gb-border-strong); border-radius:999px; color:var(--gb-primary); background:var(--gb-surface); font-family:inherit; font-size:13px; font-weight:750; cursor:pointer; }
.journey-empty button:hover { border-color:var(--gb-primary); background:var(--gb-primary-soft); }

.journey-closing {
  display:grid; grid-template-columns:1.2fr .8fr; gap:clamp(28px,6vw,90px); align-items:end;
  margin:0 -4vw; padding:clamp(70px,11vw,140px) 4vw 48px;
  background:linear-gradient(118deg,#111b4d 0%,#1b2a68 55%,#243580 100%);
}
.journey-closing>span { color:#fff; font-size:clamp(56px,10vw,126px); font-weight:800; line-height:.82; }
.journey-closing-copy>p { margin:0 0 25px; color:var(--gb-mint); font-size:16px; font-weight:700; }
.journey-closing-copy nav { display:grid; gap:9px; }
.journey-closing-copy a { display:inline-flex; align-items:center; justify-content:space-between; padding-bottom:8px; border-bottom:1px solid rgba(255,255,255,.22); color:#fff; font-size:14px; font-weight:750; text-decoration:none; }
.journey-closing-copy a b { font-size:17px; transition:transform .25s ease; }
.journey-closing-copy a:hover b { transform:translateX(5px); }

@media(max-width:760px){
  .journey-shell { width:min(100% - 32px,1180px); }
  .journey-hero { grid-template-columns:1fr; gap:19px; }
  .journey-hero>p, .journey-hero h1, .journey-hero>span { grid-column:auto; }
  .journey-stats { grid-template-columns:repeat(2,1fr); row-gap:26px; }
  .journey-stat:nth-child(3) { padding-left:0; border-left:0; }
  .journey-stat:nth-child(n+3) { padding-top:24px; border-top:1px solid var(--gb-border); }
  .journey-timeline::before { left:20px; }
  .timeline-item { grid-template-columns:44px 1fr; gap:12px; padding:30px 0; }
  .timeline-item::before { left:calc(20px - 5px); top:38px; }
  .timeline-index { display:none; }
  .timeline-body { grid-column:2; }
  .timeline-date { padding-top:3px; }
  .timeline-item:hover { transform:none; }
  .journey-closing { grid-template-columns:1fr; align-items:start; margin:0 -16px; padding-right:16px; padding-left:16px; }
  .journey-closing>span { font-size:68px; }
}

@media (prefers-reduced-motion: reduce) {
  .timeline-item, .journey-filter { transition:none; }
  .timeline-item:hover, .journey-filter:hover { transform:none; }
  .timeline-link b, .journey-closing-copy a b { transition:none; }
}
</style>
