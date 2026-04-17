<script setup>
import { computed, onMounted, ref } from 'vue'
import { apiPost } from '@/api/http'
import TagManageDialog from '@/components/TagManageDialog.vue'
import { getAuthUser } from '@/utils/auth'
import { getProblemTags } from '@/utils/problemTags'

const user = getAuthUser()

const difficultyOptions = ['入门', '普及', '提高']
const permissionOptions = [
  { label: '公开', value: 'PUBLIC' },
  { label: '登录可见', value: 'LOGIN_REQUIRED' },
  { label: '比赛可见', value: 'CONTEST_ONLY' },
]

const allTags = ref([])
const openTagModal = ref(false)
const saving = ref(false)
const feedback = ref({ type: '', text: '' })

const form = ref({
  title: '',
  difficulty: '普及',
  timeLimitMs: 1000,
  memoryLimitMb: 256,
  permissionType: 'PUBLIC',
  tags: [],
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  testcasePath: '',
})

const tagText = computed(() => form.value.tags.join(','))

function updateTags(tags) {
  const names = tags.map((item) => item.name)
  allTags.value = names
  form.value.tags = form.value.tags.filter((item) => names.includes(item))
}

function toggleTag(tag) {
  if (form.value.tags.includes(tag)) {
    form.value.tags = form.value.tags.filter((item) => item !== tag)
    return
  }
  form.value.tags = [...form.value.tags, tag]
}

async function submit() {
  feedback.value = { type: '', text: '' }
  saving.value = true
  try {
    const payload = {
      creatorUserId: user?.id,
      title: form.value.title,
      description: form.value.description,
      inputFormat: form.value.inputFormat,
      outputFormat: form.value.outputFormat,
      sampleInput: form.value.sampleInput,
      sampleOutput: form.value.sampleOutput,
      difficulty: form.value.difficulty,
      permissionType: form.value.permissionType,
      tags: tagText.value,
      timeLimitMs: Number(form.value.timeLimitMs),
      memoryLimitMb: Number(form.value.memoryLimitMb),
      testcasePath: form.value.testcasePath,
    }

    const resp = await apiPost('/problems', payload)
    if (!resp?.success) throw new Error(resp?.message || '保存失败')

    feedback.value = { type: 'success', text: `题目创建成功，题号 P${String(resp.data.id).padStart(4, '0')}` }
    form.value = {
      title: '',
      difficulty: '普及',
      timeLimitMs: 1000,
      memoryLimitMb: 256,
      permissionType: 'PUBLIC',
      tags: [],
      description: '',
      inputFormat: '',
      outputFormat: '',
      sampleInput: '',
      sampleOutput: '',
      testcasePath: '',
    }
  } catch (err) {
    feedback.value = { type: 'error', text: err.message || '保存失败' }
  } finally {
    saving.value = false
  }
}

async function loadTags() {
  const tags = await getProblemTags()
  allTags.value = tags.map((item) => item.name)
}

onMounted(loadTags)
</script>

<template>
  <section class="panel">
    <h2>上传题目</h2>
    <form class="form" @submit.prevent="submit">
      <label>题目名称<input v-model="form.title" required placeholder="请输入内容" /></label>

      <div class="line">
        <label>等级
          <select v-model="form.difficulty">
            <option v-for="item in difficultyOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <label>时间限制
          <input v-model="form.timeLimitMs" type="number" min="1" required placeholder="请输入内容" />
        </label>
        <label>内存限制
          <input v-model="form.memoryLimitMb" type="number" min="1" required placeholder="请输入内容" />
        </label>
      </div>

      <label>权限
        <select v-model="form.permissionType">
          <option v-for="item in permissionOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
        </select>
      </label>

      <label>标签</label>
      <div class="tag-tools">
        <button type="button" class="ghost" @click="openTagModal = true">管理标签</button>
        <p>已选：{{ form.tags.length ? form.tags.join('、') : '-' }}</p>
      </div>
      <div class="tag-list">
        <button
          v-for="tag in allTags"
          :key="tag"
          type="button"
          :class="['tag-btn', { active: form.tags.includes(tag) }]"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </button>
      </div>

      <label>题目描述<textarea v-model="form.description" required placeholder="请输入内容" /></label>
      <label>输入格式<textarea v-model="form.inputFormat" placeholder="请输入内容" /></label>
      <label>输出格式<textarea v-model="form.outputFormat" placeholder="请输入内容" /></label>
      <label>样例输入<textarea v-model="form.sampleInput" placeholder="请输入内容" /></label>
      <label>样例输出<textarea v-model="form.sampleOutput" placeholder="请输入内容" /></label>
      <label>测试数据路径<input v-model="form.testcasePath" required placeholder="请输入内容" /></label>

      <button class="primary" type="submit" :disabled="saving">{{ saving ? '提交中...' : '提交题目' }}</button>
      <p v-if="feedback.text" :class="['msg', feedback.type]">{{ feedback.text }}</p>
    </form>

    <TagManageDialog :open="openTagModal" @close="openTagModal = false" @updated="updateTags" />
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e2ebf6;
  border-radius: 14px;
  padding: 16px;
}

h2 {
  margin: 0 0 12px;
}

.form {
  display: grid;
  gap: 12px;
}

.line {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

label {
  display: grid;
  gap: 6px;
  color: #5c6f83;
}

input,
select,
textarea {
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 9px 10px;
}

textarea {
  min-height: 96px;
  resize: vertical;
}

.tag-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-tools p {
  margin: 0;
  color: #5f7489;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-btn {
  border: 1px solid #d7e4f2;
  background: #fff;
  color: #496580;
  border-radius: 999px;
  padding: 5px 12px;
  cursor: pointer;
}

.tag-btn.active {
  background: #ebf4ff;
  border-color: #95c3fa;
  color: #1e6ec4;
}

button {
  width: fit-content;
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  cursor: pointer;
}

.primary {
  background: #2d8ff2;
  color: #fff;
}

.ghost {
  background: #eef4fd;
  color: #48637f;
}

.msg {
  margin: 0;
}

.msg.success {
  color: #1d8d5e;
}

.msg.error {
  color: #cc4141;
}

@media (max-width: 900px) {
  .line {
    grid-template-columns: 1fr;
  }
}
</style>
