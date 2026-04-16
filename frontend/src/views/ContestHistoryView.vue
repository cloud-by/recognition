<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchContestDetail, fetchContestList, fetchContestProblemOptions, updateContest } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const contests = ref([])
const problems = ref([])
const loading = ref(false)
const keyword = ref('')
const error = ref('')
const notice = ref('')
const detailDialog = ref(null)
const editDialog = ref(null)
const active = ref(null)
const editForm = reactive({
  id: null,
  title: '',
  startTime: '',
  endTime: '',
  contestType: 'ACM',
  rankingPolicy: 'FORMAL',
  freezeBoard: false,
  allowedIpRule: '',
  problemIds: [],
})

const filtered = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  return contests.value
    .slice()
    .sort((a, b) => new Date(b.startTime) - new Date(a.startTime))
    .filter((item) => !q || `${item.id} ${item.title} ${item.status} ${item.allowedIpRule || ''}`.toLowerCase().includes(q))
})

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [contestResp, problemResp] = await Promise.all([fetchContestList(user?.id), fetchContestProblemOptions()])
    contests.value = contestResp.data || []
    problems.value = problemResp.data || []
  } catch (err) {
    error.value = err.message || '加载历史失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(item) {
  const resp = await fetchContestDetail(item.id)
  if (!resp.success) {
    error.value = resp.message || '加载详情失败'
    return
  }
  active.value = resp.data
  detailDialog.value?.showModal()
}

async function openEdit(item) {
  const resp = await fetchContestDetail(item.id)
  if (!resp.success) {
    error.value = resp.message || '加载比赛失败'
    return
  }
  const data = resp.data
  Object.assign(editForm, {
    id: data.id,
    title: data.title,
    startTime: toLocalInput(data.startTime),
    endTime: toLocalInput(data.endTime),
    contestType: data.contestType,
    rankingPolicy: data.rankingPolicy,
    freezeBoard: data.freezeBoard,
    allowedIpRule: data.allowedIpRule || '',
    problemIds: data.problems.map((x) => x.problemId),
  })
  editDialog.value?.showModal()
}

async function saveEdit() {
  const resp = await updateContest(editForm.id, {
    operatorUserId: user?.id,
    title: editForm.title,
    startTime: editForm.startTime,
    endTime: editForm.endTime,
    contestType: editForm.contestType,
    rankingPolicy: editForm.rankingPolicy,
    freezeBoard: editForm.freezeBoard,
    allowedIpRule: editForm.allowedIpRule,
    problemIds: editForm.problemIds,
  })
  if (!resp.success) {
    error.value = resp.message || '更新失败'
    return
  }
  notice.value = `已更新：${resp.data.title}`
  editDialog.value?.close()
  await loadData()
}

function toLocalInput(value) {
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

onMounted(loadData)
</script>

<template>
  <section class="panel">
    <div class="head-row">
      <h3>创建比赛历史</h3>
      <input v-model="keyword" placeholder="关键词模糊搜索（默认时间降序）" />
    </div>
    <p v-if="notice" class="ok">{{ notice }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading" class="empty">加载中...</p>
    <ul v-else class="list">
      <li v-for="item in filtered" :key="item.id">
        <div>
          <strong>#{{ item.id }} {{ item.title }}</strong>
          <p>{{ item.startTime }} ~ {{ item.endTime }} ｜ {{ item.status }}</p>
          <small>IP规则：{{ item.allowedIpRule || '不限' }}</small>
        </div>
        <div class="actions">
          <button class="ghost" @click="openDetail(item)">查看信息</button>
          <button v-if="item.status === 'NOT_STARTED'" @click="openEdit(item)">编辑</button>
        </div>
      </li>
    </ul>
  </section>

  <dialog ref="detailDialog" class="dialog">
    <h3>比赛信息</h3>
    <template v-if="active">
      <p>{{ active.title }}（{{ active.status }}）</p>
      <p>时间：{{ active.startTime }} ~ {{ active.endTime }}</p>
      <p>题目列表：</p>
      <ul>
        <li v-for="p in active.problems" :key="p.problemId">#{{ p.sortOrder }} {{ p.title }}（{{ p.difficulty }}）</li>
      </ul>
      <p>IP规则：{{ active.allowedIpRule || '不限' }}</p>
    </template>
    <form method="dialog"><button class="ghost">关闭</button></form>
  </dialog>

  <dialog ref="editDialog" class="dialog wide">
    <h3>编辑比赛（仅未开始）</h3>
    <form class="form" @submit.prevent="saveEdit">
      <label>比赛名<input v-model="editForm.title" required /></label>
      <div class="row">
        <label>开始时间<input v-model="editForm.startTime" type="datetime-local" required /></label>
        <label>结束时间<input v-model="editForm.endTime" type="datetime-local" required /></label>
      </div>
      <label>IP规则<input v-model="editForm.allowedIpRule" placeholder="10.10.,192.168." /></label>
      <label>题目（滚动 + 关键词搜索可在创建页完成，这里支持直接勾选）</label>
      <div class="problems">
        <label v-for="p in problems" :key="p.id"><input v-model="editForm.problemIds" :value="p.id" type="checkbox" />#{{ p.id }} {{ p.title }}</label>
      </div>
      <div class="actions">
        <button type="submit">保存</button>
        <button type="button" class="ghost" @click="editDialog?.close()">取消</button>
      </div>
    </form>
  </dialog>
</template>

<style scoped>
.panel { background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; padding: 16px; }
.head-row { display: flex; justify-content: space-between; gap: 8px; align-items: center; margin-bottom: 8px; }
input { border: 1px solid #cedaea; border-radius: 8px; padding: 8px 10px; }
.list { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.list li { border: 1px solid #e6edf5; border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; gap: 10px; }
p { margin: 6px 0 0; color: #617387; }
small { color: #617387; }
button { border: 0; background: #2d8fee; color: #fff; border-radius: 8px; padding: 9px 14px; cursor: pointer; }
button.ghost { background: #eef4ff; color: #426082; }
.actions { display: flex; gap: 8px; align-items: center; }
.dialog { border: 1px solid #c8d8ea; border-radius: 10px; min-width: 560px; padding: 18px; }
.dialog.wide { min-width: 760px; }
.form { display: grid; gap: 10px; }
.row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.problems { max-height: 260px; overflow: auto; border: 1px solid #d9e5f1; border-radius: 8px; padding: 8px; display: grid; gap: 4px; }
.ok { color: #1f7a3e; }
.error { color: #d93025; }
.empty { color: #738293; }
</style>
