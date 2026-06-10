import axios from 'axios'
import router from '@/router'

// Android emulator uses 10.0.2.2, browser uses localhost
const isAndroidEmulator = typeof navigator !== 'undefined' && navigator.userAgent.includes('Android')
const baseURL = (isAndroidEmulator ? 'http://10.0.2.2:8080' : 'http://localhost:8080') + '/api'

const client = axios.create({
  baseURL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

// Request interceptor: attach JWT
client.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor: handle 401 globally
client.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    }
    return Promise.reject(err)
  }
)

export default client
