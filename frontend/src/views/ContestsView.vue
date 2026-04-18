<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { enterContest, fetchContestDetail, fetchContestList, registerContest } from '@/api/contests'
import { getAuthUser, isManagerUser } from '@/utils/auth'

const router = useRouter()
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
const contestStats = computed(() => ({
  all: contests.value.length,
  waiting: contests.value.filter((item) => item.status === 'NOT_STARTED').length,
  running: contests.value.filter((item) => item.status === 'RUNNING').length,
  joined: contests.value.filter((item) => item.joined).length,
}))
const isManager = computed(() => isManagerUser())

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

async function doEnter(item) {
  notice.value = ''
  error.value = ''
  try {
    const resp = await enterContest(item.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '进入比赛失败'
      return
    }
    notice.value = `进入成功（IP：${resp.data.ip || '未知'}）`
    await openDetail(item)
  } catch (err) {
    error.value = err.message || '进入比赛失败'
  }
}

function goCreate() {
  router.push('/contests/new')
}

function goEdit(item) {
  router.push(`/contests/${item.id}/edit`)
}

onMounted(loadContests)
</script>

<template>
  <section class="page">
    <section class="stats">
      <span>总比赛 {{ contestStats.all }}</span>
      <span>待开始 {{ contestStats.waiting }}</span>
      <span>进行中 {{ contestStats.running }}</span>
      <span>已报名 {{ contestStats.joined }}</span>
    </section>

    <section class="panel">
      <div class="head-row">
        <h3>比赛列表</h3>
        <button v-if="isManager" class="primary" @click="goCreate">创建比赛</button>
      </div>
      <p v-if="notice" class="notice">{{ notice }}</p>
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="loading" class="state">正在加载比赛...</p>

      <div v-else class="list">
        <article v-for="item in sortedContests" :key="item.id" class="item">
          <div>
            <h3>{{ item.title }}</h3>
            <p>比赛时间：{{ item.startTime }} ~ {{ item.endTime }}</p>
            <p>排名：{{ item.rankingPolicy === 'FORMAL' ? '正式比赛' : '课堂模式' }} ｜题目数：{{ item.problemCount }} ｜状态：{{ statusText[item.status] }}</p>
            <p v-if="item.contestContent" class="desc">简介：{{ item.contestContent }}</p>
          </div>
          <div class="actions">
            <button
              v-if="isManager && item.status === 'NOT_STARTED'"
              class="ghost"
              @click="goEdit(item)"
            >
              编辑比赛
            </button>
            <button @click="openDetail(item)">查看题目</button>
            <button v-if="item.joined" class="primary" @click="doEnter(item)">进入比赛</button>
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
  </section>

  <dialog ref="detailDialog" class="dialog">
    <h3>比赛详情</h3>
    <p v-if="detailLoading">加载中...</p>
    <p v-else-if="detailError" class="error">{{ detailError }}</p>
    <template v-else-if="activeContest">
      <p>{{ activeContest.title }}（{{ statusText[activeContest.status] }}）</p>
      <p v-if="activeContest.contestContent">比赛介绍：{{ activeContest.contestContent }}</p>
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
.page {
  display: grid;
  gap: 14px;
}

.stats {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.stats span {
  padding: 7px 12px;
  border-radius: 999px;
  background: #eef5ff;
  color: #4c6683;
  font-size: 13px;
}

.panel {
  background: #fff;
  border: 1px solid #e6ecf3;
  border-radius: 12px;
  padding: 16px;
}

.head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.list {
  display: grid;
  gap: 10px;
}

.item {
  border: 1px solid #e6ecf3;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  background: #fbfdff;
}

.item h3 {
  margin: 0;
}

.item p {
  margin: 6px 0 0;
  color: #617387;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

button {
  border: 1px solid #c8d8ea;
  background: #f4f8ff;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

button.primary {
  background: #2d8fee;
  color: #fff;
  border-color: transparent;
}

button.ghost {
  background: #eef4ff;
  color: #426082;
}

.error { color: #d93025; }
.notice { color: #1f7a3e; }
.joined { color: #1f7a3e; }
.empty { color: #738293; }
.state { margin: 0; color: #738293; }
.desc { max-width: 760px; }
.dialog { border: 1px solid #c8d8ea; border-radius: 10px; min-width: 420px; }
</style>
