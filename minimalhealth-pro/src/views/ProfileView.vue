<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusBar from '../components/StatusBar.vue'
import TabBar from '../components/TabBar.vue'
import { getUserProfile, type UserProfileData } from '../api/user'
const router = useRouter()
const activeTab = ref('profile')
const user = ref<UserProfileData | null>(null)

onMounted(async () => {
  try { const res = await getUserProfile(); user.value = res.data.data } catch (e) {}
})

function onTab(t: string) {
  if (t === 'dashboard') router.push('/dashboard')
  else if (t === 'timeline') router.push('/timeline')
  else if (t === 'diet') router.push('/diet')
  else if (t === 'exercise') router.push('/exercise')
  else if (t === 'profile') return
}

function handleLogout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  router.push('/')
}

const menuIcons: Record<string, string> = {
  goals: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="6"/><circle cx="12" cy="12" r="2" fill="var(--accent)"/></svg>',
  body: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>',
  reminder: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>',
  privacy: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--accent)" stroke-width="2" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>',
}
const menuItems = [
  { name: '健康目标', iconKey: 'goals', path: '/profile/goals' },
  { name: '身体数据', iconKey: 'body', path: '/profile/body' },
  { name: '提醒设置', iconKey: 'reminder', path: '/reminders' },
  { name: '隐私与数据', iconKey: 'privacy', path: '' },
]
</script>
<template>
  <div class="page profile">
    <StatusBar time="22:08" />
    <div class="content">
      <div class="profile-card">
        <div class="avatar"><span>{{ user?.avatarInitial || '用' }}</span></div>
        <div class="user-info">
          <span class="user-name">{{ user?.name || '未设置姓名' }}</span>
          <span class="user-email">{{ user?.email || '' }}</span>
        </div>
        <span class="arrow">›</span>
      </div>
      <div class="menu">
        <button class="menu-item" v-for="m in menuItems" :key="m.name" @click="m.path && router.push(m.path)">
          <span class="menu-icon" v-html="menuIcons[m.iconKey]"></span>
          <span class="menu-name">{{ m.name }}</span>
          <span class="menu-arrow">›</span>
        </button>
      </div>
      <button class="logout" @click="handleLogout">退出登录</button>
    </div>
    <TabBar :activeTab="activeTab" @update:activeTab="onTab" />
  </div>
</template>
<style scoped>
.profile { width: 100%; height: 100vh; height: 100dvh; background: var(--bg-primary); display: flex; flex-direction: column; }
.content { flex: 1; overflow-y: auto; -webkit-overflow-scrolling: touch; padding: 0 24px; display: flex; flex-direction: column; gap: 28px; padding-bottom: 100px; }
.profile-card {
  display: flex; align-items: center; gap: 16px;
  background: var(--bg-white); border-radius: var(--radius-xl); padding: 24px; margin-top: 24px;
}
.avatar {
  width: 64px; height: 64px; border-radius: 32px; background: var(--bg-light-green);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; color: var(--accent); font-weight: var(--font-medium);
}
.user-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.user-name { font-size: 18px; font-weight: var(--font-medium); color: var(--text-primary); }
.user-email { font-size: 14px; color: var(--text-secondary); }
.arrow { font-size: 24px; color: var(--text-placeholder); }
.menu { background: var(--bg-white); border-radius: var(--radius-lg); padding: 8px; display: flex; flex-direction: column; gap: 2px; }
.menu-item {
  display: flex; align-items: center; gap: 14px; padding: 14px 18px;
  border: none; background: none; border-radius: var(--radius-sm);
  cursor: pointer; font-family: Inter, sans-serif; text-align: left; width: 100%;
}
.menu-item:hover { background: var(--bg-light); }
.menu-icon { font-size: 18px; }
.menu-name { flex: 1; font-size: 15px; font-weight: var(--font-medium); color: var(--text-primary); }
.menu-arrow { font-size: 18px; color: var(--text-placeholder); }
.logout {
  width: 100%; height: 52px; background: var(--bg-white); border: none;
  border-radius: var(--radius-md); color: var(--error); font-size: 15px;
  font-weight: var(--font-medium); font-family: Inter, sans-serif; cursor: pointer;
}
</style>
