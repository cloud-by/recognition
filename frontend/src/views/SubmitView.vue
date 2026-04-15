<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPost } from '@/api/http'
import { getAuthUser } from '@/utils/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref('')
const problem = ref(null)
const user = ref(null)

const form = reactive({
  language: 'cpp',
  sourceCode: '',
})

const problemCode = computed(() => `P${String(route.params.id).padStart(4, '0')}`)

async function loadProblem() {
  loading.value = true
  error.value = ''
  try {
    const q = user.value?.id ? `?viewerUserId=${user.value.id}` : ''
    const resp = await apiGet(`/problems/${route.params.id}${q}`)
    problem.value = resp.data
    if (!problem.value) {
      error.value = resp.message || '题目不存在'
    }
  } catch (err) {
    error.value = err.message || '题目加载失败'
  } finally {
    loading.value = false
  }
}

async function submitCode() {
  if (!user.value?.id) {
    error.value = '请先登录后再提交代码'
    return
  }

  if (!form.sourceCode.trim()) {
    error.value = '代码不能为空'
    return
  }

  submitting.value = true
  error.value = ''
  success.value = ''

  try {
    const resp = await apiPost('/submissions', {
      userId: user.value.id,
      problemId: Number(route.params.id),
      language: form.language,
      sourceCode: form.sourceCode,
    })

    if (!resp.success) {
      error.value = resp.message || '提交失败'
      return
    }

    success.value = `提交成功，记录已入库（submissionId: ${resp.data?.submissionId || '-' }），后续将接入 Judge0。`
  } catch (err) {
    error.value = err.message || '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  user.value = getAuthUser()
  if (!user.value?.id) {
    router.push('/login')
    return
  }
  loadProblem()
})
</script>

<template>
  <section class="submit-wrap">
    <button class="back" @click="router.push(`/problems/${route.params.id}`)">← 返回题目</button>

    <article class="panel">
      <header class="head">
        <div>
          <p class="pid">{{ problemCode }}</p>
          <h2>代码提交</h2>
          <p class="desc">{{ problem?.title || '正在加载题目信息...' }}</p>
        </div>
      </header>

      <p v-if="loading" class="state">题目信息加载中...</p>
      <p v-else-if="error" class="state error">{{ error }}</p>

      <form v-else class="form" @submit.prevent="submitCode">
        <label>
          语言
          <select v-model="form.language">
            <option value="c">C</option>
            <option value="cpp">C++</option>
          </select>
        </label>

        <label>
          代码
          <textarea
            v-model="form.sourceCode"
            placeholder="请输入或粘贴你的代码..."
            rows="18"
            required
          />
        </label>

        <button type="submit" class="submit" :disabled="submitting">
          {{ submitting ? '提交中...' : '提交到评测队列' }}
        </button>
      </form>

      <p v-if="success" class="state success">{{ success }}</p>
    </article>
  </section>
</template>

<style scoped>
.submit-wrap { display: grid; gap: 14px; }

.back {
  width: fit-content;
  border: 0;
  color: #2c8cec;
  background: #edf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.panel {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 14px;
  padding: 22px;
}

.pid { margin: 0; color: #7a8898; }
h2 { margin: 8px 0; }
.desc { margin: 0; color: #5f7082; }

.form {
  display: grid;
  gap: 14px;
  margin-top: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #4d6074;
}

select,
textarea {
  border: 1px solid #d5deea;
  border-radius: 10px;
  padding: 10px 12px;
}

textarea {
  font-family: 'Fira Code', 'JetBrains Mono', monospace;
}

.submit {
  width: fit-content;
  border: 0;
  border-radius: 10px;
  padding: 10px 16px;
  color: #fff;
  background: linear-gradient(135deg, #36a3f7, #2c80eb);
  cursor: pointer;
}

.submit:disabled { opacity: 0.7; cursor: not-allowed; }

.state { margin-top: 10px; color: #72869a; }
.error { color: #d93025; }
.success { color: #1e8b49; }
</style>
