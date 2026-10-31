import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { get, set } from 'idb-keyval'
import { groupAvailableEquipment, searchAvailability } from './api'
import { sampleEquipment } from './preview-data'

vi.mock('idb-keyval', () => ({ get: vi.fn(), set: vi.fn().mockResolvedValue(undefined) }))
beforeEach(() => { vi.mocked(get).mockReset(); vi.mocked(set).mockClear() })
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('public search and offline snapshots', () => {
  it('counts machines per hospital without including faulty or unavailable equipment', () => {
    const first = sampleEquipment[0]
    const data = groupAvailableEquipment([first, { ...first, id: 800, statusUpdatedAt: '2099-01-01T00:00:00Z' }, sampleEquipment[2]])
    expect(data).toHaveLength(1)
    expect(data[0]).toMatchObject({ id: 101, equipmentCount: 2, status: 'AVAILABLE', updatedAt: '2099-01-01T00:00:00Z' })
  })

  it('stores only the public result snapshot after a successful search', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify([sampleEquipment[0]]), { status: 200 })))
    const result = await searchAvailability('Rabat', 'MRI', false)
    expect(result.source).toBe('api')
    expect(set).toHaveBeenCalledWith('healthlink:public-search:v1:["Rabat","MRI"]', {
      hospitals: result.hospitals, fetchedAt: result.fetchedAt,
    })
  })

  it('keeps a successful public response usable when browser storage is unavailable', async () => {
    vi.mocked(set).mockRejectedValueOnce(new Error('Storage quota exceeded'))
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify([sampleEquipment[0]]), { status: 200 })))
    await expect(searchAvailability('Rabat', 'MRI', false)).resolves.toMatchObject({ source: 'api' })
  })

  it('marks fallback results as saved and preserves their original timestamp', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Network unavailable')))
    const snapshot = { hospitals: groupAvailableEquipment([sampleEquipment[0]]), fetchedAt: '2026-10-01T10:42:00.000Z' }
    vi.mocked(get).mockResolvedValue(snapshot)
    const result = await searchAvailability('Rabat', 'MRI', false)
    expect(result).toEqual({ ...snapshot, source: 'offline' })
    expect(set).not.toHaveBeenCalled()
  })

  it('never substitutes synthetic preview hospitals for a failed live search', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Network unavailable')))
    vi.mocked(get).mockResolvedValue(undefined)
    await expect(searchAvailability('Rabat', 'MRI', false)).rejects.toThrow('Network unavailable')
  })

  it('keeps explicit design previews out of the offline public-data store', async () => {
    const fetcher = vi.fn(); vi.stubGlobal('fetch', fetcher)
    await expect(searchAvailability('Rabat', 'MRI', true)).resolves.toMatchObject({ source: 'preview' })
    expect(fetcher).not.toHaveBeenCalled(); expect(get).not.toHaveBeenCalled(); expect(set).not.toHaveBeenCalled()
  })
})
