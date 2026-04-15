<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createContest, fetchContestList, fetchContestProblemOptions } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const loading = ref(false)
const message = ref('')
const error = ref('')
const problems = ref([])
const contests = ref([])

const form = reactive({
  title: '',
  startTime: '',
  endTime: '',
  contestType: 'ACM',
  rankingPolicy: 'FORMAL',
  freezeBoard: false,
  problemIds: [],
})

async function loadBaseData() {
  const [problemResp, contestResp] = await Promise.all([
    fetchContestProblemOptions(),
    fetchContestList(user?.id),
  ])
  problems.value = problemResp.data || []
  contests.value = contestResp.data || []
}

async function submitContest() {
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    const resp = await createContest({
      creatorUserId: user?.id,
      title: form.title,
      startTime: form.startTime,
      endTime: form.endTime,
      contestType: form.contestType,
      rankingPolicy: form.rankingPolicy,
      freezeBoard: form.freezeBoard,
      problemIds: form.problemIds,
    })

    if (!resp.success) {
      error.value = resp.message || '创建比赛失败'
      return
    }

    message.value = `比赛创建成功：${resp.data.title}`
    form.title = ''
    form.startTime = ''
    form.endTime = ''
    form.contestType = 'ACM'
    form.rankingPolicy = 'FORMAL'
    form.freezeBoard = false
    form.problemIds = []
    await loadBaseData()
  } catch (err) {
    error.value = err.message || '创建比赛失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await loadBaseData()
  } catch (err) {
    error.value = err.message || '初始化比赛管理数据失败'
  }
})
</script>

<template>
  <section class="panel">
    <h2>比赛管理（老师/管理员）</h2>
    <p>创建比赛时需填写比赛名称、时间，并从题库选择题目。</p>

    <form class="form" @submit.prevent="submitContest">
      <label>比赛名称<input v-model="form.title" required maxlength="200" /></label>
      <div class="time-row">
        <label>开始时间<input v-model="form.startTime" type="datetime-local" required /></label>
        <label>结束时间<input v-model="form.endTime" type="datetime-local" required /></label>
      </div>
      <label>
        比赛模式
        <select v-model="form.contestType">
          <option value="ACM">ACM</option>
          <option value="OI">OI</option>
          <option value="IOI">IOI</option>
          <option value="PRACTICE">PRACTICE</option>
        </select>
      </label>
      <label>
        排名策略
        <select v-model="form.rankingPolicy">
          <option value="FORMAL">正式比赛（含罚时）</option>
          <option value="CLASSROOM">课堂模式（不计罚时）</option>
        </select>
      </label>
      <label class="check"><input v-model="form.freezeBoard" type="checkbox" />启用封榜</label>

      <fieldset>
        <legend>选择题目（可多选）</legend>
        <label v-for="problem in problems" :key="problem.id" class="problem-option">
          <input v-model="form.problemIds" :value="problem.id" type="checkbox" />
          #{{ problem.id }} {{ problem.title }}（{{ problem.difficulty }}）
        </label>
      </fieldset>

      <button type="submit" :disabled="loading || !form.problemIds.length">
        {{ loading ? '创建中...' : '创建比赛' }}
      </button>
    </form>

    <p v-if="message" class="ok">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
  </section>

  <section class="panel">
    <h3>已有比赛</h3>
    <ul>
      <li v-for="item in contests" :key="item.id">
        {{ item.title }}（{{ item.startTime }} ~ {{ item.endTime }}，{{ item.status }}）
      </li>
    </ul>
  </section>
</template>

<style scoped>
.panel { background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.form { display: grid; gap: 12px; }
label { display: grid; gap: 6px; color: #5b6978; }
input, select { border: 1px solid #cedaea; border-radius: 8px; padding: 8px 10px; }
.time-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
fieldset { border: 1px solid #d9e5f1; border-radius: 8px; padding: 10px; max-height: 220px; overflow: auto; }
.problem-option { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.check { display: flex; align-items: center; gap: 8px; }
button { width: fit-content; border: 0; background: #2d8fee; color: #fff; border-radius: 8px; padding: 9px 14px; cursor: pointer; }
button:disabled { opacity: 0.65; cursor: not-allowed; }
.ok { color: #1f7a3e; }
.error { color: #d93025; }
</style>
