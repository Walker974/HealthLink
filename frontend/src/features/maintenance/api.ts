import { api } from '@/lib/api/client'
import type { AuditEntry, Page, WorkOrder } from './types'

export type RepairAction = 'accept' | 'complete' | 'verify' | 'reject'
export const maintenanceApi = {
  list: (equipmentId: number, page = 0) => api<Page<WorkOrder>>(`/work-orders?equipmentId=${equipmentId}&page=${page}&size=20`, { authenticated: true }),
  report: (equipmentId: number, equipmentVersion: number, description: string) => api<WorkOrder>('/work-orders', {
    authenticated: true, method: 'POST', body: JSON.stringify({ equipmentId, equipmentVersion, description }),
  }),
  action: (order: WorkOrder, action: RepairAction, notes?: string) => api<WorkOrder>(`/work-orders/${order.id}/${action}`, {
    authenticated: true, method: 'POST', body: JSON.stringify({ version: order.version,
      ...(action === 'complete' ? { repairNotes: notes } : action !== 'accept' ? { notes } : {}),
    }),
  }),
  history: (equipmentId: number, page = 0) => api<Page<AuditEntry>>(`/equipment/${equipmentId}/history?page=${page}&size=20`, { authenticated: true }),
}
