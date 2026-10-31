import { useState } from 'react'
import { Link } from 'react-router-dom'
import { History, MonitorCog, Plus, RefreshCw, Wrench } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/shared/status-badge'
import { EquipmentLookup } from '@/components/shared/equipment-lookup'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { ReportFaultDialog } from '@/features/maintenance/components/report-fault-dialog'
import { RegisterEquipmentDialog } from '../components/register-equipment-dialog'
import { formatTime } from '@/lib/utils'
import { usePreview } from '@/lib/use-preview'

export default function EquipmentPage() {
  const workspace = useWorkspace()
  const preview = usePreview()
  const [reportId, setReportId] = useState<number | null>(null)
  const [register, setRegister] = useState(false)
  function historyHref(id: number) { const url = new URL(preview.href('/workspace/history'), window.location.origin); url.searchParams.set('equipment', String(id)); return url.pathname + url.search }
  return <><div className="page-heading"><div><span className="section-kicker">KEEPING SERVICES WORKING</span><h1>{workspace.role === 'RADIOLOGY_OPERATOR' ? 'My department’s equipment' : 'Equipment'}</h1><p>Current status, clear ownership, and the next step for every machine.</p></div><div><Button variant="outline" onClick={() => void workspace.refresh()}><RefreshCw size={15} />Refresh</Button>{workspace.role === 'HOSPITAL_MANAGER' && <Button onClick={() => setRegister(true)}><Plus size={16} />Register equipment</Button>}</div></div>
    <EquipmentLookup />
    {workspace.loading ? <LoadingState /> : workspace.error ? <ErrorState error={workspace.error} retry={() => void workspace.refresh()} />
      : !workspace.equipment.length ? <EmptyState title="Open an equipment record" description="Use the record ID above to see equipment in your assigned hospital or department." />
        : <div className="equipment-grid">{workspace.equipment.map(machine => {
          const openOrder = workspace.orders.some(order => order.equipmentId === machine.id && order.status !== 'VERIFIED')
          return <article key={machine.id} className="surface equipment-card"><div><span className="equipment-symbol"><MonitorCog size={22} /></span><StatusBadge status={machine.status} /></div><h2>{machine.model}</h2><p>{machine.serialNumber}</p>
            <div className="equipment-meta"><div><span>Imaging type</span><strong>{machine.imagingTypeCode}</strong></div><div><span>Last status update</span><strong>{formatTime(machine.statusUpdatedAt)}</strong></div><div><span>Department</span><strong>#{machine.departmentId}</strong></div><div><span>Equipment ID</span><strong>#{machine.id}</strong></div></div>
            <div className="equipment-actions">{workspace.role === 'RADIOLOGY_OPERATOR' && <Button variant="outline" disabled={openOrder || machine.status === 'UNDER_MAINTENANCE' || !workspace.online} onClick={() => setReportId(machine.id)}><Wrench size={15} />{openOrder ? 'Fault already reported' : 'Report a fault'}</Button>}<Button asChild variant="outline"><Link to={historyHref(machine.id)}><History size={15} />View history</Link></Button></div>
          </article>
        })}</div>}
    <ReportFaultDialog equipment={workspace.equipment.find(machine => machine.id === reportId) || null} onClose={() => setReportId(null)} />
    {workspace.role === 'HOSPITAL_MANAGER' && <RegisterEquipmentDialog open={register} onClose={() => setRegister(false)} />}
  </>
}
