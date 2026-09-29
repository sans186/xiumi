<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import TaskCard from '../components/TaskCard.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { Plus, Inbox } from 'lucide-vue-next'

const router = useRouter()
const tab = ref('open')
const tasks = ref([])
const stats = ref({})
const loading = ref(false)

const tabs = computed(() => {
  const list = [
    { key: 'open', label: '待接取' },
    { key: 'mine', label: '我的任务' },
    { key: 'all', label: '全部' },
    { key: 'done', label: '已完成' }
  ]
  if (store.permissions.canReview) {
    list.splice(2, 0, { key: 'review', label: '待审核' })
  }
  return list
})

async function load() {
  loading.value = true
  try {
    tasks.value = await api.tasks(tab.value)
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await api.taskStats()
  } catch (e) {
    /* 忽略 */
  }
}

onMounted(async () => {
  await load()
  loadStats()
})
watch(tab, load)
</script>

<template>
  <AppLayout>
    <!-- 概览 -->
    <section class="card mb-4 flex items-center gap-4 bg-gradient-to-r from-teal-700 to-teal-500 p-4 text-white">
      <div>
        <div class="text-xs opacity-80">我的积分</div>
        <div class="text-2xl font-bold">{{ store.summary.score ?? 0 }}</div>
      </div>
      <div class="h-8 w-px bg-white/30"></div>
      <div>
        <div class="text-xs opacity-80">当前排名</div>
        <div class="text-2xl font-bold">
          {{ store.summary.rank ? '#' + store.summary.rank : '—' }}
          <span class="text-xs font-normal opacity-80">/ {{ store.summary.totalMembers || 0 }} 人</span>
        </div>
      </div>
      <RouterLink :to="{ name: 'rank' }" class="ml-auto rounded-lg bg-white/20 px-3 py-1.5 text-xs">
        查看榜单
      </RouterLink>
    </section>

    <!-- 统计条 -->
    <div class="mb-3 grid grid-cols-3 gap-2 text-center">
      <div class="card py-2">
        <div class="text-lg font-bold text-amber-600">{{ stats.open ?? 0 }}</div>
        <div class="text-[11px] text-slate-500">待接取</div>
      </div>
      <div class="card py-2">
        <div class="text-lg font-bold text-teal-600">{{ stats.mine ?? 0 }}</div>
        <div class="text-[11px] text-slate-500">进行中</div>
      </div>
      <div class="card py-2">
        <div class="text-lg font-bold text-amber-600">{{ stats.pendingReview ?? 0 }}</div>
        <div class="text-[11px] text-slate-500">待审核</div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="mb-3 flex gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="shrink-0 rounded-lg px-3 py-1.5 text-sm transition"
        :class="tab === t.key ? 'bg-white font-medium text-teal-700 shadow-sm' : 'text-slate-500'"
        @click="tab = t.key"
      >
        {{ t.label }}
      </button>
    </div>

    <div v-if="loading" class="py-16 text-center text-sm text-slate-400">加载中…</div>

    <div v-else-if="!tasks.length" class="card flex flex-col items-center gap-2 py-14 text-slate-400">
      <Inbox class="h-8 w-8" />
      <span class="text-sm">暂无任务</span>
    </div>

    <div v-else class="space-y-3">
      <RouterLink
        v-for="t in tasks"
        :key="t.id"
        :to="{ name: 'task-detail', params: { id: t.id } }"
        class="block"
      >
        <TaskCard :task="t" />
      </RouterLink>
    </div>

    <!-- 移动端发布按钮 -->
    <button
      v-if="store.permissions.canPublish"
      class="fixed bottom-20 right-4 z-30 flex h-12 w-12 items-center justify-center rounded-full bg-teal-600 text-white shadow-lg shadow-teal-300 md:hidden"
      @click="router.push({ name: 'task-create' })"
    >
      <Plus class="h-6 w-6" />
    </button>
  </AppLayout>
</template>
