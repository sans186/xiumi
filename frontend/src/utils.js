/** 时间工具：后端返回 'yyyy-MM-dd HH:mm:ss' 文本 */

export function fmtShort(s) {
  if (!s) return ''
  return s.slice(5, 16) // MM-DD HH:mm
}

export function fmtFull(s) {
  return s || ''
}

function diffMs(s) {
  if (!s) return null
  const t = new Date(s.replace(' ', 'T')).getTime()
  if (Number.isNaN(t)) return null
  return t - Date.now()
}

export function isExpired(s) {
  const d = diffMs(s)
  return d !== null && d <= 0
}

/** 剩余时间文案 */
export function remainText(s) {
  const d = diffMs(s)
  if (d === null) return ''
  if (d <= 0) return '已超时'
  const min = Math.floor(d / 60000)
  if (min < 60) return '剩 ' + min + ' 分钟'
  const hour = Math.floor(min / 60)
  if (hour < 24) return '剩 ' + hour + ' 小时 ' + (min % 60) + ' 分'
  const day = Math.floor(hour / 24)
  return '剩 ' + day + ' 天 ' + (hour % 24) + ' 小时'
}

/** 文件大小文案 */
export function fmtSize(bytes) {
  if (bytes === null || bytes === undefined || bytes === '') return ''
  const n = Number(bytes)
  if (Number.isNaN(n)) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB'
  return (n / 1024 / 1024).toFixed(1) + ' MB'
}

/** 紧急程度样式 */
export function urgencyClass(s) {
  const d = diffMs(s)
  if (d === null) return 'text-slate-400'
  if (d <= 0) return 'text-rose-600 font-semibold'
  if (d < 2 * 3600 * 1000) return 'text-rose-500'
  if (d < 24 * 3600 * 1000) return 'text-amber-600'
  return 'text-slate-500'
}
