<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const quickEntries = [
  { title: '题目检索', desc: '按题号、关键字、时空限制组合查询', to: '/problems', icon: '🔎' },
  { title: '提交记录', desc: '查看最近提交状态并排查错误', to: '/submissions', icon: '🧾' },
  { title: '比赛管理', desc: '浏览正在进行与历史比赛', to: '/contests', icon: '🏆' },
]

const highlights = [
  { label: '活跃题目源', value: '5+' },
  { label: '支持比赛模式', value: 'ACM / Practice' },
  { label: '管理功能', value: '防作弊日志' },
]
</script>

<template>
  <section class="welcome">
    <div>
      <p class="tag">OJ Platform</p>
      <h2>在线判题平台基础框架</h2>
      <p>已完成用户端 / 管理端主流程，当前版本重点优化题目检索、记录浏览与页面一致性体验。</p>
    </div>
    <div class="stats">
      <article v-for="item in highlights" :key="item.label" class="stat-item">
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>
  </section>

  <section class="grid">
    <article v-for="item in quickEntries" :key="item.to" class="card">
      <div class="head">
        <span class="icon">{{ item.icon }}</span>
        <h3>{{ item.title }}</h3>
      </div>
      <p>{{ item.desc }}</p>
      <button @click="router.push(item.to)">立即进入</button>
    </article>
  </section>
</template>

<style scoped>
.welcome {
  background: linear-gradient(125deg, #253449 0%, #2e4866 45%, #2f6793 100%);
  border-radius: 16px;
  padding: 22px;
  color: #f8fbff;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 18px;
}

.tag {
  margin: 0;
  color: #9dc8f6;
  font-size: 12px;
  text-transform: uppercase;
}

h2 {
  margin: 8px 0;
  font-size: 34px;
}

.welcome p {
  color: #d1e3f6;
}

.stats {
  display: grid;
  gap: 10px;
}

.stat-item {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 12px;
  padding: 12px;
}

.stat-item p {
  margin: 0;
  font-size: 12px;
  color: #c9def4;
}

.stat-item strong {
  font-size: 18px;
}

.grid {
  margin-top: 16px;
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

.card {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 12px 32px rgba(25, 44, 74, 0.06);
}

.head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: grid;
  place-items: center;
  background: #ebf4ff;
}

.card h3 {
  margin: 0;
}

.card p {
  color: #6d7b89;
  min-height: 40px;
}

.card button {
  margin-top: 8px;
  border: 0;
  background: linear-gradient(135deg, #3595f4, #2f82e8);
  color: #fff;
  border-radius: 10px;
  padding: 9px 14px;
  cursor: pointer;
}

@media (max-width: 900px) {
  .welcome {
    grid-template-columns: 1fr;
  }

  h2 {
    font-size: 28px;
  }
}
</style>
