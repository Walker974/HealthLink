import { useQuery } from '@tanstack/react-query'
import { api } from '@/lib/api/client'
import { useAuth } from './auth-provider'
import type { StaffProfile } from './types'

export function useStaffProfile() {
  const auth = useAuth()
  return useQuery({ queryKey: ['staff', 'me'], enabled: auth.authenticated,
    queryFn: () => api<StaffProfile>('/staff/me', { authenticated: true }), retry: false,
  })
}
