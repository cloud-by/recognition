<script setup>
import { onMounted, ref } from 'vue'
import { apiGet } from '@/api/http'

const loading = ref(false)
const error = ref('')
const logs = ref([])

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

async function loadLogs() {
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

onMounted(loadLogs)
</script>

<template>
  <section class="panel">
    <header class="top">
      <div>
        <h2>管理端工作台</h2>
        <p>统一查看用户、题目、比赛和防作弊能力占位信息。</p>
      </div>
      <ul>
        <li>用户管理：注册/封禁/重置密码</li>
        <li>题目管理：新增题目、测试数据路径</li>
        <li>比赛管理：创建比赛、设置封榜</li>
      </ul>
    </header>

    <section class="panel sub-panel">
      <h3>防作弊日志</h3>
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
  </section>
</template>

<style scoped>
.panel {
  display: grid;
  gap: 14px;
}

.top {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 18px;
  display: grid;
  gap: 12px;
}

h2,
h3 {
  margin: 0;
}

.top p {
  color: #6b7989;
  margin: 8px 0 0;
}

.top ul {
  margin: 0;
  padding-left: 18px;
  color: #5f6f80;
}

.sub-panel {
  background: #fff;
  border: 1px solid #e6edf5;
  border-radius: 14px;
  padding: 18px;
}

.filters {
  display: flex;
  gap: 12px;
  align-items: end;
  margin: 12px 0;
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
}

.state.error {
  color: #d64545;
}
</style>
