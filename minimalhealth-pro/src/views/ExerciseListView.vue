<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBar from '../components/StatusBar.vue'
import TabBar from '../components/TabBar.vue'
import { getExercises, startExercise, finishExercise, type ExerciseListData } from '../api/exercise'
const router = useRouter()
const activeTab = ref('exercise')
const data = ref<ExerciseListData | null>(null)
const showAdd = ref(false)
const newEx = ref({ exerciseType: '跑步', durationMin: 30, calories: 200, distanceKm: 3, routeDesc: '' })

onMounted(async () => { await loadData() })
async function loadData() {
  try { const res = await getExercises(); data.value = res.data.data } catch (e) {}
}

async function handleAdd() {
  if (!newEx.value.exerciseType) return
  try {
    const r1 = await startExercise(newEx.value.exerciseType)
    const sid = r1.data.data.sessionId
    await finishExercise(sid, {
      durationMin: newEx.value.durationMin,
      calories: newEx.value.calories,
      distanceKm: newEx.value.distanceKm,
      routeDesc: newEx.value.routeDesc
    })
    newEx.value = { exerciseType: '跑步', durationMin: 30, calories: 200, distanceKm: 3, routeDesc: '' }
    showAdd.value = false
    await loadData()
  } catch (e: any) { alert('添加失败') }
}

function onTab(t: string) {
  if (t === 'dashboard') router.push('/dashboard')
  else if (t === 'timeline') router.push('/timeline')
  else if (t === 'diet') router.push('/diet')
  else if (t === 'exercise') return
  else if (t === 'profile') router.push('/profile')
}

const exTypes = ['跑步', '瑜伽', '骑行', '游泳', '步行', '力量训练']
</script>
<template>
  <div class="page exercise-list">
    <StatusBar time="08:00" />
    <div class="content">
      <h2>运动</h2>
      <button class="start-run-btn" @click="router.push('/exercise/running')">
        <span class="run-icon"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="13.5" cy="5.5" r="2" fill="white"/><path d="M7 23l3-7 2 2v7"/><path d="M13 13l2-2 3 1.5V16"/><path d="M18 7.5A6.5 6.5 0 0 1 21 11"/></svg></span>
        <span>开始跑步</span>
        <span class="arrow">→</span>
      </button>
      <div class="week-stats">
        <div class="ws-card"><span class="ws-val">{{ data?.weeklyStats?.count ?? 0 }}</span><span class="ws-label">次</span></div>
        <div class="ws-card"><span class="ws-val">{{ (data?.weeklyStats?.totalDistanceKm ?? 0).toFixed(1) }}</span><span class="ws-label">公里</span></div>
        <div class="ws-card"><span class="ws-val">{{ (data?.weeklyStats?.totalCalories ?? 0).toLocaleString() }}</span><span class="ws-label">千卡</span></div>
      </div>
      <div class="history">
        <div class="h-item" v-for="r in (data?.records || [])" :key="r.id">
          <div class="h-icon" :class="r.exerciseType === '瑜伽' ? 'yoga' : 'run'"></div>
          <div class="h-info">
            <span class="h-name">{{ r.exerciseType }}</span>
            <span class="h-meta">{{ r.distanceKm ? r.distanceKm + 'km · ' : '' }}{{ r.routeDesc || r.calories + 'kcal' }}</span>
          </div>
          <span class="h-badge">{{ r.status === 'in_progress' ? '进行中' : r.recordDate }}</span>
        </div>
        <div class="h-empty" v-if="(data?.records || []).length === 0"><span class="empty-text">本周暂无运动记录</span></div>
      </div>

      <!-- 添加运动 -->
      <button class="add-btn" @click="showAdd=!showAdd">{{ showAdd ? '取消' : '+ 添加运动' }}</button>
      <div class="add-form" v-if="showAdd">
        <div class="type-row">
          <button v-for="t in exTypes" :key="t" :class="{ active: newEx.exerciseType === t }" @click="newEx.exerciseType = t">{{ t }}</button>
        </div>
        <div class="row-2">
          <input type="number" v-model.number="newEx.durationMin" placeholder="时长(分钟)" />
          <input type="number" v-model.number="newEx.calories" placeholder="热量(kcal)" />
        </div>
        <div class="row-2">
          <input type="number" v-model.number="newEx.distanceKm" placeholder="距离(km)" step="0.1" />
          <input v-model="newEx.routeDesc" placeholder="路线(可选)" />
        </div>
        <button class="submit-btn" @click="handleAdd">确认添加</button>
      </div>
    </div>
    <TabBar :activeTab="activeTab" @update:activeTab="onTab" />
  </div>
</template>
<style scoped>
.exercise-list { width: 100%; height: 100vh; height: 100dvh; background: var(--bg-primary); display: flex; flex-direction: column; }
.content { flex: 1; overflow-y: auto; -webkit-overflow-scrolling: touch; -webkit-overflow-scrolling: touch; padding: 0 24px 100px; display: flex; flex-direction: column; gap: 16px; }
.content h2 { font-size: 30px; font-weight: var(--font-light); color: var(--text-primary); padding-top: 24px; }
.start-run-btn {
  width: 100%; padding: 18px;
  background: linear-gradient(135deg, #8FA89B, #6B8A7D);
  border: none; border-radius: var(--radius-lg);
  display: flex; align-items: center; justify-content: center; gap: 12px;
  color: #fff; font-size: 18px; font-weight: 500;
  font-family: Inter, sans-serif; cursor: pointer;
}
.run-icon { font-size: 24px; }
.arrow { font-size: 18px; opacity: 0.7; }
.week-stats { display: flex; gap: 12px; }
.ws-card { flex: 1; background: var(--bg-white); border-radius: var(--radius-md); padding: 18px; display: flex; flex-direction: column; gap: 4px; }
.ws-val { font-size: 22px; font-weight: var(--font-light); color: var(--text-primary); }
.ws-label { font-size: 13px; color: var(--text-secondary); }
.history { display: flex; flex-direction: column; gap: 12px; }
.h-item { display: flex; align-items: center; gap: 14px; background: var(--bg-white); border-radius: var(--radius-md); padding: 18px; }
.h-icon { width: 44px; height: 44px; border-radius: var(--radius-sm); }
.h-icon.run { background: #E8F5E9; }
.h-icon.yoga { background: #F3E5F5; }
.h-info { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.h-name { font-size: 15px; font-weight: var(--font-medium); color: var(--text-primary); }
.h-meta { font-size: 13px; color: var(--text-secondary); }
.h-badge { font-size: 12px; padding: 4px 12px; background: var(--bg-light); border-radius: 10px; color: var(--text-secondary); }
.h-empty { text-align: center; padding: 48px 0; }
.empty-text { font-size: 14px; color: var(--text-placeholder); }
.add-btn { width: 100%; padding: 14px; background: var(--bg-white); border: 2px dashed var(--border); border-radius: var(--radius-md); font-size: 15px; font-family: Inter, sans-serif; color: var(--accent); cursor: pointer; }
.add-form { background: var(--bg-white); border-radius: var(--radius-lg); padding: 18px; display: flex; flex-direction: column; gap: 12px; }
.type-row { display: flex; flex-wrap: wrap; gap: 8px; }
.type-row button { padding: 8px 14px; border: none; border-radius: var(--radius-sm); background: var(--bg-light); font-size: 13px; font-family: Inter, sans-serif; cursor: pointer; color: var(--text-secondary); }
.type-row button.active { background: var(--accent); color: #fff; }
.row-2 { display: flex; gap: 12px; }
.row-2 input { flex: 1; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; font-family: Inter, sans-serif; }
.submit-btn { width: 100%; padding: 14px; background: var(--accent); border: none; border-radius: var(--radius-sm); color: #fff; font-size: 15px; font-family: Inter, sans-serif; cursor: pointer; }
</style>
