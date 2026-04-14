<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const quickEntries = [
  { title: '题目检索', desc: '按题号、关键字、时空限制组合查询', to: '/problems' },
  { title: '提交记录', desc: '查看最近提交状态并排查错误', to: '/submissions' },
  { title: '比赛管理', desc: '浏览正在进行与历史比赛', to: '/contests' },
]
</script>

<template>
  <section class="welcome">
    <h2>在线判题平台基础框架</h2>
    <p>已完成用户端 / 管理端主流程，当前版本重点优化了题库检索与跳转体验。</p>
  </section>

  <section class="grid">
    <article v-for="item in quickEntries" :key="item.to" class="card">
      <h3>{{ item.title }}</h3>
      <p>{{ item.desc }}</p>
      <button @click="router.push(item.to)">立即进入</button>
    </article>
  </section>
</template>

<style scoped>
.welcome {
  background: #fff;
  border: 1px solid #e6ecf3;
  border-radius: 12px;
  padding: 18px;
}

h2 { margin: 0; color: #273142; }
p { color: #6d7b89; }

.grid {
  margin-top: 14px;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
}

.card {
  background: #fff;
  border: 1px solid #e6ecf3;
  border-radius: 12px;
  padding: 16px;
}

.card button {
  margin-top: 6px;
  border: 0;
  background: #2d8fee;
  color: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
}
</style>
