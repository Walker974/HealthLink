import { lazy, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowUpRight, CheckCircle2, ClipboardCheck, Clock3, MonitorCog, Plus, RefreshCw, ShieldCheck, Wrench } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { EquipmentLookup } from '@/components/shared/equipment-lookup'
import { StatusBadge } from '@/components/shared/status-badge'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { usePreview } from '@/lib/use-preview'
import { RepairActions } from '@/features/maintenance/components/repair-actions'
import { RegisterEquipmentDialog } from '../components/register-equipment-dialog'
import { formatTime } from '@/lib/utils'

const AdministrationPage = lazy(() => import('@/features/organization/pages/administration-page'))
const MinistryPage = lazy(() => import('@/features/ministry/pages/ministry-page'))

export default function OverviewPage() {
  const workspace = useWorkspace()
  const preview = usePreview()
  const [register, setRegister] = useState(false)
  if (workspace.role === 'PLATFORM_ADMIN') return <AdministrationPage />
  if (workspace.role === 'MINISTRY_OFFICER') return <MinistryPage />
  const working = workspace.equipment.filter(item => item.status === 'AVAILABLE').length
  const open = workspace.orders.filter(item => item.status !== 'VERIFIED').length
  const pending = workspace.orders.filter(item => item.status === 'AWAITING_VERIFICATION')
  const attention = workspace.role === 'HOSPITAL_MANAGER' ? pending : workspace.orders.filter(item => item.status !== 'VERIFIED')
  function href(path: string) { const url = new URL(preview.href(path), window.location.origin); if (workspace.selectedId) url.searchParams.set('equipment', String(workspace.selectedId)); return url.pathname + url.search }
  return <><div className="page-heading"><div><span className="section-kicker">YOUR TEAM. A CLEARER PICTURE.</span><h1>Hello, {workspace.name.split(' ')[0]}.</h1><p>{workspace.role === 'HOSPITAL_MANAGER' ? 'Here’s what needs your attention at your hospital.' : workspace.role === 'MAINTENANCE_TECHNICIAN' ? 'Keep repairs moving, one clear step at a time.' : 'Keep your department’s equipment status clear and current.'}</p></div><div><Button variant="outline" onClick={() => void workspace.refresh()}><RefreshCw size={15} />Refresh</Button>{workspace.role === 'HOSPITAL_MANAGER' && <Button onClick={() => setRegister(true)}><Plus size={15} />Register equipment</Button>}</div></div>
    <EquipmentLookup />
    <div className="stat-grid"><div className="surface stat-card"><div><span>{workspace.preview ? 'Working equipment' : 'Loaded equipment'}</span><strong>{workspace.preview ? working : workspace.equipment.length}</strong><p>{workspace.preview ? `of ${workspace.equipment.length} sample machines` : 'within your assignment'}</p></div><span className="stat-icon"><CheckCircle2 size={19} /></span></div>
      <div className="surface stat-card amber"><div><span>{workspace.preview ? 'Open repairs' : 'Loaded open repairs'}</span><strong>{open}</strong><p>{workspace.preview ? 'in the sample workspace' : 'for the current record'}</p></div><span className="stat-icon"><Wrench size={19} /></span></div>
      <div className="surface stat-card terra"><div><span>Awaiting verification</span><strong>{pending.length}</strong><p>{workspace.preview ? 'ready for an independent check' : 'in the loaded records'}</p></div><span className="stat-icon"><ClipboardCheck size={19} /></span></div></div>
    <div className="dashboard-grid"><section className="surface dashboard-panel"><div className="panel-heading"><div><h2>{workspace.role === 'HOSPITAL_MANAGER' ? 'Ready for your review' : 'Repair queue'}</h2><p>The next step, with the context you need.</p></div><Link to={href('/workspace/repairs')}>View repairs<ArrowUpRight size={13} /></Link></div>
      {workspace.loading || workspace.orderLoading ? <LoadingState /> : workspace.error || workspace.orderError ? <ErrorState error={workspace.error || workspace.orderError} /> : !attention.length ? <EmptyState title="Nothing waiting here" description={workspace.preview ? 'No matching repairs need attention.' : 'Open an equipment record to see its maintenance queue.'} /> : attention.map(order => <article key={order.id} className="repair-queue-item"><StatusBadge status={order.status} /><h3>{workspace.equipment.find(item => item.id === order.equipmentId)?.model || `Equipment #${order.equipmentId}`}</h3><p>{order.repairNotes || order.description}</p><div className="repair-queue-footer"><span><Clock3 size={11} className="inline mr-1" />{formatTime(order.completedAt || order.reportedAt)}</span><RepairActions order={order} /></div></article>)}
    </section><aside className="surface workflow-card"><ShieldCheck size={29} strokeWidth={1.5} /><h3>Working again means<br />independently verified.</h3><p>A completed repair stays unavailable until a different manager checks it. That’s how we keep public information accountable.</p><div className="workflow-steps"><div><span>1</span>Operator reports a fault</div><div><span>2</span>Technician repairs & tests</div><div><span>3</span>Manager verifies independently</div></div></aside></div>
    {!!workspace.equipment.length && <section className="surface dashboard-panel workspace-section"><div className="panel-heading"><div><h2>{workspace.preview ? 'Equipment at a glance' : 'Current equipment record'}</h2><p>{workspace.preview ? 'Sample equipment across the radiology department.' : 'Open another ID to inspect another authorized record.'}</p></div><Link to={href('/workspace/equipment')}>View equipment<ArrowUpRight size={13} /></Link></div><div className="mini-equipment-list">{workspace.equipment.map(machine => <div className="mini-equipment-item" key={machine.id}><div><span className="equipment-symbol"><MonitorCog size={18} /></span><div><strong>{machine.model}</strong><p>{machine.serialNumber} · {machine.imagingTypeCode}</p></div></div><StatusBadge status={machine.status} /></div>)}</div></section>}
    {workspace.role === 'HOSPITAL_MANAGER' && <RegisterEquipmentDialog open={register} onClose={() => setRegister(false)} />}
  </>
}
