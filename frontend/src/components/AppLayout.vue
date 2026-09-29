<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { store } from '../store'
import {
  LayoutGrid, ClipboardCheck, Trophy, Bell, User, Users, FolderTree, Settings, Plus
} from 'lucide-vue-next'

defineProps({ title: { type: String, default: '新媒体小组' } })

const route = useRoute()

const navItems = computed(() => {
  const list = [
    { name: 'hall', label: '任务大厅', icon: LayoutGrid },
  ]
  if (store.permissions.canReview) {
    list.push({ name: 'review', label: '待我审核', icon: ClipboardCheck })
  }
  list.push({ name: 'rank', label: '排行榜', icon: Trophy })
  list.push({ name: 'notices', label: '消息', icon: Bell })
  list.push({ name: 'me', label: '我的', icon: User })
  return list
})

const manageItems = computed(() => {
  if (!store.permissions.canManage) return []
  return [
    { name: 'members', label: '成员管理', icon: Users },
    { name: 'groups', label: '小组管理', icon: FolderTree },
    { name: 'settings', label: '系统设置', icon: Settings }
  ]
})

const currentTitle = computed(() => {
  const all = [...navItems.value, ...manageItems.value]
  const hit = all.find((i) => i.name === route.name)
  return hit ? hit.label : '新媒体小组'
})
</script>

<template>
  <div class="min-h-full md:flex">
    <!-- 桌面端侧边栏 -->
    <aside class="hidden md:flex md:w-60 md:shrink-0 md:flex-col border-r border-slate-200 bg-white">
      <div class="px-6 py-5">
        <div class="text-base font-bold text-slate-800">新媒体小组</div>
        <div class="text-xs text-slate-400">任务 · 积分 · 协作</div>
      </div>
      <nav class="flex-1 space-y-1 px-3">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          :to="{ name: item.name }"
          class="flex items-center gap-3 rounded-xl px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
          active-class="!bg-teal-50 !text-teal-700 font-medium"
        >
          <component :is="item.icon" class="h-4 w-4" />
          <span>{{ item.label }}</span>
          <span
            v-if="item.name === 'notices' && store.unread > 0"
            class="ml-auto rounded-full bg-rose-500 px-1.5 text-xs text-white"
          >{{ store.unread }}</span>
        </RouterLink>

        <template v-if="manageItems.length">
          <div class="px-3 pb-1 pt-4 text-xs font-medium text-slate-400">管理员</div>
          <RouterLink
            v-for="item in manageItems"
            :key="item.name"
            :to="{ name: item.name }"
            class="flex items-center gap-3 rounded-xl px-3 py-2 text-sm text-slate-600 transition hover:bg-slate-100"
            active-class="!bg-teal-50 !text-teal-700 font-medium"
          >
            <component :is="item.icon" class="h-4 w-4" />
            <span>{{ item.label }}</span>
          </RouterLink>
        </template>
      </nav>
      <div class="border-t border-slate-100 p-3">
        <RouterLink
          v-if="store.permissions.canPublish"
          :to="{ name: 'task-create' }"
          class="btn-primary w-full"
        >
          <Plus class="h-4 w-4" />发布任务
        </RouterLink>
      </div>
    </aside>

    <div class="flex min-w-0 flex-1 flex-col">
      <!-- 移动端顶栏 -->
      <header
        class="sticky top-0 z-20 flex items-center justify-between border-b border-slate-200 bg-white/95 px-4 py-3 backdrop-blur md:hidden"
      >
        <h1 class="text-base font-semibold text-slate-800">{{ currentTitle }}</h1>
        <RouterLink :to="{ name: 'notices' }" class="relative p-1 text-slate-500">
          <Bell class="h-5 w-5" />
          <span
            v-if="store.unread > 0"
            class="absolute -right-0.5 -top-0.5 min-w-4 rounded-full bg-rose-500 px-1 text-center text-[10px] leading-4 text-white"
          >{{ store.unread }}</span>
        </RouterLink>
      </header>

      <main class="mx-auto w-full max-w-5xl flex-1 px-4 py-4 pb-24 md:px-8 md:py-8 md:pb-8">
        <slot />
      </main>
    </div>

    <!-- 移动端底部导航 -->
    <nav
      class="pb-safe fixed inset-x-0 bottom-0 z-30 flex border-t border-slate-200 bg-white/95 backdrop-blur md:hidden"
    >
      <RouterLink
        v-for="item in navItems"
        :key="item.name"
        :to="{ name: item.name }"
        class="relative flex flex-1 flex-col items-center gap-0.5 py-2 text-[11px] text-slate-400"
        active-class="!text-teal-600"
      >
        <component :is="item.icon" class="h-5 w-5" />
        <span>{{ item.label }}</span>
        <span
          v-if="item.name === 'notices' && store.unread > 0"
          class="absolute right-[22%] top-1 min-w-4 rounded-full bg-rose-500 px-1 text-center text-[10px] leading-4 text-white"
        >{{ store.unread }}</span>
      </RouterLink>
    </nav>
  </div>
</template>
