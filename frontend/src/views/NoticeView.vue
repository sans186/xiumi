<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { fmtFull } from '../utils'
import { Bell, CheckCheck } from 'lucide-vue-next'

const router = useRouter()
const list = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await api.notifications(false)
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function readAll() {
  try {
    await api.readAll()
    store.unread = 0
    await load()
    toast('已全部标记为已读', 'success')
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function open(n) {
  if (!n.isRead) {
    await api.readOne(n.id)
    store.unread = Math.max(0, store.unread - 1)
    n.isRead = 1
  }
  if (n.taskId) router.push({ name: 'task-detail', params: { id: n.taskId } })
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mb-3 flex items-center justify-between">
      <h2 class="flex items-center gap-2 text-sm font-semibold text-slate-700">
        <Bell class="h-4 w-4" />消息中心
      </h2>
      <button class="btn-ghost text-xs" @click="readAll">
        <CheckCheck class="h-3.5 w-3.5" />全部已读
      </button>
    </div>

    <div v-if="loading" class="py-16 text-center text-sm text-slate-400">加载中…</div>

    <div v-else-if="!list.length" class="card py-14 text-center text-sm text-slate-400">暂无消息</div>

    <div v-else class="card divide-y divide-slate-100">
      <div
        v-for="n in list"
        :key="n.id"
        class="cursor-pointer p-3 transition hover:bg-slate-50"
        :class="n.isRead ? '' : 'bg-teal-50/40'"
        @click="open(n)"
      >
        <div class="flex items-start gap-2">
          <span v-if="!n.isRead" class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-teal-500"></span>
          <div class="min-w-0 flex-1">
            <div class="text-sm font-medium text-slate-800">{{ n.title }}</div>
            <div v-if="n.content" class="mt-0.5 whitespace-pre-wrap text-xs text-slate-500">{{ n.content }}</div>
            <div class="mt-1 text-[11px] text-slate-400">{{ fmtFull(n.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
