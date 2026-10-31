import type { components } from '@/lib/api/schema'
import type { ApiModel } from '@/lib/api/types'

export type Equipment = ApiModel<components['schemas']['EquipmentResponse']>
export type EquipmentStatus = Equipment['status']
export type RegisterEquipment = components['schemas']['RegisterEquipmentRequest']
