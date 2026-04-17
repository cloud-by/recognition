<script setup>
import { computed, ref, watch } from 'vue'
import { addProblemTag, getProblemTags, removeProblemTag } from '@/utils/problemTags'

const props = defineProps({
  open: { type: Boolean, default: false },
})

const emit = defineEmits(['close', 'updated'])

const queryInput = ref('')
const keyword = ref('')
const newTag = ref('')
const tags = ref([])
const loading = ref(false)
const feedback = ref('')

const displayedTags = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return tags.value
  return tags.value.filter((item) => item.name.toLowerCase().includes(text))
})

async function refresh() {
  loading.value = true
  feedback.value = ''
  try {
    tags.value = await getProblemTags()
  } catch (err) {
    feedback.value = err.message || '加载标签失败'
  } finally {
    loading.value = false
  }
}

function search() {
  keyword.value = queryInput.value.trim()
}

async function handleRefresh() {
  keyword.value = ''
  queryInput.value = ''
  await refresh()
}

async function createTag() {
  const value = newTag.value.trim()
  if (!value) return
  try {
    loading.value = true
    feedback.value = ''
    await addProblemTag(value)
    newTag.value = ''
    await refresh()
    emit('updated', tags.value)
  } catch (err) {
    feedback.value = err.message || '添加标签失败'
  } finally {
    loading.value = false
  }
}

async function deleteTag(tag) {
  try {
    loading.value = true
    feedback.value = ''
    tags.value = await removeProblemTag(tag.id)
    emit('updated', tags.value)
  } catch (err) {
    feedback.value = err.message || '删除标签失败'
  } finally {
    loading.value = false
  }
}

watch(
  () => props.open,
  (value) => {
    if (!value) return
    queryInput.value = ''
    keyword.value = ''
    newTag.value = ''
    refresh()
  },
)
</script>

<template>
  <div v-if="open" class="overlay" @click.self="emit('close')">
    <section class="dialog">
      <header>
        <h3>管理标签</h3>
        <button type="button" class="text-btn" @click="emit('close')">关闭</button>
      </header>

      <form class="search-bar" @submit.prevent="search">
        <input v-model="queryInput" type="text" placeholder="请输入内容" />
        <button type="submit" class="primary" :disabled="loading">查询</button>
        <button type="button" class="ghost" :disabled="loading" @click="handleRefresh">刷新</button>
      </form>

      <form class="add-bar" @submit.prevent="createTag">
        <input v-model="newTag" type="text" placeholder="请输入内容" />
        <button type="submit" class="primary" :disabled="loading">添加</button>
      </form>

      <div class="tag-list">
        <p v-if="loading" class="empty">加载中...</p>
        <p v-else-if="!displayedTags.length" class="empty">暂无标签</p>
        <div v-else class="items">
          <div v-for="tag in displayedTags" :key="tag.id" class="tag-item">
            <span>{{ tag.name }}</span>
            <button type="button" class="danger" :disabled="loading" @click="deleteTag(tag)">删除</button>
          </div>
        </div>
        <p v-if="feedback" class="feedback">{{ feedback }}</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(30, 42, 60, 0.35);
  display: grid;
  place-items: center;
  z-index: 99;
}

.dialog {
  width: min(640px, 92vw);
  background: linear-gradient(180deg, #fff, #f9fbff);
  border-radius: 16px;
  border: 1px solid #dbe6f3;
  padding: 18px;
  display: grid;
  gap: 12px;
  box-shadow: 0 18px 40px rgba(23, 49, 79, 0.16);
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

h3 {
  margin: 0;
  color: #22374d;
}

.search-bar,
.add-bar {
  display: flex;
  gap: 8px;
}

input {
  flex: 1;
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 9px 10px;
}

button {
  border: 0;
  border-radius: 8px;
  padding: 8px 12px;
  cursor: pointer;
}

.primary {
  background: #2d8ff2;
  color: #fff;
}

.ghost,
.text-btn {
  background: #eef4fd;
  color: #48637f;
}

.danger {
  background: #ffe7e7;
  color: #cc3434;
  padding: 4px 8px;
  font-size: 12px;
}

.items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  border: 1px solid #deebf8;
  border-radius: 999px;
  padding: 5px 5px 5px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2a4360;
}

.empty {
  margin: 0;
  color: #7c8ea2;
}

.feedback {
  margin: 2px 0 0;
  color: #c64545;
}
</style>
