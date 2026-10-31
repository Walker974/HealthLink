import { api } from '@/lib/api/client'
import type { Equipment, RegisterEquipment } from './types'

export const equipmentApi = {
  get: (id: number) => api<Equipment>(`/equipment/${id}`, { authenticated: true }),
  register: (body: RegisterEquipment) => api<Equipment>('/equipment', { authenticated: true, method: 'POST', body: JSON.stringify(body) }),
}
