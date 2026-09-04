import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi, type AuthUser, type LoginResult } from '@/api/auth'

const readRoles = (): string[] => {
  try {
    const value: unknown = JSON.parse(localStorage.getItem('roleCodes') ?? '[]')
    return Array.isArray(value) ? value.filter((role): role is string => typeof role === 'string') : []
  } catch {
    return []
  }
}

const saveRoles = (roles: string[]) => localStorage.setItem('roleCodes', JSON.stringify(roles))

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') ?? '')
  const realName = ref(localStorage.getItem('realName') ?? '')
  const roleCodes = ref(readRoles())

  // An empty role list means the server did not expose role data. In that case
  // keep the existing menu visible rather than accidentally hiding valid work.
  const rolesLoaded = computed(() => roleCodes.value.length > 0)
  const hasAnyRole = (...roles: string[]) =>
    !rolesLoaded.value || roles.some((role) => roleCodes.value.includes(role))
  const setSession = (data: LoginResult | AuthUser, preserveRoles = false) => {
    token.value = 'token' in data ? data.token : token.value
    realName.value = data.realName
    const roles = data.roleCodes ?? data.roles
    if (roles) roleCodes.value = roles.map((role) => role.toUpperCase())
    else if (!preserveRoles) roleCodes.value = []
    if ('token' in data) localStorage.setItem('token', data.token)
    localStorage.setItem('realName', data.realName)
    saveRoles(roleCodes.value)
  }

  async function login(username: string, password: string) {
    setSession(await authApi.login(username, password))
  }

  async function loadCurrentUser() {
    if (!token.value) return
    setSession(await authApi.me(), true)
  }

  function logout() {
    token.value = ''
    realName.value = ''
    roleCodes.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('realName')
    localStorage.removeItem('roleCodes')
  }

  return { token, realName, roleCodes, rolesLoaded, hasAnyRole, login, loadCurrentUser, logout }
})
