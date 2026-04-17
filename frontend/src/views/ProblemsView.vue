<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { getAuthChangeEventName, getAuthUser, isAdminUser } from '@/utils/auth'
import { useRouter } from 'vue-router'
import { apiDelete, apiGet, apiPut } from '@/api/http'
import { emitOpenTagDialog } from '@/utils/uiEvents'
import { getProblemTags } from '@/utils/problemTags'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const problems = ref([])
const displayedProblems = ref([])
const total = computed(() => displayedProblems.value.length)

const filters = ref({
  keyword: '',
  maxTime: '',
  maxMemory: '',
})

const difficultyClassMap = {
  入门: 'lv-beginner',
  普及: 'lv-normal',
  提高: 'lv-advanced',
}

const overview = computed(() => ({
  beginner: displayedProblems.value.filter((item) => item.difficulty === '入门').length,
  normal: displayedProblems.value.filter((item) => !item.difficulty || item.difficulty === '普及').length,
  advanced: displayedProblems.value.filter((item) => item.difficulty === '提高').length,
}))

const manageDialogOpen = ref(false)
const editDialogOpen = ref(false)
const manageKeyword = ref('')
const manageFeedback = ref('')
const savingEdit = ref(false)
const tagOptions = ref([])

const difficultyOptions = ['入门', '普及', '提高']
const permissionOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '登录可见', value: 'LOGIN_REQUIRED' },
  { label: '比赛可见', value: 'CONTEST_ONLY' },
]

const editForm = ref({
  id: null,
  title: '',
  difficulty: '普及',
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  permissionType: 'PUBLIC',
  tags: [],
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  testcasePath: '',
})

const managedProblems = computed(() => {
  const text = manageKeyword.value.trim().toLowerCase()
  if (!text) return problems.value
  return problems.value.filter((problem) => {
    const title = String(problem.title || '').toLowerCase()
    const tags = String(problem.tags || '').toLowerCase()
    return String(problem.id).includes(text) || title.includes(text) || tags.includes(text)
  })
})

function applyFilters() {
  const keyword = filters.value.keyword.trim().toLowerCase()
  const maxTime = Number(filters.value.maxTime)
  const maxMemory = Number(filters.value.maxMemory)

  displayedProblems.value = problems.value.filter((problem) => {
    if (!problem) return false
    const title = String(problem.title || '').toLowerCase()
    const passKeyword = !keyword || String(problem.id || '').includes(keyword) || title.includes(keyword)
    const passTime = !maxTime || Number(problem.timeLimitMs || 0) <= maxTime
    const passMemory = !maxMemory || Number(problem.memoryLimitMb || 0) <= maxMemory
    return passKeyword && passTime && passMemory
  })
}

async function loadProblems() {
  loading.value = true
  error.value = ''
  try {
    const user = getAuthUser()
    const params = new URLSearchParams()
    if (user?.id) params.set('viewerUserId', user.id)
    const query = params.toString() ? `?${params.toString()}` : ''
    const resp = await apiGet(`/problems${query}`)
    problems.value = resp.data || []
    displayedProblems.value = [...problems.value]
  } catch (err) {
    error.value = err.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

function refreshList() {
  displayedProblems.value = [...problems.value]
}

function goDetail(problemId) {
  router.push(`/problems/${problemId}`)
}

function difficultyClass(difficulty = '普及') {
  return difficultyClassMap[difficulty] || 'lv-normal'
}

function openManageProblemsDialog() {
  manageKeyword.value = ''
  manageFeedback.value = ''
  manageDialogOpen.value = true
}

function openEditDialog(problem) {
  editForm.value = {
    id: problem.id,
    title: problem.title || '',
    difficulty: problem.difficulty || '普及',
    timeLimitMs: problem.timeLimitMs || 1000,
    memoryLimitMb: problem.memoryLimitMb || 256,
    permissionType: problem.permissionType || 'PUBLIC',
    tags: String(problem.tags || '').split(',').map((item) => item.trim()).filter(Boolean),
    description: problem.description || '',
    inputFormat: problem.inputFormat || '',
    outputFormat: problem.outputFormat || '',
    sampleInput: problem.sampleInput || '',
    sampleOutput: problem.sampleOutput || '',
    testcasePath: problem.testcasePath || '',
  }
  editDialogOpen.value = true
  loadTagOptions()
}

async function loadTagOptions() {
  const tags = await getProblemTags()
  tagOptions.value = tags.map((item) => item.name)
}

function toggleEditTag(tag) {
  if (editForm.value.tags.includes(tag)) {
    editForm.value.tags = editForm.value.tags.filter((item) => item !== tag)
    return
  }
  editForm.value.tags = [...editForm.value.tags, tag]
}

async function saveProblemEdit() {
  const user = getAuthUser()
  if (!user?.id || !editForm.value.id) return
  savingEdit.value = true
  manageFeedback.value = ''
  try {
    const payload = {
      editorUserId: user.id,
      title: editForm.value.title,
      description: editForm.value.description,
      inputFormat: editForm.value.inputFormat,
      outputFormat: editForm.value.outputFormat,
      sampleInput: editForm.value.sampleInput,
      sampleOutput: editForm.value.sampleOutput,
      difficulty: editForm.value.difficulty,
      permissionType: editForm.value.permissionType,
      tags: editForm.value.tags.join(','),
      timeLimitMs: Number(editForm.value.timeLimitMs),
      memoryLimitMb: Number(editForm.value.memoryLimitMb),
      testcasePath: editForm.value.testcasePath,
    }
    const resp = await apiPut(`/problems/${editForm.value.id}`, payload)
    if (!resp?.success) throw new Error(resp?.message || '保存失败')
    await loadProblems()
    applyFilters()
    editDialogOpen.value = false
    manageFeedback.value = `题目 P${String(editForm.value.id).padStart(4, '0')} 更新成功`
  } catch (err) {
    manageFeedback.value = err.message || '编辑题目失败'
  } finally {
    savingEdit.value = false
  }
}

async function deleteProblem(problem) {
  const user = getAuthUser()
  if (!user?.id) return
  const yes = window.confirm(`确定删除题目 P${String(problem.id).padStart(4, '0')} 吗？删除后题号将自动重排。`)
  if (!yes) return

  manageFeedback.value = ''
  try {
    const resp = await apiDelete(`/problems/${problem.id}?operatorUserId=${user.id}`)
    if (!resp?.success) throw new Error(resp?.message || '删除失败')
    problems.value = resp.data || []
    applyFilters()
    if (!filters.value.keyword && !filters.value.maxTime && !filters.value.maxMemory) {
      displayedProblems.value = [...problems.value]
    }
    manageFeedback.value = '删除成功，题号已同步重排。'
  } catch (err) {
    manageFeedback.value = err.message || '删除题目失败'
  }
}

onMounted(loadProblems)

onMounted(() => {
  window.addEventListener(getAuthChangeEventName(), loadProblems)
})

onUnmounted(() => {
  window.removeEventListener(getAuthChangeEventName(), loadProblems)
})
</script>

<template>
  <section class="problem-page">
    <section class="panel filter-panel">
      <form class="filters" @submit.prevent="applyFilters">
        <label>
          关键词
          <input v-model="filters.keyword" type="text" placeholder="请输入内容" />
        </label>
        <label>
          时间限制 ≤
          <input v-model="filters.maxTime" type="number" min="0" placeholder="请输入内容" />
        </label>
        <label>
          内存限制 ≤
          <input v-model="filters.maxMemory" type="number" min="0" placeholder="请输入内容" />
        </label>

        <div class="actions">
          <button type="submit" class="primary">查询</button>
          <button type="button" class="ghost" @click="refreshList">刷新</button>
          <button v-if="isAdminUser()" type="button" class="ghost" @click="router.push('/admin/problems/create')">上传题目</button>
          <button v-if="isAdminUser()" type="button" class="ghost" @click="emitOpenTagDialog">管理标签</button>
          <button v-if="isAdminUser()" type="button" class="ghost" @click="openManageProblemsDialog">编辑题目</button>
        </div>
      </form>

      <div class="overview">
        <span class="pill">共 {{ total }} 题</span>
        <span class="pill beginner">入门 {{ overview.beginner }}</span>
        <span class="pill normal">普及 {{ overview.normal }}</span>
        <span class="pill advanced">提高 {{ overview.advanced }}</span>
      </div>
    </section>

    <div v-if="manageDialogOpen" class="overlay" @click.self="manageDialogOpen = false">
      <section class="dialog wide">
        <header>
          <h3>编辑题目</h3>
          <button type="button" class="ghost" @click="manageDialogOpen = false">关闭</button>
        </header>
        <form class="search-row" @submit.prevent>
          <input v-model="manageKeyword" type="text" placeholder="模糊搜索题号 / 标题 / 标签" />
        </form>
        <p class="hint">默认展示全部题目，可直接编辑或删除。</p>
        <div class="table-wrap">
          <table class="manage-table">
            <thead>
            <tr>
              <th>题号</th>
              <th>标题</th>
              <th>标签</th>
              <th>操作</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="item in managedProblems" :key="`manage-${item.id}`">
              <td>P{{ String(item.id).padStart(4, '0') }}</td>
              <td>{{ item.title }}</td>
              <td>{{ item.tags || '-' }}</td>
              <td class="ops">
                <button type="button" class="primary small" @click="openEditDialog(item)">编辑</button>
                <button type="button" class="danger small" @click="deleteProblem(item)">删除</button>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
        <p v-if="manageFeedback" class="feedback">{{ manageFeedback }}</p>
      </section>
    </div>

    <div v-if="editDialogOpen" class="overlay" @click.self="editDialogOpen = false">
      <section class="dialog edit-dialog">
        <header>
          <h3>编辑题目 P{{ String(editForm.id || '').padStart(4, '0') }}</h3>
          <button type="button" class="ghost" @click="editDialogOpen = false">关闭</button>
        </header>

        <form class="edit-form" @submit.prevent="saveProblemEdit">
          <label>题目名称<input v-model="editForm.title" required placeholder="请输入内容" /></label>

          <div class="line">
            <label>等级
              <select v-model="editForm.difficulty">
                <option v-for="item in difficultyOptions" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label>时间限制
              <input v-model="editForm.timeLimitMs" type="number" min="1" required placeholder="请输入内容" />
            </label>
            <label>内存限制
              <input v-model="editForm.memoryLimitMb" type="number" min="1" required placeholder="请输入内容" />
            </label>
          </div>

          <label>权限
            <select v-model="editForm.permissionType">
              <option v-for="item in permissionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </label>

          <label>标签</label>
          <div class="tag-list">
            <button
              v-for="tag in tagOptions"
              :key="tag"
              type="button"
              :class="['tag-btn', { active: editForm.tags.includes(tag) }]"
              @click="toggleEditTag(tag)"
            >
              {{ tag }}
            </button>
          </div>

          <label>题目描述<textarea v-model="editForm.description" required placeholder="请输入内容" /></label>
          <label>输入格式<textarea v-model="editForm.inputFormat" placeholder="请输入内容" /></label>
          <label>输出格式<textarea v-model="editForm.outputFormat" placeholder="请输入内容" /></label>
          <label>样例输入<textarea v-model="editForm.sampleInput" placeholder="请输入内容" /></label>
          <label>样例输出<textarea v-model="editForm.sampleOutput" placeholder="请输入内容" /></label>
          <label>测试数据路径<input v-model="editForm.testcasePath" required placeholder="请输入内容" /></label>

          <button class="primary" type="submit" :disabled="savingEdit">{{ savingEdit ? '保存中...' : '保存修改' }}</button>
        </form>
      </section>
    </div>

    <section class="panel table-panel">
      <p v-if="loading" class="state">加载中...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <div v-else-if="displayedProblems.length" class="table-wrap">
        <table>
          <thead>
          <tr>
            <th>题号</th>
            <th>题目名称</th>
            <th>等级</th>
            <th>时间限制</th>
            <th>内存限制</th>
            <th>权限</th>
            <th>标签</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="p in displayedProblems" :key="p.id">
            <td>P{{ String(p.id).padStart(4, '0') }}</td>
            <td class="title">
              <button class="title-link" @click="goDetail(p.id)">{{ p.title }}</button>
            </td>
            <td>
              <span :class="['difficulty', difficultyClass(p.difficulty)]">{{ p.difficulty || '普及' }}</span>
            </td>
            <td>{{ p.timeLimitMs }} ms</td>
            <td>{{ p.memoryLimitMb }} MB</td>
            <td>{{ p.permissionType }}</td>
            <td>{{ p.tags || '-' }}</td>
            <td>
              <button class="link" @click="goDetail(p.id)">查看详情</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-block">
        <p>暂无匹配题目</p>
        <button class="ghost" @click="refreshList">刷新</button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.problem-page {
  display: grid;
  gap: 14px;
}

.panel {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid #e2ebf6;
  border-radius: 14px;
  padding: 16px;
  box-shadow: 0 10px 26px rgba(52, 93, 141, 0.06);
}

.filters {
  display: flex;
  gap: 10px;
  align-items: end;
  flex-wrap: wrap;
}

label {
  display: grid;
  gap: 6px;
  color: #5c6a79;
  font-size: 13px;
}

input,
textarea,
select {
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 9px 10px;
  min-width: 170px;
  background: #fbfdff;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

button {
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
}

.primary {
  background: #2990f3;
  color: #fff;
}

.ghost {
  background: #eef4fd;
  color: #4c627b;
}

.overview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.pill {
  background: #eef4ff;
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 13px;
  color: #567;
}

.pill.beginner {
  background: #fff0ea;
  color: #d84f35;
}

.pill.normal {
  background: #e9f8ee;
  color: #2f9156;
}

.pill.advanced {
  background: #edf1ff;
  color: #4b68d1;
}

.table-wrap {
  overflow: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 860px;
}

th,
td {
  text-align: left;
  padding: 12px 8px;
  border-bottom: 1px solid #edf2f8;
  color: #2f3f50;
}

th {
  color: #607286;
  font-size: 13px;
}

.title-link,
.link {
  border: 0;
  background: transparent;
  color: #2a7de3;
  padding: 0;
  cursor: pointer;
}

.difficulty {
  font-weight: 600;
}

.lv-beginner {
  color: #d84f35;
}

.lv-normal {
  color: #2f9156;
}

.lv-advanced {
  color: #4b68d1;
}

.state {
  margin: 0;
  color: #62758b;
}

.state.error {
  color: #cf4545;
}

.empty-block {
  display: grid;
  justify-items: center;
  gap: 10px;
  color: #74859a;
  padding: 18px 0;
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(21, 34, 52, 0.48);
  display: grid;
  place-items: center;
  z-index: 80;
  backdrop-filter: blur(2px);
}

.dialog {
  width: min(920px, 94vw);
  max-height: 88vh;
  overflow: auto;
  background: linear-gradient(180deg, #ffffff, #f6f9ff);
  border: 1px solid #d8e5f6;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 20px 44px rgba(20, 38, 61, 0.2);
  display: grid;
  gap: 12px;
}

.wide {
  width: min(980px, 95vw);
}

.edit-dialog {
  width: min(980px, 95vw);
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

h3 {
  margin: 0;
  color: #1f3347;
}

.search-row input {
  width: 100%;
  min-width: 0;
}

.hint {
  margin: 0;
  color: #6c7f95;
  font-size: 13px;
}

.manage-table {
  min-width: 700px;
}

.ops {
  display: flex;
  gap: 8px;
}

.small {
  padding: 6px 10px;
  font-size: 12px;
}

.danger {
  background: #ffe9e9;
  color: #c93737;
}

.feedback {
  margin: 0;
  color: #355f8f;
}

.edit-form {
  display: grid;
  gap: 10px;
}

.line {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

textarea {
  min-height: 90px;
  resize: vertical;
}

.tag-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-btn {
  border: 1px solid #d7e4f2;
  background: #fff;
  color: #496580;
  border-radius: 999px;
  padding: 5px 12px;
}

.tag-btn.active {
  background: #ebf4ff;
  border-color: #95c3fa;
  color: #1e6ec4;
}
</style>
