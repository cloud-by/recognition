<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchContestArena } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const user = getAuthUser()

const loading = ref(false)
const error = ref('')
const arena = ref(null)
const activeTab = ref('problems')

const statusText = {
  NOT_STARTED: '未开始',
  RUNNING: '进行中',
  FINISHED: '已结束',
}

const tabList = computed(() => [
  { key: 'problems', name: `题目(${arena.value?.problems?.length || 0})` },
  { key: 'submissions', name: `提交记录(${arena.value?.submissions?.length || 0})` },
  { key: 'rank', name: `榜单(${arena.value?.leaderboard?.length || 0})` },
])

async function loadArena() {
  loading.value = true
  error.value = ''
  try {
    const resp = await fetchContestArena(route.params.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '加载比赛页面失败'
      return
    }
    arena.value = resp.data
  } catch (err) {
    error.value = err.message || '加载比赛页面失败'
  } finally {
    loading.value = false
  }
}

function goSubmit(problemId) {
  router.push(`/problems/${problemId}/submit`)
}

onMounted(loadArena)
</script>

<template>
  <section class="arena-page">
    <header class="arena-head">
      <div>
        <h2>{{ arena?.title || '比赛页面' }}</h2>
        <p>{{ arena?.contestContent || '暂无介绍' }}</p>
      </div>
      <div class="meta" v-if="arena">
        <span>状态：{{ statusText[arena.status] }}</span>
        <span>{{ arena.startTime }} ~ {{ arena.endTime }}</span>
      </div>
    </header>

    <section class="card" v-if="loading">加载中...</section>
    <section class="card" v-else-if="error">{{ error }}</section>

    <template v-else-if="arena">
      <section class="tab-row">
        <button
          v-for="tab in tabList"
          :key="tab.key"
          type="button"
          :class="['tab-btn', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          {{ tab.name }}
        </button>
      </section>

      <section v-if="activeTab === 'problems'" class="card">
        <table class="table">
          <thead>
          <tr>
            <th>#</th>
            <th>题目</th>
            <th>难度</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="problem in arena.problems" :key="problem.problemId">
            <td>{{ problem.sortOrder }}</td>
            <td>{{ problem.title }}</td>
            <td>{{ problem.difficulty }}</td>
            <td><button class="small" @click="goSubmit(problem.problemId)">提交代码</button></td>
          </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeTab === 'submissions'" class="card">
        <table class="table">
          <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>题号</th>
            <th>结果</th>
            <th>语言</th>
            <th>时间</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="row in arena.submissions" :key="row.id">
            <td>{{ row.id }}</td>
            <td>{{ row.nickname }}</td>
            <td>P{{ String(row.problemId).padStart(4, '0') }}</td>
            <td>{{ row.judgeStatus }}</td>
            <td>{{ row.language }}</td>
            <td>{{ row.submitTime }}</td>
          </tr>
          </tbody>
        </table>
      </section>

      <section v-if="activeTab === 'rank'" class="card">
        <table class="table">
          <thead>
          <tr>
            <th>排名</th>
            <th>用户</th>
            <th>解题数</th>
            <th>罚时</th>
            <th>提交数</th>
            <th>各题情况</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="row in arena.leaderboard" :key="row.userId">
            <td>{{ row.rank }}</td>
            <td>{{ row.nickname }}</td>
            <td>{{ row.solved }}</td>
            <td>{{ row.penalty }}</td>
            <td>{{ row.submitCount }}</td>
            <td>{{ row.problemStates.join(' | ') }}</td>
          </tr>
          </tbody>
        </table>
      </section>
    </template>
  </section>
</template>

<style scoped>
.arena-page { display: grid; gap: 12px; }
.arena-head { background: #fff; border: 1px solid #e5edf7; border-radius: 12px; padding: 16px; display: flex; justify-content: space-between; gap: 10px; }
.arena-head h2 { margin: 0; }
.arena-head p { color: #5f6f84; margin: 8px 0 0; }
.meta { display: grid; gap: 6px; color: #5f6f84; text-align: right; }
.card { background: #fff; border: 1px solid #e5edf7; border-radius: 12px; padding: 14px; }
.tab-row { display: flex; gap: 8px; }
.tab-btn { border: 1px solid #cbd9ea; background: #f0f6ff; color: #3c5f84; border-radius: 999px; padding: 8px 14px; cursor: pointer; }
.tab-btn.active { background: #2d8fee; color: #fff; border-color: transparent; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border-bottom: 1px solid #edf2f8; text-align: left; padding: 8px; font-size: 14px; }
.small { border: 1px solid #b7d2f2; background: #eef6ff; color: #2e71b8; border-radius: 8px; padding: 5px 10px; cursor: pointer; }
</style>
