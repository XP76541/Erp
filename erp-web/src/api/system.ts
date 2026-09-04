import http from './http'
import type { PageResult } from './http'

export interface SystemUser { id: number; username: string; realName: string; isActive: number; roleIds: number[] }
export interface Role { id: number; code: string; name: string; remark: string }
export interface RoleView { role: Role; permissionIds: number[] }
export interface Permission { id: number; code: string; name: string; type: string; parentId: number; sort: number }
export interface OperationLog { id: number; userName: string; module: string; action: string; docType?: string; docId?: number; docNo?: string; detail?: string; ip?: string; createdAt: string }
export interface BackupInfo { fileName: string; size: number; createdAt: string }

export const systemApi = {
  users(params: { page: number; size: number; keyword?: string; active?: number }) { return http.get<unknown, PageResult<SystemUser>>('/system/users', { params }) },
  createUser(data: { username: string; realName: string; password: string; active: number; roleIds: number[] }) { return http.post<unknown, number>('/system/users', data) },
  updateUser(id: number, data: { realName: string; active: number; roleIds: number[] }) { return http.put(`/system/users/${id}`, data) },
  resetPassword(id: number, password: string) { return http.put(`/system/users/${id}/password`, { password }) },
  roles() { return http.get<unknown, RoleView[]>('/system/roles') },
  permissions() { return http.get<unknown, Permission[]>('/system/permissions') },
  updateRole(id: number, data: { name: string; remark: string; permissionIds: number[] }) { return http.put(`/system/roles/${id}`, data) },
  logs(params: Record<string, unknown>) { return http.get<unknown, PageResult<OperationLog>>('/system/operation-logs', { params }) },
  backups() { return http.get<unknown, BackupInfo[]>('/system/backups') },
  createBackup() { return http.post<unknown, BackupInfo>('/system/backup') },
  downloadBackup(fileName: string) { return `/api/system/backups/${encodeURIComponent(fileName)}` },
}
