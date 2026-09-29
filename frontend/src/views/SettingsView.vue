<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { toast } from '../store'

const form = ref({ claimScore: 1, completeScore: 1, reviewScore: 1, autoAssignGiveClaimScore: 0 })
const saving = ref(false)

async function load() {
  try {
    form.value = await api.settings()
  } catch (e) {
    toast(e.message, 'error')
  }
}

async function save() {
  saving.value = true
  try {
    await api.saveSettings({
      claimScore: Number(form.value.claimScore),
      completeScore: Number(form.value.completeScore),
      reviewScore: Number(form.value.reviewScore),
      autoAssignGiveClaimScore: Number(form.value.autoAssignGiveClaimScore)
    })
    toast('设置已保存', 'success')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <h2 class="mb-4 text-sm font-semibold text-slate-700">系统设置</h2>

    <div class="card mb-4 space-y-4 p-4">
      <div class="grid grid-cols-3 gap-3">
        <div>
          <label class="label">默认接取分</label>
          <input v-model.number="form.claimScore" type="number" min="0" class="input" />
        </div>
        <div>
          <label class="label">默认按时完成分</label>
          <input v-model.number="form.completeScore" type="number" min="0" class="input" />
        </div>
        <div>
          <label class="label">默认审核通过分</label>
          <input v-model.number="form.reviewScore" type="number" min="0" class="input" />
        </div>
      </div>

      <div>
        <label class="label">超时自动分配时，是否也给「接取分」</label>
        <select v-model="form.autoAssignGiveClaimScore" class="input">
          <option :value="0">不给（默认：接取分只奖励主动抢单的人）</option>
          <option :value="1">给（自动分配到的人也拿接取分）</option>
        </select>
        <p class="mt-1 text-xs text-slate-400">
          无论是否给接取分，自动分配到的成员只要在 DDL 前提交并通过审核，都能拿到完成分和审核分。
        </p>
      </div>

      <button class="btn-primary w-full" :disabled="saving" @click="save">保存设置</button>
    </div>

    <div class="card p-4 text-xs leading-relaxed text-slate-500">
      <div class="mb-1 font-medium text-slate-700">规则说明</div>
      1. 管理员发布任务时设定「接取时限」和「DDL」，可单独设置该任务的三项分值；<br />
      2. 接取时限内无人接取 → 系统按成员 id 顺序自动分配，并通知本人；<br />
      3. 成员完成排版后提交秀米预览链接，DDL 前提交得按时完成分；<br />
      4. 审核请求按管理员顺序轮流分配，通过得审核分，打回需填写意见；<br />
      5. 排行榜按积分降序，同一分数按完成任务数排序。
    </div>
  </AppLayout>
</template>
