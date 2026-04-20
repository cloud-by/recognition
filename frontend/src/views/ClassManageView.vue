<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { batchInviteStudents, createClass, deleteClass, fetchClasses, fetchClassStudentRecords } from '@/api/classes'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const classes = ref([])
const error = ref('')
const ok = ref('')
const listKeyword = ref('')
const inviteText = ref('')
const records = ref([])
const selectedClass = ref(null)
const studentDialogVisible = ref(false)
const loadingRecords = ref(false)
const form = reactive({ name: '', description: '' })

const canCreateClass = computed(() => user?.role === 'TEACHER')

async function loadClasses() {
  const resp = await fetchClasses(user.id, listKeyword.value)
  classes.value = resp.data || []
}

async function submitClass() {
  if (!canCreateClass.value) {
    error.value = '仅老师可创建班级'
    return
  }
  error.value = ''
  ok.value = ''
  const resp = await createClass({ teacherId: user.id, name: form.name, description: form.description })
  if (!resp.success) {
    error.value = resp.message || '创建失败'
    return
  }
  ok.value = '班级创建成功'
  form.name = ''
  form.description = ''
  await loadClasses()
}

async function submitBatchInvite(item) {
  error.value = ''
  ok.value = ''
  const usernames = inviteText.value
    .split(/[\n,，\s]+/)
    .map((x) => x.trim())
    .filter(Boolean)
  if (!usernames.length) {
    error.value = '请先输入学生用户名（可换行/逗号分隔）'
    return
  }
  const resp = await batchInviteStudents(item.id, { operatorUserId: user.id, studentUsernames: usernames })
  if (!resp.success) {
    error.value = resp.message || '批量邀请失败'
    return
  }
  const bound = resp.data?.boundStudents?.length || 0
  const invalid = resp.data?.invalidUsers?.length || 0
  ok.value = `邀请完成：绑定 ${bound} 人，失败 ${invalid} 人`
  inviteText.value = ''
  await loadClasses()
}

async function showRecords(item) {
  selectedClass.value = item
  studentDialogVisible.value = true
  loadingRecords.value = true
  const resp = await fetchClassStudentRecords(item.id, user.id)
  records.value = resp.success ? (resp.data?.students || []) : []
  if (!resp.success) error.value = resp.message || '加载学生记录失败'
  loadingRecords.value = false
}

function closeDialog() {
  studentDialogVisible.value = false
  records.value = []
}

async function removeClass(id) {
  await deleteClass(id, user.id)
  await loadClasses()
}

onMounted(loadClasses)
</script>

<template>
  <section class="page">
    <section v-if="canCreateClass" class="panel form-panel">
      <form class="form" @submit.prevent="submitClass">
        <label>班级名称<input v-model="form.name" required /></label>
        <label>班级说明<textarea v-model="form.description" rows="3" /></label>
        <button type="submit">创建班级</button>
      </form>
    </section>

    <section class="panel">
      <div class="header">
        <span>班级数量：{{ classes.length }}</span>
        <input v-model="listKeyword" placeholder="搜索班级名称" @input="loadClasses" />
      </div>
      <ul v-if="classes.length" class="class-list">
        <li v-for="item in classes" :key="item.id">
          <div class="main">
            <strong>{{ item.name }}</strong>
            <p>老师：{{ item.teacherNickname }}｜学生：{{ item.studentCount }} 人</p>
            <p>{{ item.description || '无描述' }}</p>
            <div class="actions">
              <button class="link" @click="showRecords(item)">查看学生记录</button>
              <button v-if="user.role !== 'STUDENT'" class="link danger" @click="removeClass(item.id)">删除</button>
            </div>
          </div>

          <div v-if="user.role !== 'STUDENT'" class="invite-box">
            <textarea v-model="inviteText" rows="3" placeholder="输入学生用户名，支持换行/逗号批量邀请" />
            <button @click="submitBatchInvite(item)">批量邀请学生</button>
          </div>
        </li>
      </ul>
      <p v-else class="empty">暂无班级。</p>
    </section>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="ok" class="ok">{{ ok }}</p>

    <section v-if="studentDialogVisible" class="dialog-mask" @click.self="closeDialog">
      <article class="dialog">
        <header>
          <h3>{{ selectedClass?.name }}｜学生记录</h3>
          <button class="link" @click="closeDialog">关闭</button>
        </header>
        <p v-if="loadingRecords" class="empty">加载中...</p>
        <table v-else class="table">
          <thead>
          <tr><th>用户</th><th>昵称</th><th>提交数</th><th>AC数</th><th>最近提交</th></tr>
          </thead>
          <tbody>
          <tr v-for="s in records" :key="s.userId">
            <td>{{ s.username }}</td>
            <td>{{ s.nickname }}</td>
            <td>{{ s.totalSubmissions }}</td>
            <td>{{ s.acCount }}</td>
            <td>{{ s.lastSubmitTime || '-' }}</td>
          </tr>
          <tr v-if="!records.length"><td colspan="5" class="empty">暂无记录</td></tr>
          </tbody>
        </table>
      </article>
    </section>
  </section>
</template>

<style scoped>
.page { display: grid; gap: 12px; }
.panel { background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; padding: 16px; }
.form { display: grid; gap: 10px; }
label { display: grid; gap: 6px; color: #607186; }
input, textarea { border: 1px solid #cedaea; border-radius: 8px; padding: 8px 10px; }
button { width: fit-content; border: 0; background: #2d8fee; color: #fff; border-radius: 8px; padding: 8px 12px; }
.header { color: #607186; margin-bottom: 10px; display: flex; justify-content: space-between; gap: 8px; }
.class-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 10px; }
.class-list li { border: 1px solid #e6edf5; border-radius: 10px; padding: 12px; display: grid; gap: 10px; }
.actions { display: flex; gap: 8px; margin-top: 8px; }
.link { background: #e9f2fd; color: #1f68b6; }
.danger { background: #ffeeec; color: #c43a2f; }
.error { color: #d93025; }
.ok { color: #1f7a3e; }
.empty { margin: 0; color: #7a8b9e; }
.invite-box { display: grid; gap: 6px; }
.dialog-mask { position: fixed; inset: 0; background: rgba(0,0,0,.32); display: grid; place-items: center; z-index: 99; }
.dialog { width: min(900px, 92vw); max-height: 80vh; overflow: auto; background: #fff; border-radius: 12px; padding: 16px; }
.dialog header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.table { width: 100%; border-collapse: collapse; }
.table th, .table td { border-bottom: 1px solid #edf2f9; padding: 8px; text-align: left; }
</style>
