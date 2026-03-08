import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

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
      window.location.href = '/login'
    } else {
      ElMessage.error(error.message || 'Network error')
    }
    return Promise.reject(error)
  }
)

export default service
