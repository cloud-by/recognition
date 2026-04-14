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
    if (filters.value.userId) {
      params.set('userId', filters.value.userId)
    }
    if (filters.value.behaviorType) {
      params.set('behaviorType', filters.value.behaviorType)
    }

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
  <section>
    <h2>管理端工作台（基础版）</h2>
    <ol>
      <li>用户管理：注册/封禁/重置密码（接口占位）</li>
      <li>题目管理：新增题目、上传测试数据路径</li>
      <li>比赛管理：创建比赛、设置封榜</li>
      <li>防作弊日志：按用户和行为类型查询异常行为</li>
    </ol>

    <h3>防作弊日志</h3>
    <form class="filters" @submit.prevent="loadLogs">
      <label>
        用户ID：
        <input v-model="filters.userId" type="number" min="1" placeholder="例如 1" />
      </label>
      <label>
        行为类型：
        <select v-model="filters.behaviorType">
          <option v-for="item in behaviorOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>
      <button type="submit">查询</button>
    </form>

    <p v-if="loading">日志加载中...</p>
    <p v-else-if="error">{{ error }}</p>
    <table v-else class="log-table">
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
  </section>
</template>

<style scoped>
.filters {
  display: flex;
  gap: 12px;
  align-items: end;
  margin: 12px 0;
}

.filters label {
  display: grid;
  gap: 6px;
}

.log-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
}

.log-table th,
.log-table td {
  border: 1px solid #e2e8f0;
  padding: 8px;
  text-align: left;
}
</style>
