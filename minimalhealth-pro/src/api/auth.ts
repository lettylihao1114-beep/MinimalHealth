import client from './client'

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: {
    id: number
    name: string | null
    phone: string
    profileComplete: boolean
  }
}

export function passwordLogin(account: string, password: string) {
  return client.post<{ data: LoginResponse }>('/auth/login/password', { account, password })
}

export function register(phone: string, password: string) {
  return client.post<{ data: LoginResponse }>('/auth/register', { phone, password })
}

export function refreshToken(refreshToken: string) {
  return client.post<{ data: LoginResponse }>('/auth/refresh', { refreshToken })
}

export function sendSms(phone: string, type: string) {
  return client.post('/auth/sms/send', { phone, type })
}

export function smsLogin(phone: string, code: string) {
  return client.post<{ data: LoginResponse }>('/auth/sms/login', { phone, code })
}
