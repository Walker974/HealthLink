import { api } from '@/lib/api/client'
import type { Page } from '@/features/maintenance/types'
import type { Region, Hospital, Department } from './types'

export const organizationApi = {
  regions: (page = 0) => api<Page<Region>>(`/organization/regions?page=${page}&size=100`),
  hospitals: (regionId: number, page = 0) => api<Page<Hospital>>(`/organization/regions/${regionId}/hospitals?page=${page}&size=100`),
  hospital: (id: number) => api<Hospital>(`/organization/hospitals/${id}`),
  departments: (hospitalId: number) => api<Page<Department>>(`/organization/hospitals/${hospitalId}/departments?size=100`, { authenticated: true }),
  createRegion: (body: Omit<Region, 'id'>) => api<Region>('/organization/regions', { authenticated: true, method: 'POST', body: JSON.stringify(body) }),
  createHospital: (body: Omit<Hospital, 'id'>) => api<Hospital>('/organization/hospitals', { authenticated: true, method: 'POST', body: JSON.stringify(body) }),
  createDepartment: (body: Omit<Department, 'id'>) => api<Department>('/organization/departments', { authenticated: true, method: 'POST', body: JSON.stringify(body) }),
}
