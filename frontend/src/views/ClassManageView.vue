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
  <section class="panel">
    <h2>班级管理</h2>
    <form class="form" @submit.prevent="submitClass">
      <label>班级名称<input v-model="form.name" required /></label>
      <label>班级说明<textarea v-model="form.description" rows="3" /></label>
      <button type="submit">创建班级</button>
    </form>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="ok" class="ok">{{ ok }}</p>
  </section>

  <section class="panel">
    <h3>我的班级</h3>
    <ul>
      <li v-for="item in classes" :key="item.id">
        {{ item.name }} - {{ item.description || '无描述' }}
        <button class="link" @click="removeClass(item.id)">删除</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.panel { background:#fff; border:1px solid #e6ecf3; border-radius:12px; padding:16px; margin-bottom:12px; }
.form { display:grid; gap:10px; }
label { display:grid; gap:6px; }
input, textarea { border:1px solid #cedaea; border-radius:8px; padding:8px 10px; }
button { width:fit-content; border:0; background:#2d8fee; color:#fff; border-radius:8px; padding:8px 12px; }
.link { margin-left:8px; padding:0; background:transparent; color:#2d8fee; }
.error { color:#d93025; }
.ok { color:#1f7a3e; }
</style>
