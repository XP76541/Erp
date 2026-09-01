import http from './http'
import type { PageResult } from './http'

export interface Product {
  id?: number
  code?: string
  categoryId?: number
  name: string
  spec?: string
  unit: string
  barcode?: string
  purchasePrice?: number
  salePrice?: number
  minSalePrice?: number
  isActive?: number
  createdAt?: string
}

export interface ProductQuery {
  page: number
  size: number
  keyword?: string
}

export const productApi = {
  page(params: ProductQuery) {
    return http.get<unknown, PageResult<Product>>('/products', { params })
  },
  create(data: Product) {
    return http.post<unknown, number>('/products', data)
  },
  update(id: number, data: Product) {
    return http.put(`/products/${id}`, data)
  },
  toggleStatus(id: number, active: boolean) {
    return http.put(`/products/${id}/status`, { active })
  },
}
