<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { UserPlus, KeyRound, Coins, Pencil } from 'lucide-vue-next'

const users = ref([])
const groups = ref([])
const keyword = ref('')
const showCreate = ref(false)
const submitting = ref(false)
const form = ref({ username: '', nickname: '', role: 'EMPLOYEE', email: '', groupId: null, initialPassword: '' })

/* 编辑弹窗：一次改完再保存，避免列表里手滑改错状态 */
const editing = ref(null)
const editForm = ref({ id: null, nickname: '', role: 'EMPLOYEE', groupId: null, email: '', status: 'ACTIVE' })

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return users.value
  return users.value.filter(u =>
    (u.nickname || '').toLowerCase().includes(k) || (u.username || '').toLowerCase().includes(k))
})

const activeAdminCount = computed(() =>
  users.value.filter(u => u.role === 'ADMIN' && u.status === 'ACTIVE').length)

function groupName(id) {
  const g = groups.value.find(x => x.id === id)
  return g ? g.name : '未分组'
}
function isSelf(u) {
  return !!store.user && store.user.id === u.id
}
/** 是否允许停用该成员（自己不行、唯一的在职管理员不行） */
function canDisable(u) {
  return !isSelf(u) && !(u.role === 'ADMIN' && activeAdminCount.value <= 1)
}
function disableReason(u) {
  if (isSelf(u)) return '不能停用自己的账号'
  if (u.role === 'ADMIN' && activeAdminCount.value <= 1) return '系统需保留至少一位在职管理员'
  return ''
}

async function load() {
  try {
    users.value = await api.users()
    groups.value = await api.groups()
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function createUser() {
  submitting.value = true
  try {
    await api.createUser({
      ...form.value,
      username: form.value.username.trim(),
      nickname: form.value.nickname.trim(),
      initialPassword: form.value.initialPassword || undefined
    })
    toast('账号已创建，把初始密码发给本人', 'success')
    showCreate.value = false
    form.value = { username: '', nickname: '', role: 'EMPLOYEE', email: '', groupId: null, initialPassword: '' }
    await load()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}

function openEdit(u) {
  editing.value = u.id
  editForm.value = {
    id: u.id,
    nickname: u.nickname,
    role: u.role,
    groupId: u.groupId ?? null,
    email: u.email || '',
    status: u.status
  }
}

async function saveEdit() {
  const u = users.value.find(x => x.id === editForm.value.id)
  if (u && editForm.value.status === 'DISABLED' && !canDisable(u)) {
    toast(disableReason(u), 'error')
    return
  }
  try {
    await api.updateUser(editForm.value.id, {
      nickname: editForm.value.nickname,
      role: editForm.value.role,
      email: editForm.value.email,
      groupId: editForm.value.groupId,
      status: editForm.value.status
    })
    toast('已保存', 'success')
    editing.value = null
    await load()
  } catch (e) {
    toast(e.message, 'error')
    await load()
  }
}

async function resetPassword(u) {
  if (!confirm(`重置 ${u.nickname} 的密码？其下次登录必须修改密码。`)) return
  try {
    const data = await api.resetPassword(u.id)
    prompt(`新密码如下，请复制并告知本人：`, data.password)
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function adjustScore(u) {
  const v = prompt(`给 ${u.nickname} 加减分（正数加分，负数扣分）：`, '1')
  if (v === null) return
  const score = Number(v)
  if (!Number.isInteger(score) || score === 0) {
    toast('请输入非零整数', 'error')
    return
  }
  const remark = prompt('备注（可选）：', '管理员手动调整') || ''
  try {
    await api.adjustScore(u.id, score, remark)
    toast('已调整积分', 'success')
  } catch (e) {
    toast(e.message, 'error')
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <!-- 标题栏 -->
    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
      <h2 class="text-sm font-semibold text-slate-700">成员管理（{{ users.length }} 人）</h2>
      <div class="flex items-center gap-2">
        <input v-model="keyword" class="input w-40" placeholder="搜索姓名/账号" />
        <button class="btn-primary shrink-0 text-xs" @click="showCreate = !showCreate">
          <UserPlus class="h-4 w-4" />新建账号
        </button>
      </div>
    </div>

    <!-- 新建账号 -->
    <form v-if="showCreate" class="card mb-4 space-y-3 p-4" @submit.prevent="createUser">
      <div class="grid gap-3 md:grid-cols-2">
        <div>
          <label class="label">登录名 *（3-20 位字母数字下划线）</label>
          <input v-model="form.username" class="input" placeholder="zhangsan" />
        </div>
        <div>
          <label class="label">姓名/昵称 *</label>
          <input v-model="form.nickname" class="input" placeholder="张三" />
        </div>
        <div>
          <label class="label">角色</label>
          <select v-model="form.role" class="input">
            <option value="EMPLOYEE">成员</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
        <div>
          <label class="label">小组</label>
          <select v-model="form.groupId" class="input">
            <option :value="null">未分组</option>
            <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
          </select>
        </div>
        <div>
          <label class="label">邮箱（可选，用于邮件通知）</label>
          <input v-model="form.email" class="input" placeholder="you@example.com" />
        </div>
        <div>
          <label class="label">初始密码（留空自动生成）</label>
          <input v-model="form.initialPassword" class="input" placeholder="至少 6 位" />
        </div>
      </div>
      <p class="text-xs text-slate-400">新账号首次登录后必须修改密码才能使用系统。</p>
      <button class="btn-primary w-full" :disabled="submitting">创建账号</button>
    </form>

    <!-- 成员列表 -->
    <div class="card divide-y divide-slate-100">
      <div v-for="u in filtered" :key="u.id" class="flex flex-wrap items-center gap-3 p-3">
        <div
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-teal-50 text-sm font-medium text-teal-700"
        >{{ (u.nickname || u.username || '?').slice(0, 1) }}</div>

        <div class="min-w-0 flex-1">
          <div class="flex flex-wrap items-center gap-1.5">
            <span class="truncate font-medium text-slate-800">{{ u.nickname }}</span>
            <span class="tag" :class="u.role === 'ADMIN' ? 'bg-teal-100 text-teal-700' : 'bg-slate-100 text-slate-500'">
              {{ u.role === 'ADMIN' ? '管理员' : '成员' }}
            </span>
            <span
              class="tag"
              :class="u.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-400'"
            >{{ u.status === 'ACTIVE' ? '在职' : '停用' }}</span>
            <span v-if="isSelf(u)" class="tag bg-slate-100 text-slate-500">我</span>
          </div>
          <div class="mt-0.5 truncate text-xs text-slate-400">
            @{{ u.username }} · {{ groupName(u.groupId) }}
            <span v-if="u.mustChange === 1" class="text-amber-600">· 待改初始密码</span>
            <span v-if="u.lastLoginAt">· 最近登录 {{ u.lastLoginAt }}</span>
          </div>
        </div>

        <div class="flex shrink-0 flex-wrap justify-end gap-1.5">
          <button class="btn-ghost px-2 py-1 text-xs" @click="openEdit(u)">
            <Pencil class="h-3.5 w-3.5" />编辑
          </button>
          <button class="btn-ghost px-2 py-1 text-xs" @click="adjustScore(u)">
            <Coins class="h-3.5 w-3.5" />加减分
          </button>
          <button class="btn-ghost px-2 py-1 text-xs" @click="resetPassword(u)">
            <KeyRound class="h-3.5 w-3.5" />重置密码
          </button>
        </div>
      </div>
      <p v-if="!filtered.length" class="p-6 text-center text-sm text-slate-400">没有匹配的成员</p>
    </div>

    <!-- 编辑弹窗 -->
    <div
      v-if="editing !== null"
      class="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 p-0 md:items-center md:p-6"
      @click.self="editing = null"
    >
      <div class="w-full max-w-md rounded-t-2xl bg-white p-4 md:rounded-2xl">
        <h3 class="mb-3 text-sm font-semibold text-slate-800">编辑成员</h3>

        <div class="space-y-3">
          <div>
            <label class="label">姓名/昵称</label>
            <input v-model="editForm.nickname" class="input" />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="label">角色</label>
              <select v-model="editForm.role" class="input">
                <option value="EMPLOYEE">成员</option>
                <option value="ADMIN">管理员</option>
              </select>
            </div>
            <div>
              <label class="label">小组</label>
              <select v-model="editForm.groupId" class="input">
                <option :value="null">未分组</option>
                <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
              </select>
            </div>
          </div>

          <div>
            <label class="label">邮箱</label>
            <input v-model="editForm.email" class="input" placeholder="用于邮件通知，可留空" />
          </div>

          <div>
            <label class="label">账号状态</label>
            <div class="flex gap-2">
              <button
                class="btn flex-1"
                :class="editForm.status === 'ACTIVE' ? 'bg-emerald-600 text-white hover:bg-emerald-700' : 'bg-slate-100 text-slate-600'"
                @click="editForm.status = 'ACTIVE'"
              >在职</button>
              <button
                class="btn flex-1"
                :class="editForm.status === 'DISABLED' ? 'bg-rose-500 text-white hover:bg-rose-600' : 'bg-slate-100 text-slate-600'"
                :disabled="!canDisable(users.find(x => x.id === editing))"
                @click="editForm.status = 'DISABLED'"
              >停用</button>
            </div>
            <p
              v-if="!canDisable(users.find(x => x.id === editing))"
              class="mt-1 text-xs text-amber-600"
            >{{ disableReason(users.find(x => x.id === editing)) }}</p>
            <p v-else class="mt-1 text-xs text-slate-400">停用后该成员无法登录，历史记录保留。</p>
          </div>
        </div>

        <div class="mt-4 flex gap-2">
          <button class="btn-ghost flex-1" @click="editing = null">取消</button>
          <button class="btn-primary flex-1" @click="saveEdit">保存</button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
