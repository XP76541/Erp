import http from './http'

export interface Warehouse {
  id?: number
  code: string
  name: string
  type: string
  isActive?: number
  createdAt?: string
}

export const warehouseApi = {
  listAll() {
    return http.get<unknown, Warehouse[]>('/warehouses')
  },
  create(data: Warehouse) {
    return http.post<unknown, number>('/warehouses', data)
  },
  update(id: number, data: Warehouse) {
    return http.put(`/warehouses/${id}`, data)
  },
  toggleStatus(id: number, active: boolean) {
    return http.put(`/warehouses/${id}/status`, { active })
  },
}
