import axios from 'axios'
import router from '@/router'

// 统一用 localhost，模拟器/真机通过 adb reverse tcp:8080 tcp:8080 转发
const baseURL = 'http://localhost:8080/api'

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
