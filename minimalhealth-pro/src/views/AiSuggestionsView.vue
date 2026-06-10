<script setup lang="ts">
import { ref, onMounted } from 'vue'
import StatusBar from '../components/StatusBar.vue'
import { getSuggestions, type AiSuggestionsData } from '../api/ai'
const data = ref<AiSuggestionsData | null>(null)

const iconSvgs: Record<string, string> = {
  walking: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="13" cy="4" r="2" fill="var(--accent)"/><path d="M7 21l3-6 3 1 2-5 3 2"/><path d="M12 13l-3-2-2 3"/></svg>',
  sleep: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>',
  water: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round"><path d="M12 2c-5.33 4.55-8 8.48-8 11.8 0 4.98 3.8 8.2 8 8.2s8-3.22 8-8.2c0-3.32-2.67-7.25-8-11.8Z"/><path d="M12 8v4l2.5 2.5"/></svg>',
  general: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="1.5" stroke-linecap="round"><path d="M12 3l1.5 5.5L19 10l-5.5 1.5L12 17l-1.5-5.5L5 10l5.5-1.5z"/><line x1="19" y1="17" x2="19" y2="21"/><line x1="17" y1="19" x2="21" y2="19"/></svg>',
}

onMounted(async () => {
  try { const res = await getSuggestions(); data.value = res.data.data } catch (e) {}
})
</script>
<template>
  <div class="page ai">
    <StatusBar time="08:20" />
    <div class="content">
      <div class="header">
        <span class="greeting">{{ data?.greeting || '早上好' }}</span>
        <h2>今日健康建议</h2>
      </div>
      <div class="cards">
        <div class="card" v-for="s in (data?.suggestions || [])" :key="s.title">
          <div class="card-icon" v-html="iconSvgs[s.iconKey] || iconSvgs.general"></div>
          <div class="card-body">
            <span class="card-title">{{ s.title }}</span>
            <p class="card-desc">{{ s.description }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.ai { width: 100%; height: 100vh; height: 100dvh; background: var(--bg-primary); display: flex; flex-direction: column; }
.content { flex: 1; overflow-y: auto; padding: 0 24px; display: flex; flex-direction: column; gap: 24px; padding-bottom: 24px; }
.header { display: flex; flex-direction: column; gap: 8px; padding-top: 24px; }
.greeting { font-size: 14px; color: var(--text-secondary); }
.header h2 { font-size: 28px; font-weight: var(--font-light); color: var(--text-primary); }
.cards { display: flex; flex-direction: column; gap: 16px; }
.card {
  display: flex; gap: 14px; background: var(--bg-white);
  border-radius: var(--radius-lg); padding: 18px;
}
.card-icon { width: 40px; height: 40px; background: var(--bg-light-green); border-radius: var(--radius-sm); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.card-body { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.card-title { font-size: 15px; font-weight: var(--font-medium); color: var(--text-primary); }
.card-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
</style>
