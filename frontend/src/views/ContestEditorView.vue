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

const pageMode = computed(() => route.meta.mode === 'training' ? 'training' : 'contest')
const modeLabel = computed(() => pageMode.value === 'training' ? '课堂训练' : '比赛')
const lockedRankingPolicy = computed(() => pageMode.value === 'training' ? 'CLASSROOM' : 'FORMAL')

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
const pageTitle = computed(() => (isEdit.value ? `编辑${modeLabel.value}（仅未开始）` : `创建${modeLabel.value}`))
const submitText = computed(() => (isEdit.value ? '保存修改' : `创建${modeLabel.value}`))
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
    rankingPolicy: lockedRankingPolicy.value,
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
  if (!isEdit.value) {
    form.rankingPolicy = lockedRankingPolicy.value
    return
  }
  pageLoading.value = true
  try {
    const resp = await fetchContestDetail(route.params.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || `加载${modeLabel.value}失败`
      return
    }
    const data = resp.data
    if (data.status !== 'NOT_STARTED') {
      error.value = `仅未开始${modeLabel.value}可编辑`
      return
    }
    if (data.rankingPolicy !== lockedRankingPolicy.value) {
      error.value = `当前页面仅支持${modeLabel.value}编辑`
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
    error.value = err.message || `加载${modeLabel.value}失败`
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
      rankingPolicy: lockedRankingPolicy.value,
      freezeBoard: form.freezeBoard,
      allowedIpRule: showIpRule.value ? form.allowedIpRule : '',
      problemIds: [...new Set(form.problemIds)].sort((a, b) => a - b),
    }

    const resp = isEdit.value
      ? await updateContest(route.params.id, { operatorUserId: user?.id, ...payload })
      : await createContest({ creatorUserId: user?.id, ...payload })

    if (!resp.success) {
      error.value = resp.message || `${isEdit.value ? '更新' : '创建'}${modeLabel.value}失败`
      return
    }

    message.value = `${isEdit.value ? modeLabel.value + '更新成功' : modeLabel.value + '创建成功'}：${resp.data.title}`
    if (isEdit.value) {
      setTimeout(() => router.push('/contests'), 500)
    } else {
      resetForm()
    }
  } catch (err) {
    error.value = err.message || `${isEdit.value ? '更新' : '创建'}${modeLabel.value}失败`
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
      <div>
        <p class="eyebrow">{{ pageMode === 'training' ? 'Classroom' : 'Contest' }}</p>
        <h2>{{ pageTitle }}</h2>
      </div>
      <button class="ghost" type="button" @click="backToHall">返回比赛大厅</button>
    </header>

    <p v-if="message" class="ok">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="pageLoading" class="empty">加载中...</p>

    <form v-else class="form" @submit.prevent="submit">
      <section class="card">
        <h3>基础信息</h3>
        <label>{{ modeLabel }}名称<input v-model="form.title" required maxlength="200" /></label>
        <label>
          {{ modeLabel }}内容（用于介绍）
          <textarea v-model="form.contestContent" rows="4" maxlength="5000" placeholder="请输入介绍、规则或注意事项"></textarea>
        </label>
      </section>

      <section class="card">
        <h3>时间与规则</h3>
        <div class="row">
          <label>开始时间<input v-model="form.startTime" type="datetime-local" required /></label>
          <label>结束时间<input v-model="form.endTime" type="datetime-local" required /></label>
        </div>

        <div class="row">
          <label>
            模式
            <input :value="lockedRankingPolicy === 'FORMAL' ? '正式比赛' : '课堂训练'" readonly />
          </label>
          <label class="check"><input v-model="form.freezeBoard" type="checkbox" />启用封榜</label>
        </div>

        <label v-if="showIpRule">
          正式比赛IP限制（可选，多个规则逗号分隔）
          <input v-model="form.allowedIpRule" placeholder="如 10.10.,192.168.20" />
        </label>
      </section>

      <section class="card">
        <h3>题目选择</h3>
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
      </section>

      <button class="submit-btn" :disabled="loading || !form.problemIds.length" type="submit">{{ loading ? '提交中...' : submitText }}</button>
    </form>
  </section>
</template>

<style scoped>
.editor-page {
  max-width: 1120px;
  margin: 28px auto;
  padding: 0 18px 32px;
}

.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #6b7280;
}

.head-tip {
  margin: 8px 0 0;
  color: #738395;
  font-size: 13px;
}

h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
  color: #1f3145;
}

.form {
  display: grid;
  gap: 14px;
}

.card {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96));
  border: 1px solid #dde9f7;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 12px 25px rgba(45, 86, 133, 0.06);
}

.card h3 {
  margin: 0 0 12px;
  font-size: 18px;
  color: #1f3145;
}

label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 10px;
  color: #42566d;
  font-weight: 600;
}

input,
textarea {
  box-sizing: border-box;
  width: 100%;
  border: 1px solid #cedaea;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
  background: #fff;
  color: #1e2a3a;
  min-height: 42px;
}

input:focus,
textarea:focus {
  outline: none;
  border-color: #3a95ef;
  box-shadow: 0 0 0 3px rgba(58, 149, 239, 0.14);
}

textarea {
  min-height: 106px;
  resize: vertical;
}

input[type='datetime-local'] {
  min-height: 44px;
}

input[readonly] {
  background: #f4f8fd;
  color: #536982;
}

.row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.check {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  min-height: 44px;
  background: #f6f9fd;
  border: 1px solid #cedaea;
  border-radius: 10px;
  padding: 0 12px;
}

.check input {
  width: auto;
  margin: 0;
}

.problems {
  max-height: 300px;
  overflow: auto;
  border: 1px solid #d6e3f2;
  border-radius: 12px;
  padding: 8px;
  background: #f9fbff;
}

.problem-option {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 8px 10px;
  font-weight: 500;
  color: #384e66;
}

.problem-option:hover {
  background: #edf4ff;
  border-color: #d4e4f8;
}

.problem-option:last-child {
  margin-bottom: 0;
}

.problem-option input {
  width: auto;
}

.selected,
.ok,
.error,
.empty {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid transparent;
}

.selected { background: #edf5ff; color: #275188; border-color: #cfdef2; }
.ok { background: #ecfdf5; color: #0f6f40; border-color: #a7f3d0; margin-bottom: 12px; }
.error { background: #fef2f2; color: #b42318; border-color: #fecaca; margin-bottom: 12px; }
.empty { background: #f9fafb; color: #4b5563; border-color: #e5e7eb; margin-bottom: 12px; }

.submit-btn {
  justify-self: start;
  min-width: 180px;
  padding: 11px 20px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.26);
}

.submit-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}

.ghost {
  border: 1px solid #deebfa;
  border-radius: 10px;
  padding: 8px 12px;
  background: #f5f8fd;
  color: #4e6075;
  cursor: pointer;
}

@media (max-width: 860px) {
  .row { grid-template-columns: 1fr; }
  .head-row { flex-direction: column; align-items: flex-start; }
  h2 { font-size: 24px; }
}
</style>
