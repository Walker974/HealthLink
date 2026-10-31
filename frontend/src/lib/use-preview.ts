import { useSearchParams } from 'react-router-dom'
import { env } from './env'
import { staffRoles, type StaffRole } from '@/features/auth/types'

export function usePreview() {
  const [params] = useSearchParams()
  const enabled = env.designPreview && params.get('preview') === '1'
  const candidate = params.get('role')
  const role: StaffRole = staffRoles.find(item => item === candidate) || 'HOSPITAL_MANAGER'
  function href(path: string, nextRole = role) {
    if (!enabled) return path
    const next = new URL(path, window.location.origin)
    next.searchParams.set('preview', '1'); next.searchParams.set('role', nextRole)
    return `${next.pathname}${next.search}${next.hash}`
  }
  return { enabled, role, href }
}
