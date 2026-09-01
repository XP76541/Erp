import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') ?? '',
    realName: localStorage.getItem('realName') ?? '',
  }),
  actions: {
    async login(username: string, password: string) {
      const data = await authApi.login(username, password)
      this.token = data.token
      this.realName = data.realName
      localStorage.setItem('token', data.token)
      localStorage.setItem('realName', data.realName)
    },
    logout() {
      this.token = ''
      this.realName = ''
      localStorage.removeItem('token')
      localStorage.removeItem('realName')
    },
  },
})
