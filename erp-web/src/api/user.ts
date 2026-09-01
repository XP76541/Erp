import http from './http'

export interface UserOption {
  id: number
  username: string
  realName: string
}

export const userApi = {
  listActive() {
    return http.get<unknown, UserOption[]>('/users')
  },
}
