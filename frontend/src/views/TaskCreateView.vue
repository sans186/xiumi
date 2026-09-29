<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { store, toast } from '../store'
import { fmtSize } from '../utils'
import { Send, Paperclip, X } from 'lucide-vue-next'

const MAX_ATTACH = 50 * 1024 * 1024
const ALLOWED_EXT = ['.zip', '.rar', '.7z', '.docx']

const router = useRouter()
const form = ref({
  title: '',
  content: '',
  claimDeadline: '',
  ddl: '',
  claimScore: 1,
  completeScore: 1,
  reviewScore: 1
})
const submitting = ref(false)
const attachFile = ref(null)
const attachInput = ref(null)

function pickAttach(e) {
  const f = e.target.files && e.target.files[0]
  if (!f) return
  const lower = f.name.toLowerCase()
  if (!ALLOWED_EXT.some(ext => lower.endsWith(ext))) {
    toast('只能上传 zip / rar / 7z 压缩包，或单个 docx 文档', 'error')
    e.target.value = ''
    return
  }
  if (f.size > MAX_ATTACH) {
    toast('附件不能超过 50MB', 'error')
    e.target.value = ''
    return
  }
  attachFile.value = f
}

function clearAttach() {
  attachFile.value = null
  if (attachInput.value) attachInput.value.value = ''
}

function toInput(d) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toApi(v) {
  return v ? v.replace('T', ' ') + ':00' : ''
}

onMounted(async () => {
  const now = new Date()
  // 默认：接取时限 12 小时，完成时限 36 小时
  form.value.claimDeadline = toInput(new Date(now.getTime() + 12 * 60 * 60 * 1000))
  form.value.ddl = toInput(new Date(now.getTime() + 36 * 60 * 60 * 1000))
  if (store.permissions.canManage) {
    try {
      const s = await api.settings()
      form.value.claimScore = s.claimScore
      form.value.completeScore = s.completeScore
      form.value.reviewScore = s.reviewScore
    } catch (e) {
      /* 忽略 */
    }
  }
})

async function submit() {
  if (!form.value.title.trim()) {
    toast('请填写任务标题', 'error')
    return
  }
  submitting.value = true
  try {
    const data = await api.createTask({
      title: form.value.title.trim(),
      content: form.value.content,
      claimDeadline: toApi(form.value.claimDeadline),
      ddl: toApi(form.value.ddl),
      claimScore: Number(form.value.claimScore),
      completeScore: Number(form.value.completeScore),
      reviewScore: Number(form.value.reviewScore)
    })
    let warn = ''
    if (attachFile.value) {
      try {
        await api.uploadTaskAttachment(data.id, attachFile.value)
      } catch (e) {
        warn = '，但附件上传失败：' + e.message
      }
    }
    if (warn) {
      toast('任务已发布' + warn, 'error')
    } else {
      toast('任务已发布，已通知全员', 'success')
    }
    router.replace({ name: 'task-detail', params: { id: data.id } })
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AppLayout>
    <form class="card space-y-4 p-4 md:p-6" @submit.prevent="submit">
      <div>
        <label class="label">任务标题 *</label>
        <input v-model="form.title" class="input" placeholder="例：公众号推文《开学季书单》排版" />
      </div>

      <div>
        <label class="label">任务说明</label>
        <textarea
          v-model="form.content"
          class="input min-h-20"
          placeholder="要求、素材位置、注意事项…"
        />
      </div>

      <div class="grid gap-4 md:grid-cols-2">
        <div>
          <label class="label">接取时限 *（到点无人接取则自动按顺序分配）</label>
          <input v-model="form.claimDeadline" type="datetime-local" class="input" />
        </div>
        <div>
          <label class="label">完成时限 DDL *（在此时间前提交才算按时）</label>
          <input v-model="form.ddl" type="datetime-local" class="input" />
        </div>
      </div>

      <div>
        <label class="label">任务附件（可选，zip / rar / 7z 压缩包或 docx 文档，单个不超过 50MB）</label>
        <input
          ref="attachInput"
          type="file"
          accept=".zip,.rar,.7z,.docx"
          class="hidden"
          @change="pickAttach"
        />
        <div v-if="!attachFile" class="flex flex-wrap items-center gap-2">
          <button type="button" class="btn-ghost text-xs" @click="attachInput && attachInput.click()">
            <Paperclip class="h-3.5 w-3.5" />选择附件
          </button>
          <span class="text-xs text-slate-400">素材包、Word 需求文档之类，随任务一起发给成员</span>
        </div>
        <div v-else class="flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-2">
          <Paperclip class="h-4 w-4 shrink-0 text-slate-400" />
          <span class="min-w-0 flex-1 truncate text-sm text-slate-700">{{ attachFile.name }}</span>
          <span class="shrink-0 text-xs text-slate-400">{{ fmtSize(attachFile.size) }}</span>
          <button
            type="button"
            class="shrink-0 rounded-lg p-1 text-slate-400 hover:bg-slate-200 hover:text-rose-500"
            @click="clearAttach"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </div>

      <div class="grid grid-cols-3 gap-3">
        <div>
          <label class="label">接取分</label>
          <input v-model.number="form.claimScore" type="number" min="0" class="input" />
        </div>
        <div>
          <label class="label">按时完成分</label>
          <input v-model.number="form.completeScore" type="number" min="0" class="input" />
        </div>
        <div>
          <label class="label">审核通过分</label>
          <input v-model.number="form.reviewScore" type="number" min="0" class="input" />
        </div>
      </div>
      <p class="text-xs text-slate-400">
        提示：自动分配到的人默认不给"接取分"，但按时完成、审核通过的分照常给；可在系统设置里开启。
      </p>

      <button class="btn-primary w-full py-2.5" :disabled="submitting">
        <Send class="h-4 w-4" />
        {{ submitting ? '发布中…' : '发布任务' }}
      </button>
    </form>
  </AppLayout>
</template>
