<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAuthChangeEventName, getAuthUser, setAuthUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const user = ref(null)

const allMenus = [
  { name: '首页', path: '/', icon: '⌂' },
  { name: '题目列表', path: '/problems', icon: '▤' },
  { name: '提交记录', path: '/submissions', icon: '↻' },
  { name: '比赛大厅', path: '/contests', icon: '🏁' },
  { name: '比赛管理', path: '/contests/manage', icon: '🛠', managerOnly: true },
  { name: '班级管理', path: '/classes/manage', icon: '🏫', teacherOnly: true },
  { name: '管理端', path: '/admin', icon: '⚙', adminOnly: true },
]

const menus = computed(() => allMenus.filter((item) => {
  if (item.adminOnly) return user.value?.role === 'ADMIN'
  if (item.teacherOnly) return user.value?.role === 'TEACHER'
  if (item.managerOnly) return ['ADMIN', 'TEACHER'].includes(user.value?.role)
  return true
}))
const pageTitle = computed(() => {
  const matched = [...allMenus]
    .sort((a, b) => b.path.length - a.path.length)
    .find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`))
  return matched?.name || '在线评测'
})

function refreshUser() {
  user.value = getAuthUser()
}

function logout() {
  setAuthUser(null)
  refreshUser()
  router.push('/login')
}

onMounted(() => {
  refreshUser()
  window.addEventListener('storage', refreshUser)
  window.addEventListener(getAuthChangeEventName(), refreshUser)
})

onUnmounted(() => {
  window.removeEventListener('storage', refreshUser)
  window.removeEventListener(getAuthChangeEventName(), refreshUser)
})
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
        <h1>{{ pageTitle }}</h1>
        <div class="user-actions">
          <template v-if="user?.id">
            <span class="welcome">你好，{{ user.nickname || user.username }}</span>
            <span v-if="user?.role" class="role-badge">{{ user.role === 'ADMIN' ? '管理员' : user.role === 'TEACHER' ? '老师' : '学生' }}</span>
            <button class="text-btn" @click="logout">退出</button>
          </template>
          <template v-else>
            <RouterLink to="/login" class="text-btn">登录</RouterLink>
            <RouterLink to="/register" class="text-btn primary">注册</RouterLink>
          </template>
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
  grid-template-columns: 82px 1fr;
  min-height: 100vh;
  background: radial-gradient(circle at top, #f2f8ff, #edf2f8 35%, #e8eef5 100%);
}

.sidebar {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(8px);
  border-right: 1px solid #e5eaf0;
  display: grid;
  grid-template-rows: auto 1fr;
  justify-items: center;
  padding: 16px 10px;
  gap: 20px;
}

.brand {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #5ea7ff 0%, #57d5a2 100%);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
  box-shadow: 0 10px 30px rgba(62, 149, 255, 0.3);
}

.sidebar nav {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.menu-link {
  width: 58px;
  border-radius: 14px;
  padding: 10px 4px;
  display: grid;
  gap: 6px;
  justify-items: center;
  color: #637080;
  text-decoration: none;
  font-size: 12px;
  transition: all 0.2s ease;
}

.menu-link:hover,
.menu-link.router-link-active {
  background: #ebf4ff;
  color: #277dde;
  transform: translateY(-1px);
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
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid #e5eaf0;
  padding: 12px 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h1 {
  margin: 0;
  font-size: 24px;
  color: #1e2a3a;
}

.user-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.welcome {
  color: #5a6b7f;
  font-size: 14px;
}

.role-badge {
  font-size: 12px;
  color: #24548c;
  background: #e7f1ff;
  border: 1px solid #b9d8ff;
  border-radius: 999px;
  padding: 2px 8px;
}

.text-btn {
  border: 1px solid #deebfa;
  background: #f5f8fd;
  color: #526172;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s ease;
}

.text-btn:hover {
  border-color: #c2dcfb;
  color: #2f83e4;
}

.text-btn.primary {
  color: #fff;
  background: linear-gradient(135deg, #3798f4, #2a7de7);
  border-color: transparent;
}

.content {
  padding: 18px 24px 24px;
}
</style>
