<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAuthUser } from '@/utils/auth'

const router = useRouter()
const user = getAuthUser()

const quickEntries = computed(() => {
  const menus = [
    { title: '题目检索', subtitle: '筛选题目并开始练习', to: '/problems', icon: '🧩', accent: 'blue' },
    { title: '提交记录', subtitle: '查看评测状态和结果', to: '/submissions', icon: '📈', accent: 'green' },
    { title: '比赛大厅', subtitle: '报名并进入比赛', to: '/contests', icon: '🏁', accent: 'orange' },
  ]

  if (['ADMIN', 'TEACHER'].includes(user?.role)) {
    menus.push({ title: '比赛管理', subtitle: '创建比赛和配置题目', to: '/contests/manage', icon: '🛠️', accent: 'purple' })
  }

  if (user?.role === 'TEACHER') {
    menus.push({ title: '班级管理', subtitle: '管理课堂与教学班级', to: '/classes/manage', icon: '🏫', accent: 'teal' })
  }

  if (user?.role === 'ADMIN') {
    menus.push({ title: '管理端', subtitle: '查看系统日志与风险行为', to: '/admin', icon: '⚙️', accent: 'dark' })
  }

  return menus
})

const summaryItems = computed(() => {
  const roleText = user?.role === 'ADMIN' ? '管理员' : user?.role === 'TEACHER' ? '老师' : '学生'
  return [
    { label: '当前角色', value: roleText },
    { label: '可用功能', value: `${quickEntries.value.length} 项` },
    { label: '推荐下一步', value: '打开题目列表' },
  ]
})
</script>

<template>
  <section class="dashboard">
    <div class="summary-grid">
      <article v-for="item in summaryItems" :key="item.label" class="summary-card">
        <p class="label">{{ item.label }}</p>
        <p class="value">{{ item.value }}</p>
      </article>
    </div>

    <section class="quick-grid">
      <article
        v-for="item in quickEntries"
        :key="item.to"
        :class="['quick-card', `accent-${item.accent}`]"
      >
        <div class="card-head">
          <span class="icon">{{ item.icon }}</span>
          <div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.subtitle }}</p>
          </div>
        </div>
        <button @click="router.push(item.to)">进入</button>
      </article>
    </section>
  </section>
</template>

<style scoped>
.dashboard {
  display: grid;
  gap: 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.summary-card {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(245, 250, 255, 0.95));
  border: 1px solid #dde9f7;
  border-radius: 14px;
  padding: 14px 16px;
}

.label {
  margin: 0;
  color: #6f8094;
  font-size: 12px;
}

.value {
  margin: 6px 0 0;
  font-size: 20px;
  color: #23374d;
  font-weight: 700;
}

.quick-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
}

.quick-card {
  background: #fff;
  border: 1px solid #e2ebf5;
  border-radius: 14px;
  padding: 16px;
  display: grid;
  gap: 14px;
  box-shadow: 0 12px 25px rgba(45, 86, 133, 0.06);
}

.card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: #eef4fd;
}

h3 {
  margin: 0;
  font-size: 18px;
  color: #1f3145;
}

.quick-card p {
  margin: 6px 0 0;
  color: #718398;
  font-size: 13px;
}

button {
  width: fit-content;
  border: 0;
  color: #fff;
  background: linear-gradient(135deg, #389af4, #2e80e8);
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

.accent-green .icon { background: #ebf9ef; }
.accent-orange .icon { background: #fff2de; }
.accent-purple .icon { background: #f0edff; }
.accent-teal .icon { background: #e6fbfa; }
.accent-dark .icon { background: #e9ecf2; }
</style>
