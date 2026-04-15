<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchContestDetail, fetchContestList, registerContest } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const user = getAuthUser()
const contests = ref([])
const loading = ref(false)
const error = ref('')
const notice = ref('')
const detailDialog = ref(null)
const detailLoading = ref(false)
const detailError = ref('')
const activeContest = ref(null)

const statusText = {
  NOT_STARTED: '未开始',
  RUNNING: '进行中',
  FINISHED: '已结束',
}

const sortedContests = computed(() => contests.value)

async function loadContests() {
  loading.value = true
  error.value = ''
  try {
    const resp = await fetchContestList(user?.id)
    contests.value = resp.data || []
  } catch (err) {
    error.value = err.message || '加载比赛失败'
  } finally {
    loading.value = false
  }
}

async function doRegister(item) {
  notice.value = ''
  error.value = ''
  try {
    const resp = await registerContest(item.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '报名失败'
      return
    }
    notice.value = `已报名：${item.title}`
    await loadContests()
  } catch (err) {
    error.value = err.message || '报名失败'
  }
}

async function openDetail(item) {
  detailLoading.value = true
  detailError.value = ''
  activeContest.value = null
  detailDialog.value?.showModal()
  try {
    const resp = await fetchContestDetail(item.id)
    if (!resp.success) {
      detailError.value = resp.message || '加载详情失败'
      return
    }
    activeContest.value = resp.data
  } catch (err) {
    detailError.value = err.message || '加载详情失败'
  } finally {
    detailLoading.value = false
  }
}

onMounted(loadContests)
</script>

<template>
  <section class="panel">
    <header>
      <h2>比赛大厅</h2>
      <p>当前用户和管理员都可以查看比赛信息；未开始比赛可报名。</p>
    </header>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading">正在加载比赛...</p>

    <div v-else class="list">
      <article v-for="item in sortedContests" :key="item.id" class="item">
        <div>
          <h3>{{ item.title }}</h3>
          <p>比赛时间：{{ item.startTime }} ~ {{ item.endTime }}</p>
          <p>模式：{{ item.contestType }} ｜题目数：{{ item.problemCount }} ｜状态：{{ statusText[item.status] }}</p>
        </div>
        <div class="actions">
          <button @click="openDetail(item)">查看题目</button>
          <button
            v-if="item.status === 'NOT_STARTED' && !item.joined"
            class="primary"
            @click="doRegister(item)"
          >
            报名比赛
          </button>
          <span v-else-if="item.joined" class="joined">已报名</span>
        </div>
      </article>
      <p v-if="!sortedContests.length" class="empty">暂无比赛。</p>
    </div>
  </section>

  <dialog ref="detailDialog" class="dialog">
    <h3>比赛详情</h3>
    <p v-if="detailLoading">加载中...</p>
    <p v-else-if="detailError" class="error">{{ detailError }}</p>
    <template v-else-if="activeContest">
      <p>{{ activeContest.title }}（{{ statusText[activeContest.status] }}）</p>
      <p>已报名人数：{{ activeContest.participantCount }}</p>
      <ul>
        <li v-for="problem in activeContest.problems" :key="problem.problemId">
          #{{ problem.sortOrder }} {{ problem.title }}（{{ problem.difficulty }}）
        </li>
      </ul>
    </template>
    <form method="dialog"><button>关闭</button></form>
  </dialog>
</template>

<style scoped>
.panel { background: #fff; border: 1px solid #e6ecf3; border-radius: 12px; padding: 16px; }
header h2 { margin: 0; }
header p { color: #6d7b89; margin: 8px 0 12px; }
.list { display: grid; gap: 10px; }
.item { border: 1px solid #e6ecf3; border-radius: 10px; padding: 12px; display: flex; justify-content: space-between; gap: 12px; }
.actions { display: flex; align-items: center; gap: 8px; }
button { border: 1px solid #c8d8ea; background: #f4f8ff; padding: 8px 12px; border-radius: 8px; cursor: pointer; }
button.primary { background: #2d8fee; color: #fff; border-color: transparent; }
.error { color: #d93025; }
.notice { color: #1f7a3e; }
.joined { color: #1f7a3e; }
.empty { color: #738293; }
.dialog { border: 1px solid #c8d8ea; border-radius: 10px; min-width: 420px; }
</style>
