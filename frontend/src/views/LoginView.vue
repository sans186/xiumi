<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, token } from '../api'
import { loadMe, toast } from '../store'
import { Sparkles, LogIn } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const loading = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    toast('请输入账号和密码', 'error')
    return
  }
  loading.value = true
  try {
    const data = await api.login(username.value.trim(), password.value)
    token.set(data.token)
    await loadMe()
    if (data.mustChangePassword) {
      router.replace({ name: 'change-password' })
    } else {
      router.replace(route.query.redirect || { name: 'hall' })
    }
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full items-center justify-center px-5 py-10">
    <div class="w-full max-w-sm">
      <div class="mb-8 text-center">
        <div
          class="mx-auto mb-3 flex h-14 w-14 items-center justify-center rounded-2xl bg-teal-600 text-white shadow-lg shadow-teal-200"
        >
          <Sparkles class="h-7 w-7" />
        </div>
        <h1 class="text-xl font-bold text-slate-800">新媒体小组</h1>
        <p class="mt-1 text-sm text-slate-500">任务接取 · 秀米审核 · 积分排行</p>
      </div>

      <form class="card space-y-4 p-5" @submit.prevent="submit">
        <div>
          <label class="label">登录名</label>
          <input v-model="username" class="input" placeholder="由管理员分配" autocomplete="username" />
        </div>
        <div>
          <label class="label">密码</label>
          <input
            v-model="password"
            type="password"
            class="input"
            placeholder="初始密码"
            autocomplete="current-password"
            @keyup.enter="submit"
          />
        </div>
        <button class="btn-primary w-full py-2.5" :disabled="loading">
          <LogIn class="h-4 w-4" />
          {{ loading ? '登录中…' : '登录' }}
        </button>
        <p class="text-center text-xs text-slate-400">
          账号由管理员创建，忘记密码请联系管理员重置
        </p>
      </form>
    </div>
  </div>
</template>
