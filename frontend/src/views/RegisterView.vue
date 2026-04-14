<script setup>
import { reactive, ref } from 'vue'

const form = reactive({
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const error = ref('')
const success = ref('')

function onSubmit() {
  error.value = ''
  success.value = ''
  if (form.password !== form.confirmPassword) {
    error.value = '两次密码输入不一致，请检查。'
    return
  }
  success.value = `已模拟注册用户 ${form.nickname || form.email}（后续可接真实接口）`
}
</script>

<template>
  <section class="auth-wrap">
    <article class="auth-card">
      <p class="tag">Create Account</p>
      <h2>注册 Recognition OJ</h2>
      <p class="desc">创建账号后即可开始刷题、查看评测记录并参加比赛。</p>

      <form class="form" @submit.prevent="onSubmit">
        <label>
          昵称
          <input v-model="form.nickname" type="text" placeholder="请输入昵称" required />
        </label>
        <label>
          邮箱
          <input v-model="form.email" type="email" placeholder="请输入邮箱" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </label>
        <label>
          确认密码
          <input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" required />
        </label>

        <button type="submit">注 册</button>
      </form>

      <p v-if="error" class="tips error">{{ error }}</p>
      <p v-if="success" class="tips">{{ success }}</p>
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
  width: min(520px, 100%);
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

input {
  border: 1px solid #d4deea;
  border-radius: 10px;
  padding: 10px 12px;
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

.tips {
  margin: 14px 0 0;
  color: #2d8a49;
  background: #ecf9f1;
  border-radius: 10px;
  padding: 8px 10px;
}

.tips.error {
  color: #b2382f;
  background: #feeceb;
}
</style>
