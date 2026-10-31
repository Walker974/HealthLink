import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router-dom'
import { equipmentApi } from '@/features/equipment/api'
import type { Equipment, RegisterEquipment } from '@/features/equipment/types'
import { maintenanceApi, type RepairAction } from '@/features/maintenance/api'
import type { WorkOrder } from '@/features/maintenance/types'
import type { StaffProfile, StaffRole } from '@/features/auth/types'
import { sampleEquipment, sampleOrders } from '@/features/availability/preview-data'
import { usePreview } from '@/lib/use-preview'
import { ApiError } from '@/lib/api/client'
import { useOnline } from '@/lib/use-online'

export interface WorkspaceIdentity { role: StaffRole; name: string; hospitalName: string; profile?: StaffProfile }
interface WorkspaceData extends WorkspaceIdentity {
  preview: boolean; online: boolean; equipment: Equipment[]; orders: WorkOrder[]; selectedId?: number;
  loading: boolean; error: unknown; orderLoading: boolean; orderError: unknown;
  orderPage: number; orderPages: number;
  selectEquipment: (id: number) => void; setOrderPage: (page: number) => void; refresh: () => Promise<void>;
  reportFault: (equipment: Equipment, description: string) => Promise<void>;
  repairAction: (order: WorkOrder, action: RepairAction, notes: string) => Promise<void>;
  registerEquipment: (input: RegisterEquipment) => Promise<void>;
}
const WorkspaceContext = createContext<WorkspaceData | null>(null)

export function WorkspaceProvider({ identity, children }: { identity: WorkspaceIdentity; children: ReactNode }) {
  const preview = usePreview()
  const online = useOnline()
  const [params, setParams] = useSearchParams()
  const queryClient = useQueryClient()
  const [demoEquipment, setDemoEquipment] = useState(() => structuredClone(sampleEquipment))
  const [demoOrders, setDemoOrders] = useState(() => structuredClone(sampleOrders))
  const candidate = Number(params.get('equipment'))
  const selectedId = Number.isSafeInteger(candidate) && candidate > 0 ? candidate : undefined
  const orderCandidate = Number(params.get('page'))
  const orderPage = Number.isSafeInteger(orderCandidate) && orderCandidate >= 0 ? orderCandidate : 0
  const hospitalStaff = ['HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN'].includes(identity.role)
  const equipmentQuery = useQuery({ queryKey: ['workspace', 'equipment', selectedId],
    enabled: !preview.enabled && hospitalStaff && !!selectedId, queryFn: () => equipmentApi.get(selectedId!), retry: false,
  })
  const ordersQuery = useQuery({ queryKey: ['workspace', 'orders', selectedId, orderPage],
    enabled: !preview.enabled && hospitalStaff && !!equipmentQuery.data,
    queryFn: () => maintenanceApi.list(selectedId!, orderPage), retry: false,
  })
  const equipment = preview.enabled ? demoEquipment : equipmentQuery.data ? [equipmentQuery.data] : []
  const orders = preview.enabled ? demoOrders : ordersQuery.data?.items || []
  function selectEquipment(id: number) {
    const next = new URLSearchParams(params); next.set('equipment', String(id)); next.delete('page'); setParams(next)
  }
  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['workspace'] }),
      queryClient.invalidateQueries({ queryKey: ['availability'] }),
      queryClient.invalidateQueries({ queryKey: ['equipment-history'] }),
    ])
  }
  async function reportFault(machine: Equipment, description: string) {
    if (preview.enabled) {
      if (demoOrders.some(order => order.equipmentId === machine.id && order.status !== 'VERIFIED')) throw new ApiError(409, 'This sample machine already has an open repair.')
      const now = new Date().toISOString()
      setDemoEquipment(items => items.map(item => item.id === machine.id ? { ...item, status: 'OUT_OF_SERVICE', version: item.version + 1, statusUpdatedAt: now } : item))
      setDemoOrders(items => [...items, { id: Math.max(300, ...items.map(item => item.id)) + 1,
        version: 0, equipmentId: machine.id, equipmentVersion: machine.version + 1, equipmentStatus: 'OUT_OF_SERVICE', hospitalId: machine.hospitalId,
        departmentId: machine.departmentId, status: 'OPEN', description, reporterId: 1, repairerId: null, verifierId: null,
        repairNotes: null, verificationNotes: null, reportedAt: now, acceptedAt: null, completedAt: null, reviewedAt: null, verifiedAt: null,
      }])
    } else { await maintenanceApi.report(machine.id, machine.version, description); await refresh() }
  }
  async function repairAction(order: WorkOrder, action: RepairAction, notes: string) {
    if (preview.enabled) {
      const now = new Date().toISOString()
      const status = action === 'accept' || action === 'reject' ? 'IN_PROGRESS' : action === 'complete' ? 'AWAITING_VERIFICATION' : 'VERIFIED'
      const equipmentStatus = action === 'verify' ? 'AVAILABLE' : 'UNDER_MAINTENANCE'
      setDemoOrders(items => items.map(item => item.id === order.id ? { ...item, status, equipmentStatus, version: item.version + 1,
        ...(action === 'accept' ? { repairerId: 2, acceptedAt: now } : action === 'complete' ? { repairNotes: notes, completedAt: now }
          : { verifierId: 3, verificationNotes: notes, reviewedAt: now, ...(action === 'verify' ? { verifiedAt: now } : { completedAt: null }) }),
      } : item))
      setDemoEquipment(items => items.map(item => item.id === order.equipmentId ? { ...item, status: equipmentStatus, version: item.version + 1, statusUpdatedAt: now } : item))
    } else { await maintenanceApi.action(order, action, notes); await refresh() }
  }
  async function registerEquipment(input: RegisterEquipment) {
    if (preview.enabled) setDemoEquipment(items => [...items, { ...input, id: Math.max(...items.map(item => item.id)) + 1,
      version: 0, status: 'AVAILABLE', statusUpdatedAt: new Date().toISOString(), hospitalId: 101, hospitalName: identity.hospitalName,
      city: 'Rabat', address: 'Sample address · Hay Riad',
    }])
    else { const created = await equipmentApi.register(input); selectEquipment(created.id); await refresh() }
  }
  const value = useMemo(() => ({ ...identity, preview: preview.enabled, online, equipment, orders, selectedId,
    loading: !preview.enabled && !!selectedId && equipmentQuery.isPending,
    error: preview.enabled ? null : equipmentQuery.error, orderLoading: !preview.enabled && !!equipmentQuery.data && ordersQuery.isPending,
    orderError: preview.enabled ? null : ordersQuery.error, orderPage, orderPages: preview.enabled ? 1 : ordersQuery.data?.totalPages || 0,
    selectEquipment, setOrderPage: (page: number) => { const next = new URLSearchParams(params); next.set('page', String(page)); setParams(next) },
    refresh, reportFault, repairAction, registerEquipment,
  }), [identity, preview.enabled, online, equipment, orders, selectedId, equipmentQuery.isPending, equipmentQuery.error,
    equipmentQuery.data, ordersQuery.isPending, ordersQuery.error, ordersQuery.data, orderPage, params])
  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>
}
export function useWorkspace() {
  const context = useContext(WorkspaceContext)
  if (!context) throw new Error('Workspace data requires WorkspaceProvider')
  return context
}
