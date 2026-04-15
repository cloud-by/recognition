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
  failed: submissions.value.filter((item) => !['AC', 'PENDING', 'JUDGING'].includes(item.judgeStatus?.toUpperCase())).length,
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
  <section class="page">
    <section class="summary-grid">
      <article class="summary-card">
        <p>总提交</p>
        <strong>{{ stats.total }}</strong>
      </article>
      <article class="summary-card ok">
        <p>通过</p>
        <strong>{{ stats.accepted }}</strong>
      </article>
      <article class="summary-card pending">
        <p>评测中</p>
        <strong>{{ stats.running }}</strong>
      </article>
      <article class="summary-card failed">
        <p>未通过</p>
        <strong>{{ stats.failed }}</strong>
      </article>
    </section>

    <section class="panel">
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
            <th>详情</th>
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
            <td class="detail">{{ item.judgeDetail || '-' }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 14px;
}

.summary-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.summary-card {
  background: #fff;
  border: 1px solid #e3ebf6;
  border-radius: 14px;
  padding: 12px 14px;
}

.summary-card p {
  margin: 0;
  font-size: 12px;
  color: #70839a;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  color: #23374d;
}

.summary-card.ok {
  background: #effcf3;
}

.summary-card.pending {
  background: #fff8e8;
}

.summary-card.failed {
  background: #fff0ee;
}

.panel {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 16px;
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
  margin: 0;
}
</style>
