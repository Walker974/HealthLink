import type { HospitalListing, ImagingCode } from './types'
import type { Equipment } from '@/features/equipment/types'
import type { WorkOrder } from '@/features/maintenance/types'

const now = Date.now()
const minutesAgo = (minutes: number) => new Date(now - minutes * 60_000).toISOString()

// Explicit design fixtures. They are never used as a fallback for a failed live request.
export const sampleHospitals: HospitalListing[] = [
  { id: 101, name: 'Hay Riad Regional Hospital', city: 'Rabat', address: 'Sample address · Avenue Annakhil, Hay Riad', area: 'Hay Riad',
    status: 'AVAILABLE', equipmentCount: 2, updatedAt: minutesAgo(4), position: [33.9638, -6.8656] },
  { id: 102, name: 'Salé Provincial Hospital', city: 'Rabat', address: 'Sample address · Avenue Hassan II, Salé', area: 'Salé',
    status: 'AVAILABLE', equipmentCount: 1, updatedAt: minutesAgo(12), position: [34.0589, -6.7989] },
  { id: 103, name: 'Souissi Imaging Center', city: 'Rabat', address: 'Sample address · Avenue Mehdi Ben Barka, Souissi', area: 'Souissi',
    status: 'UNDER_MAINTENANCE', equipmentCount: 1, updatedAt: minutesAgo(43), position: [33.9742, -6.8408] },
  { id: 104, name: 'Agdal University Hospital', city: 'Rabat', address: 'Sample address · Avenue Ibn Sina, Agdal', area: 'Agdal',
    status: 'OUT_OF_SERVICE', equipmentCount: 1, updatedAt: minutesAgo(18), position: [33.9987, -6.8536] },
]

export function previewAvailability(city: string, type: ImagingCode): HospitalListing[] {
  if (city.toLowerCase() !== 'rabat') return []
  return sampleHospitals.map((hospital, index) => ({ ...hospital,
    status: type === 'XRAY' && index === 3 ? 'AVAILABLE' : hospital.status,
    equipmentCount: type === 'US' ? hospital.equipmentCount + 1 : hospital.equipmentCount,
  }))
}

export const sampleEquipment: Equipment[] = [
  { id: 201, version: 3, serialNumber: 'DEMO-MRI-0071', model: 'MRI scanner · Room 2', status: 'AVAILABLE', statusUpdatedAt: minutesAgo(4),
    departmentId: 10, imagingTypeCode: 'MRI', hospitalId: 101, hospitalName: sampleHospitals[0].name, city: 'Rabat', address: sampleHospitals[0].address },
  { id: 202, version: 4, serialNumber: 'DEMO-CT-0072', model: 'CT scanner · Room 3', status: 'UNDER_MAINTENANCE', statusUpdatedAt: minutesAgo(64),
    departmentId: 10, imagingTypeCode: 'CT', hospitalId: 101, hospitalName: sampleHospitals[0].name, city: 'Rabat', address: sampleHospitals[0].address },
  { id: 203, version: 1, serialNumber: 'DEMO-US-0140', model: 'Ultrasound · Bay A', status: 'OUT_OF_SERVICE', statusUpdatedAt: minutesAgo(135),
    departmentId: 10, imagingTypeCode: 'US', hospitalId: 101, hospitalName: sampleHospitals[0].name, city: 'Rabat', address: sampleHospitals[0].address },
  { id: 204, version: 0, serialNumber: 'DEMO-XRAY-0141', model: 'Digital X-ray · Bay B', status: 'AVAILABLE', statusUpdatedAt: minutesAgo(20),
    departmentId: 10, imagingTypeCode: 'XRAY', hospitalId: 101, hospitalName: sampleHospitals[0].name, city: 'Rabat', address: sampleHospitals[0].address },
]

export const sampleOrders: WorkOrder[] = [
  { id: 301, version: 2, equipmentId: 202, equipmentVersion: 4, equipmentStatus: 'UNDER_MAINTENANCE', hospitalId: 101, departmentId: 10,
    status: 'AWAITING_VERIFICATION', description: 'Scanner startup calibration fails.', repairNotes: 'Replaced the calibration module and completed operational checks.',
    verificationNotes: null, reporterId: 1, repairerId: 2, verifierId: null,
    reportedAt: minutesAgo(140), acceptedAt: minutesAgo(120), completedAt: minutesAgo(22), reviewedAt: null, verifiedAt: null },
  { id: 302, version: 0, equipmentId: 203, equipmentVersion: 1, equipmentStatus: 'OUT_OF_SERVICE', hospitalId: 101, departmentId: 10,
    status: 'OPEN', description: 'Intermittent power loss during equipment self-test.', repairNotes: null,
    verificationNotes: null, reporterId: 1, repairerId: null, verifierId: null,
    reportedAt: minutesAgo(135), acceptedAt: null, completedAt: null, reviewedAt: null, verifiedAt: null },
]
