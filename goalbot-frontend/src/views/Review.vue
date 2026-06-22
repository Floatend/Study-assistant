<template>
  <section class="page-section">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="filters.type" clearable placeholder="类型" style="width: 160px">
          <el-option v-for="item in reviewTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="range"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </div>
      <div class="toolbar-right">
        <el-button :icon="Search" plain @click="load">查询</el-button>
        <el-button :icon="Refresh" plain :loading="dailyLoading" @click="makeDaily">每日复盘</el-button>
        <el-button :icon="Refresh" plain :loading="weeklyLoading" @click="makeWeekly">每周复盘</el-button>
      </div>
    </div>

    <div class="panel">
      <div class="panel-body review-list">
        <div v-for="item in reviews" :key="item.id" class="review-item">
          <div class="review-head">
            <el-tag effect="plain">{{ labelOf(reviewTypeOptions, item.type) }}</el-tag>
            <span>{{ item.reviewDate }}</span>
          </div>
          <h3>{{ item.summary }}</h3>
          <MarkdownContent v-if="item.aiAdvice" :content="item.aiAdvice" />
          <p v-else class="empty-text">暂无内容</p>
        </div>
        <p v-if="!reviews.length" class="empty-text">暂无复盘</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { generateDailyReview, generateWeeklyReview } from '@/api/ai'
import { fetchReviews } from '@/api/review'
import MarkdownContent from '@/components/MarkdownContent.vue'
import type { Review } from '@/types/review'
import { labelOf, reviewTypeOptions } from '@/utils/enums'

const reviews = ref<Review[]>([])
const range = ref<[string, string] | []>([])
const filters = reactive<{ type?: number }>({})
const dailyLoading = ref(false)
const weeklyLoading = ref(false)

onMounted(load)

async function load() {
  reviews.value = await fetchReviews({
    type: filters.type,
    startDate: range.value[0],
    endDate: range.value[1]
  })
}

async function makeDaily() {
  dailyLoading.value = true
  try {
    await generateDailyReview()
    await load()
  } finally {
    dailyLoading.value = false
  }
}

async function makeWeekly() {
  weeklyLoading.value = true
  try {
    await generateWeeklyReview()
    await load()
  } finally {
    weeklyLoading.value = false
  }
}
</script>

<style scoped>
.review-list {
  display: grid;
  gap: 14px;
}

.review-item {
  padding: 16px;
  border: 1px solid #edf1f4;
  border-radius: 8px;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #7b8794;
  font-size: 13px;
}

.review-item h3 {
  margin: 12px 0 8px;
  font-size: 16px;
}
</style>
