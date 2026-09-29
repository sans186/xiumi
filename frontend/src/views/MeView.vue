<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { api, token } from '../api'
import { store, toast, resetStore, SCORE_TYPE } from '../store'
import { fmtFull } from '../utils'
import {
  UserRound, Settings, Users, FolderTree, Plus, KeyRound, LogOut, Mail, ChevronRight
} from 'lucide-vue-next'

const router = useRouter()
const scores = ref([])
const email = ref('')

async function load() {
  try {
    scores.value = await api.myScores()
  } catch (e) {
    /* 忽略 */
  }
  email.value = store.user ? store.user.email || '' : ''
}

async function saveEmail() {
  try {
    await api.updateEmail(email.value)
    if (store.user) store.user.email = email.value
    toast('邮箱已保存', 'success')
  } catch (e) {
    toast(e.message, 'error')
  }
}

function logout() {
  api.logout().catch(() => {})
  token.clear()
  resetStore()
  router.replace({ name: 'login' })
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <section class="card mb-4 flex items-center gap-4 p-4">
      <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-600 text-white">
        <UserRound class="h-6 w-6" />
      </div>
      <div class="min-w-0">
        <div class="truncate text-base font-semibold text-slate-800">
          {{ store.user?.nickname }}
          <span class="tag ml-1" :class="store.user?.role === 'ADMIN' ? 'bg-teal-100 text-teal-700' : 'bg-slate-100 text-slate-500'">
            {{ store.user?.role === 'ADMIN' ? '管理员' : '成员' }}
          </span>
          <span v-if="store.permissions.isLeader" class="tag ml-1 bg-amber-100 text-amber-700">组长</span>
        </div>
        <div class="text-xs text-slate-400">{{ store.user?.username }}</div>
      </div>
      <div class="ml-auto text-right">
        <div class="text-xl font-bold text-amber-600">{{ store.summary.score ?? 0 }}</div>
        <div class="text-[11px] text-slate-400">分 · 第 {{ store.summary.rank || '-' }} 名</div>
      </div>
    </section>

    <!-- 邮箱（用于邮件通知） -->
    <section class="card mb-4 p-4">
      <h3 class="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-700">
        <Mail class="h-4 w-4" />通知邮箱
      </h3>
      <p class="mb-2 text-xs text-slate-500">填写后，新任务、被分配、审核请求等会同时发邮件给你。</p>
      <div class="flex gap-2">
        <input v-model="email" class="input" placeholder="you@example.com" />
        <button class="btn-primary shrink-0" @click="saveEmail">保存</button>
      </div>
    </section>

    <!-- 管理入口 -->
    <section v-if="store.permissions.canManage || store.permissions.canPublish" class="card mb-4 divide-y divide-slate-100">
      <RouterLink
        v-if="store.permissions.canPublish"
        :to="{ name: 'task-create' }"
        class="flex items-center gap-3 p-3 text-sm text-slate-700"
      >
        <Plus class="h-4 w-4 text-teal-600" />发布任务
        <ChevronRight class="ml-auto h-4 w-4 text-slate-300" />
      </RouterLink>
      <RouterLink
        v-if="store.permissions.canManage"
        :to="{ name: 'members' }"
        class="flex items-center gap-3 p-3 text-sm text-slate-700"
      >
        <Users class="h-4 w-4 text-teal-600" />成员管理
        <ChevronRight class="ml-auto h-4 w-4 text-slate-300" />
      </RouterLink>
      <RouterLink
        v-if="store.permissions.canManage"
        :to="{ name: 'groups' }"
        class="flex items-center gap-3 p-3 text-sm text-slate-700"
      >
        <FolderTree class="h-4 w-4 text-teal-600" />小组管理
        <ChevronRight class="ml-auto h-4 w-4 text-slate-300" />
      </RouterLink>
      <RouterLink
        v-if="store.permissions.canManage"
        :to="{ name: 'settings' }"
        class="flex items-center gap-3 p-3 text-sm text-slate-700"
      >
        <Settings class="h-4 w-4 text-teal-600" />系统设置
        <ChevronRight class="ml-auto h-4 w-4 text-slate-300" />
      </RouterLink>
    </section>

    <!-- 积分明细 -->
    <section class="card mb-4">
      <h3 class="border-b border-slate-100 p-3 text-sm font-semibold text-slate-700">我的积分明细</h3>
      <div v-if="!scores.length" class="py-10 text-center text-sm text-slate-400">还没有积分记录</div>
      <div v-else class="divide-y divide-slate-100">
        <div v-for="s in scores" :key="s.id" class="flex items-center gap-3 p-3">
          <div class="min-w-0 flex-1">
            <div class="truncate text-sm text-slate-700">{{ s.remark || SCORE_TYPE[s.type] }}</div>
            <div class="text-xs text-slate-400">{{ SCORE_TYPE[s.type] }} · {{ fmtFull(s.createdAt) }}</div>
          </div>
          <div class="font-semibold" :class="s.score >= 0 ? 'text-emerald-600' : 'text-rose-600'">
            {{ s.score >= 0 ? '+' : '' }}{{ s.score }}
          </div>
        </div>
      </div>
    </section>

    <div class="flex gap-2">
      <RouterLink :to="{ name: 'change-password' }" class="btn-ghost flex-1">
        <KeyRound class="h-4 w-4" />修改密码
      </RouterLink>
      <button class="btn-danger flex-1" @click="logout">
        <LogOut class="h-4 w-4" />退出登录
      </button>
    </div>
  </AppLayout>
</template>
