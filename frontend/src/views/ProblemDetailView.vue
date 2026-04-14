<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet } from '@/api/http'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const error = ref('')
const problem = ref(null)

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

onMounted(loadDetail)
</script>

<template>
  <section class="detail-card">
    <button class="back" @click="router.push('/problems')">← 返回题目列表</button>

    <p v-if="loading">加载中...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <article v-else-if="problem">
      <h2>P{{ String(problem.id).padStart(4, '0') }} {{ problem.title }}</h2>
      <div class="meta">
        <span>时间限制：{{ problem.timeLimitMs }} ms</span>
        <span>内存限制：{{ problem.memoryLimitMb }} MB</span>
      </div>

      <h3>题目描述</h3>
      <pre>{{ problem.description }}</pre>

      <h3>输入格式</h3>
      <pre>{{ problem.inputFormat || '暂无' }}</pre>

      <h3>输出格式</h3>
      <pre>{{ problem.outputFormat || '暂无' }}</pre>

      <h3>样例输入</h3>
      <pre>{{ problem.sampleInput || '暂无' }}</pre>

      <h3>样例输出</h3>
      <pre>{{ problem.sampleOutput || '暂无' }}</pre>
    </article>
  </section>
</template>

<style scoped>
.detail-card {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 12px;
  padding: 22px;
}

.back {
  border: 0;
  color: #2c8cec;
  background: #edf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

h2 { margin: 14px 0; }
.meta {
  color: #62707f;
  display: flex;
  gap: 18px;
  margin-bottom: 12px;
}

pre {
  background: #f7f9fc;
  border: 1px solid #e6edf4;
  border-radius: 10px;
  padding: 12px;
  white-space: pre-wrap;
  font-family: inherit;
}

.error { color: #d93025; }
</style>
