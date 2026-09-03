import http from './http'

export interface ProductCategory {
  id?: number
  parentId: number
  name: string
  sort?: number
  isActive?: number
  createdAt?: string
}

export const categoryApi = {
  listAll() {
    return http.get<unknown, ProductCategory[]>('/categories')
  },
  create(data: ProductCategory) {
    return http.post<unknown, number>('/categories', data)
  },
  update(id: number, data: ProductCategory) {
    return http.put(`/categories/${id}`, data)
  },
  toggleStatus(id: number, active: boolean) {
    return http.put(`/categories/${id}/status`, { active })
  },
}
