<script setup>
import { computed, onMounted, reactive, ref, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiGet, apiPost } from '@/api/http'
import { getAuthUser } from '@/utils/auth'
import * as monaco from 'monaco-editor'

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

const editorContainer = ref(null)
let editor = null

// 语言对应的代码模板
const codeTemplates = {
  c: ``,
  cpp:``,
  java: ``,
  python: ``
}

const languageMap = {
  c: 'c',
  cpp: 'cpp',
  java: 'java',
  python: 'python'
}

const problemCode = computed(() => `P${String(route.params.id).padStart(4, '0')}`)

// 初始化编辑器
async function initEditor() {
  if (!editorContainer.value) return
  
  try {
    // 确保容器有明确的高度和宽度
    editorContainer.value.style.width = '100%'
    editorContainer.value.style.height = '500px'
    
    editor = monaco.editor.create(editorContainer.value, {
      value: form.sourceCode || codeTemplates[form.language],
      language: languageMap[form.language],
      theme: 'vs-dark',
      automaticLayout: true,  // 自动布局，关键配置
      fontSize: 14,
      minimap: { 
        enabled: true,
        scale: 1
      },
      scrollBeyondLastLine: false,
      lineNumbers: 'on',
      roundedSelection: false,
      selectOnLineNumbers: true,
      wordWrap: 'on',
      tabSize: 4,
      insertSpaces: true,
      fontFamily: "'Fira Code', 'JetBrains Mono', 'Consolas', monospace",
      fontLigatures: true,
      cursorBlinking: 'smooth',
      cursorSmoothCaretAnimation: 'on',
      smoothScrolling: true,
      // 确保编辑器可编辑
      readOnly: false,
      domReadOnly: false
    })

    // 监听内容变化
    editor.onDidChangeModelContent(() => {
      form.sourceCode = editor.getValue()
    })
    
    // 聚焦编辑器
    editor.focus()
    
    console.log('Editor initialized successfully')
  } catch (err) {
    console.error('Editor initialization failed:', err)
  }
}

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

// 切换语言
function handleLanguageChange() {
  const newCode = codeTemplates[form.language] || codeTemplates.cpp
  form.sourceCode = newCode
  if (editor) {
    const model = editor.getModel()
    monaco.editor.setModelLanguage(model, languageMap[form.language])
    editor.setValue(newCode)
    editor.focus()  // 切换后聚焦
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

    success.value = `提交成功：#${resp.data?.submissionId || '-'}，状态 ${resp.data?.judgeStatus || '-'}，耗时 ${resp.data?.runtimeMs || 0}ms，内存 ${resp.data?.memoryKb || 0}KB`
    
    setTimeout(() => {
      success.value = ''
    }, 3000)
  } catch (err) {
    error.value = err.message || '提交失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

// 设置默认代码
function setDefaultCode() {
  form.sourceCode = codeTemplates[form.language] || codeTemplates.cpp
}

onMounted(async () => {
  user.value = getAuthUser()
  if (!user.value?.id) {
    router.push('/login')
    return
  }
  await loadProblem()
  setDefaultCode()
  
  // 使用 nextTick 确保 DOM 完全渲染
  await nextTick()
  setTimeout(() => {
    initEditor()
  }, 200)
})

// 组件销毁时清理编辑器实例
onBeforeUnmount(() => {
  if (editor) {
    editor.dispose()
    editor = null
  }
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
        <div class="language-selector">
          <label>选择语言：</label>
          <select v-model="form.language" @change="handleLanguageChange">
            <option value="c">C</option>
            <option value="cpp">C++</option>
            <option value="java">Java</option>
            <option value="python">Python</option>
          </select>
        </div>

        <div class="editor-wrapper">
          <label>代码编辑器：</label>
          <div ref="editorContainer" class="monaco-editor-container"></div>
        </div>

        <button type="submit" class="submit" :disabled="submitting">
          {{ submitting ? '提交并评测中...' : '提交并调用 Judge0 评测' }}
        </button>
      </form>

      <p v-if="success" class="state success">{{ success }}</p>
    </article>
  </section>
</template>

<style scoped>
.submit-wrap { 
  display: grid; 
  gap: 14px; 
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.back {
  width: fit-content;
  border: 0;
  color: #2c8cec;
  background: #edf5ff;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.back:hover {
  background: #e0edfc;
  transform: translateX(-2px);
}

.panel {
  background: #fff;
  border: 1px solid #e7edf4;
  border-radius: 14px;
  padding: 22px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.pid { 
  margin: 0; 
  color: #7a8898;
  font-size: 14px;
}

h2 { 
  margin: 8px 0; 
  font-size: 24px;
  color: #2c3e50;
}

.desc { 
  margin: 0; 
  color: #5f7082;
  font-size: 14px;
}

.form {
  display: grid;
  gap: 20px;
  margin-top: 16px;
}

.language-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.language-selector label {
  font-weight: 500;
  color: #4d6074;
  margin: 0;
}

.language-selector select {
  border: 1px solid #d5deea;
  border-radius: 8px;
  padding: 8px 12px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: border-color 0.3s;
}

.language-selector select:hover {
  border-color: #2c8cec;
}

.language-selector select:focus {
  outline: none;
  border-color: #2c8cec;
  box-shadow: 0 0 0 2px rgba(44, 140, 236, 0.1);
}

.editor-wrapper {
  display: grid;
  gap: 8px;
}

.editor-wrapper label {
  font-weight: 500;
  color: #4d6074;
}

/* 修改这里：确保编辑器容器可以点击 */
.monaco-editor-container {
  width: 100%;
  height: 500px;
  min-height: 500px;
  border: 1px solid #d5deea;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* 确保 Monaco Editor 生成的元素可以接收点击 */
.monaco-editor-container :deep(.monaco-editor) {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

.submit {
  width: fit-content;
  border: 0;
  border-radius: 10px;
  padding: 12px 24px;
  color: #fff;
  background: linear-gradient(135deg, #36a3f7, #2c80eb);
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
  margin-top: 10px;
}

.submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(44, 128, 235, 0.3);
}

.submit:disabled { 
  opacity: 0.7; 
  cursor: not-allowed; 
}

.state { 
  margin-top: 10px; 
  padding: 10px;
  border-radius: 8px;
  color: #72869a;
}

.error { 
  background: #fee;
  color: #d93025; 
  border-left: 3px solid #d93025;
}

.success { 
  background: #e8f5e9;
  color: #1e8b49;
  border-left: 3px solid #1e8b49;
}

@media (max-width: 768px) {
  .submit-wrap {
    padding: 10px;
  }
  
  .panel {
    padding: 15px;
  }
  
  .monaco-editor-container {
    height: 400px;
    min-height: 400px;
  }
  
  .submit {
    width: 100%;
  }
}
</style>