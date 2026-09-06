<template>
  <LiquidBackdrop />
  <RouterView v-if="route.meta.public" />
  <AdminLayout v-else />
</template>

<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import LiquidBackdrop from '@/components/LiquidBackdrop.vue'

const route = useRoute()
const AdminLayout = defineAsyncComponent(() => import('@/layouts/AdminLayout.vue'))
</script>
