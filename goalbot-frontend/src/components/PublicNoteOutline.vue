<template>
  <nav class="note-outline" aria-label="文章目录">
    <button v-for="heading in headings" :key="heading.id" :class="{ active: activeId === heading.id }"
      :aria-current="activeId === heading.id ? 'location' : undefined" :style="{ '--heading-indent': `${Math.max(0, heading.level - baseLevel) * 12}px` }"
      type="button" @click="$emit('select', heading.id)">{{ heading.text }}</button>
    <p v-if="!headings.length">正文没有标题</p>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ headings: { id: string; text: string; level: number }[]; activeId: string }>()
defineEmits<{ select: [id: string] }>()
const baseLevel = computed(() => Math.min(...props.headings.map((heading) => heading.level)))
</script>

<style scoped>
.note-outline { display:flex; flex-direction:column; gap:var(--space-1); }
.note-outline button { width:100%; min-height:44px; padding:var(--space-2) var(--space-2) var(--space-2) calc(var(--space-2) + var(--heading-indent)); border:0; border-radius:var(--radius-sm); color:var(--muted); background:transparent; font-size:14px; line-height:1.5; text-align:left; overflow-wrap:anywhere; cursor:pointer; transition:color .2s ease, background-color .2s ease; }
.note-outline button:hover, .note-outline button.active { color:var(--brand-strong); background:var(--brand-soft); }
.note-outline p { margin:0; color:var(--muted); font-size:14px; }
</style>
