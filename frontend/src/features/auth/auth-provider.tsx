import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { getKeycloak, initializeKeycloak } from './keycloak'
import { staffRoles, type StaffRole } from './types'
import { ApiError, setTokenProvider } from '@/lib/api/client'
import { queryClient } from '@/lib/query-client'

interface AuthState { ready: boolean; authenticated: boolean; roles: StaffRole[]; name: string }
interface AuthContextValue extends AuthState {
  initialize: () => Promise<boolean>; login: () => Promise<void>; logout: () => Promise<void>
}
const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ ready: false, authenticated: false, roles: [], name: '' })
  const synchronize = useCallback(() => {
    const client = getKeycloak()
    const claims = client.tokenParsed
    setState({ ready: true, authenticated: !!client.authenticated,
      roles: staffRoles.filter(role => claims?.realm_access?.roles?.includes(role)),
      name: claims?.name || claims?.preferred_username || 'Hospital staff',
    })
  }, [])
  const initialize = useCallback(async () => {
    const client = getKeycloak()
    client.onAuthSuccess = synchronize
    client.onAuthRefreshSuccess = synchronize
    client.onAuthLogout = () => { queryClient.clear(); synchronize() }
    client.onAuthRefreshError = () => { client.clearToken(); synchronize() }
    const result = await initializeKeycloak()
    synchronize()
    return result
  }, [synchronize])

  useEffect(() => {
    setTokenProvider(async () => {
      const client = getKeycloak()
      if (!client.authenticated) throw new ApiError(401, 'Sign in to your HealthLink account.')
      try { await client.updateToken(30) }
      catch { client.clearToken(); synchronize(); throw new ApiError(401, 'Your session has expired. Sign in again.') }
      if (!client.token) throw new ApiError(401, 'Sign in to continue.')
      return client.token
    })
    return () => setTokenProvider(undefined)
  }, [synchronize])

  const login = useCallback(async () => {
    await initialize()
    await getKeycloak().login({ redirectUri: `${window.location.origin}/workspace` })
  }, [initialize])
  const logout = useCallback(async () => {
    queryClient.clear()
    await getKeycloak().logout({ redirectUri: window.location.origin })
  }, [])
  const value = useMemo(() => ({ ...state, initialize, login, logout }), [state, initialize, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth requires AuthProvider')
  return context
}
