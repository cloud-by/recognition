<script setup>
import { onMounted, reactive, ref } from 'vue'
import { createClass, deleteClass, fetchTeacherClasses } from '@/api/classes'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const classes = ref([])
const error = ref('')
const ok = ref('')
const form = reactive({ name: '', description: '' })

async function loadClasses() {
  const resp = await fetchTeacherClasses(user.id)
  classes.value = resp.data || []
}

async function submitClass() {
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

async function removeClass(id) {
  await deleteClass(id, user.id)
  await loadClasses()
}

onMounted(loadClasses)
</script>

<template>
  <section class="page">
    <section class="panel form-panel">
      <form class="form" @submit.prevent="submitClass">
        <label>班级名称<input v-model="form.name" required /></label>
        <label>班级说明<textarea v-model="form.description" rows="3" /></label>
        <button type="submit">创建班级</button>
      </form>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="ok" class="ok">{{ ok }}</p>
    </section>

    <section class="panel">
      <div class="header">
        <span>班级数量：{{ classes.length }}</span>
      </div>
      <ul v-if="classes.length" class="class-list">
        <li v-for="item in classes" :key="item.id">
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.description || '无描述' }}</p>
          </div>
          <button class="link" @click="removeClass(item.id)">删除</button>
        </li>
      </ul>
      <p v-else class="empty">暂无班级，先创建一个吧。</p>
    </section>
  </section>
</template>

<style scoped>
.page {
  display: grid;
  gap: 12px;
}

.panel {
  background: #fff;
  border: 1px solid #e6ecf3;
  border-radius: 12px;
  padding: 16px;
}

.form {
  display: grid;
  gap: 10px;
}

label {
  display: grid;
  gap: 6px;
  color: #607186;
}

input,
textarea {
  border: 1px solid #cedaea;
  border-radius: 8px;
  padding: 8px 10px;
}

button {
  width: fit-content;
  border: 0;
  background: #2d8fee;
  color: #fff;
  border-radius: 8px;
  padding: 8px 12px;
}

.header {
  color: #607186;
  margin-bottom: 10px;
}

.class-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
}

.class-list li {
  border: 1px solid #e6edf5;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.class-list p {
  margin: 6px 0 0;
  color: #6e7f93;
}

.link {
  margin-left: 8px;
  background: #ffeeec;
  color: #c43a2f;
}

.error {
  color: #d93025;
}

.ok {
  color: #1f7a3e;
}

.empty {
  margin: 0;
  color: #7a8b9e;
}
</style>
