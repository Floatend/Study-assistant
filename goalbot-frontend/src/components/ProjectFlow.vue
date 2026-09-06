<template>
  <figure class="project-flow">
    <figcaption>{{ title }}</figcaption>
    <ol>
      <li v-for="(step, index) in steps" :key="step.title">
        <div class="flow-step">
          <strong>{{ step.title }}</strong>
          <p>{{ step.description }}</p>
        </div>
        <el-icon v-if="index < steps.length - 1" class="flow-arrow" aria-hidden="true"><Right /></el-icon>
      </li>
    </ol>
  </figure>
</template>

<script setup lang="ts">
import { Right } from '@element-plus/icons-vue'
import type { ProjectStory } from '@/data/projects'

defineProps<{ title: string; steps: ProjectStory['flow'] }>()
</script>

<style scoped>
.project-flow { display: flex; flex-direction: column; gap: var(--space-5); margin: 0; padding-block: var(--space-5); border-block: 1px solid var(--line); }
figcaption { color: var(--muted); font-size: 14px; }
ol { display: flex; flex-direction: column; gap: var(--space-4); margin: 0; padding: 0; list-style: none; }
li { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: var(--space-4); }
.flow-step { display: flex; flex-direction: column; gap: var(--space-2); flex: 1; }
strong { color: var(--brand-strong); font-size: 18px; }
p { margin: 0; color: var(--muted); font-size: 14px; line-height: 1.6; overflow-wrap: anywhere; }
.flow-arrow { color: var(--accent); font-size: 20px; transform: rotate(90deg); }
@media (min-width: 600px) {
  ol { flex-direction: row; }
  li { flex-direction: row; }
  .flow-arrow { align-self: center; flex-shrink: 0; transform: none; }
}
</style>
