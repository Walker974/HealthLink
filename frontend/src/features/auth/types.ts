import type { components } from '@/lib/api/schema'
import type { ApiModel } from '@/lib/api/types'

export const staffRoles = ['RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN', 'HOSPITAL_MANAGER', 'MINISTRY_OFFICER', 'PLATFORM_ADMIN'] as const satisfies readonly NonNullable<components['schemas']['StaffResponse']['role']>[]
export type StaffRole = typeof staffRoles[number]
export const roleLabels: Record<StaffRole, string> = {
  RADIOLOGY_OPERATOR: 'Radiology operator', MAINTENANCE_TECHNICIAN: 'Maintenance technician',
  HOSPITAL_MANAGER: 'Hospital manager', MINISTRY_OFFICER: 'Ministry officer', PLATFORM_ADMIN: 'Platform administrator',
}
export type StaffProfile = ApiModel<components['schemas']['StaffResponse'], 'hospitalId' | 'departmentId'>
