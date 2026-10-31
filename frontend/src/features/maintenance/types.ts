import type { components } from '@/lib/api/schema'
import type { ApiModel } from '@/lib/api/types'

export type WorkOrder = ApiModel<components['schemas']['WorkOrderResponse'],
  'repairNotes' | 'verificationNotes' | 'repairerId' | 'verifierId' | 'acceptedAt' | 'completedAt' | 'reviewedAt' | 'verifiedAt'>
export type WorkOrderStatus = WorkOrder['status']
export interface Page<T> { items: T[]; page: number; size: number; totalElements: number; totalPages: number }
export type AuditEntry = ApiModel<components['schemas']['AuditResponse'], 'workOrderId' | 'previousEquipmentStatus' | 'previousWorkOrderStatus' | 'workOrderStatus'>
