import type { EquipmentStatus } from '@/features/equipment/types'

export const imagingTypes = [
  { code: 'MRI', label: 'MRI', name: 'Magnetic resonance imaging', short: 'Detailed imaging' },
  { code: 'CT', label: 'CT scan', name: 'Computed tomography', short: 'Cross-sectional scans' },
  { code: 'XRAY', label: 'X-ray', name: 'X-ray imaging', short: 'Bones & chest imaging' },
  { code: 'US', label: 'Ultrasound', name: 'Ultrasound imaging', short: 'Sound-wave imaging' },
] as const
export type ImagingCode = typeof imagingTypes[number]['code']
export interface HospitalListing {
  id: number; name: string; city: string; address: string; area?: string;
  status: EquipmentStatus; equipmentCount: number; updatedAt: string;
  position?: [number, number];
}
export interface AvailabilityResult {
  hospitals: HospitalListing[]; fetchedAt: string; source: 'api' | 'offline' | 'preview';
}
