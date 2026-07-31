import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types/common'

type SiteRequestConfig = AxiosRequestConfig & {
  silent?: boolean
}

const http = axios.create({
  baseURL: '/',
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('linge-owner-auth-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as Result<unknown>
    if (typeof body?.code === 'number') {
      if (body.code !== 0) {
        if (body.code === 401) clearExpiredSession()
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      return body.data
    }
    return response.data
  },
  (error) => {
    if (error?.response?.status === 401) clearExpiredSession()
    const responseMessage = error?.response?.data?.message
    const status = error?.response?.status
    const message = responseMessage || (status ? `请求失败（HTTP ${status}）` : error?.message || '网络请求失败')
    if (!error?.config?.silent) ElMessage.error(message)
    return Promise.reject(error)
  }
)

export function request<T>(config: SiteRequestConfig) {
  return http.request<unknown, T>(config)
}

function clearExpiredSession() {
  localStorage.removeItem('linge-owner-auth-token')
  localStorage.removeItem('linge-owner-auth-user')
  if (window.location.pathname !== '/login') {
    const redirect = `${window.location.pathname}${window.location.search}`
    window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`)
  }
}
