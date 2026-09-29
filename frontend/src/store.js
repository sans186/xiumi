import { reactive } from 'vue'
import { api, token } from './api'

export const store = reactive({
  user: null,
  permissions: {},
  summary: {},
  unread: 0,
  loaded: false,
  toasts: []
})

export function toast(message, type = 'info') {
  const id = Date.now() + Math.random()
  store.toasts.push({ id, message, type })
  setTimeout(() => {
    const i = store.toasts.findIndex((t) => t.id === id)
    if (i >= 0) store.toasts.splice(i, 1)
  }, 2600)
}

export async function loadMe() {
  if (!token.get()) {
    store.user = null
    store.loaded = true
    return null
  }
  try {
    const data = await api.me()
    store.user = data.user
    store.permissions = data.permissions || {}
    store.summary = data.summary || {}
    store.unread = data.unread || 0
    store.loaded = true
    return data
  } catch (e) {
    store.user = null
    store.loaded = true
    return null
  }
}

export function resetStore() {
  store.user = null
  store.permissions = {}
  store.summary = {}
  store.unread = 0
}

/** 分数类型中文 */
export const SCORE_TYPE = {
  CLAIM: '接取任务',
  CLAIM_BACK: '逾期扣回接取分',
  COMPLETE: '按时完成',
  REVIEW: '审核通过',
  MANUAL: '管理员调整'
}

export const STATUS_META = {
  OPEN: { text: '待接取', cls: 'bg-slate-100 text-slate-600' },
  IN_PROGRESS: { text: '进行中', cls: 'bg-teal-100 text-teal-700' },
  PENDING_REVIEW: { text: '待审核', cls: 'bg-amber-100 text-amber-700' },
  COMPLETED: { text: '已完成', cls: 'bg-emerald-100 text-emerald-700' },
  CANCELLED: { text: '已作废', cls: 'bg-slate-100 text-slate-500' }
}
