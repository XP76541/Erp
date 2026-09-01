import http from './http'

export interface LoginResult {
  token: string
  username: string
  realName: string
}

export interface LoginUser {
  userId: number
  username: string
  realName: string
}

export const authApi = {
  login(username: string, password: string) {
    return http.post<unknown, LoginResult>('/auth/login', { username, password })
  },
  me() {
    return http.get<unknown, LoginUser>('/auth/me')
  },
  logout() {
    return http.post('/auth/logout')
  },
}
