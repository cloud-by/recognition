<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPatch } from '@/api/http'
import { createClass, deleteClass, fetchTeacherClasses } from '@/api/classes'
import { getAuthUser, setAuthUser } from '@/utils/auth'

const router = useRouter()
const user = ref(getAuthUser())

const loading = ref(false)
const loadError = ref('')
const saveMessage = ref('')
const saveError = ref('')

const profileForm = reactive({
  username: user.value?.username || '',
  nickname: user.value?.nickname || '',
  oldPassword: '',
  newPassword: '',
})

const history = reactive({
  submissions: [],
  contests: [],
})

const teacherClasses = ref([])
const classForm = reactive({ name: '', description: '' })
const inviteDraft = reactive({})

const userManage = reactive({
  keyword: '',
  users: [],
  selected: null,
  selectedSubmissions: [],
  selectedLoading: false,
  selectedError: '',
})

const roleText = computed(() => {
  if (user.value?.role === 'ADMIN') return '管理员'
  if (user.value?.role === 'TEACHER') return '老师'
  return '学生'
})

const filteredUsers = computed(() => {
  const q = userManage.keyword.trim().toLowerCase()
  return userManage.users.filter((item) => {
    if (item.role === 'ADMIN') return false
    if (!q) return true
    return `${item.id} ${item.username} ${item.nickname || ''} ${item.role}`.toLowerCase().includes(q)
  })
})

async function loadCommonData() {
  loading.value = true
  loadError.value = ''
  try {
    const [submissionResp, contestResp] = await Promise.all([
      apiGet(`/submissions?userId=${user.value?.id}`),
      apiGet(`/contests?userId=${user.value?.id}`),
    ])
    history.submissions = submissionResp.data || []
    history.contests = contestResp.data || []
  } catch (err) {
    loadError.value = err.message || '用户中心数据加载失败'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saveError.value = ''
  saveMessage.value = ''
  try {
    if (profileForm.username !== user.value?.username) {
      const resp = await apiPatch(`/users/${user.value?.id}/username`, { newUsername: profileForm.username })
      if (!resp.success) {
        saveError.value = resp.message || '用户名修改失败'
        return
      }
      user.value = {
        ...user.value,
        username: resp.data.username,
      }
      setAuthUser(user.value)
    }

    user.value = {
      ...user.value,
      nickname: profileForm.nickname,
    }
    setAuthUser(user.value)

    saveMessage.value = profileForm.newPassword
      ? '基本信息已更新。密码修改接口待后端开放。'
      : '基本信息已更新。'
  } catch (err) {
    saveError.value = err.message || '保存失败'
  }
}

async function loadTeacherClasses() {
  if (user.value?.role !== 'TEACHER') return
  const resp = await fetchTeacherClasses(user.value.id)
  teacherClasses.value = resp.data || []
}

async function createTeacherClass() {
  saveError.value = ''
  const resp = await createClass({
    teacherId: user.value.id,
    name: classForm.name,
    description: classForm.description,
  })
  if (!resp.success) {
    saveError.value = resp.message || '创建班级失败'
    return
  }
  classForm.name = ''
  classForm.description = ''
  await loadTeacherClasses()
}

async function removeTeacherClass(classId) {
  await deleteClass(classId, user.value.id)
  await loadTeacherClasses()
}

function saveInviteName(classId) {
  inviteDraft[classId] = (inviteDraft[classId] || '').trim()
}

async function loadUserManageData() {
  if (user.value?.role !== 'ADMIN') return
  const resp = await apiGet('/users')
  userManage.users = resp.data || []
}

async function inspectUser(target) {
  userManage.selected = target
  userManage.selectedSubmissions = []
  userManage.selectedError = ''
  userManage.selectedLoading = true
  try {
    const resp = await apiGet(`/submissions?userId=${target.id}`)
    userManage.selectedSubmissions = resp.data || []
  } catch (err) {
    userManage.selectedError = err.message || '加载该用户记录失败'
  } finally {
    userManage.selectedLoading = false
  }
}

onMounted(async () => {
  await loadCommonData()
  await Promise.all([loadTeacherClasses(), loadUserManageData()])
})
</script>

<template>
  <section class="page">
    <section class="panel profile-card">
      <div>
        <h3>{{ user?.nickname || user?.username }} 的用户中心</h3>
        <p class="muted">角色：{{ roleText }}（通用页支持学生 / 老师 / 管理员）</p>
      </div>
      <div class="chips">
        <span>做题记录 {{ history.submissions.length }}</span>
        <span>比赛/训练 {{ history.contests.length }}</span>
      </div>
    </section>

    <section class="panel">
      <h3>安全设置</h3>
      <form class="form" @submit.prevent="saveProfile">
        <label>
          用户名
          <input v-model="profileForm.username" required />
        </label>
        <label>
          昵称
          <input v-model="profileForm.nickname" placeholder="用于页面展示" />
        </label>
        <div class="row">
          <label>
            原密码
            <input v-model="profileForm.oldPassword" type="password" placeholder="可选" />
          </label>
          <label>
            新密码
            <input v-model="profileForm.newPassword" type="password" placeholder="可选" />
          </label>
        </div>
        <div class="actions">
          <button type="submit">保存设置</button>
          <p v-if="saveMessage" class="ok">{{ saveMessage }}</p>
          <p v-if="saveError" class="error">{{ saveError }}</p>
        </div>
      </form>
    </section>

    <section class="panel">
      <div class="section-head">
        <h3>历史记录</h3>
        <div class="quick-links">
          <button class="ghost" @click="router.push('/problems')">题目列表</button>
          <button class="ghost" @click="router.push('/submissions')">全部提交</button>
          <button class="ghost" @click="router.push('/contests')">比赛与训练</button>
        </div>
      </div>
      <p v-if="loading" class="muted">加载记录中...</p>
      <p v-else-if="loadError" class="error">{{ loadError }}</p>
      <div v-else class="history-grid">
        <article class="mini-panel">
          <h4>最近提交</h4>
          <ul>
            <li v-for="item in history.submissions.slice(0, 6)" :key="item.id">
              #{{ item.id }} · P{{ item.problemId }} · {{ item.judgeStatus }}
            </li>
            <li v-if="!history.submissions.length" class="muted">暂无记录</li>
          </ul>
        </article>
        <article class="mini-panel">
          <h4>参加过的比赛/训练</h4>
          <ul>
            <li v-for="item in history.contests.slice(0, 6)" :key="item.id">
              #{{ item.id }} · {{ item.title }} · {{ item.status }}
            </li>
            <li v-if="!history.contests.length" class="muted">暂无记录</li>
          </ul>
        </article>
      </div>
    </section>

    <section v-if="user?.role === 'TEACHER'" class="panel">
      <div class="section-head">
        <h3>我的班级（老师）</h3>
        <span class="muted">支持创建、邀请、管理基础班级</span>
      </div>
      <form class="form" @submit.prevent="createTeacherClass">
        <div class="row">
          <label>
            班级名称
            <input v-model="classForm.name" required />
          </label>
          <label>
            班级说明
            <input v-model="classForm.description" />
          </label>
        </div>
        <button type="submit">创建班级</button>
      </form>

      <ul class="class-list">
        <li v-for="item in teacherClasses" :key="item.id">
          <div>
            <strong>{{ item.name }}</strong>
            <p class="muted">{{ item.description || '暂无描述' }}</p>
            <div class="invite-box">
              <input v-model="inviteDraft[item.id]" placeholder="输入学生用户名后邀请" @blur="saveInviteName(item.id)" />
              <button class="ghost" type="button" @click="saveInviteName(item.id)">加入候选</button>
              <span class="muted">{{ inviteDraft[item.id] ? `待邀请：${inviteDraft[item.id]}` : '暂无待邀请学生' }}</span>
            </div>
          </div>
          <div class="actions vertical">
            <button class="ghost" type="button">修改名称</button>
            <button class="ghost" type="button">移出学生</button>
            <button class="danger" type="button" @click="removeTeacherClass(item.id)">解散班级</button>
          </div>
        </li>
      </ul>
      <p v-if="!teacherClasses.length" class="muted">你还没有班级，先创建一个吧。</p>
    </section>

    <section v-if="user?.role === 'ADMIN'" class="panel">
      <div class="section-head">
        <h3>用户管理（管理员）</h3>
        <span class="muted">可查看非管理员账号历史记录</span>
      </div>
      <label>
        搜索用户
        <input v-model="userManage.keyword" placeholder="按ID/用户名/昵称筛选（已排除管理员）" />
      </label>
      <div class="admin-grid">
        <ul class="user-list">
          <li v-for="item in filteredUsers" :key="item.id">
            <div>
              <strong>{{ item.nickname || item.username }}</strong>
              <p class="muted">#{{ item.id }} · {{ item.role }}</p>
            </div>
            <button class="ghost" @click="inspectUser(item)">查看记录</button>
          </li>
        </ul>

        <article class="mini-panel">
          <h4>记录详情</h4>
          <p v-if="!userManage.selected" class="muted">请先从左侧选择一个用户。</p>
          <template v-else>
            <p class="muted">
              当前查看：{{ userManage.selected.nickname || userManage.selected.username }}
              （{{ userManage.selected.role }}）
            </p>
            <p v-if="userManage.selectedLoading" class="muted">加载中...</p>
            <p v-else-if="userManage.selectedError" class="error">{{ userManage.selectedError }}</p>
            <ul v-else>
              <li v-for="item in userManage.selectedSubmissions.slice(0, 8)" :key="item.id">
                #{{ item.id }} · P{{ item.problemId }} · {{ item.judgeStatus }}
              </li>
              <li v-if="!userManage.selectedSubmissions.length" class="muted">该用户暂无提交记录</li>
            </ul>
          </template>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page { display: grid; gap: 14px; }
.panel { background: #fff; border: 1px solid #e6edf5; border-radius: 14px; padding: 16px; }
.profile-card { display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chips span { background: #edf5ff; color: #466381; padding: 6px 10px; border-radius: 999px; font-size: 12px; }
.form { display: grid; gap: 10px; }
label { display: grid; gap: 6px; color: #5f7185; }
input { border: 1px solid #cfdaea; border-radius: 8px; padding: 8px 10px; }
.row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
button { border: 0; background: linear-gradient(135deg, #3798f4, #2a7de7); color: #fff; border-radius: 8px; padding: 8px 12px; cursor: pointer; }
button.ghost { background: #edf4ff; color: #3d5b7b; }
button.danger { background: #ffefed; color: #be3a2e; }
.actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.actions.vertical { flex-direction: column; align-items: flex-end; }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.quick-links { display: flex; gap: 8px; }
.history-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.mini-panel { border: 1px solid #e5edf7; border-radius: 10px; padding: 12px; }
ul { margin: 0; padding-left: 18px; display: grid; gap: 6px; }
.class-list { list-style: none; padding: 0; margin-top: 12px; }
.class-list li { border: 1px solid #e6edf5; border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; gap: 10px; margin-bottom: 10px; }
.invite-box { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; margin-top: 8px; }
.admin-grid { display: grid; grid-template-columns: 1.2fr 1fr; gap: 12px; margin-top: 10px; }
.user-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 8px; }
.user-list li { border: 1px solid #e6edf5; border-radius: 10px; padding: 10px 12px; display: flex; justify-content: space-between; gap: 8px; align-items: center; }
.muted { color: #738395; margin: 0; }
.ok { color: #1f7a3e; margin: 0; }
.error { color: #d64545; margin: 0; }

@media (max-width: 960px) {
  .history-grid,
  .admin-grid,
  .row {
    grid-template-columns: 1fr;
  }
}
</style>
