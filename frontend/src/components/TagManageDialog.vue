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

const displayedTags = computed(() => {
  const text = keyword.value.trim().toLowerCase()
  if (!text) return tags.value
  return tags.value.filter((item) => item.toLowerCase().includes(text))
})

function refresh() {
  tags.value = getProblemTags()
}

function search() {
  keyword.value = queryInput.value.trim()
}

function handleRefresh() {
  keyword.value = ''
  refresh()
}

function createTag() {
  const value = newTag.value.trim()
  if (!value) return
  tags.value = addProblemTag(value)
  newTag.value = ''
  emit('updated', tags.value)
}

function deleteTag(tag) {
  tags.value = removeProblemTag(tag)
  emit('updated', tags.value)
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
        <button type="submit" class="primary">查询</button>
        <button type="button" class="ghost" @click="handleRefresh">刷新</button>
      </form>

      <form class="add-bar" @submit.prevent="createTag">
        <input v-model="newTag" type="text" placeholder="请输入内容" />
        <button type="submit" class="primary">添加</button>
      </form>

      <div class="tag-list">
        <p v-if="!displayedTags.length" class="empty">暂无标签</p>
        <div v-else class="items">
          <div v-for="tag in displayedTags" :key="tag" class="tag-item">
            <span>{{ tag }}</span>
            <button type="button" class="danger" @click="deleteTag(tag)">删除</button>
          </div>
        </div>
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
  background: #fff;
  border-radius: 14px;
  border: 1px solid #dbe6f3;
  padding: 16px;
  display: grid;
  gap: 12px;
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
}

.items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  border: 1px solid #deebf8;
  border-radius: 999px;
  padding: 5px 6px 5px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2a4360;
}

.empty {
  margin: 0;
  color: #7c8ea2;
}
</style>
