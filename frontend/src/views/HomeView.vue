<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAuthUser } from '@/utils/auth'

const router = useRouter()
const user = getAuthUser()

const quickEntries = computed(() => {
  const menus = [
    { title: '题目检索', to: '/problems' },
    { title: '提交记录', to: '/submissions' },
    { title: '比赛大厅', to: '/contests' },
  ]

  if (user?.role === 'ADMIN') {
    menus.push({ title: '比赛管理', to: '/contests/manage' })
  }

  return menus
})
</script>

<template>
  <section class="grid">
    <article v-for="item in quickEntries" :key="item.to" class="card">
      <h3>{{ item.title }}</h3>
      <button @click="router.push(item.to)">立即进入</button>
    </article>
  </section>
</template>

<style scoped>
.grid {
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

.card h3 {
  margin: 0;
}

.card button {
  margin-top: 10px;
  border: 0;
  background: #2d8fee;
  color: #fff;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
}
</style>
