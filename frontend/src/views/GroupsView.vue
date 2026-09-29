<script setup>
import { ref, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { api } from '../api'
import { toast } from '../store'
import { FolderPlus, Trash2 } from 'lucide-vue-next'

const groups = ref([])
const users = ref([])
const editing = ref(null)
const form = ref({ name: '', leaderId: null, description: '', memberIds: [] })
const submitting = ref(false)

async function load() {
  try {
    groups.value = await api.groups()
    users.value = await api.usersSimple()
  } catch (e) {
    toast(e.message, 'error')
  }
}

function startCreate() {
  editing.value = 'new'
  form.value = { name: '', leaderId: null, description: '', memberIds: [] }
}

function startEdit(g) {
  editing.value = g.id
  form.value = {
    name: g.name,
    leaderId: g.leaderId,
    description: g.description || '',
    memberIds: (g.members || []).map((m) => m.id)
  }
}

async function save() {
  if (!form.value.name.trim()) {
    toast('请填写组名', 'error')
    return
  }
  submitting.value = true
  try {
    if (editing.value === 'new') {
      await api.createGroup(form.value)
      toast('小组已创建', 'success')
    } else {
      await api.updateGroup(editing.value, form.value)
      toast('已保存', 'success')
    }
    editing.value = null
    await load()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function remove(g) {
  if (!confirm(`确定删除小组「${g.name}」？成员会被移出该组。`)) return
  try {
    await api.deleteGroup(g.id)
    toast('已删除', 'success')
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

function toggleMember(id) {
  const i = form.value.memberIds.indexOf(id)
  if (i >= 0) form.value.memberIds.splice(i, 1)
  else form.value.memberIds.push(id)
}

onMounted(load)
</script>

<template>
  <AppLayout>
    <div class="mb-3 flex items-center justify-between">
      <h2 class="text-sm font-semibold text-slate-700">小组管理</h2>
      <button class="btn-primary text-xs" @click="startCreate">
        <FolderPlus class="h-4 w-4" />新建小组
      </button>
    </div>
    <p class="mb-4 text-xs text-slate-500">
      组长可以是管理员，也可以是普通成员；组长同样可以发布任务。小组主要用于分组展示与分组排名。
    </p>

    <!-- 编辑表单 -->
    <div v-if="editing" class="card mb-4 space-y-3 p-4">
      <div>
        <label class="label">组名 *</label>
        <input v-model="form.name" class="input" placeholder="例：排版组" />
      </div>
      <div>
        <label class="label">组长（管理员或成员均可）</label>
        <select v-model="form.leaderId" class="input">
          <option :value="null">暂不设置</option>
          <option v-for="u in users" :key="u.id" :value="u.id">
            {{ u.nickname }}（{{ u.role === 'ADMIN' ? '管理员' : '成员' }}）
          </option>
        </select>
      </div>
      <div>
        <label class="label">简介</label>
        <input v-model="form.description" class="input" placeholder="负责…" />
      </div>
      <div>
        <label class="label">组成员</label>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="u in users"
            :key="u.id"
            type="button"
            class="tag border"
            :class="form.memberIds.includes(u.id)
              ? 'border-teal-500 bg-teal-50 text-teal-700'
              : 'border-slate-200 text-slate-500'"
            @click="toggleMember(u.id)"
          >
            {{ u.nickname }}
          </button>
        </div>
      </div>
      <div class="flex gap-2">
        <button class="btn-ghost flex-1" @click="editing = null">取消</button>
        <button class="btn-primary flex-1" :disabled="submitting" @click="save">保存</button>
      </div>
    </div>

    <!-- 列表 -->
    <div class="space-y-3">
      <div v-for="g in groups" :key="g.id" class="card p-4">
        <div class="flex items-start justify-between gap-2">
          <div class="min-w-0">
            <div class="font-semibold text-slate-800">{{ g.name }}</div>
            <div class="mt-0.5 text-xs text-slate-500">
              组长 {{ g.leaderName || '未设置' }} · {{ g.memberCount }} 人 · 总分 {{ g.totalScore }}
            </div>
            <div v-if="g.description" class="mt-1 text-xs text-slate-400">{{ g.description }}</div>
          </div>
          <div class="flex shrink-0 gap-1">
            <button class="btn-ghost px-2 py-1 text-xs" @click="startEdit(g)">编辑</button>
            <button class="btn-ghost px-2 py-1 text-xs text-rose-600" @click="remove(g)">
              <Trash2 class="h-3.5 w-3.5" />
            </button>
          </div>
        </div>
        <div v-if="g.members && g.members.length" class="mt-2 flex flex-wrap gap-1">
          <span v-for="m in g.members" :key="m.id" class="tag bg-slate-100 text-slate-600">
            {{ m.nickname }}
          </span>
        </div>
      </div>
      <div v-if="!groups.length" class="card py-14 text-center text-sm text-slate-400">还没有小组</div>
    </div>
  </AppLayout>
</template>
