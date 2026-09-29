<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { fmtShort, urgencyClass } from '../utils'
import { ClipboardCheck, ExternalLink } from 'lucide-vue-next'

const router = useRouter()
const tasks = ref([])
const loading = ref(false)
const comment = ref({})
const busyId = ref(null)

const mine = computed(() => tasks.value.filter((t) => t.reviewerId === (store.user && store.user.id)))
const others = computed(() => tasks.value.filter((t) => t.reviewerId !== (store.user && store.user.id)))

async function load() {
  loading.value = true
  try {
    tasks.value = await api.tasks('review')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function review(task, approve, giveScore = true) {
  const c = comment.value[task.id] || ''
  busyId.value = task.id
  try {
    await api.reviewTask(task.id, approve, c, giveScore)
    toast(approve ? (giveScore ? '已通过并给分' : '已通过（本次不给分）') : '已打回', 'success')
    comment.value[task.id] = ''
    await load()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <h2 class="mb-3 flex items-center gap-2 text-sm font-semibold text-slate-700">
      <ClipboardCheck class="h-4 w-4" />审核中心
    </h2>
    <p class="mb-4 text-xs text-slate-500">
      员工提交秀米预览链接后，系统会按顺序轮流把审核请求分给管理员；分配给你上面会有标记，其他人也可以代审。
    </p>

    <div v-if="loading" class="py-16 text-center text-sm text-slate-400">加载中…</div>

    <template v-else>
      <template v-if="mine.length">
        <h3 class="mb-2 text-xs font-medium text-teal-600">轮到我审核（{{ mine.length }}）</h3>
        <div class="space-y-3">
          <div v-for="t in mine" :key="t.id" class="card border-teal-100 p-4">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <div class="font-semibold text-slate-800">{{ t.title }}</div>
                <div class="mt-0.5 text-xs text-slate-500">
                  {{ t.assigneeName }} 提交于 {{ fmtShort(t.submittedAt) }}
                </div>
              </div>
              <span class="tag shrink-0 bg-teal-100 text-teal-700">待我审核</span>
            </div>
            <a
              :href="t.previewUrl"
              target="_blank"
              rel="noopener"
              class="mt-2 inline-flex items-center gap-1 break-all text-xs text-teal-600 hover:underline"
            >
              <ExternalLink class="h-3.5 w-3.5" />在秀米中打开预览
            </a>
            <div v-if="t.submitNote" class="mt-1 text-xs text-slate-500">备注：{{ t.submitNote }}</div>
            <div class="mt-2 text-xs" :class="urgencyClass(t.ddl)">DDL {{ fmtShort(t.ddl) }}</div>
            <textarea
              v-model="comment[t.id]"
              class="input mt-2 min-h-14"
              placeholder="评语（可选）"
            ></textarea>
            <div class="mt-2 flex flex-wrap gap-2">
              <button class="btn-danger flex-1" :disabled="busyId === t.id" @click="review(t, false)">
                打回修改
              </button>
              <button class="btn-primary flex-1" :disabled="busyId === t.id" @click="review(t, true, true)">
                通过并给分 +{{ t.reviewScore }}
              </button>
              <button class="btn-ghost flex-1" :disabled="busyId === t.id" @click="review(t, true, false)">
                通过不给分
              </button>
            </div>
          </div>
        </div>
      </template>

      <div v-if="!mine.length && !others.length" class="card py-14 text-center text-sm text-slate-400">
        暂无待审核任务
      </div>

      <template v-if="others.length">
        <h3 class="mb-2 mt-5 text-xs font-medium text-slate-500">其他待审核（{{ others.length }}）</h3>
        <div class="space-y-2">
          <RouterLink
            v-for="t in others"
            :key="t.id"
            :to="{ name: 'task-detail', params: { id: t.id } }"
            class="card block p-3"
          >
            <div class="flex items-center justify-between gap-2">
              <span class="truncate text-sm text-slate-700">{{ t.title }}</span>
              <span class="shrink-0 text-xs text-slate-400">{{ t.reviewerName || '未分配' }}</span>
            </div>
            <div class="mt-0.5 text-xs text-slate-400">{{ t.assigneeName }} · {{ fmtShort(t.submittedAt) }}</div>
          </RouterLink>
        </div>
      </template>
    </template>
  </AppLayout>
</template>
