<script setup>
import { computed } from 'vue'
import { STATUS_META } from '../store'
import { fmtShort, remainText, urgencyClass, isExpired } from '../utils'
import { Clock, CalendarClock, Trophy, UserRound, Zap, Paperclip } from 'lucide-vue-next'

const props = defineProps({
  task: { type: Object, required: true }
})

const meta = computed(() => STATUS_META[props.task.status] || STATUS_META.OPEN)
const overdue = computed(() => props.task.overdue)
</script>

<template>
  <div class="card p-4 transition hover:shadow-md">
    <div class="flex items-start justify-between gap-3">
      <h3 class="min-w-0 flex-1 text-[15px] font-semibold leading-snug text-slate-800">
        {{ task.title }}
      </h3>
      <span class="tag shrink-0" :class="meta.cls">{{ meta.text }}</span>
    </div>

    <p v-if="task.content" class="mt-1 line-clamp-2 text-xs leading-relaxed text-slate-500">
      {{ task.content }}
    </p>

    <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs">
      <span class="inline-flex items-center gap-1" :class="urgencyClass(task.claimDeadline)">
        <Clock class="h-3.5 w-3.5" />
        <template v-if="task.status === 'OPEN'">抢单截止 {{ remainText(task.claimDeadline) }}</template>
        <template v-else>接取于 {{ fmtShort(task.claimedAt) }}</template>
      </span>
      <span class="inline-flex items-center gap-1" :class="urgencyClass(task.ddl)">
        <CalendarClock class="h-3.5 w-3.5" />
        DDL {{ fmtShort(task.ddl) }}
        <span v-if="overdue" class="tag bg-rose-100 text-rose-600">已逾期</span>
      </span>
      <span class="inline-flex items-center gap-1 text-amber-600">
        <Trophy class="h-3.5 w-3.5" />
        {{ task.claimScore }}+{{ task.completeScore }}+{{ task.reviewScore }} 分
      </span>
      <span
        v-if="task.attachName"
        class="inline-flex items-center gap-1 text-slate-500"
        :title="task.attachName"
      >
        <Paperclip class="h-3.5 w-3.5" />有附件
      </span>
    </div>

    <div class="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
      <span v-if="task.assigneeName" class="inline-flex items-center gap-1">
        <UserRound class="h-3.5 w-3.5" />
        {{ task.assigneeName }}
        <span v-if="task.assignType === 'AUTO'" class="tag bg-slate-100 text-slate-500">自动分配</span>
        <span v-else-if="task.assignType === 'MANUAL'" class="tag bg-slate-100 text-slate-500">管理员指派</span>
      </span>
      <span v-else-if="task.nextCandidateName" class="inline-flex items-center gap-1 text-slate-400">
        <Zap class="h-3.5 w-3.5" />
        无人接取将轮到 {{ task.nextCandidateName }}
      </span>
      <span v-if="task.reviewerName && task.status === 'PENDING_REVIEW'">
        审核人 {{ task.reviewerName }}
      </span>
    </div>
  </div>
</template>
