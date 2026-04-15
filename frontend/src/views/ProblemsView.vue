<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAuthUser } from '@/utils/auth'
import { useRouter } from 'vue-router'
import { apiGet } from '@/api/http'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const problems = ref([])
const total = computed(() => filteredProblems.value.length)

const filters = ref({
  keyword: '',
  maxTime: '',
  maxMemory: '',
})

const difficultyClassMap = {
  入门: 'lv-beginner',
  普及: 'lv-normal',
  提高: 'lv-advanced',
}

const filteredProblems = computed(() => {
  const keyword = filters.value.keyword.trim().toLowerCase()
  const maxTime = Number(filters.value.maxTime)
  const maxMemory = Number(filters.value.maxMemory)

  return problems.value.filter((problem) => {
    const passKeyword = !keyword || String(problem.id).includes(keyword) || problem.title.toLowerCase().includes(keyword)
    const passTime = !maxTime || problem.timeLimitMs <= maxTime
    const passMemory = !maxMemory || problem.memoryLimitMb <= maxMemory
    return passKeyword && passTime && passMemory
  })
})

async function loadProblems() {
  loading.value = true
  error.value = ''
  try {
    const keyword = filters.value.keyword.trim()
    const user = getAuthUser()
    const params = new URLSearchParams()
    if (keyword) params.set('keyword', keyword)
    if (user?.id) params.set('viewerUserId', user.id)
    const query = params.toString() ? `?${params.toString()}` : ''
    const resp = await apiGet(`/problems${query}`)
    problems.value = resp.data || []
  } catch (err) {
    error.value = err.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.value = { keyword: '', maxTime: '', maxMemory: '' }
  loadProblems()
}

function goDetail(problemId) {
  router.push(`/problems/${problemId}`)
}

function difficultyClass(difficulty = '普及') {
  return difficultyClassMap[difficulty] || 'lv-normal'
}

onMounted(loadProblems)
</script>

<template>
  <section class="hero">
    <h2>题目列表</h2>
    <p>按关键词、时空限制快速检索，支持点击题目跳转详情页。</p>
  </section>

  <section class="panel">
    <form class="filters" @submit.prevent="loadProblems">
      <label>
        关键词
        <input v-model="filters.keyword" type="text" placeholder="算法、标题或题号" />
      </label>
      <label>
        时间限制 ≤
        <input v-model="filters.maxTime" type="number" min="0" placeholder="ms" />
      </label>
      <label>
        内存限制 ≤
        <input v-model="filters.maxMemory" type="number" min="0" placeholder="MB" />
      </label>

      <div class="actions">
        <button type="submit" class="primary">搜索</button>
        <button type="button" class="ghost" @click="resetFilters">清空筛选</button>
      </div>
    </form>

    <p class="meta">共计 {{ total }} 条结果</p>

    <p v-if="loading" class="state">加载中...</p>
    <p v-else-if="error" class="state error">{{ error }}</p>

    <div v-else class="table-wrap">
      <table>
        <thead>
        <tr>
          <th>题号</th>
          <th>题目名称</th>
          <th>等级</th>
          <th>时间限制</th>
          <th>内存限制</th>
          <th>权限</th>
          <th>标签</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-if="!filteredProblems.length">
          <td colspan="8" class="empty">暂无匹配题目</td>
        </tr>
        <tr v-for="p in filteredProblems" :key="p.id">
          <td>P{{ String(p.id).padStart(4, '0') }}</td>
          <td class="title">
            <button class="title-link" @click="goDetail(p.id)">{{ p.title }}</button>
          </td>
          <td>
            <span :class="['difficulty', difficultyClass(p.difficulty)]">{{ p.difficulty || '普及' }}</span>
          </td>
          <td>{{ p.timeLimitMs }} ms</td>
          <td>{{ p.memoryLimitMb }} MB</td>
          <td>{{ p.permissionType }}</td>
          <td>{{ p.tags || "-" }}</td>
          <td>
            <button class="link" @click="goDetail(p.id)">查看详情</button>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.hero {
  background: linear-gradient(135deg, #2f333a 0%, #444c58 100%);
  color: #fff;
  border-radius: 14px;
  padding: 28px;
  margin-bottom: 16px;
}

.hero h2 {
  margin: 0;
  font-size: 38px;
}

.hero p {
  margin: 8px 0 0;
  color: #dbe3ed;
}

.panel {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 12px;
  padding: 18px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: end;
  flex-wrap: wrap;
}

label {
  display: grid;
  gap: 6px;
  color: #5c6a79;
  font-size: 13px;
}

input {
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 9px 10px;
  min-width: 170px;
}

.actions { display: flex; gap: 8px; }

button {
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
}

.primary { background: #2990f3; color: #fff; }
.ghost { background: #f2f6fb; color: #5d6d7e; }

.meta {
  margin: 14px 0;
  color: #637182;
}

.table-wrap {
  overflow: auto;
  border-top: 1px solid #edf2f8;
}

table { width: 100%; border-collapse: collapse; }
th, td { padding: 12px 10px; text-align: left; border-bottom: 1px solid #edf2f8; }
th { color: #8593a3; font-weight: 600; }
.title { color: #2e8ae8; }
.title-link {
  background: transparent;
  color: #2e8ae8;
  padding: 0;
  font-size: 15px;
}
.link { color: #2e8ae8; background: transparent; padding: 0; }
.empty, .state { color: #98a6b5; }
.error { color: #e74c3c; }

.difficulty { font-weight: 700; }
.lv-beginner { color: #e74c3c; }
.lv-normal { color: #2e9c52; }
.lv-advanced { color: #2d6cdf; }
</style>
