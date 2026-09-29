<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import { api, attachmentUrl } from '../api'
import { store, toast, STATUS_META } from '../store'
import { fmtShort, fmtFull, remainText, urgencyClass, fmtSize } from '../utils'
import {
  Hand, Link2, CheckCircle2, Undo2, UserPlus, Ban, ExternalLink, ArrowLeft,
  Paperclip, Download, Upload
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const task = ref(null)
const logs = ref([])
const loading = ref(true)

/* 提交表单 */
const showSubmit = ref(false)
const previewUrl = ref('')
const note = ref('')

/* 审核表单 */
const showReview = ref(false)
const reviewComment = ref('')

/* 指派 */
const showAssign = ref(false)
const users = ref([])
const assignTo = ref(null)

const me = computed(() => store.user)
const meta = computed(() => (task.value ? STATUS_META[task.value.status] : null))
const isMine = computed(() => task.value && me.value && task.value.assigneeId === me.value.id)

/* 任务附件：发布者/管理员可上传或替换 */
const attachInput = ref(null)
const uploading = ref(false)
const downloading = ref(false)

/** 原生直链下载（默认方式） */
function nativeDownload() {
  toast('已开始下载' + (task.value.attachName ? '：' + task.value.attachName : '') + '，请在浏览器下载记录中查看', 'success')
}

async function pickAttach(e) {
  const f = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!f) return
  const lower = f.name.toLowerCase()
  if (!['.zip', '.rar', '.7z', '.docx'].some((ext) => lower.endsWith(ext))) {
    toast('只能上传 zip / rar / 7z 压缩包，或单个 docx 文档', 'error')
    return
  }
  if (f.size > 50 * 1024 * 1024) {
    toast('附件不能超过 50MB', 'error')
    return
  }
  uploading.value = true
  try {
    await api.uploadTaskAttachment(task.value.id, f)
    toast('附件已上传', 'success')
    await load()
  } catch (err) {
    toast(err.message, 'error')
  } finally {
    uploading.value = false
  }
}

/** 兜底下载：浏览器直链没反应时，改用 Blob 方式 */
async function downloadAttach() {
  downloading.value = true
  try {
    const size = await api.downloadTaskAttachment(task.value.id, task.value.attachName)
    toast('已开始下载（' + fmtSize(size) + '）', 'success')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    downloading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const data = await api.taskDetail(route.params.id)
    task.value = data.task
    logs.value = data.logs || []
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    loading.value = false
  }
}

async function doClaim() {
  try {
    await api.claimTask(task.value.id)
    toast('接取成功，+' + task.value.claimScore + ' 分', 'success')
    await load()
  } catch (e) {
    toast(e.message, 'error')
    await load()
  }
}

/** 已提交待审核时也允许改完再交，打开表单时带出上次填的内容 */
function toggleSubmitForm() {
  showSubmit.value = !showSubmit.value
  if (showSubmit.value && task.value) {
    previewUrl.value = task.value.previewUrl || ''
    note.value = task.value.submitNote || ''
  }
}

async function doSubmit() {
  if (!previewUrl.value.trim()) {
    toast('请填写秀米预览链接', 'error')
    return
  }
  const resubmit = task.value.status === 'PENDING_REVIEW'
  try {
    await api.submitTask(task.value.id, previewUrl.value.trim(), note.value)
    toast(resubmit ? '已重新提交，等待审核' : '已提交，等待管理员审核', 'success')
    showSubmit.value = false
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function doReview(approve, giveScore = true) {
  try {
    await api.reviewTask(task.value.id, approve, reviewComment.value, giveScore)
    toast(
      approve ? (giveScore ? '已通过并给分' : '已通过（本次不给分）') : '已打回',
      'success'
    )
    showReview.value = false
    reviewComment.value = ''
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function openAssign() {
  try {
    users.value = (await api.usersSimple()).filter((u) => u.status === 'ACTIVE')
    showAssign.value = true
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function doAssign() {
  if (!assignTo.value) {
    toast('请选择成员', 'error')
    return
  }
  try {
    await api.assignTask(task.value.id, assignTo.value)
    toast('已指派', 'success')
    showAssign.value = false
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function doCancel() {
  if (!confirm('确定作废该任务？')) return
  try {
    await api.cancelTask(task.value.id)
    toast('任务已作废', 'success')
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <button class="mb-3 inline-flex items-center gap-1 text-sm text-slate-500" @click="router.back()">
      <ArrowLeft class="h-4 w-4" />返回
    </button>

    <div v-if="loading" class="py-16 text-center text-sm text-slate-400">加载中…</div>

    <template v-else-if="task">
      <div class="card p-4 md:p-6">
        <div class="flex items-start justify-between gap-3">
          <h1 class="text-lg font-bold text-slate-800">{{ task.title }}</h1>
          <span class="tag" :class="meta.cls">{{ meta.text }}</span>
        </div>

        <p v-if="task.content" class="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-slate-600">
          {{ task.content }}
        </p>

        <div class="mt-4 grid gap-3 sm:grid-cols-2">
          <div class="rounded-xl bg-slate-50 p-3">
            <div class="text-xs text-slate-500">抢单截止</div>
            <div class="text-sm font-medium" :class="urgencyClass(task.claimDeadline)">
              {{ fmtFull(task.claimDeadline) }}
              <span class="text-xs">（{{ remainText(task.claimDeadline) }}）</span>
            </div>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <div class="text-xs text-slate-500">完成时限 DDL</div>
            <div class="text-sm font-medium" :class="urgencyClass(task.ddl)">
              {{ fmtFull(task.ddl) }}
              <span class="text-xs">（{{ remainText(task.ddl) }}）</span>
            </div>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <div class="text-xs text-slate-500">负责人</div>
            <div class="text-sm font-medium text-slate-700">
              {{ task.assigneeName || '暂无人接取' }}
              <span v-if="task.assignType === 'AUTO'" class="tag ml-1 bg-slate-200 text-slate-600">自动分配</span>
              <span v-else-if="task.assignType === 'MANUAL'" class="tag ml-1 bg-slate-200 text-slate-600">指派</span>
            </div>
          </div>
          <div class="rounded-xl bg-slate-50 p-3">
            <div class="text-xs text-slate-500">分值（接取 / 按时完成 / 审核通过）</div>
            <div class="text-sm font-medium text-amber-600">
              {{ task.claimScore }} / {{ task.completeScore }} / {{ task.reviewScore }}
            </div>
          </div>
        </div>

        <div v-if="task.previewUrl" class="mt-3 rounded-xl bg-amber-50 p-3">
          <div class="text-xs text-slate-500">秀米预览链接</div>
          <a
            :href="task.previewUrl"
            target="_blank"
            rel="noopener"
            class="inline-flex items-center gap-1 break-all text-sm text-teal-600 hover:underline"
          >
            <Link2 class="h-3.5 w-3.5 shrink-0" />{{ task.previewUrl }}
          </a>
          <div v-if="task.submitNote" class="mt-1 text-xs text-slate-600">备注：{{ task.submitNote }}</div>
        </div>

        <!-- 任务附件 -->
        <div class="mt-3 rounded-xl bg-slate-50 p-3">
          <div class="flex flex-wrap items-center gap-2">
            <Paperclip class="h-4 w-4 shrink-0 text-slate-400" />
            <template v-if="task.attachName">
              <span class="min-w-0 flex-1 truncate text-sm text-slate-700">{{ task.attachName }}</span>
              <span class="shrink-0 text-xs text-slate-400">{{ fmtSize(task.attachSize) }}</span>
              <!-- 默认走浏览器原生下载（带 token 直链），兼容性最好 -->
              <a
                class="btn-ghost px-2 py-1 text-xs"
                :href="attachmentUrl(task.id)"
                :download="task.attachName"
                @click="nativeDownload"
              >
                <Download class="h-3.5 w-3.5" />下载
              </a>
              <button
                class="btn-ghost px-2 py-1 text-xs"
                :disabled="downloading"
                title="浏览器直链没反应时点这里"
                @click="downloadAttach"
              >
                {{ downloading ? '下载中' : '换个方式下载' }}
              </button>
            </template>
            <span v-else class="flex-1 text-sm text-slate-400">任务附件：暂无</span>
            <template v-if="store.permissions.canPublish">
              <input
                ref="attachInput"
                type="file"
                accept=".zip,.rar,.7z,.docx"
                class="hidden"
                @change="pickAttach"
              />
              <button
                class="btn-ghost px-2 py-1 text-xs"
                :disabled="uploading"
                @click="attachInput && attachInput.click()"
              >
                <Upload class="h-3.5 w-3.5" />{{ uploading ? '上传中' : (task.attachName ? '替换' : '上传') }}
              </button>
            </template>
          </div>
          <p v-if="store.permissions.canPublish" class="mt-1 text-xs text-slate-400">
            zip / rar / 7z 压缩包或 docx 文档，单个不超过 50MB；任务作废后自动清理
          </p>
        </div>

        <div v-if="task.reviewComment" class="mt-3 rounded-xl bg-slate-50 p-3">
          <div class="text-xs text-slate-500">审核意见</div>
          <div class="text-sm text-slate-700">{{ task.reviewComment }}</div>
          <div v-if="task.reviewerName" class="mt-1 text-xs text-slate-400">
            {{ task.reviewerName }} · {{ fmtShort(task.reviewedAt) }}
          </div>
        </div>

        <!-- 操作区 -->
        <div class="mt-4 flex flex-wrap gap-2">
          <button v-if="task.canClaim" class="btn-primary" @click="doClaim">
            <Hand class="h-4 w-4" />接取任务（+{{ task.claimScore }}）
          </button>

          <button
            v-if="isMine && ['IN_PROGRESS', 'PENDING_REVIEW'].includes(task.status)"
            class="btn-primary"
            @click="toggleSubmitForm"
          >
            <ExternalLink class="h-4 w-4" />
            {{ task.status === 'PENDING_REVIEW' ? '修改后重新提交' : '提交审核' }}
          </button>

          <button
            v-if="store.permissions.canReview && task.status === 'PENDING_REVIEW'"
            class="btn-primary"
            @click="showReview = !showReview"
          >
            <CheckCircle2 class="h-4 w-4" />去审核
          </button>

          <button
            v-if="store.permissions.canManage && !['COMPLETED', 'CANCELLED'].includes(task.status)"
            class="btn-ghost"
            @click="openAssign"
          >
            <UserPlus class="h-4 w-4" />指派
          </button>
          <button
            v-if="store.permissions.canManage && !['COMPLETED', 'CANCELLED'].includes(task.status)"
            class="btn-ghost"
            @click="doCancel"
          >
            <Ban class="h-4 w-4" />作废
          </button>
        </div>

        <!-- 提交表单 -->
        <div v-if="showSubmit" class="mt-4 space-y-3 rounded-xl border border-teal-100 bg-teal-50/50 p-3">
          <p v-if="task.status === 'PENDING_REVIEW'" class="text-xs text-teal-700">
            当前还在审核中，直接改完重新提交即可（会覆盖上一次的预览链接）
          </p>
          <div>
            <label class="label">秀米预览链接 *</label>
            <input v-model="previewUrl" class="input" placeholder="https://xiumi.us/..." />
          </div>
          <div>
            <label class="label">备注（可选）</label>
            <input v-model="note" class="input" placeholder="有什么需要审核同学注意的" />
          </div>
          <p class="text-xs text-slate-500">
            在 {{ fmtFull(task.ddl) }} 前提交可获得按时完成分 {{ task.completeScore }} 分；
            超过 DDL 提交不获得完成分，且会扣回已到手的接取分
          </p>
          <button class="btn-primary w-full" @click="doSubmit">
            {{ task.status === 'PENDING_REVIEW' ? '重新提交' : '确认提交' }}
          </button>
        </div>

        <!-- 审核表单 -->
        <div v-if="showReview" class="mt-4 space-y-3 rounded-xl border border-amber-100 bg-amber-50/50 p-3">
          <div>
            <label class="label">评语（可选）</label>
            <textarea v-model="reviewComment" class="input min-h-16" placeholder="排版不错 / 这里需要改…" />
          </div>
          <p class="text-xs text-slate-500">
            质量好才给分的话，点"通过不给分"即可；系统只统计你给出的分。
          </p>
          <div class="flex flex-wrap gap-2">
            <button class="btn-danger flex-1" @click="doReview(false)">
              <Undo2 class="h-4 w-4" />打回修改
            </button>
            <button class="btn-primary flex-1" @click="doReview(true, true)">
              <CheckCircle2 class="h-4 w-4" />通过并给分（+{{ task.reviewScore }}）
            </button>
            <button class="btn-ghost flex-1" @click="doReview(true, false)">
              <CheckCircle2 class="h-4 w-4" />通过不给分
            </button>
          </div>
        </div>

        <!-- 指派 -->
        <div v-if="showAssign" class="mt-4 space-y-3 rounded-xl border border-slate-200 bg-slate-50 p-3">
          <select v-model="assignTo" class="input">
            <option :value="null">请选择成员</option>
            <option v-for="u in users" :key="u.id" :value="u.id">{{ u.nickname }}（{{ u.username }}）</option>
          </select>
          <button class="btn-primary w-full" @click="doAssign">确认指派</button>
        </div>
      </div>

      <!-- 时间线 -->
      <div class="card mt-4 p-4 md:p-6">
        <h2 class="mb-3 text-sm font-semibold text-slate-700">流转记录</h2>
        <ol class="space-y-3">
          <li v-for="l in logs" :key="l.id" class="flex gap-3">
            <div class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-teal-400"></div>
            <div class="min-w-0">
              <div class="text-sm text-slate-700">{{ l.detail }}</div>
              <div class="text-xs text-slate-400">{{ l.actorName || '系统' }} · {{ fmtFull(l.createdAt) }}</div>
            </div>
          </li>
        </ol>
      </div>
    </template>
  </AppLayout>
</template>
