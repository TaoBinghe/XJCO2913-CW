import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import { getAdminUiUrl } from './appBase'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface RequestInstance extends AxiosInstance {
  <T = any>(config: AxiosRequestConfig): Promise<T>
  request<T = any>(config: AxiosRequestConfig): Promise<T>
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
}

function getApiBaseUrl(): string {
  if (import.meta.env.DEV) {
    return ''
  }

  return (import.meta.env.VITE_ADMIN_API_BASE_URL || '').trim()
}

const service = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 15000
}) as RequestInstance

service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 0) {
      ElMessage.error(res.message || 'Request failed')
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      removeToken()
      ElMessage.error('Session expired, please login again')
      window.location.replace(getAdminUiUrl('login'))
    } else {
      ElMessage.error(error.message || 'Network error')
    }
    return Promise.reject(error)
  }
)

export default service
