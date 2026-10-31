import type { ReactNode } from 'react'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { ErrorState } from '@/components/shared/page-state'
import type { StaffRole } from '../types'

export function RequireRole({ roles, children }: { roles: StaffRole[]; children: ReactNode }) {
  const workspace = useWorkspace()
  return roles.includes(workspace.role) ? children : <ErrorState error={new Error('This page is outside your assigned role. Use the workspace navigation to continue.')} />
}
