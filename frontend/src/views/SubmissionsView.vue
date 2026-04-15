<script setup>
import { computed, onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'

import { getAuthUser } from '@/utils/auth'

const userId = getAuthUser()?.id
const loading = ref(false)
const message = ref('')
const submissions = ref([])

const stats = computed(() => ({
  total: submissions.value.length,
  accepted: submissions.value.filter((item) => item.judgeStatus?.toUpperCase() === 'AC').length,
  running: submissions.value.filter((item) => ['PENDING', 'JUDGING'].includes(item.judgeStatus?.toUpperCase())).length,
}))

function statusClass(status = '') {
  const normalized = status.toUpperCase()
  if (normalized === 'AC') return 'ok'
  if (['PENDING', 'JUDGING'].includes(normalized)) return 'pending'
  return 'failed'
}

onMounted(async () => {
  loading.value = true
  try {
    if (!userId) {
      message.value = '请先登录后查看提交记录。'
      return
    }
    const resp = await apiGet(`/submissions?userId=${userId}`)
    submissions.value = resp.data || []
    if (!submissions.value.length) {
      message.value = '暂无提交记录，先去题目列表试试吧。'
    }
  } catch {
    message.value = '当前暂无提交记录，或后端未启动。'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="panel">
    <div class="header">
      <div>
        <h2>提交记录</h2>
        <p>可追踪最近提交状态、语言与结果，便于定位问题。</p>
      </div>
      <div class="summary">
        <span>总提交：{{ stats.total }}</span>
        <span class="ok">通过：{{ stats.accepted }}</span>
        <span class="pending">评测中：{{ stats.running }}</span>
      </div>
    </div>

    <p v-if="loading" class="state">加载中...</p>
    <p v-else-if="message" class="state">{{ message }}</p>

    <div v-else class="table-wrap">
      <table>
        <thead>
        <tr>
          <th>#</th>
          <th>题目ID</th>
          <th>语言</th>
          <th>状态</th>
          <th>提交时间</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="item in submissions" :key="item.id">
          <td>{{ item.id }}</td>
          <td>P{{ String(item.problemId).padStart(4, '0') }}</td>
          <td>{{ item.language }}</td>
          <td>
            <span :class="['badge', statusClass(item.judgeStatus)]">{{ item.judgeStatus }}</span>
          </td>
          <td>{{ item.submitTime || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 16px;
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

h2 {
  margin: 0;
}

p {
  margin: 8px 0 0;
  color: #6c7b8c;
}

.summary {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.summary span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f4f7fb;
  color: #58708a;
  font-size: 13px;
}

.summary span.ok {
  background: #e9f9ef;
  color: #189a54;
}

.summary span.pending {
  background: #fff5df;
  color: #b57609;
}

.table-wrap {
  overflow: auto;
  border-top: 1px solid #edf2f8;
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
  color: #8492a2;
}

.badge {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.badge.ok {
  color: #188b52;
  background: #e9f9ef;
}

.badge.pending {
  color: #b57609;
  background: #fff5df;
}

.badge.failed {
  color: #c73e35;
  background: #ffebea;
}

.state {
  color: #7f90a1;
}
</style>
