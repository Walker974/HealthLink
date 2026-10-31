import { afterEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, setTokenProvider } from './client'
import { maintenanceApi } from '@/features/maintenance/api'
import { sampleOrders } from '@/features/availability/preview-data'

afterEach(() => { setTokenProvider(undefined); vi.unstubAllGlobals(); vi.restoreAllMocks() })

describe('API authentication and workflow requests', () => {
  it('keeps public searches anonymous even during an authenticated session', async () => {
    const token = vi.fn().mockResolvedValue('private-token')
    setTokenProvider(token)
    const fetcher = vi.fn().mockResolvedValue(new Response('[]', { status: 200 }))
    vi.stubGlobal('fetch', fetcher)
    await api('/equipment/available?type=MRI&city=Fes')
    const options = fetcher.mock.calls[0][1] as RequestInit
    expect(new Headers(options.headers).has('Authorization')).toBe(false)
    expect(options.credentials).toBe('omit')
    expect(token).not.toHaveBeenCalled()
  })

  it('requires a session before sending protected requests', async () => {
    const fetcher = vi.fn(); vi.stubGlobal('fetch', fetcher)
    await expect(api('/staff/me', { authenticated: true })).rejects.toMatchObject({ status: 401 })
    expect(fetcher).not.toHaveBeenCalled()
  })

  it('blocks protected requests offline before refreshing tokens or contacting the API', async () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    const token = vi.fn(); setTokenProvider(token)
    const fetcher = vi.fn(); vi.stubGlobal('fetch', fetcher)
    await expect(api('/work-orders', { authenticated: true, method: 'POST' })).rejects.toMatchObject({ status: 0 })
    expect(token).not.toHaveBeenCalled(); expect(fetcher).not.toHaveBeenCalled()
  })

  it('sends the current order version and verification findings with a bearer token', async () => {
    setTokenProvider(async () => 'test-access-token')
    const fetcher = vi.fn().mockResolvedValue(new Response(JSON.stringify(sampleOrders[0]), { status: 200 }))
    vi.stubGlobal('fetch', fetcher)
    await maintenanceApi.action(sampleOrders[0], 'verify', 'Independent checks passed')
    const [url, options] = fetcher.mock.calls[0] as [string, RequestInit]
    expect(url).toBe('/api/work-orders/301/verify')
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer test-access-token')
    expect(JSON.parse(options.body as string)).toEqual({ version: 2, notes: 'Independent checks passed' })
  })

  it('returns a conflict to the caller without retrying a mutation', async () => {
    setTokenProvider(async () => 'test-access-token')
    const fetcher = vi.fn().mockResolvedValue(new Response('{"detail":"The record changed. Reload it."}', { status: 409 }))
    vi.stubGlobal('fetch', fetcher)
    await expect(maintenanceApi.action(sampleOrders[0], 'verify', 'Checks passed')).rejects.toEqual(new ApiError(409, 'The record changed. Reload it.'))
    expect(fetcher).toHaveBeenCalledTimes(1)
  })
})
