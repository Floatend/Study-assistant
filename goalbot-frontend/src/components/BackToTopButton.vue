<template>
  <Transition name="back-to-top">
    <button
      v-if="visible"
      class="back-to-top-button"
      type="button"
      title="回到开头"
      aria-label="回到开头"
      @click="scrollToTop"
    >
      <el-icon><Top /></el-icon>
      <span>回到开头</span>
    </button>
  </Transition>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Top } from '@element-plus/icons-vue'

const visible = ref(false)

function updateVisibility() {
  visible.value = window.scrollY > 420
}

function scrollToTop() {
  const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth'
  window.scrollTo({ top: 0, behavior })
}

onMounted(() => {
  updateVisibility()
  window.addEventListener('scroll', updateVisibility, { passive: true })
})

onBeforeUnmount(() => window.removeEventListener('scroll', updateVisibility))
</script>

<style scoped>
.back-to-top-button {
  position: fixed;
  right: clamp(16px, 3vw, 38px);
  bottom: 24px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 38px;
  padding: 0 13px;
  border: 1px solid #d8e1fb;
  border-radius: 999px;
  color: #3559e8;
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 10px 26px rgba(35, 57, 126, .16);
  backdrop-filter: blur(10px);
  font: inherit;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: border-color .2s ease, color .2s ease, transform .2s ease, box-shadow .2s ease;
}

.back-to-top-button:hover {
  border-color: #4d6bfe;
  color: #1c318e;
  box-shadow: 0 12px 30px rgba(35, 57, 126, .22);
  transform: translateY(-3px);
}

.back-to-top-enter-active,
.back-to-top-leave-active {
  transition: opacity .2s ease, transform .2s ease;
}

.back-to-top-enter-from,
.back-to-top-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (prefers-reduced-motion: reduce) {
  .back-to-top-button,
  .back-to-top-enter-active,
  .back-to-top-leave-active {
    transition: none;
  }
}
</style>
