import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * 后端统一响应结构,见 erp-server Result.java
 */
interface ApiBody<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  records: T[]
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiBody<unknown>
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.message || '操作失败')
        return Promise.reject(new Error(body.message))
      }
      // 拦截器已解包 Result.data,调用方直接拿到业务数据
      return body.data as never
    }
    return body as never
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
      }
      ElMessage.error('未登录或登录已过期')
    } else {
      ElMessage.error(error.response?.data?.message || '网络异常,请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default http
