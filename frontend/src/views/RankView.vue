<script setup>
import { ref, onMounted, watch } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { Trophy, Crown } from 'lucide-vue-next'

const tab = ref('all')
const groupId = ref(null)
const groups = ref([])
const list = ref([])
const groupList = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    if (tab.value === 'group') {
      groupList.value = await api.groupRank()
    } else {
      list.value = await api.rank(tab.value, groupId.value)
    }
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadGroups() {
  try {
    groups.value = await api.groups()
  } catch (e) {
    /* 忽略 */
  }
}

onMounted(async () => {
  await loadGroups()
  load()
})
watch([tab, groupId], load)
</script>

<template>
  <AppLayout>
    <div class="mb-3 flex gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1">
      <button
        v-for="t in [
          { key: 'all', label: '总榜' },
          { key: 'month', label: '本月' },
          { key: 'group', label: '小组榜' }
        ]"
        :key="t.key"
        class="shrink-0 rounded-lg px-3 py-1.5 text-sm transition"
        :class="tab === t.key ? 'bg-white font-medium text-teal-700 shadow-sm' : 'text-slate-500'"
        @click="tab = t.key"
      >
        {{ t.label }}
      </button>
    </div>

    <select
      v-if="tab !== 'group'"
      v-model="groupId"
      class="input mb-3"
    >
      <option :value="null">全部成员</option>
      <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.name }}</option>
    </select>

    <div v-if="loading" class="py-16 text-center text-sm text-slate-400">加载中…</div>

    <!-- 个人榜 -->
    <div v-else-if="tab !== 'group'" class="card divide-y divide-slate-100">
      <div
        v-for="(p, i) in list"
        :key="p.id"
        class="flex items-center gap-3 p-3"
        :class="store.user && store.user.id === p.id ? 'bg-teal-50/60' : ''"
      >
        <div
          class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-sm font-bold"
          :class="i === 0 ? 'bg-amber-100 text-amber-600'
            : i === 1 ? 'bg-slate-200 text-slate-600'
            : i === 2 ? 'bg-orange-100 text-orange-600' : 'bg-slate-100 text-slate-400'"
        >
          <Crown v-if="i === 0" class="h-4 w-4" />
          <span v-else>{{ i + 1 }}</span>
        </div>
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-medium text-slate-800">
            {{ p.nickname }}
            <span v-if="store.user && store.user.id === p.id" class="tag ml-1 bg-teal-100 text-teal-700">我</span>
          </div>
          <div class="text-xs text-slate-400">
            {{ p.group_name || '未分组' }} · 完成 {{ p.completed_count }} 个 · 进行中 {{ p.pending_count }} 个
          </div>
        </div>
        <div class="flex items-center gap-1 text-base font-bold text-amber-600">
          <Trophy class="h-4 w-4" />{{ p.score }}
        </div>
      </div>
      <div v-if="!list.length" class="py-14 text-center text-sm text-slate-400">暂无数据</div>
    </div>

    <!-- 小组榜 -->
    <div v-else class="card divide-y divide-slate-100">
      <div v-for="(g, i) in groupList" :key="g.id" class="flex items-center gap-3 p-3">
        <div class="flex h-8 w-8 items-center justify-center rounded-full bg-teal-50 text-sm font-bold text-teal-600">
          {{ i + 1 }}
        </div>
        <div class="min-w-0 flex-1">
          <div class="truncate text-sm font-medium text-slate-800">{{ g.name }}</div>
          <div class="text-xs text-slate-400">
            组长 {{ g.leader_name || '未设置' }} · {{ g.member_count }} 人
          </div>
        </div>
        <div class="text-base font-bold text-amber-600">{{ g.score }}</div>
      </div>
      <div v-if="!groupList.length" class="py-14 text-center text-sm text-slate-400">暂无小组</div>
    </div>
  </AppLayout>
</template>
