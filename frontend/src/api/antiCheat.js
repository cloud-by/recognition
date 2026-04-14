import { apiPost } from '@/api/http'

const USER_ID = 1
const THROTTLE_MS = 10_000
const lastReportAt = new Map()

function shouldReport(eventType) {
  const now = Date.now()
  const previous = lastReportAt.get(eventType) || 0
  if (now - previous < THROTTLE_MS) {
    return false
  }
  lastReportAt.set(eventType, now)
  return true
}

async function report(eventType, detailInfo) {
  if (!shouldReport(eventType)) {
    return
  }

  try {
    await apiPost('/anti-cheat/logs', {
      userId: USER_ID,
      behaviorType: eventType,
      detailInfo,
      occurredTime: new Date().toISOString(),
    })
  } catch {
    // 防作弊日志不上抛错误，避免影响用户做题主流程
  }
}

export function startAntiCheatTracking(getCurrentRoutePath) {
  const onBlur = () => report('WINDOW_BLUR', `route=${getCurrentRoutePath()}`)
  const onFocus = () => report('WINDOW_FOCUS', `route=${getCurrentRoutePath()}`)
  const onVisibilityChange = () => {
    const state = document.hidden ? 'hidden' : 'visible'
    report('TAB_VISIBILITY_CHANGE', `state=${state};route=${getCurrentRoutePath()}`)
  }

  window.addEventListener('blur', onBlur)
  window.addEventListener('focus', onFocus)
  document.addEventListener('visibilitychange', onVisibilityChange)

  return () => {
    window.removeEventListener('blur', onBlur)
    window.removeEventListener('focus', onFocus)
    document.removeEventListener('visibilitychange', onVisibilityChange)
  }
}
