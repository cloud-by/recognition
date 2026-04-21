<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { apiGet, apiPost } from '@/api/http'
import { setAuthUser, setToken } from '@/utils/auth'

const router = useRouter()
const form = reactive({
  username: '',
  password: '',
  remember: true,
})

const message = ref('')
const error = ref('')
const loading = ref(false)
async function onSubmit() {
  loading.value = true
  message.value = ''
  error.value = ''
  try {
    const resp = await apiPost('/auth/login', {
      username: form.username,
      password: form.password,
    })

    if (!resp.success || !resp.data) {
      error.value = resp.message || '登录失败'
      return
    }
    setToken(resp.data)
    const resp2=await apiGet('/auth/user-info')
    setAuthUser(resp2.data)
    message.value = `登录成功，欢迎你：${resp2.data.nickname || resp2.data.username}`
    setTimeout(() => router.push('/problems'), 600)
  } catch (err) {
    error.value = err.message || '登录失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-wrap">
    <article class="auth-card">
      <p class="tag">Welcome Back</p>
      <h2>登录 Recognition OJ</h2>
      <p class="desc">登录后可提交代码、查看提交记录并参加比赛。</p>

      <form class="form" @submit.prevent="onSubmit">
        <label>
          用户名 / 邮箱
          <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </label>
        <label class="check">
          <input v-model="form.remember" type="checkbox" />
          记住我
        </label>

        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登 录' }}</button>
      </form>

      <p v-if="message" class="tips">{{ message }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </article>
  </section>
</template>

<style scoped>
.auth-wrap {
  min-height: calc(100vh - 160px);
  display: grid;
  place-items: center;
}

.auth-card {
  width: min(460px, 100%);
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 20px 40px rgba(23, 52, 86, 0.08);
}

.tag {
  margin: 0;
  color: #3f93ef;
  font-size: 12px;
  text-transform: uppercase;
}

h2 {
  margin: 8px 0;
}

.desc {
  color: #6e7d8d;
  margin: 0 0 16px;
}

.form {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  color: #617184;
  font-size: 13px;
}

input[type='text'],
input[type='password'] {
  border: 1px solid #d4deea;
  border-radius: 10px;
  padding: 10px 12px;
}

.check {
  display: flex;
  align-items: center;
  gap: 8px;
}

button {
  margin-top: 4px;
  border: 0;
  padding: 10px 14px;
  border-radius: 10px;
  color: #fff;
  background: linear-gradient(135deg, #3595f4, #2f82e8);
  cursor: pointer;
}

button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.tips {
  margin: 14px 0 0;
  color: #2d8a49;
  background: #ecf9f1;
  border-radius: 10px;
  padding: 8px 10px;
}

.error {
  margin-top: 10px;
  color: #d93025;
}
</style>
