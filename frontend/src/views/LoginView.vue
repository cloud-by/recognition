<script setup>
import { reactive, ref } from 'vue'

const form = reactive({
  username: '',
  password: '',
  remember: true,
})

const message = ref('')

function onSubmit() {
  message.value = `已模拟登录：${form.username || '未填写用户名'}（后续可接真实接口）`
}
</script>

<template>
  <section class="auth-wrap">
    <article class="auth-card">
      <p class="tag">Welcome Back</p>
      <h2>登录 Recognition OJ</h2>
      <p class="desc">登录后可查看提交记录、参与比赛并使用管理功能（按权限）。</p>

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

        <button type="submit">登 录</button>
      </form>

      <p v-if="message" class="tips">{{ message }}</p>
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

.tips {
  margin: 14px 0 0;
  color: #2d8a49;
  background: #ecf9f1;
  border-radius: 10px;
  padding: 8px 10px;
}
</style>
