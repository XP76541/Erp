import http from './http'

export interface UserOption {
  id: number
  username: string
  realName: string
}

export interface User {
  id: number
  username: string
  realName: string
  isActive: number
}

export const userApi = {
  listActive() {
    return http.get<unknown, UserOption[]>('/users')
  },
  page(params: { page?: number; size?: number }) {
    return http.get<unknown, { records: User[]; total: number }>('/users', { params })
  },
}
