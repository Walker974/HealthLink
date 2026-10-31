import { useQuery } from '@tanstack/react-query'
import { get, set } from 'idb-keyval'
import { z } from 'zod'
import { api } from '@/lib/api/client'
import type { Equipment } from '@/features/equipment/types'
import type { AvailabilityResult, HospitalListing, ImagingCode } from './types'
import { previewAvailability } from './preview-data'

const listingSchema = z.object({
  id: z.number(), name: z.string(), city: z.string(), address: z.string(),
  status: z.enum(['AVAILABLE', 'OUT_OF_SERVICE', 'UNDER_MAINTENANCE']),
  equipmentCount: z.number(), updatedAt: z.string(),
})
const snapshotSchema = z.object({ hospitals: z.array(listingSchema), fetchedAt: z.string().datetime() })

export function groupAvailableEquipment(equipment: Equipment[]): HospitalListing[] {
  const hospitals = new Map<number, HospitalListing>()
  for (const machine of equipment) {
    if (machine.status !== 'AVAILABLE') continue
    if (!Number.isSafeInteger(machine.hospitalId) || !machine.hospitalName || !machine.city) {
      throw new Error('Hospital details are temporarily unavailable. Please try again shortly.')
    }
    const existing = hospitals.get(machine.hospitalId)
    if (existing) {
      existing.equipmentCount++
      if (machine.statusUpdatedAt > existing.updatedAt) existing.updatedAt = machine.statusUpdatedAt
    } else hospitals.set(machine.hospitalId, { id: machine.hospitalId, name: machine.hospitalName,
      city: machine.city, address: machine.address, status: 'AVAILABLE', equipmentCount: 1, updatedAt: machine.statusUpdatedAt,
    })
  }
  return [...hospitals.values()].sort((a, b) => a.name.localeCompare(b.name))
}

export async function searchAvailability(city: string, type: ImagingCode, preview: boolean): Promise<AvailabilityResult> {
  if (preview) return { hospitals: previewAvailability(city, type), fetchedAt: new Date().toISOString(), source: 'preview' }
  const key = `healthlink:public-search:v1:${JSON.stringify([city, type])}`
  try {
    if (!navigator.onLine) throw new Error('offline')
    const data = await api<Equipment[]>(`/equipment/available?${new URLSearchParams({ type, city })}`)
    const result: AvailabilityResult = { hospitals: groupAvailableEquipment(data), fetchedAt: new Date().toISOString(), source: 'api' }
    // A blocked/full browser store must never turn a successful API read into an error.
    await set(key, { hospitals: result.hospitals, fetchedAt: result.fetchedAt }).catch(() => undefined)
    return result
  } catch (error) {
    const cached = snapshotSchema.safeParse(await get(key).catch(() => undefined))
    if (cached.success) return { ...cached.data, source: 'offline' }
    throw error
  }
}

export function useAvailability(city: string, type: ImagingCode, preview: boolean) {
  return useQuery({ queryKey: ['availability', preview ? 'preview' : 'live', city, type],
    queryFn: () => searchAvailability(city, type, preview), networkMode: 'always', staleTime: 5_000, retry: false,
  })
}
