<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet } from '@/api/http'
import { isLoggedIn } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const problem = ref(null)
const loggedIn = ref(false)

const difficultyClassMap = {
  入门: 'lv-beginner',
  普及: 'lv-normal',
  提高: 'lv-advanced',
}

const problemCode = computed(() => `P${String(problem.value?.id || route.params.id).padStart(4, '0')}`)

function difficultyClass(difficulty = '普及') {
  return difficultyClassMap[difficulty] || 'lv-normal'
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const resp = await apiGet(`/problems/${route.params.id}`)
    problem.value = resp.data
    if (!problem.value) {
      error.value = resp.message || '题目不存在'
    }
  } catch (err) {
    error.value = err.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

function goSubmit() {
  if (!loggedIn.value) {
    router.push('/login')
    return
  }
  router.push(`/problems/${route.params.id}/submit`)
}

onMounted(() => {
  loggedIn.value = isLoggedIn()
  loadDetail()
})
</script>

<template>
  <section class="detail-wrap">
    <button class="back" @click="router.push('/problems')">← 返回题目列表</button>

    <p v-if="loading">加载中...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <article v-else-if="problem" class="detail-card">
      <header class="header">
        <div>
          <p class="pid">{{ problemCode }}</p>
          <h2>{{ problem.title }}</h2>
          <p class="limits">时间限制 {{ problem.timeLimitMs }} ms · 内存限制 {{ problem.memoryLimitMb }} MB</p>
        </div>
        <div class="actions">
          <span :class="['difficulty', difficultyClass(problem.difficulty)]">{{ problem.difficulty || '普及' }}</span>
          <button class="submit-btn" @click="goSubmit">{{ loggedIn ? '提交代码' : '登录后才可提交' }}</button>
        </div>
      </header>

      <section class="section">
        <h3>题目描述</h3>
        <pre>{{ problem.description }}</pre>
      </section>

      <section class="grid">
        <div>
          <h3>输入格式</h3>
          <pre>{{ problem.inputFormat || '暂无' }}</pre>
        </div>
        <div>
          <h3>输出格式</h3>
          <pre>{{ problem.outputFormat || '暂无' }}</pre>
        </div>
      </section>

      <section class="grid">
        <div>
          <h3>样例输入</h3>
          <pre>{{ problem.sampleInput || '暂无' }}</pre>
        </div>
        <div>
          <h3>样例输出</h3>
          <pre>{{ problem.sampleOutput || '暂无' }}</pre>
        </div>
      </section>
    </article>
  </section>
</template>

<style scoped>
.detail-wrap {
  display: grid;
  gap: 14px;
}

.detail-card {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 16px;
  padding: 22px;
  box-shadow: 0 12px 30px rgba(34, 63, 99, 0.06);
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2f8;
}

.pid {
  margin: 0;
  color: #738496;
  font-size: 13px;
}

h2 { margin: 6px 0 8px; }

.limits {
  margin: 0;
  color: #62707f;
}

.actions {
  display: grid;
  gap: 10px;
  align-content: start;
  justify-items: end;
}

.section,
.grid {
  margin-top: 16px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

h3 {
  margin: 0 0 10px;
}

pre {
  background: #f7f9fc;
  border: 1px solid #e6edf4;
  border-radius: 10px;
  padding: 12px;
  white-space: pre-wrap;
  font-family: inherit;
  margin: 0;
}

.back {
  width: fit-content;
  border: 0;
  color: #2c8cec;
  background: #edf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.submit-btn {
  border: 0;
  border-radius: 10px;
  padding: 10px 15px;
  color: #fff;
  background: linear-gradient(135deg, #3aa0f6, #2d80ea);
  cursor: pointer;
}

.difficulty { font-weight: 700; }
.lv-beginner { color: #e74c3c; }
.lv-normal { color: #2e9c52; }
.lv-advanced { color: #2d6cdf; }

.error { color: #d93025; }

@media (max-width: 900px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
