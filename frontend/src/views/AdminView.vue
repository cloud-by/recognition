<script setup>
import { computed, onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'
import { getAuthUser, isAdminUser } from '@/utils/auth'

const loading = ref(false)
const error = ref('')
const logs = ref([])
const authUser = ref(getAuthUser())
const canViewAdmin = ref(isAdminUser())

const filters = ref({
  userId: '',
  behaviorType: '',
})

const behaviorOptions = [
  { label: '全部行为', value: '' },
  { label: '窗口失焦', value: 'WINDOW_BLUR' },
  { label: '窗口聚焦', value: 'WINDOW_FOCUS' },
  { label: '标签页可见性变化', value: 'TAB_VISIBILITY_CHANGE' },
]

const riskSummary = computed(() => ({
  all: logs.value.length,
  blur: logs.value.filter((item) => item.behaviorType === 'WINDOW_BLUR').length,
  visibility: logs.value.filter((item) => item.behaviorType === 'TAB_VISIBILITY_CHANGE').length,
}))

async function loadLogs() {
  if (!canViewAdmin.value) {
    error.value = '仅管理员账号可以访问管理端页面。'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams()
    if (filters.value.userId) params.set('userId', filters.value.userId)
    if (filters.value.behaviorType) params.set('behaviorType', filters.value.behaviorType)

    const query = params.toString()
    const resp = await apiGet(`/anti-cheat/logs${query ? `?${query}` : ''}`)
    logs.value = resp.data || []
  } catch (err) {
    error.value = err.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  authUser.value = getAuthUser()
  canViewAdmin.value = isAdminUser()
  if (canViewAdmin.value) {
    loadLogs()
  }
})
</script>

<template>
  <section class="panel">
    <section v-if="canViewAdmin" class="admin-grid">
      <div class="summary">
        <span>日志总数 {{ riskSummary.all }}</span>
        <span>失焦 {{ riskSummary.blur }}</span>
        <span>可见性变更 {{ riskSummary.visibility }}</span>
      </div>

      <form class="filters" @submit.prevent="loadLogs">
        <label>
          用户ID
          <input v-model="filters.userId" type="number" min="1" placeholder="例如 1" />
        </label>
        <label>
          行为类型
          <select v-model="filters.behaviorType">
            <option v-for="item in behaviorOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <button type="submit">查询</button>
      </form>

      <p v-if="loading" class="state">日志加载中...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <div v-else class="table-wrap">
        <table class="log-table">
          <thead>
          <tr>
            <th>ID</th>
            <th>用户</th>
            <th>行为类型</th>
            <th>详情</th>
            <th>发生时间</th>
          </tr>
          </thead>
          <tbody>
          <tr v-if="!logs.length">
            <td colspan="5">暂无日志</td>
          </tr>
          <tr v-for="log in logs" :key="log.id">
            <td>{{ log.id }}</td>
            <td>{{ log.userId }}</td>
            <td>{{ log.behaviorType }}</td>
            <td>{{ log.detailInfo || '-' }}</td>
            <td>{{ log.occurredTime }}</td>
          </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-else class="sub-panel">
      <p class="state error">当前账号 {{ authUser?.username || '游客' }} 不是管理员，无法查看管理端内容。</p>
    </section>
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 16px;
}

.admin-grid {
  display: grid;
  gap: 12px;
}

.summary {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary span {
  background: #eef4ff;
  color: #4d6581;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 13px;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: end;
  flex-wrap: wrap;
}

.filters label {
  display: grid;
  gap: 6px;
  color: #5c6c7e;
  font-size: 13px;
}

input,
select {
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 8px 10px;
  min-width: 180px;
}

button {
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
  background: linear-gradient(135deg, #3595f4, #2f82e8);
  color: #fff;
}

.table-wrap {
  overflow: auto;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
}

.log-table th,
.log-table td {
  border-bottom: 1px solid #eaf0f7;
  padding: 10px;
  text-align: left;
}

.log-table th {
  color: #7c8c9d;
}

.state {
  color: #7f8da0;
  margin: 0;
}

.state.error {
  color: #d64545;
}

.sub-panel {
  padding: 10px 0;
}
</style>
