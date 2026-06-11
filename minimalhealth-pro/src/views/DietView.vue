<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBar from '../components/StatusBar.vue'
import TabBar from '../components/TabBar.vue'
import { getDiet, addMeal, deleteMeal, type DietData } from '../api/diet'
const router = useRouter()
const activeTab = ref('diet')
const diet = ref<DietData | null>(null)
const showAdd = ref(false)
const newMeal = ref({ mealType: 'breakfast', calories: 0 })
const selectedFoods = ref(new Set<string>())

const referenceFoods = [
  { name: '米饭', kcal: 116, cat: '主食' },
  { name: '面条', kcal: 110, cat: '主食' },
  { name: '馒头', kcal: 223, cat: '主食' },
  { name: '全麦面包', kcal: 68, cat: '主食' },
  { name: '燕麦粥', kcal: 68, cat: '主食' },
  { name: '鸡胸肉', kcal: 167, cat: '蛋白质' },
  { name: '鸡蛋', kcal: 72, cat: '蛋白质' },
  { name: '三文鱼', kcal: 208, cat: '蛋白质' },
  { name: '豆腐', kcal: 81, cat: '蛋白质' },
  { name: '牛奶', kcal: 42, cat: '蛋白质' },
  { name: '西兰花', kcal: 34, cat: '蔬菜' },
  { name: '番茄', kcal: 19, cat: '蔬菜' },
  { name: '黄瓜', kcal: 15, cat: '蔬菜' },
  { name: '菠菜', kcal: 23, cat: '蔬菜' },
  { name: '苹果', kcal: 52, cat: '水果' },
  { name: '香蕉', kcal: 89, cat: '水果' },
  { name: '橙子', kcal: 47, cat: '水果' },
  { name: '葡萄', kcal: 69, cat: '水果' },
  { name: '坚果', kcal: 553, cat: '零食' },
  { name: '黑巧克力', kcal: 546, cat: '零食' },
  { name: '沙拉碗', kcal: 120, cat: '轻食' },
  { name: '三明治', kcal: 210, cat: '轻食' },
  { name: '酸奶', kcal: 61, cat: '饮品' },
]

onMounted(async () => { await loadDiet() })

async function loadDiet() {
  try { const res = await getDiet(); diet.value = res.data.data } catch (e) {}
}

async function handleDeleteMeal(id: number) {
  if (!confirm('确定删除？')) return
  try { await deleteMeal(id); await loadDiet() } catch (e) {}
}

async function handleAddMeal() {
  if (newMeal.value.calories <= 0) { alert('请输入热量'); return }
  try {
    await addMeal(newMeal.value.mealType, newMeal.value.calories)
    newMeal.value = { mealType: 'breakfast', calories: 0 }
    selectedFoods.value.clear()
    showAdd.value = false
    await loadDiet()
  } catch (e: any) { alert('添加失败') }
}

function toggleFood(food: { name: string; kcal: number }) {
  const s = selectedFoods.value
  if (s.has(food.name)) {
    s.delete(food.name)
  } else {
    s.add(food.name)
  }
  // force reactivity
  selectedFoods.value = new Set(s)
  // recalc total
  const total = referenceFoods.filter(f => selectedFoods.value.has(f.name)).reduce((sum, f) => sum + f.kcal, 0)
  newMeal.value.calories = total
}

function onTab(t: string) {
  if (t === 'dashboard') router.push('/dashboard')
  else if (t === 'timeline') router.push('/timeline')
  else if (t === 'diet') return
  else if (t === 'exercise') router.push('/exercise')
  else if (t === 'profile') router.push('/profile')
}

const mealTypes = [
  { value: 'breakfast', label: '早餐' },
  { value: 'lunch', label: '午餐' },
  { value: 'dinner', label: '晚餐' },
  { value: 'snack', label: '加餐' },
]
</script>
<template>
  <div class="page diet">
    <StatusBar time="12:15" />
    <div class="content">
      <div class="header">
        <h2>饮食追踪</h2>
        <div class="cal-card">
          <span class="cal-label">今日摄入</span>
          <span class="cal-number">{{ (diet?.totalCalories ?? 0).toLocaleString() }}<span class="cal-total"> / {{ (diet?.calorieGoal ?? 2100).toLocaleString() }} kcal</span></span>
        </div>
      </div>
      <div class="meals">
        <div class="meal-card" v-for="m in (diet?.meals || [])" :key="m.mealType" :class="{ empty: !m.recorded }" @click="m.recorded && handleDeleteMeal(m.id)">
          <template v-if="m.recorded">
            <div class="meal-header"><span class="meal-name">{{ m.mealTypeLabel }}</span><span class="meal-cal">{{ m.calories }} kcal</span></div>
            <p class="meal-food" v-if="m.foods && m.foods !== '快速记录'">{{ m.foods }}</p>
          </template>
          <template v-else><span class="meal-name">{{ m.mealTypeLabel }}</span><span class="meal-hint">尚未记录</span></template>
        </div>
      </div>

      <!-- 添加餐食 -->
      <button class="add-btn" @click="showAdd=!showAdd">{{ showAdd ? '取消' : '+ 添加餐食' }}</button>
      <div class="add-form" v-if="showAdd">
        <div class="meal-type-row">
          <button v-for="mt in mealTypes" :key="mt.value" :class="{ active: newMeal.mealType === mt.value }" @click="newMeal.mealType = mt.value">{{ mt.label }}</button>
        </div>
        <div class="cal-row">
          <input class="cal-input" type="number" v-model.number="newMeal.calories" placeholder="输入热量 (kcal)" />
          <button class="submit-btn" @click="handleAddMeal">确认添加</button>
        </div>
        <!-- 参考热量（多选） -->
        <div class="ref-section">
          <div class="ref-header">
            <span class="ref-title">🍽️ 点击食物多选，热量自动累加</span>
            <button class="ref-clear" v-if="selectedFoods.size > 0" @click="selectedFoods.clear(); newMeal.calories = 0">清空</button>
          </div>
          <div class="ref-selected" v-if="selectedFoods.size > 0">
            <span>已选：</span>
            <span class="sel-badge" v-for="f in referenceFoods.filter(f => selectedFoods.has(f.name))" :key="f.name">{{ f.name }} +{{ f.kcal }}</span>
          </div>
          <div class="ref-grid">
            <button
              v-for="food in referenceFoods"
              :key="food.name"
              class="ref-chip"
              :class="{ active: selectedFoods.has(food.name) }"
              @click="toggleFood(food)"
            >
              <span class="ref-name">{{ food.name }}</span>
              <span class="ref-kcal">{{ food.kcal }}<span class="ref-unit">kcal</span></span>
            </button>
          </div>
        </div>
      </div>
    </div>
    <TabBar :activeTab="activeTab" @update:activeTab="onTab" />
  </div>
</template>
<style scoped>
.diet { width: 100%; height: 100vh; height: 100dvh; background: var(--bg-primary); display: flex; flex-direction: column; }
.content { flex: 1; overflow-y: auto; -webkit-overflow-scrolling: touch; padding: 0 24px; display: flex; flex-direction: column; gap: 16px; padding-bottom: 100px; }
.header { display: flex; flex-direction: column; gap: 20px; padding-top: 24px; }
.header h2 { font-size: 30px; font-weight: var(--font-light); color: var(--text-primary); }
.cal-card { display: flex; flex-direction: column; gap: 4px; }
.cal-label { font-size: 14px; color: var(--text-secondary); }
.cal-number { font-size: 48px; font-weight: var(--font-light); color: var(--text-primary); }
.cal-total { font-size: 20px; color: var(--text-secondary); }
.meals { display: flex; flex-direction: column; gap: 12px; }
.meal-card { background: var(--bg-white); border-radius: var(--radius-lg); padding: 18px; display: flex; flex-direction: column; gap: 8px; }
.meal-header { display: flex; align-items: center; justify-content: space-between; }
.meal-name { font-size: 16px; font-weight: var(--font-medium); color: var(--text-primary); }
.meal-cal { font-size: 14px; font-weight: var(--font-medium); color: var(--accent); }
.meal-food { font-size: 14px; color: var(--text-secondary); line-height: 1.5; }
.meal-card.empty { flex-direction: row; justify-content: space-between; align-items: center; }
.meal-hint { font-size: 14px; color: var(--text-placeholder); }
.add-btn { width: 100%; padding: 14px; background: var(--bg-white); border: 2px dashed var(--border); border-radius: var(--radius-md); font-size: 15px; font-family: Inter, sans-serif; color: var(--accent); cursor: pointer; }
.add-form { background: var(--bg-white); border-radius: var(--radius-lg); padding: 18px; display: flex; flex-direction: column; gap: 12px; }
.meal-type-row { display: flex; gap: 8px; }
.meal-type-row button { flex: 1; padding: 10px; border: none; border-radius: var(--radius-sm); background: var(--bg-light); font-size: 13px; font-family: Inter, sans-serif; cursor: pointer; color: var(--text-secondary); }
.meal-type-row button.active { background: var(--accent); color: #fff; }
.cal-row { display: flex; gap: 12px; }
.cal-input { flex: 1; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; font-family: Inter, sans-serif; }
.submit-btn { padding: 12px 24px; background: var(--accent); border: none; border-radius: var(--radius-sm); color: #fff; font-size: 14px; font-family: Inter, sans-serif; cursor: pointer; }
.ref-section { display: flex; flex-direction: column; gap: 8px; }
.ref-header { display: flex; align-items: center; justify-content: space-between; }
.ref-title { font-size: 13px; color: var(--text-secondary); font-weight: var(--font-medium); }
.ref-clear { font-size: 12px; color: var(--accent); background: none; border: none; font-family: Inter, sans-serif; cursor: pointer; padding: 4px 8px; }
.ref-selected { display: flex; flex-wrap: wrap; align-items: center; gap: 4px; font-size: 12px; color: var(--text-secondary); }
.sel-badge { background: var(--accent); color: #fff; padding: 2px 8px; border-radius: var(--radius-pill); font-size: 11px; }
.ref-grid { display: flex; flex-wrap: wrap; gap: 6px; }
.ref-chip { display: flex; align-items: center; gap: 4px; padding: 6px 10px; background: var(--bg-light); border: 1px solid transparent; border-radius: var(--radius-pill); font-size: 12px; font-family: Inter, sans-serif; cursor: pointer; color: var(--text-primary); transition: all 0.15s; }
.ref-chip:hover { background: var(--accent); color: #fff; }
.ref-chip.active { background: var(--accent); color: #fff; border-color: var(--accent); }
.ref-name { font-weight: var(--font-medium); }
.ref-kcal { color: var(--text-placeholder); display: flex; align-items: center; gap: 1px; }
.ref-chip:hover .ref-kcal, .ref-chip.active .ref-kcal { color: rgba(255,255,255,0.8); }
.ref-unit { font-size: 10px; }
</style>
