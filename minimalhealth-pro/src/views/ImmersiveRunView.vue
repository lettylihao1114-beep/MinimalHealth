<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { startExercise, getRunningStatus } from '../api/exercise'
import client from '../api/client'

const router = useRouter()

// ===== 核心状态 =====
const isRunning = ref(true)
const elapsedSeconds = ref(0)
const distanceKm = ref(0)
const heartRate = ref(0)
const pace = ref('--')
const routeName = ref('户外跑步')
const calories = ref(0)
const mapReady = ref(false)
const gpsAvailable = ref(true)

// ===== 计时器 =====
let timerInterval: number
let heartRateInterval: number
let saveInterval: number
let currentExerciseId = 0

// ===== 地图 =====
let map: L.Map | null = null
let routePolyline: L.Polyline | null = null
let currentMarker: L.Marker | null = null
const routePoints: [number, number][] = []

// ===== GPS 追踪 =====
let watchId: number | null = null

// ===== 基础常量 =====
const DEFAULT_LAT = 39.9042
const DEFAULT_LNG = 116.4074

const elapsedTime = computed(() => {
  const m = Math.floor(elapsedSeconds.value / 60).toString().padStart(2, '0')
  const s = (elapsedSeconds.value % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const calculatePace = () => {
  if (distanceKm.value <= 0) return '--'
  const totalMin = elapsedSeconds.value / 60
  const pm = Math.floor(totalMin / distanceKm.value)
  const ps = Math.round((totalMin / distanceKm.value - pm) * 60)
  return `${pm}'${ps.toString().padStart(2, '0')}"`
}

const currentTime = computed(() => {
  const d = new Date()
  return d.getHours().toString().padStart(2, '0') + ':' + d.getMinutes().toString().padStart(2, '0')
})

// ===== Haversine 距离计算 =====
function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371
  const dLat = (lat2 - lat1) * Math.PI / 180
  const dLon = (lon2 - lon1) * Math.PI / 180
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// ===== 离线 Canvas 瓦片（无网络时的后备底图） =====
const OfflineGridLayer = L.GridLayer.extend({
  createTile(coords: L.Coords) {
    const tile = document.createElement('canvas')
    const size = this.getTileSize()
    tile.width = size.x
    tile.height = size.y
    const ctx = tile.getContext('2d')!
    // 淡绿色背景，模拟地图底色
    ctx.fillStyle = '#e8efe8'
    ctx.fillRect(0, 0, size.x, size.y)
    // 街区网格线
    ctx.strokeStyle = '#cdd8cd'
    ctx.lineWidth = 0.5
    const step = 20
    for (let x = 0; x <= size.x; x += step) ctx.stroke(new Path2D(`M${x},0 L${x},${size.y}`))
    for (let y = 0; y <= size.y; y += step) ctx.stroke(new Path2D(`M0,${y} L${size.x},${y}`))
    // 粗网格（模拟主干道）
    ctx.strokeStyle = '#bcc8bc'
    ctx.lineWidth = 1
    const mainStep = 100
    for (let x = 0; x <= size.x; x += mainStep) ctx.stroke(new Path2D(`M${x},0 L${x},${size.y}`))
    for (let y = 0; y <= size.y; y += mainStep) ctx.stroke(new Path2D(`M0,${y} L${size.x},${y}`))
    return tile
  }
})

// ===== 地图初始化 =====
function initMap(lat: number, lng: number) {
  map = L.map('running-map', {
    zoomControl: false,
    attributionControl: false,
    zoom: 16,
    maxZoom: 16
  }).setView([lat, lng], 18)

  // 离线网格底图（始终可用）
  ;(new OfflineGridLayer()).addTo(map)

  // 尝试加载 OpenStreetMap 瓦片（有网时覆盖在离线网格之上）
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    crossOrigin: 'anonymous'
  }).addTo(map)

  // 自定义当前位置标记（双层圆点，亮色地图上也醒目）
  currentMarker = L.marker([lat, lng], {
    icon: L.divIcon({
      className: '',
      html: `<div style="
        position:relative;
        width:24px;height:24px;
      "><div style="
        position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);
        width:18px;height:18px;
        background:#8FA89B;
        border:3px solid #fff;
        border-radius:50%;
        box-shadow:0 0 0 3px rgba(143,168,155,0.3), 0 2px 8px rgba(0,0,0,0.3);
      "></div></div>`,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    })
  }).addTo(map)

  // 路线折线（加白色描边增强可见度）
  const polylineOptions = {
    color: '#8FA89B',
    weight: 5,
    opacity: 0.9,
    lineCap: 'round' as const,
    lineJoin: 'round' as const
  }
  // 白色描边层（提亮路线）
  L.polyline(routePoints, { ...polylineOptions, color: '#fff', weight: 9, opacity: 0.25 }).addTo(map)
  routePolyline = L.polyline(routePoints, polylineOptions).addTo(map)

  mapReady.value = true
}

// ===== GPS 追踪（真实定位） =====
function startGPSTracking() {
  if (!navigator.geolocation) {
    gpsAvailable.value = false
    return
  }

  watchId = navigator.geolocation.watchPosition(
    (pos) => {
      const pt: [number, number] = [pos.coords.latitude, pos.coords.longitude]
      addRoutePoint(pt)
      gpsAvailable.value = true
    },
    () => {
      gpsAvailable.value = false
    },
    { enableHighAccuracy: true, maximumAge: 3000, timeout: 8000 }
  )
}

// ===== 添加路线点 =====
function addRoutePoint(pt: [number, number]) {
  routePoints.push(pt)

  if (routePolyline) {
    routePolyline.setLatLngs(routePoints)
  }
  if (currentMarker) {
    currentMarker.setLatLng(pt)
  }
  if (map) {
    map.panTo(pt, { animate: true, duration: 0.5 })
  }

  // 真实距离累积
  if (routePoints.length >= 2) {
    const prev = routePoints[routePoints.length - 2]
    const seg = haversineKm(prev[0], prev[1], pt[0], pt[1])
    if (seg < 0.1) { // 过滤跳点（>100m 的跳点不计算）
      distanceKm.value += seg
    }
  }
  pace.value = calculatePace()
}

// ===== 计时控制 =====
const startTimer = () => {
  isRunning.value = true
  timerInterval = window.setInterval(() => {
    elapsedSeconds.value++
    // 模拟心率消耗（GPS 接管了距离计算）
    calories.value = Math.floor(elapsedSeconds.value * 0.15)
  }, 1000)
}

const togglePause = () => {
  if (isRunning.value) {
    clearInterval(timerInterval)
    if (watchId != null) navigator.geolocation.clearWatch(watchId)
    isRunning.value = false
  } else {
    startTimer()
    if (navigator.geolocation) startGPSTracking()
  }
}

// ===== 心率模拟 =====
const startHeartRate = () => {
  heartRate.value = 120 + Math.floor(Math.random() * 30)
  heartRateInterval = window.setInterval(() => {
    heartRate.value = 135 + Math.floor(Math.random() * 20)
  }, 3000)
}

// ===== 自动保存 =====
const autoSave = () => {
  saveInterval = window.setInterval(async () => {
    try {
      await client.post('/exercises/checkpoint', {
        exerciseId: currentExerciseId,
        elapsedSeconds: elapsedSeconds.value,
        distanceKm: distanceKm.value,
        heartRate: heartRate.value,
        calories: calories.value
      })
    } catch (e) {}
  }, 10000)
}

// ===== 结束运动 =====
const stopExercise = async () => {
  clearInterval(timerInterval)
  clearInterval(heartRateInterval)
  clearInterval(saveInterval)
  if (watchId != null) navigator.geolocation.clearWatch(watchId)

  try {
    const id = currentExerciseId
    if (!id) {
      alert('没有进行中的运动记录')
      router.push('/exercise')
      return
    }
    await client.post('/exercises/finish', {
      exerciseId: id,
      totalSeconds: elapsedSeconds.value,
      distanceKm: distanceKm.value,
      calories: calories.value,
      avgHeartRate: heartRate.value,
    })
  } catch (e: any) {
    alert(e.response?.data?.message || '保存失败')
  }

  router.push('/exercise')
}

// ===== 生命周期 =====
onMounted(async () => {
  // 尝试创建新运动，如果有进行中的则沿用已有记录
  try {
    const res = await startExercise('户外跑步')
    currentExerciseId = res.data.data.sessionId
  } catch (e: any) {
    if (e.response?.data?.message === '已有进行中的运动') {
      try {
        const statusRes = await getRunningStatus()
        currentExerciseId = statusRes.data.data?.id || 0
      } catch (_) {}
    } else {
      const errMsg = e.response?.data?.message || e.message || '未知错误'
      alert('运动创建失败: ' + errMsg)
      currentExerciseId = 0
    }
  }

  // 获取位置 → 初始化地图 → 启动 GPS 追踪
  navigator.geolocation
    ? navigator.geolocation.getCurrentPosition(
        (pos) => onMapReady(pos.coords.latitude, pos.coords.longitude, true),
        () => onMapReady(DEFAULT_LAT, DEFAULT_LNG, false),
        { enableHighAccuracy: true, timeout: 5000 }
      )
    : onMapReady(DEFAULT_LAT, DEFAULT_LNG, false)

  startTimer()
  startHeartRate()
  autoSave()
})

function onMapReady(lat: number, lng: number, gpsOk: boolean) {
  gpsAvailable.value = gpsOk
  requestAnimationFrame(() => {
    initMap(lat, lng)
    requestAnimationFrame(() => {
      if (gpsOk) startGPSTracking()
    })
  })
}

onUnmounted(() => {
  clearInterval(timerInterval)
  clearInterval(heartRateInterval)
  clearInterval(saveInterval)
  if (watchId != null) navigator.geolocation.clearWatch(watchId)
  map?.remove()
})
</script>

<template>
  <div class="page immersive-run">
    <!-- 顶部状态 -->
    <div class="top-bar">
      <span class="time">{{ currentTime }}</span>
      <span class="route-badge">{{ routeName }} · 进行中</span>
    </div>

    <!-- Leaflet 地图（替换 SVG 假路线） -->
    <div id="running-map" class="map-area"></div>

    <!-- GPS 状态提示 -->
    <div class="gps-status" v-if="!gpsAvailable">
      <span class="gps-dot"></span>
      <span>GPS 定位未开启 — 请在系统设置中授权位置权限，或使用模拟定位继续跑步</span>
    </div>

    <!-- 核心数据 -->
    <div class="info-area">
      <span class="elapsed-label">已运动</span>
      <span class="timer">{{ elapsedTime }}</span>

      <div class="metrics">
        <div class="metric">
          <span class="m-val">{{ distanceKm.toFixed(1) }}</span>
          <span class="m-unit">公里</span>
        </div>
        <div class="metric">
          <span class="m-val">{{ pace }}</span>
          <span class="m-unit">配速</span>
        </div>
        <div class="metric">
          <span class="m-val" :class="{ high: heartRate > 160 }">{{ heartRate }}</span>
          <span class="m-unit">心率</span>
        </div>
      </div>

      <!-- 控制 -->
      <div class="controls">
        <button class="ctrl-btn pause-btn" @click="togglePause">
          <svg v-if="isRunning" width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <rect x="6" y="4" width="4" height="16" rx="1"/><rect x="14" y="4" width="4" height="16" rx="1"/>
          </svg>
          <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <polygon points="6,3 20,12 6,21"/>
          </svg>
        </button>
        <button class="ctrl-btn stop-btn" @click="stopExercise">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="white">
            <rect x="6" y="6" width="12" height="12" rx="2"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.immersive-run {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: var(--bg-primary);
  display: flex; flex-direction: column;
}

.top-bar {
  padding: 16px 24px;
  display: flex; align-items: center; justify-content: space-between;
  flex-shrink: 0;
  z-index: 10;
}
.time { font-size: 15px; color: var(--text-secondary); font-weight: var(--font-medium); }
.route-badge {
  padding: 6px 14px;
  background: var(--bg-light-green);
  border-radius: var(--radius-pill);
  font-size: 12px; color: var(--accent); font-weight: 500;
}

/* Leaflet 地图容器 */
#running-map {
  flex: 1;
  width: 100%;
  z-index: 1;
  margin: 0 12px;
  width: calc(100% - 24px);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

/* 地图内部的 Leaflet 控件样式覆盖 */
:deep(.leaflet-control-zoom) { display: none; }
:deep(.leaflet-control-attribution) { display: none !important; }

/* GPS 状态提示 */
.gps-status {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 8px 16px;
  margin: 8px 24px 0;
  background: #FFF8E1;
  border-radius: var(--radius-pill);
  font-size: 12px; color: #F57F17;
  flex-shrink: 0;
}
.gps-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #F57F17;
  animation: pulse-dot 1.5s infinite;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

/* 核心数据区域 - 卡片浮层风格 */
.info-area {
  display: flex; flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px 24px 32px;
  margin: 12px 12px 16px;
  flex-shrink: 0;
  background: var(--bg-white);
  border-radius: var(--radius-xl);
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}

.elapsed-label {
  font-size: 13px; color: var(--text-secondary);
  letter-spacing: 0.5px;
}
.timer {
  font-size: 64px; font-weight: 200;
  letter-spacing: 4px;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
  line-height: 1;
  margin-top: -4px;
}

/* 指标卡片行 */
.metrics { display: flex; gap: 36px; margin-top: 8px; }
.metric { display: flex; flex-direction: column; align-items: center; gap: 2px; }
.m-val { font-size: 28px; font-weight: 200; color: var(--text-primary); }
.m-val.high { color: var(--error); }
.m-unit { font-size: 13px; color: var(--text-secondary); }

/* 控制按钮 */
.controls { display: flex; gap: 24px; align-items: center; }
.ctrl-btn {
  border: none; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  transition: transform 0.15s;
}
.ctrl-btn:active { transform: scale(0.93); }

.pause-btn {
  width: 60px; height: 60px;
  background: var(--bg-light);
  border: 2px solid var(--border);
  color: var(--text-primary);
  transition: all 0.2s;
}
.pause-btn:hover { border-color: var(--accent); color: var(--accent); }

.stop-btn {
  width: 68px; height: 68px;
  background: var(--error);
  color: #fff;
  box-shadow: 0 4px 16px rgba(229,115,115,0.35);
}
</style>
