import http from '@/api/http'

export interface LoginResult {
  token: string
  username: string
  realName: string
  /** Optional until the authentication API returns role codes. */
  roleCodes?: string[]
  roles?: string[]
}

export interface AuthUser {
  userId: number
  username: string
  realName: string
  /** Optional until the authentication API returns role codes. */
  roleCodes?: string[]
  roles?: string[]
}

export const authApi = {
  login(username: string, password: string) {
    return http.post<unknown, LoginResult>('/auth/login', { username, password })
  },
  me() {
    return http.get<unknown, AuthUser>('/auth/me')
  },
  logout() {
    return http.post('/auth/logout')
  },
}
