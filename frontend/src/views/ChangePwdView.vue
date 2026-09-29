<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'
import { store, loadMe, toast } from '../store'
import { ShieldCheck } from 'lucide-vue-next'

const router = useRouter()
const oldPassword = ref('')
const newPassword = ref('')
const confirm = ref('')
const loading = ref(false)

async function submit() {
  if (newPassword.value.length < 6) {
    toast('新密码至少 6 位', 'error')
    return
  }
  if (newPassword.value !== confirm.value) {
    toast('两次输入的新密码不一致', 'error')
    return
  }
  loading.value = true
  try {
    await api.changePassword(oldPassword.value, newPassword.value)
    await loadMe()
    toast('密码已更新', 'success')
    router.replace({ name: 'hall' })
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full items-center justify-center px-5 py-10">
    <form class="card w-full max-w-sm space-y-4 p-5" @submit.prevent="submit">
      <div class="text-center">
        <div
          class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-amber-100 text-amber-600"
        >
          <ShieldCheck class="h-6 w-6" />
        </div>
        <h1 class="text-lg font-bold text-slate-800">
          {{ store.user && store.user.mustChange === 1 ? '请先修改初始密码' : '修改密码' }}
        </h1>
        <p class="mt-1 text-xs text-slate-500">为了账号安全，请设置一个只有你自己知道的密码</p>
      </div>

      <div>
        <label class="label">原密码（管理员给的初始密码）</label>
        <input v-model="oldPassword" type="password" class="input" autocomplete="current-password" />
      </div>
      <div>
        <label class="label">新密码（至少 6 位）</label>
        <input v-model="newPassword" type="password" class="input" autocomplete="new-password" />
      </div>
      <div>
        <label class="label">确认新密码</label>
        <input
          v-model="confirm"
          type="password"
          class="input"
          autocomplete="new-password"
          @keyup.enter="submit"
        />
      </div>
      <button class="btn-primary w-full py-2.5" :disabled="loading">
        {{ loading ? '提交中…' : '确认修改' }}
      </button>
    </form>
  </div>
</template>
