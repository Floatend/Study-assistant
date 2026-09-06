<template>
  <span><template v-for="(part, index) in parts" :key="index"><mark v-if="part.match">{{ part.text }}</mark><template v-else>{{ part.text }}</template></template></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { highlightMatches } from '@/utils/noteReading'
const props = defineProps<{ text: string; keyword: string }>()
const parts = computed(() => highlightMatches(props.text, props.keyword))
</script>

<style scoped>
mark { padding:0; color:var(--text); background:var(--accent-soft); font-weight:650; }
</style>
