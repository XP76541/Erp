import http from './http'
import type { PageResult } from './http'

export interface Customer {
  id?: number
  code?: string
  name: string
  shortName?: string
  contact?: string
  phone?: string
  address?: string
  paymentTermDays?: number
  creditLimit?: number
  salespersonId?: number
  isActive?: number
  createdAt?: string
}

export interface CustomerQuery {
  page: number
  size: number
  keyword?: string
}

export const customerApi = {
  page(params: CustomerQuery) {
    return http.get<unknown, PageResult<Customer>>('/customers', { params })
  },
  create(data: Customer) {
    return http.post<unknown, number>('/customers', data)
  },
  update(id: number, data: Customer) {
    return http.put(`/customers/${id}`, data)
  },
  toggleStatus(id: number, active: boolean) {
    return http.put(`/customers/${id}/status`, { active })
  },
}
