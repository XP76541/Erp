import request from '@/utils/request'
import type { PageResult } from './http'

export interface StockRow {
  productId: number; productCode: string; productName: string; productSpec?: string
  categoryId?: number; warehouseId: number; warehouseName: string
  quantity: number; unitCost: number; totalValue: number
}
export interface LedgerRow {
  id: number; docType: string; docNo: string; productId: number; productName: string
  warehouseId: number; warehouseName: string; direction: number; quantity: number
  unitCost: number; amount: number; balanceQuantity: number; balanceAmount: number
  bizDate: string; createdAt: string
}
export const inventoryQueryApi = {
  stocks: (params: { warehouseId?: number; productId?: number; categoryId?: number; page?: number; size?: number }) =>
    request.get<PageResult<StockRow>>('/inventory/stocks', { params }),
  ledgers: (params: { warehouseId?: number; productId?: number; docType?: string; startDate?: string; endDate?: string; page?: number; size?: number }) =>
    request.get<PageResult<LedgerRow>>('/inventory/ledgers', { params }),
}
