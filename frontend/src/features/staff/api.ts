import { api } from '@/lib/api/client'
import type { StaffProfile } from '@/features/auth/types'

export const staffApi = {
  create: (body: Omit<StaffProfile, 'id'>) => api<StaffProfile>('/staff', { authenticated: true, method: 'POST', body: JSON.stringify(body) }),
}
