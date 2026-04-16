const TAG_STORAGE_KEY = 'oj_problem_tags'

const defaultTags = ['模拟', '字符串', '数学', '贪心', '图论']

function normalizeTags(tags) {
  return [...new Set((tags || []).map((tag) => String(tag || '').trim()).filter(Boolean))]
}

export function getProblemTags() {
  try {
    const raw = localStorage.getItem(TAG_STORAGE_KEY)
    if (!raw) return [...defaultTags]
    const parsed = JSON.parse(raw)
    const normalized = normalizeTags(parsed)
    return normalized.length ? normalized : [...defaultTags]
  } catch {
    return [...defaultTags]
  }
}

export function saveProblemTags(tags) {
  const normalized = normalizeTags(tags)
  localStorage.setItem(TAG_STORAGE_KEY, JSON.stringify(normalized))
  return normalized
}

export function addProblemTag(tagName) {
  const next = normalizeTags([...getProblemTags(), tagName])
  saveProblemTags(next)
  return next
}

export function removeProblemTag(tagName) {
  const target = String(tagName || '').trim()
  const next = getProblemTags().filter((item) => item !== target)
  saveProblemTags(next)
  return next
}
