<script setup>
import { ref } from 'vue'
import TagManageDialog from '@/components/TagManageDialog.vue'

const open = ref(true)
</script>

<template>
  <section class="panel">
    <h2>标签管理</h2>
    <p>可新增、删除标签。</p>
    <button @click="open = true">打开标签管理</button>
    <TagManageDialog :open="open" @close="open = false" />
  </section>
</template>

<style scoped>
.panel {
  background: #fff;
  border: 1px solid #e2ebf6;
  border-radius: 14px;
  padding: 16px;
  display: grid;
  gap: 10px;
}

h2,
p {
  margin: 0;
}

button {
  width: fit-content;
  border: 0;
  border-radius: 8px;
  padding: 8px 14px;
  background: #2d8ff2;
  color: #fff;
  cursor: pointer;
}
</style>
