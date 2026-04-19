<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { enterContest, fetchContestDetail, registerContest } from '@/api/contests'
import { getAuthUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const user = getAuthUser()

const loading = ref(false)
const error = ref('')
const notice = ref('')
const contest = ref(null)

const statusText = {
  NOT_STARTED: '未开始',
  RUNNING: '进行中',
  FINISHED: '已结束',
}

const canRegister = computed(() => contest.value?.status === 'NOT_STARTED' && !contest.value?.joined)
const canEnter = computed(() => Boolean(contest.value?.canEnter))

async function loadDetail() {
  loading.value = true
  error.value = ''
  try {
    const resp = await fetchContestDetail(route.params.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '加载比赛详情失败'
      return
    }
    contest.value = resp.data
  } catch (err) {
    error.value = err.message || '加载比赛详情失败'
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!contest.value) return
  error.value = ''
  notice.value = ''
  try {
    const resp = await registerContest(contest.value.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '报名失败'
      return
    }
    notice.value = '报名成功'
    await loadDetail()
  } catch (err) {
    error.value = err.message || '报名失败'
  }
}

async function doEnter() {
  if (!contest.value) return
  error.value = ''
  notice.value = ''
  try {
    const resp = await enterContest(contest.value.id, user?.id)
    if (!resp.success) {
      error.value = resp.message || '进入比赛失败'
      return
    }
    notice.value = `进入成功（IP：${resp.data.ip || '未知'}）`
    router.push(`/contests/${contest.value.id}/arena`)
  } catch (err) {
    error.value = err.message || '进入比赛失败'
  }
}

onMounted(loadDetail)
</script>

<template>
  <section class="detail-page">
    <div class="hero">
      <h2>{{ contest?.title || '比赛详情' }}</h2>
      <p class="subtitle">正式比赛与课堂训练统一展示页。赛前仅展示基础信息，避免题面泄露。</p>
    </div>

    <section class="card" v-if="loading">加载中...</section>
    <section class="card" v-else-if="error">{{ error }}</section>

    <template v-else-if="contest">
      <section class="card info-grid">
        <p><strong>状态：</strong>{{ statusText[contest.status] }}</p>
        <p><strong>比赛时间：</strong>{{ contest.startTime }} ~ {{ contest.endTime }}</p>
        <p><strong>最早可进入：</strong>{{ contest.enterOpenTime }}</p>
        <p><strong>已报名人数：</strong>{{ contest.participantCount }}</p>
      </section>

      <section class="card intro">
        <h3>比赛介绍</h3>
        <p>{{ contest.contestContent || '暂无介绍' }}</p>
      </section>

      <section class="card" v-if="contest.problemVisible">
        <h3>题目列表</h3>
        <ul>
          <li v-for="problem in contest.problems" :key="problem.problemId">
            #{{ problem.sortOrder }} {{ problem.title }}（{{ problem.difficulty }}）
          </li>
        </ul>
      </section>

      <section class="actions-bottom">
        <p v-if="notice" class="ok">{{ notice }}</p>
        <p v-if="error" class="error">{{ error }}</p>
        <button v-if="canRegister" class="primary" @click="doRegister">立即报名</button>
        <button v-else-if="contest.joined" class="primary" :disabled="!canEnter" @click="doEnter">
          {{ canEnter ? '进入比赛' : '未到可进入时间' }}
        </button>
        <span v-else class="muted">比赛已开始或结束，无法报名</span>
      </section>
    </template>
  </section>
</template>

<style scoped>
.detail-page { display: grid; gap: 14px; max-width: 980px; margin: 0 auto; }
.hero { background: linear-gradient(120deg, #1f6fd6, #56a7ff); color: #fff; padding: 24px; border-radius: 14px; box-shadow: 0 12px 30px rgba(28, 103, 199, 0.25); }
.hero h2 { margin: 0; }
.subtitle { margin-top: 10px; color: rgba(255, 255, 255, 0.88); }
.card { background: #fff; border: 1px solid #e5edf7; border-radius: 12px; padding: 16px; }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.intro p { color: #55657a; }
.actions-bottom { text-align: center; padding: 20px 12px 6px; }
button { border: 0; border-radius: 999px; padding: 11px 34px; background: #2d8fee; color: #fff; cursor: pointer; }
button:disabled { background: #c2cfdf; cursor: not-allowed; }
.ok { color: #1f7a3e; }
.error { color: #d93025; }
.muted { color: #748498; }
</style>
