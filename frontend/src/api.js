const TOKEN_KEY = 'xmt_token'

export const token = {
  get: () => localStorage.getItem(TOKEN_KEY) || '',
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY)
}

async function request(path, { method = 'GET', body } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  const t = token.get()
  if (t) headers['satoken'] = t

  let res
  try {
    res = await fetch('/api' + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  } catch (e) {
    throw new Error('网络异常，请检查网络后重试')
  }

  const json = await res.json().catch(() => ({ code: -1, message: '返回数据异常' }))

  if (json.code === 1003) {                       // 未登录 / 登录失效
    token.clear()
    window.dispatchEvent(new CustomEvent('xmt:logout'))
    throw new Error('登录已失效，请重新登录')
  }
  if (json.code === 1001) {                       // 必须先改密码
    window.dispatchEvent(new CustomEvent('xmt:force-change'))
    throw new Error(json.message || '请先修改初始密码')
  }
  if (json.code !== 0) {
    throw new Error(json.message || '请求失败')
  }
  return json.data
}

/** 上传：不带 Content-Type，交给浏览器带 boundary */
async function upload(path, formData) {
  const headers = {}
  const t = token.get()
  if (t) headers['satoken'] = t

  let res
  try {
    res = await fetch('/api' + path, { method: 'POST', headers, body: formData })
  } catch (e) {
    throw new Error('网络异常，请检查网络后重试')
  }
  const json = await res.json().catch(() => ({ code: -1, message: '返回数据异常' }))
  if (json.code === 1003) {
    token.clear()
    window.dispatchEvent(new CustomEvent('xmt:logout'))
    throw new Error('登录已失效，请重新登录')
  }
  if (json.code === 1001) {
    window.dispatchEvent(new CustomEvent('xmt:force-change'))
    throw new Error(json.message || '请先修改初始密码')
  }
  if (json.code !== 0) {
    throw new Error(json.message || '上传失败')
  }
  return json.data
}

/** 下载二进制：需要带 token 请求头，不能直接用 <a href> */
async function requestBlob(path) {
  const headers = {}
  const t = token.get()
  if (t) headers['satoken'] = t

  let res
  try {
    res = await fetch('/api' + path, { headers })
  } catch (e) {
    throw new Error('网络异常，请检查网络后重试')
  }
  const ct = res.headers.get('content-type') || ''
  if (ct.includes('application/json')) {
    const json = await res.json().catch(() => ({ code: -1, message: '下载失败' }))
    if (json.code === 1003) {
      token.clear()
      window.dispatchEvent(new CustomEvent('xmt:logout'))
    }
    throw new Error(json.message || '下载失败')
  }
  return res.blob()
}

/** 附件直链：交给浏览器原生下载（token 放 query，后端 sa-token 已开启 is-read-body） */
export function attachmentUrl(taskId) {
  const t = token.get()
  return '/api/tasks/' + taskId + '/attach' + (t ? '?satoken=' + encodeURIComponent(t) : '')
}

/** 触发一次浏览器下载（Blob 方式，作为直链不可用时的兜底） */
export async function saveAttachment(taskId, filename) {
  const blob = await requestBlob(`/tasks/${taskId}/attach`)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || 'attachment'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  // 不能立刻 revoke，否则部分浏览器会把下载中断掉
  setTimeout(() => URL.revokeObjectURL(url), 60000)
  return blob.size
}

export const api = {
  /* 认证 */
  login: (username, password) => request('/auth/login', { method: 'POST', body: { username, password } }),
  logout: () => request('/auth/logout', { method: 'POST' }),
  changePassword: (oldPassword, newPassword) =>
    request('/auth/change-password', { method: 'POST', body: { oldPassword, newPassword } }),

  /* 我的 */
  me: () => request('/me'),
  myScores: () => request('/me/scores'),
  updateEmail: (email) => request('/me/email', { method: 'PUT', body: { email } }),

  /* 任务 */
  tasks: (view = 'all') => request('/tasks?view=' + view),
  taskStats: () => request('/tasks/stats'),
  taskDetail: (id) => request('/tasks/' + id),
  createTask: (body) => request('/tasks', { method: 'POST', body }),
  claimTask: (id) => request(`/tasks/${id}/claim`, { method: 'POST', body: {} }),
  submitTask: (id, previewUrl, note) =>
    request(`/tasks/${id}/submit`, { method: 'POST', body: { previewUrl, note } }),
  reviewTask: (id, approve, comment, giveScore = true) =>
    request(`/tasks/${id}/review`, { method: 'POST', body: { approve, comment, giveScore } }),
  assignTask: (id, userId) => request(`/tasks/${id}/assign`, { method: 'POST', body: { userId } }),
  cancelTask: (id) => request(`/tasks/${id}/cancel`, { method: 'POST', body: {} }),
  uploadTaskAttachment: (id, file) => {
    const fd = new FormData()
    fd.append('file', file)
    return upload(`/tasks/${id}/attach`, fd)
  },
  /** 兜底下载：Blob 方式 */
  downloadTaskAttachment: (id, filename) => saveAttachment(id, filename),

  /* 排行 */
  rank: (period = 'all', groupId) =>
    request('/rank?period=' + period + (groupId ? '&groupId=' + groupId : '')),
  groupRank: () => request('/rank/groups'),

  /* 成员 / 小组 */
  users: () => request('/users'),
  usersSimple: () => request('/users/simple'),
  createUser: (body) => request('/users', { method: 'POST', body }),
  updateUser: (id, body) => request('/users/' + id, { method: 'PUT', body }),
  resetPassword: (id, password) =>
    request(`/users/${id}/reset-password`, { method: 'POST', body: password ? { password } : {} }),
  adjustScore: (id, score, remark) =>
    request(`/users/${id}/adjust-score`, { method: 'POST', body: { score, remark } }),

  groups: () => request('/groups'),
  createGroup: (body) => request('/groups', { method: 'POST', body }),
  updateGroup: (id, body) => request('/groups/' + id, { method: 'PUT', body }),
  deleteGroup: (id) => request('/groups/' + id, { method: 'DELETE' }),

  /* 消息 */
  notifications: (unreadOnly = false) => request('/notifications?unreadOnly=' + unreadOnly + '&limit=50'),
  unreadCount: () => request('/notifications/unread-count'),
  readOne: (id) => request(`/notifications/${id}/read`, { method: 'PUT' }),
  readAll: () => request('/notifications/read-all', { method: 'PUT' }),

  /* 设置 */
  settings: () => request('/settings'),
  saveSettings: (body) => request('/settings', { method: 'PUT', body })
}
