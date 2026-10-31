import { Check, Circle, Clock3, Wrench } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { EquipmentStatus } from '@/features/equipment/types'
import type { WorkOrderStatus } from '@/features/maintenance/types'

const labels = {
  AVAILABLE: 'Available', OUT_OF_SERVICE: 'Out of service', UNDER_MAINTENANCE: 'In repair',
  OPEN: 'Open', IN_PROGRESS: 'In progress', AWAITING_VERIFICATION: 'Needs verification', VERIFIED: 'Verified',
}
export function StatusBadge({ status }: { status: EquipmentStatus | WorkOrderStatus }) {
  const good = status === 'AVAILABLE' || status === 'VERIFIED'
  const bad = status === 'OUT_OF_SERVICE'
  const Icon = good ? Check : bad ? Circle : status === 'AWAITING_VERIFICATION' ? Clock3 : Wrench
  return <Badge variant="outline" className={cn('status-badge gap-1.5 rounded-full px-2.5 py-1',
    good ? 'status-good' : bad ? 'status-bad' : 'status-pending')}>
    <Icon size={12} aria-hidden="true" />{labels[status]}
  </Badge>
}
