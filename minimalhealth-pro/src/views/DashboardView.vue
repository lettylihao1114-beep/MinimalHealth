<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBar from '../components/StatusBar.vue'
import TabBar from '../components/TabBar.vue'
import { getDashboard, updateStats, type DashboardData } from '../api/dashboard'
const router = useRouter()
const activeTab = ref('dashboard')
const dashboard = ref<DashboardData | null>(null)
const showFab = ref(false)
const editingHr = ref(false)
const editingSleep = ref(false)
const editHrValue = ref(0)
const editSleepValue = ref(0.0)

onMounted(async () => {
  try {
    const res = await getDashboard()
    dashboard.value = res.data.data
  } catch (e) { /* use defaults */ }
})

function startEditHr() {
  editHrValue.value = dashboard.value?.stats?.restingHr || 0
  editingHr.value = true
}

function cancelEditHr() {
  editingHr.value = false
}

async function confirmEditHr() {
  if (editHrValue.value < 30 || editHrValue.value > 220) { alert('心率范围 30~220'); return }
  try {
    const res = await updateStats(editHrValue.value, undefined)
    if (dashboard.value) {
      dashboard.value.stats.restingHr = res.data.data.restingHr
      dashboard.value.healthScore = res.data.data.healthScore
    }
    editingHr.value = false
  } catch (e: any) { alert('更新失败') }
}

function startEditSleep() {
  editSleepValue.value = dashboard.value?.stats?.sleepHours || 0
  editingSleep.value = true
}

function cancelEditSleep() {
  editingSleep.value = false
}

async function confirmEditSleep() {
  if (editSleepValue.value < 0 || editSleepValue.value > 24) { alert('睡眠时长 0~24 小时'); return }
  try {
    const res = await updateStats(undefined, editSleepValue.value)
    if (dashboard.value) {
      dashboard.value.stats.sleepHours = res.data.data.sleepHours
      dashboard.value.healthScore = res.data.data.healthScore
    }
    editingSleep.value = false
  } catch (e: any) { alert('更新失败') }
}

function onTab(t: string) {
  if (t === 'dashboard') return
  if (t === 'timeline') router.push('/timeline')
  else if (t === 'diet') router.push('/diet')
  else if (t === 'exercise') router.push('/exercise')
  else if (t === 'profile') router.push('/profile')
}

function getScoreLabel(score: number): string {
  if (score >= 90) return '优秀'
  if (score >= 75) return '良好'
  if (score >= 60) return '一般'
  return '需改善'
}
</script>
<template>
  <div class="page dashboard">
    <StatusBar time="9:41" />
    <div class="content">
      <div class="header">
        <span class="greeting">{{ dashboard?.greeting || '下午好' }}</span>
        <h2 class="today-title">今日</h2>
      </div>
      <div class="score-card">
        <div class="score-left">
          <span class="label">健康指数</span>
          <span class="number">{{ dashboard?.healthScore ?? 87 }}</span>
          <span class="unit">/ 100 · {{ getScoreLabel(dashboard?.healthScore ?? 87) }}</span>
        </div>
        <div class="ring">
          <svg width="88" height="88" viewBox="0 0 88 88">
            <circle cx="44" cy="44" r="38" stroke="#E8EBED" stroke-width="8" fill="none"/>
            <circle cx="44" cy="44" r="38" stroke="#8FA89B" stroke-width="8" fill="none"
              :stroke-dasharray="239" :stroke-dashoffset="239 - 239 * (dashboard?.healthScore ?? 87) / 100"
              stroke-linecap="round" transform="rotate(-90 44 44)"/>
          </svg>
        </div>
      </div>
      <div class="stats-row">
        <div class="stat-card">
          <span class="stat-icon"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#8FA89B" stroke-width="1.5"><circle cx="13" cy="6" r="2"/><path d="M7 23l3-7 2 2v7"/><path d="M13 13l2-2 3 1.5V16"/></svg></span>
          <span class="stat-value">{{ (dashboard?.stats?.steps ?? 0).toLocaleString() }}</span>
          <span class="stat-label">步数</span>
        </div>
        <div class="stat-card clickable" @click="startEditHr()">
          <span class="stat-icon"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#E57373" stroke-width="1.5"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg></span>
          <template v-if="!editingHr">
            <span class="stat-value">{{ dashboard?.stats?.restingHr || '--' }}</span>
            <span class="stat-label">静息心率</span>
          </template>
          <template v-else>
            <div class="edit-inline">
              <input class="edit-input" type="number" v-model.number="editHrValue" min="30" max="220" placeholder="bpm" @keyup.enter="confirmEditHr" @keyup.escape="cancelEditHr" />
              <div class="edit-actions">
                <button class="edit-confirm" @click.stop="confirmEditHr">✓</button>
                <button class="edit-cancel" @click.stop="cancelEditHr">✕</button>
              </div>
            </div>
          </template>
        </div>
        <div class="stat-card clickable" @click="startEditSleep()">
          <span class="stat-icon"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#8FA89B" stroke-width="1.5"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg></span>
          <template v-if="!editingSleep">
            <span class="stat-value">{{ dashboard?.stats?.sleepHours != null ? dashboard.stats.sleepHours + 'h' : '--' }}</span>
            <span class="stat-label">睡眠</span>
          </template>
          <template v-else>
            <div class="edit-inline">
              <input class="edit-input" type="number" v-model.number="editSleepValue" min="0" max="24" step="0.5" placeholder="小时" @keyup.enter="confirmEditSleep" @keyup.escape="cancelEditSleep" />
              <div class="edit-actions">
                <button class="edit-confirm" @click.stop="confirmEditSleep">✓</button>
                <button class="edit-cancel" @click.stop="cancelEditSleep">✕</button>
              </div>
            </div>
          </template>
        </div>
      </div>
      <div class="activity-section">
        <div class="act-header">
          <span class="act-title">今日活动</span>
          <span class="view-all" @click="router.push('/timeline')">查看全部</span>
        </div>
        <div class="act-item" v-for="a in (dashboard?.activities || [])" :key="a.id">
          <div class="act-icon" v-html="activityIcons[a.activityType] || activityIcons.exercise"></div>
          <div class="act-info"><span class="act-name">{{ a.name }}</span><span class="act-time">{{ a.time }}</span></div>
          <span class="act-val">{{ a.valueLabel }}</span>
        </div>
        <div class="act-empty" v-if="(dashboard?.activities || []).length === 0">
          <span class="empty-text">暂无活动记录</span>
        </div>
      </div>
    </div>
    <!-- FAB 快速添加 -->
    <div class="fab-container">
      <div class="fab-menu" v-if="showFab">
        <button class="fab-item" @click="showFab=false; router.push('/diet')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round"><path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/><path d="M7 2v20"/><path d="M21 15V2v0a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Z"/></svg>
          记饮食
        </button>
        <button class="fab-item" @click="showFab=false; router.push('/exercise/running')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="13.5" cy="5.5" r="2" fill="var(--accent)"/><path d="M7 23l3-7 2 2v7"/><path d="M13 13l2-2 3 1.5V16"/><path d="M18 7.5A6.5 6.5 0 0 1 21 11"/></svg>
          开始跑步
        </button>
        <button class="fab-item" @click="showFab=false; router.push('/water')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8Z"/><path d="M12 8v4l2.5 2.5"/></svg>
          记饮水
        </button>
      </div>
      <button class="fab" @click="showFab=!showFab">{{ showFab ? '×' : '+' }}</button>
    </div>
    <TabBar :activeTab="activeTab" @update:activeTab="onTab" />
  </div>
</template>
<script lang="ts">
const activityIcons: Record<string, string> = {
  exercise: '<svg width="16" height="16" viewBox="0 0 24 24" fill="#8FA89B"><path d="M13.5 5.5a2 2 0 1 0 0-4 2 2 0 0 0 0 4ZM7 23l3-7 2 2v7h2v-8l-2.33-2.33L13 13l5 3.5V20h2v-5l-4.67-3.13L16 8a3 3 0 0 0-5.66-1L6 14v5h2v-5l2-2-1 11Z"/></svg>',
  water: '<svg width="16" height="16" viewBox="0 0 24 24" fill="#8FA89B"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8Z"/></svg>',
  meditation: '<svg width="16" height="16" viewBox="0 0 24 24" fill="#8FA89B"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6" stroke="#8FA89B" stroke-width="1.5" fill="none"/></svg>',
  meal: '<svg width="16" height="16" viewBox="0 0 24 24" fill="#8FA89B"><path d="M8 1v7a3 3 0 0 1-6 0V1m6 0v7m7-3v11a2 2 0 0 1-2 2h-3a2 2 0 0 1-2-2V5m7 0h1a3 3 0 0 1 3 3v1a3 3 0 0 1-3 3h-1m0-7v7m-5-2a4 4 0 1 0 8 0"/></svg>',
  sleep: '<svg width="16" height="16" viewBox="0 0 24 24" fill="#8FA89B"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
}
</script>
<style scoped>
.dashboard { width: 100%; height: 100vh; height: 100dvh; background: var(--bg-primary); display: flex; flex-direction: column; }
.content { flex: 1; overflow-y: auto; -webkit-overflow-scrolling: touch; padding: 0 24px; display: flex; flex-direction: column; gap: 32px; padding-bottom: 100px; }
.header { display: flex; flex-direction: column; gap: 4px; padding-top: 24px; }
.greeting { font-size: 16px; color: var(--text-secondary); }
.today-title { font-size: 36px; font-weight: var(--font-light); color: var(--text-primary); }
.score-card {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--bg-white); border-radius: var(--radius-xl); padding: 24px;
}
.score-left { display: flex; flex-direction: column; gap: 8px; }
.label { font-size: 14px; color: var(--text-secondary); }
.number { font-size: 48px; font-weight: var(--font-light); color: var(--accent); line-height: 1; }
.unit { font-size: 13px; color: var(--text-secondary); }
.stats-row { display: flex; gap: 12px; }
.stat-card {
  flex: 1; background: var(--bg-white); border-radius: var(--radius-md);
  padding: 20px; display: flex; flex-direction: column; gap: 8px;
}
.stat-icon :deep(svg) { display: block; }
.stat-value { font-size: 22px; font-weight: var(--font-light); color: var(--text-primary); }
.stat-label { font-size: 12px; color: var(--text-secondary); }
.stat-card.clickable { cursor: pointer; transition: transform 0.1s; }
.stat-card.clickable:active { transform: scale(0.97); }
.edit-inline { display: flex; flex-direction: column; gap: 6px; }
.edit-input { width: 100%; padding: 6px 8px; border: 1px solid var(--accent); border-radius: var(--radius-sm); font-size: 18px; font-family: Inter, sans-serif; text-align: center; outline: none; }
.edit-actions { display: flex; gap: 6px; justify-content: center; }
.edit-confirm, .edit-cancel { width: 28px; height: 28px; border: none; border-radius: 14px; font-size: 14px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.edit-confirm { background: var(--accent); color: #fff; }
.edit-cancel { background: var(--bg-light); color: var(--text-secondary); }
.activity-section { display: flex; flex-direction: column; gap: 16px; }
.act-header { display: flex; align-items: center; justify-content: space-between; }
.act-title { font-size: 18px; font-weight: var(--font-medium); color: var(--text-primary); }
.view-all { font-size: 13px; color: var(--accent); cursor: pointer; }
.act-item {
  display: flex; align-items: center; gap: 14px;
  background: var(--bg-white); border-radius: var(--radius-md); padding: 18px;
}
.act-icon { width: 36px; height: 36px; background: var(--bg-light-green); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; }
.act-info { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.act-name { font-size: 15px; color: var(--text-primary); font-weight: var(--font-medium); }
.act-time { font-size: 13px; color: var(--text-secondary); }
.act-val { font-size: 14px; color: var(--accent); font-weight: var(--font-medium); }
.act-empty { text-align: center; padding: 32px 0; }
.empty-text { font-size: 14px; color: var(--text-placeholder); }
.fab-container { position: absolute; bottom: 110px; right: 24px; display: flex; flex-direction: column; align-items: flex-end; gap: 12px; }
.fab-menu { display: flex; flex-direction: column; gap: 8px; }
.fab-item { padding: 12px 20px; background: var(--bg-white); border: none; border-radius: var(--radius-pill); font-size: 14px; font-family: Inter, sans-serif; color: var(--text-primary); cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.fab { width: 56px; height: 56px; border-radius: 28px; background: var(--accent); border: none; color: #fff; font-size: 28px; cursor: pointer; box-shadow: 0 4px 12px rgba(0,0,0,0.15); display: flex; align-items: center; justify-content: center; }
</style>
