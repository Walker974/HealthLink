import { env } from '@/lib/env'

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) { super(message); this.name = 'ApiError' }
}

type TokenProvider = () => Promise<string>
let tokenProvider: TokenProvider | undefined
export function setTokenProvider(provider?: TokenProvider) { tokenProvider = provider }

export async function api<T>(path: string, options: RequestInit & { authenticated?: boolean } = {}): Promise<T> {
  const { authenticated = false, ...request } = options
  if (authenticated && !navigator.onLine) throw new ApiError(0, 'Reconnect before using the staff workspace.')
  const headers = new Headers(request.headers)
  headers.set('Accept', 'application/json')
  if (request.body) headers.set('Content-Type', 'application/json')
  if (authenticated) {
    if (!tokenProvider) throw new ApiError(401, 'Sign in to continue.')
    headers.set('Authorization', `Bearer ${await tokenProvider()}`)
  }
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...request, headers, cache: 'no-store', credentials: 'omit',
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => ({})) as { detail?: string; message?: string }
    const fallback = response.status === 409
      ? 'This record changed. Refresh it and review the latest version before trying again.'
      : response.status === 403
        ? 'Your staff assignment does not allow this action.'
        : response.status === 401 ? 'Your session has expired. Sign in again.' : `Request failed (${response.status}).`
    throw new ApiError(response.status, problem.detail || problem.message || fallback)
  }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
