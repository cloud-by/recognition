<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const menus = [
  { name: '首页', path: '/', icon: '⌂' },
  { name: '题目列表', path: '/problems', icon: '▤' },
  { name: '提交记录', path: '/submissions', icon: '↻' },
  { name: '比赛管理', path: '/contests', icon: '🏁' },
  { name: '管理端', path: '/admin', icon: '⚙' },
]

const pageTitle = computed(() => menus.find((item) => route.path.startsWith(item.path))?.name || '在线评测')
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">R</div>
      <nav>
        <RouterLink v-for="item in menus" :key="item.path" :to="item.path" class="menu-link" :title="item.name">
          <span class="icon">{{ item.icon }}</span>
          <span class="name">{{ item.name }}</span>
        </RouterLink>
      </nav>
    </aside>

    <div class="main-wrap">
      <header class="topbar">
        <div>
          <p class="crumb">Recognition OJ / {{ pageTitle }}</p>
          <h1>{{ pageTitle }}</h1>
        </div>
        <div class="user-actions">
          <button class="text-btn">登录</button>
          <button class="text-btn">注册</button>
        </div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: grid;
  grid-template-columns: 76px 1fr;
  min-height: 100vh;
  background: #eef3f8;
}

.sidebar {
  background: #fff;
  border-right: 1px solid #e5eaf0;
  display: grid;
  grid-template-rows: auto 1fr;
  justify-items: center;
  padding: 16px 10px;
  gap: 20px;
}

.brand {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(130deg, #5ea7ff 0%, #53dc9a 100%);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
}

.sidebar nav {
  display: grid;
  gap: 10px;
}

.menu-link {
  width: 54px;
  border-radius: 12px;
  padding: 8px 4px;
  display: grid;
  gap: 4px;
  justify-items: center;
  color: #637080;
  text-decoration: none;
  font-size: 12px;
}

.menu-link:hover,
.menu-link.router-link-active {
  background: #edf5ff;
  color: #2b84e8;
}

.icon {
  font-size: 18px;
  line-height: 1;
}

.main-wrap {
  display: grid;
  grid-template-rows: auto 1fr;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e5eaf0;
  padding: 14px 26px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.crumb {
  margin: 0;
  color: #96a2b2;
  font-size: 12px;
}

h1 {
  margin: 4px 0 0;
  font-size: 24px;
  color: #273142;
}

.user-actions {
  display: flex;
  gap: 8px;
}

.text-btn {
  border: 0;
  background: #f5f8fd;
  color: #526172;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.content {
  padding: 24px;
}
</style>
