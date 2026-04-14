<script setup>
const menus = [
  { name: '总览', path: '/' },
  { name: '题目列表', path: '/problems' },
  { name: '提交记录', path: '/submissions' },
  { name: '比赛管理', path: '/contests' },
  { name: '管理端', path: '/admin' },
]
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <h1>OJ 平台</h1>
      <nav>
        <RouterLink v-for="item in menus" :key="item.path" :to="item.path">{{ item.name }}</RouterLink>
      </nav>
    </aside>
    <main class="content">
      <slot />
    </main>
  </div>
</template>

<style scoped>
.shell { display: grid; grid-template-columns: 220px 1fr; min-height: 100vh; }
.sidebar { background: #0f172a; color: #fff; padding: 24px 16px; }
.sidebar nav { display: grid; gap: 10px; margin-top: 16px; }
.sidebar a { color: #cbd5e1; text-decoration: none; }
.sidebar a.router-link-active { color: #fff; font-weight: bold; }
.content { padding: 24px; background: #f8fafc; }
</style>
