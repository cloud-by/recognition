<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createContest, fetchContestList, fetchContestProblemOptions, updateContest } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const loading = ref(false)
const message = ref('')
const error = ref('')
const problems = ref([])
const contests = ref([])
const keyword = ref('')
const problemDialog = ref(null)
const editDialog = ref(null)
const editContestId = ref(null)
const problemKeyword = ref('')
const selectingFor = ref('create')

const form = reactive(createDefaultForm())
const editForm = reactive(createDefaultForm())

function createDefaultForm() {
  return {
    title: '',
    startTime: '',
    endTime: '',
    contestType: 'ACM',
    rankingPolicy: 'FORMAL',
    freezeBoard: false,
    allowedIpRule: '',
    problemIds: [],
  }
}

const filteredProblems = computed(() => {
  const q = problemKeyword.value.trim().toLowerCase()
  if (!q) return problems.value
  return problems.value.filter((item) => `${item.id} ${item.title} ${item.difficulty}`.toLowerCase().includes(q))
})
const activeProblemIds = computed({
  get() {
    return selectingFor.value === 'edit' ? editForm.problemIds : form.problemIds
  },
  set(value) {
    if (selectingFor.value === 'edit') {
      editForm.problemIds = value
    } else {
      form.problemIds = value
    }
  },
})

const pendingContests = computed(() => contests.value
  .filter((item) => item.status === 'NOT_STARTED')
  .filter((item) => {
    const q = keyword.value.trim().toLowerCase()
    return !q || `${item.title} ${item.id}`.toLowerCase().includes(q)
  }))

function resetForm(target) {
  Object.assign(target, createDefaultForm())
}

function openProblemDialog(target = 'create') {
  selectingFor.value = target
  problemKeyword.value = ''
  problemDialog.value?.showModal()
}

function applyProblemSelection() {
  activeProblemIds.value = [...new Set(activeProblemIds.value)].sort((a, b) => a - b)
  problemDialog.value?.close()
}

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
      ...form,
    })

    if (!resp.success) {
      error.value = resp.message || '创建比赛失败'
      return
    }

    message.value = `比赛创建成功：${resp.data.title}`
    resetForm(form)
    await loadBaseData()
  } catch (err) {
    error.value = err.message || '创建比赛失败'
  } finally {
    loading.value = false
  }
}

function openEdit(item) {
  editContestId.value = item.id
  Object.assign(editForm, {
    title: item.title,
    startTime: toLocalInput(item.startTime),
    endTime: toLocalInput(item.endTime),
    contestType: item.contestType,
    rankingPolicy: item.rankingPolicy,
    freezeBoard: item.freezeBoard,
    allowedIpRule: item.allowedIpRule || '',
    problemIds: [],
  })
  const src = contests.value.find((row) => row.id === item.id)
  if (src?.problems?.length) {
    editForm.problemIds = src.problems.map((x) => x.problemId)
  }
  editDialog.value?.showModal()
  preloadEditProblems(item.id)
}

async function preloadEditProblems(id) {
  const detail = contests.value.find((item) => item.id === id)
  if (detail?.problems) return
  const { fetchContestDetail } = await import('@/api/contests')
  const resp = await fetchContestDetail(id)
  if (resp.success && resp.data) {
    const idx = contests.value.findIndex((x) => x.id === id)
    if (idx >= 0) {
      contests.value[idx] = { ...contests.value[idx], problems: resp.data.problems }
      editForm.problemIds = resp.data.problems.map((x) => x.problemId)
    }
  }
}

async function submitEdit() {
  if (!editContestId.value) return
  loading.value = true
  error.value = ''
  message.value = ''
  try {
    const resp = await updateContest(editContestId.value, {
      operatorUserId: user?.id,
      ...editForm,
    })
    if (!resp.success) {
      error.value = resp.message || '更新比赛失败'
      return
    }
    message.value = `比赛更新成功：${resp.data.title}`
    editDialog.value?.close()
    await loadBaseData()
  } catch (err) {
    error.value = err.message || '更新比赛失败'
  } finally {
    loading.value = false
  }
}

function toLocalInput(value) {
  if (!value) return ''
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
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
  <section class="page">
    <section class="panel">
      <h3>创建比赛</h3>
      <form class="form" @submit.prevent="submitContest">
        <label>比赛名称<input v-model="form.title" required maxlength="200" /></label>
        <div class="time-row">
          <label>开始时间<input v-model="form.startTime" type="datetime-local" required /></label>
          <label>结束时间<input v-model="form.endTime" type="datetime-local" required /></label>
        </div>
        <div class="time-row">
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
        </div>

        <label>
          正式比赛IP限制（可选，多个规则逗号分隔）
          <input v-model="form.allowedIpRule" placeholder="如 10.10.,192.168.20" />
        </label>

        <label class="check"><input v-model="form.freezeBoard" type="checkbox" />启用封榜</label>

        <div class="problem-picker">
          <button type="button" @click="openProblemDialog('create')">点击选择题目（支持滚动/搜索）</button>
          <p>已选 {{ form.problemIds.length }} 题：{{ form.problemIds.join(', ') || '暂无' }}</p>
        </div>

        <button type="submit" :disabled="loading || !form.problemIds.length">
          {{ loading ? '创建中...' : '创建比赛' }}
        </button>
      </form>

      <p v-if="message" class="ok">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>

    <section class="panel">
      <div class="head-row">
        <h3>未开始比赛（可编辑）</h3>
        <input v-model="keyword" placeholder="按比赛名模糊搜索" />
      </div>
      <ul v-if="pendingContests.length" class="contest-list">
        <li v-for="item in pendingContests" :key="item.id">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.startTime }} ~ {{ item.endTime }}</p>
            <small>IP规则：{{ item.allowedIpRule || '不限' }}</small>
          </div>
          <button type="button" class="ghost" @click="openEdit(item)">编辑</button>
        </li>
      </ul>
      <p v-else class="empty">暂无未开始比赛。</p>
    </section>
  </section>

  <dialog ref="problemDialog" class="dialog">
    <h3>题目选择</h3>
    <input v-model="problemKeyword" placeholder="按关键词/ID/难度模糊搜索" />
    <div class="problem-list">
      <label v-for="problem in filteredProblems" :key="problem.id" class="problem-option">
        <input v-model="activeProblemIds" :value="problem.id" type="checkbox" />
        #{{ problem.id }} {{ problem.title }}（{{ problem.difficulty }}）
      </label>
    </div>
    <div class="dialog-actions">
      <button type="button" @click="applyProblemSelection">确认选择</button>
      <button type="button" class="ghost" @click="problemDialog?.close()">关闭</button>
    </div>
  </dialog>

  <dialog ref="editDialog" class="dialog wide">
    <h3>编辑比赛</h3>
    <form class="form" @submit.prevent="submitEdit">
      <label>比赛名称<input v-model="editForm.title" required /></label>
      <div class="time-row">
        <label>开始时间<input v-model="editForm.startTime" type="datetime-local" required /></label>
        <label>结束时间<input v-model="editForm.endTime" type="datetime-local" required /></label>
      </div>
      <label>IP规则<input v-model="editForm.allowedIpRule" placeholder="如 10.10.,172.16." /></label>
      <label>
        排名策略
        <select v-model="editForm.rankingPolicy">
          <option value="FORMAL">正式比赛</option>
          <option value="CLASSROOM">课堂模式</option>
        </select>
      </label>
      <label class="check"><input v-model="editForm.freezeBoard" type="checkbox" />启用封榜</label>
      <div class="problem-picker">
        <small>当前题目：{{ editForm.problemIds.join(', ') || '暂无' }}</small>
        <button type="button" @click="openProblemDialog('edit')">使用题目弹窗重新选择</button>
      </div>
      <div class="dialog-actions">
        <button type="submit">保存修改</button>
        <button type="button" class="ghost" @click="editDialog?.close()">取消</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.page { display: grid; gap: 12px; }
.panel { background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; padding: 16px; }
.form { display: grid; gap: 12px; }
label { display: grid; gap: 6px; color: #5b6978; }
input, select { border: 1px solid #cedaea; border-radius: 8px; padding: 8px 10px; }
.time-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
button { width: fit-content; border: 0; background: #2d8fee; color: #fff; border-radius: 8px; padding: 9px 14px; cursor: pointer; }
button.ghost { background: #eef4ff; color: #426082; }
.check { display: flex; align-items: center; gap: 8px; }
.problem-picker { display: grid; gap: 6px; }
.ok { color: #1f7a3e; }
.error { color: #d93025; }
.contest-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.contest-list li { border: 1px solid #e6edf5; border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; gap: 10px; align-items: center; }
.contest-list p { margin: 6px 0 0; color: #5f7288; }
.empty { margin: 0; color: #74879a; }
.dialog { border: 1px solid #c8d8ea; border-radius: 10px; min-width: 560px; padding: 18px; }
.dialog.wide { min-width: 700px; }
.problem-list { max-height: 320px; overflow: auto; border: 1px solid #d9e5f1; border-radius: 10px; padding: 10px; margin-top: 10px; }
.problem-option { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.dialog-actions { display: flex; gap: 10px; margin-top: 10px; }
.head-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
</style>
