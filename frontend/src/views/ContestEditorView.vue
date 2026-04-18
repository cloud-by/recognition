<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createContest, fetchContestDetail, fetchContestProblemOptions, updateContest } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const user = getAuthUser()

const loading = ref(false)
const pageLoading = ref(false)
const message = ref('')
const error = ref('')
const problems = ref([])
const problemKeyword = ref('')

const form = reactive({
  title: '',
  contestContent: '',
  startTime: '',
  endTime: '',
  rankingPolicy: 'FORMAL',
  freezeBoard: false,
  allowedIpRule: '',
  problemIds: [],
})

const isEdit = computed(() => Boolean(route.params.id))
const pageTitle = computed(() => (isEdit.value ? '编辑比赛（仅未开始）' : '创建比赛'))
const submitText = computed(() => (isEdit.value ? '保存修改' : '创建比赛'))
const showIpRule = computed(() => form.rankingPolicy === 'FORMAL')

const filteredProblems = computed(() => {
  const q = problemKeyword.value.trim().toLowerCase()
  if (!q) return problems.value
  return problems.value.filter((item) => `${item.id} ${item.title} ${item.difficulty}`.toLowerCase().includes(q))
})

function toLocalInput(value) {
  if (!value) return ''
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function resetForm() {
  Object.assign(form, {
    title: '',
    contestContent: '',
    startTime: '',
    endTime: '',
    rankingPolicy: 'FORMAL',
    freezeBoard: false,
    allowedIpRule: '',
    problemIds: [],
  })
}

async function loadBaseData() {
  const problemResp = await fetchContestProblemOptions()
  problems.value = problemResp.data || []
}

async function loadContestForEdit() {
  if (!isEdit.value) return
  pageLoading.value = true
  try {
    const resp = await fetchContestDetail(route.params.id)
    if (!resp.success) {
      error.value = resp.message || '加载比赛失败'
      return
    }
    const data = resp.data
    if (data.status !== 'NOT_STARTED') {
      error.value = '仅未开始比赛可编辑'
      return
    }
    Object.assign(form, {
      title: data.title,
      contestContent: data.contestContent || '',
      startTime: toLocalInput(data.startTime),
      endTime: toLocalInput(data.endTime),
      rankingPolicy: data.rankingPolicy,
      freezeBoard: data.freezeBoard,
      allowedIpRule: data.allowedIpRule || '',
      problemIds: (data.problems || []).map((item) => item.problemId),
    })
  } catch (err) {
    error.value = err.message || '加载比赛失败'
  } finally {
    pageLoading.value = false
  }
}

async function submit() {
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    const payload = {
      title: form.title,
      contestContent: form.contestContent,
      startTime: form.startTime,
      endTime: form.endTime,
      rankingPolicy: form.rankingPolicy,
      freezeBoard: form.freezeBoard,
      allowedIpRule: showIpRule.value ? form.allowedIpRule : '',
      problemIds: [...new Set(form.problemIds)].sort((a, b) => a - b),
    }

    const resp = isEdit.value
      ? await updateContest(route.params.id, { operatorUserId: user?.id, ...payload })
      : await createContest({ creatorUserId: user?.id, ...payload })

    if (!resp.success) {
      error.value = resp.message || `${isEdit.value ? '更新' : '创建'}比赛失败`
      return
    }

    message.value = `${isEdit.value ? '比赛更新成功' : '比赛创建成功'}：${resp.data.title}`
    if (isEdit.value) {
      setTimeout(() => router.push('/contests'), 500)
    } else {
      resetForm()
    }
  } catch (err) {
    error.value = err.message || `${isEdit.value ? '更新' : '创建'}比赛失败`
  } finally {
    loading.value = false
  }
}

function backToHall() {
  router.push('/contests')
}

onMounted(async () => {
  try {
    await loadBaseData()
    await loadContestForEdit()
  } catch (err) {
    error.value = err.message || '初始化数据失败'
  }
})
</script>

<template>
  <section class="editor-page">
    <header class="head-row">
      <h2>{{ pageTitle }}</h2>
      <button class="ghost" type="button" @click="backToHall">返回比赛大厅</button>
    </header>

    <p v-if="message" class="ok">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="pageLoading" class="empty">加载中...</p>

    <form v-else class="form" @submit.prevent="submit">
      <label>比赛名称<input v-model="form.title" required maxlength="200" /></label>
      <label>
        比赛内容（用于介绍比赛）
        <textarea v-model="form.contestContent" rows="4" maxlength="5000" placeholder="请输入比赛介绍、规则或注意事项"></textarea>
      </label>

      <div class="row">
        <label>开始时间<input v-model="form.startTime" type="datetime-local" required /></label>
        <label>结束时间<input v-model="form.endTime" type="datetime-local" required /></label>
      </div>

      <div class="row">
        <label>
          排名策略
          <select v-model="form.rankingPolicy">
            <option value="FORMAL">正式比赛（含罚时）</option>
            <option value="CLASSROOM">课堂模式（不计罚时）</option>
          </select>
        </label>
        <label class="check"><input v-model="form.freezeBoard" type="checkbox" />启用封榜</label>
      </div>

      <label v-if="showIpRule">
        正式比赛IP限制（可选，多个规则逗号分隔）
        <input v-model="form.allowedIpRule" placeholder="如 10.10.,192.168.20" />
      </label>

      <label>
        题目搜索
        <input v-model="problemKeyword" placeholder="按关键词/ID/难度搜索" />
      </label>
      <div class="problems">
        <label v-for="p in filteredProblems" :key="p.id" class="problem-option">
          <input v-model="form.problemIds" :value="p.id" type="checkbox" />
          #{{ p.id }} {{ p.title }}（{{ p.difficulty }}）
        </label>
      </div>
      <p class="selected">已选 {{ form.problemIds.length }} 题：{{ form.problemIds.join(', ') || '暂无' }}</p>

      <button :disabled="loading || !form.problemIds.length" type="submit">{{ loading ? '提交中...' : submitText }}</button>
    </form>
  </section>
</template>

<style scoped>
.editor-page { max-width: 1080px; margin: 24px auto; padding: 20px; background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; }
.head-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 14px; }
.form { display: grid; gap: 12px; }
label { display: grid; gap: 6px; color: #5b6978; }
input, select, textarea { border: 1px solid #cedaea; border-radius: 8px; padding: 8px 10px; font: inherit; }
.row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.check { display: flex; align-items: center; gap: 8px; }
.problems { max-height: 340px; overflow: auto; border: 1px solid #d9e5f1; border-radius: 8px; padding: 8px; display: grid; gap: 4px; }
.problem-option { display: flex; align-items: center; gap: 8px; }
button { width: fit-content; border: 0; background: #2d8fee; color: #fff; border-radius: 8px; padding: 9px 14px; cursor: pointer; }
button.ghost { background: #eef4ff; color: #426082; }
.ok { color: #1f7a3e; }
.error { color: #d93025; }
.empty, .selected { color: #738293; margin: 0; }
</style>
