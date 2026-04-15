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

const overview = computed(() => ({
  beginner: filteredProblems.value.filter((item) => item.difficulty === '入门').length,
  normal: filteredProblems.value.filter((item) => !item.difficulty || item.difficulty === '普及').length,
  advanced: filteredProblems.value.filter((item) => item.difficulty === '提高').length,
}))

const filteredProblems = computed(() => {
  const keyword = filters.value.keyword.trim().toLowerCase()
  const maxTime = Number(filters.value.maxTime)
  const maxMemory = Number(filters.value.maxMemory)

  return problems.value.filter((problem) => {
    if (!problem) return false
    const title = String(problem.title || '').toLowerCase()
    const passKeyword = !keyword || String(problem.id || '').includes(keyword) || title.includes(keyword)
    const passTime = !maxTime || Number(problem.timeLimitMs || 0) <= maxTime
    const passMemory = !maxMemory || Number(problem.memoryLimitMb || 0) <= maxMemory
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
  <section class="problem-page">
    <section class="panel filter-panel">
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

      <div class="overview">
        <span class="pill">共 {{ total }} 题</span>
        <span class="pill beginner">入门 {{ overview.beginner }}</span>
        <span class="pill normal">普及 {{ overview.normal }}</span>
        <span class="pill advanced">提高 {{ overview.advanced }}</span>
      </div>
    </section>

    <section class="panel table-panel">
      <p v-if="loading" class="state">加载中...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <div v-else-if="filteredProblems.length" class="table-wrap">
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
            <td>{{ p.tags || '-' }}</td>
            <td>
              <button class="link" @click="goDetail(p.id)">查看详情</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-block">
        <p>暂无匹配题目</p>
        <button class="ghost" @click="resetFilters">恢复默认筛选</button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.problem-page {
  display: grid;
  gap: 14px;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e2ebf6;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 26px rgba(52, 93, 141, 0.06);
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
  background: #fbfdff;
}

.actions {
  display: flex;
  gap: 8px;
}

button {
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
}

.primary {
  background: #2990f3;
  color: #fff;
}

.ghost {
  background: #f2f6fb;
  color: #5d6d7e;
}

.overview {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pill {
  padding: 6px 11px;
  border-radius: 999px;
  background: #eff5fc;
  color: #4f6781;
  font-size: 13px;
}

.pill.beginner {
  background: #ffefee;
  color: #c73e35;
}

.pill.normal {
  background: #ebf9ef;
  color: #25874a;
}

.pill.advanced {
  background: #eaf2ff;
  color: #2d6cdf;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  padding: 12px 10px;
  text-align: left;
  border-bottom: 1px solid #edf2f8;
}

th {
  color: #8593a3;
  font-weight: 600;
}

.title-link,
.link {
  background: transparent;
  color: #2e8ae8;
  padding: 0;
}

.state {
  margin: 0;
  color: #98a6b5;
}

.error {
  color: #e74c3c;
}

.empty-block {
  display: grid;
  justify-items: center;
  gap: 10px;
  color: #74879a;
  padding: 30px 0;
}

.difficulty {
  font-weight: 700;
}

.lv-beginner {
  color: #e74c3c;
}

.lv-normal {
  color: #2e9c52;
}

.lv-advanced {
  color: #2d6cdf;
}
</style>
