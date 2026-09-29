<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { store, toast, loadMe, resetStore } from './store'
import { token } from './api'

const router = useRouter()

function onLogout() {
  token.clear()
  resetStore()
  router.replace({ name: 'login' })
}

function onForceChange() {
  if (store.user) store.user.mustChange = 1
  router.replace({ name: 'change-password' })
}

onMounted(() => {
  window.addEventListener('xmt:logout', onLogout)
  window.addEventListener('xmt:force-change', onForceChange)
  if (token.get()) loadMe()
})
onUnmounted(() => {
  window.removeEventListener('xmt:logout', onLogout)
  window.removeEventListener('xmt:force-change', onForceChange)
})
</script>

<template>
  <router-view />

  <!-- 全局轻提示 -->
  <div class="pointer-events-none fixed inset-x-0 top-4 z-50 flex flex-col items-center gap-2 px-4">
    <TransitionGroup name="toast">
      <div
        v-for="t in store.toasts"
        :key="t.id"
        class="pointer-events-auto max-w-[90vw] rounded-xl px-4 py-2 text-sm shadow-lg"
        :class="t.type === 'error'
          ? 'bg-rose-600 text-white'
          : t.type === 'success' ? 'bg-emerald-600 text-white' : 'bg-slate-800 text-white'"
      >
        {{ t.message }}
      </div>
    </TransitionGroup>
  </div>
</template>

<style>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
