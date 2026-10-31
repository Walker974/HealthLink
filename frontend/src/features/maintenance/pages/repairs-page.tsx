import { useMemo, useState } from 'react'
import { RefreshCw, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/shared/status-badge'
import { EquipmentLookup } from '@/components/shared/equipment-lookup'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { RepairActions } from '../components/repair-actions'
import { formatTime } from '@/lib/utils'
import { cn } from '@/lib/utils'

const filters = [{ value: 'ALL', label: 'All repairs' }, { value: 'OPEN', label: 'Open' }, { value: 'IN_PROGRESS', label: 'In progress' }, { value: 'AWAITING_VERIFICATION', label: 'To verify' }, { value: 'VERIFIED', label: 'Verified' }]
export default function RepairsPage() {
  const workspace = useWorkspace()
  const [filter, setFilter] = useState(workspace.role === 'HOSPITAL_MANAGER' ? 'AWAITING_VERIFICATION' : 'ALL')
  const source = useMemo(() => workspace.preview && workspace.selectedId ? workspace.orders.filter(order => order.equipmentId === workspace.selectedId) : workspace.orders, [workspace.orders, workspace.preview, workspace.selectedId])
  const orders = source.filter(order => filter === 'ALL' || order.status === filter)
  return <><div className="page-heading"><div><span className="section-kicker">A REPAIR IS A TEAM EFFORT</span><h1>Repairs & verification</h1><p>{workspace.preview ? 'Coordinate maintenance and independently verify completed repairs.' : 'Open equipment by ID to view its repair records and available actions.'}</p></div><Button variant="outline" onClick={() => void workspace.refresh()}><RefreshCw size={15} />Refresh</Button></div>
    <EquipmentLookup />
    <div className="workspace-tabs" aria-label="Filter work orders">{filters.map(item => <Button variant={filter === item.value ? 'secondary' : 'ghost'} key={item.value} aria-pressed={filter === item.value} onClick={() => setFilter(item.value)}>{item.label}<span className="count-pill">{source.filter(order => item.value === 'ALL' || order.status === item.value).length}</span></Button>)}</div>
    {workspace.loading || workspace.orderLoading ? <LoadingState label="Loading repair records…" /> : workspace.error || workspace.orderError ? <ErrorState error={workspace.error || workspace.orderError} retry={() => void workspace.refresh()} /> : !orders.length ? <EmptyState title="No matching repairs" description={workspace.selectedId || workspace.preview ? 'There are no matching work orders on this page. Try another status filter.' : 'Open an equipment record above to see its work orders.'} />
      : <div className="work-order-list">{orders.map(order => <article key={order.id} className="surface work-order-card"><div className="work-order-top"><div><span className="section-kicker">WORK ORDER #{order.id}</span><h2>{workspace.equipment.find(machine => machine.id === order.equipmentId)?.model || `Equipment #${order.equipmentId}`}</h2><p>Reported {formatTime(order.reportedAt)} · Department #{order.departmentId}</p></div><StatusBadge status={order.status} /></div>
        <div className="work-order-description">{order.description}</div>{order.repairNotes && <div className="work-order-notes"><strong>Technician’s repair summary</strong>{order.repairNotes}</div>}{order.verificationNotes && <div className="work-order-notes"><strong>Manager’s review</strong>{order.verificationNotes}</div>}
        <div className="work-order-actions"><span><ShieldCheck size={14} />{order.status === 'VERIFIED' ? 'Returned to public availability' : 'Unavailable until independent verification'}</span><RepairActions order={order} /></div>
      </article>)}</div>}
    {!workspace.preview && workspace.orderPages > 1 && <div className="pagination-controls"><Button variant="outline" disabled={workspace.orderPage === 0} onClick={() => workspace.setOrderPage(workspace.orderPage - 1)}>Previous</Button><span>Page {workspace.orderPage + 1} of {workspace.orderPages}</span><Button variant="outline" disabled={workspace.orderPage + 1 >= workspace.orderPages} onClick={() => workspace.setOrderPage(workspace.orderPage + 1)}>Next</Button></div>}
  </>
}
