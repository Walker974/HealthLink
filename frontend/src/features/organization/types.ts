import type { components } from '@/lib/api/schema'
import type { ApiModel } from '@/lib/api/types'

export type Region = ApiModel<components['schemas']['RegionResponse']>
export type Hospital = ApiModel<components['schemas']['HospitalResponse']>
export type Department = ApiModel<components['schemas']['DepartmentResponse']>
