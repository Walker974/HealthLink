import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Check, ClipboardList, History, RefreshCw, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { EquipmentLookup } from '@/components/shared/equipment-lookup'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { maintenanceApi } from '../api'
import { formatTime } from '@/lib/utils'

const actionLabels: Record<string, string> = { EQUIPMENT_REGISTERED: 'Equipment registered', FAULT_REPORTED: 'Fault reported', REPAIR_ACCEPTED: 'Repair accepted', REPAIR_COMPLETED: 'Repair submitted for verification', REPAIR_REJECTED: 'Repair returned for further work', REPAIR_VERIFIED: 'Repair independently verified' }
export default function HistoryPage() {
  const workspace = useWorkspace()
  const [page, setPage] = useState(0)
  const id = workspace.selectedId || (workspace.preview ? 202 : undefined)
  const query = useQuery({ queryKey: ['equipment-history', id, page], enabled: !workspace.preview && !!id,
    queryFn: () => maintenanceApi.history(id!, page), retry: false,
  })
  const sample = workspace.orders.find(order => order.equipmentId === id)
  const entries = workspace.preview ? (sample ? [
    { id: 'sample-1', action: 'FAULT_REPORTED', occurredAt: sample.reportedAt, actorRole: 'RADIOLOGY_OPERATOR', details: sample.description },
    ...(sample.acceptedAt ? [{ id: 'sample-2', action: 'REPAIR_ACCEPTED', occurredAt: sample.acceptedAt, actorRole: 'MAINTENANCE_TECHNICIAN', details: 'Repair accepted by the assigned technician.' }] : []),
    ...(sample.completedAt ? [{ id: 'sample-3', action: 'REPAIR_COMPLETED', occurredAt: sample.completedAt, actorRole: 'MAINTENANCE_TECHNICIAN', details: sample.repairNotes || 'Repair checks completed.' }] : []),
    ...(sample.reviewedAt ? [{ id: 'sample-4', action: sample.status === 'VERIFIED' ? 'REPAIR_VERIFIED' : 'REPAIR_REJECTED', occurredAt: sample.reviewedAt, actorRole: 'HOSPITAL_MANAGER', details: sample.verificationNotes || 'Independent review completed.' }] : []),
  ] : []) : query.data?.items || []
  return <><div className="page-heading"><div><span className="section-kicker">EVERY STEP HAS A RECORD</span><h1>Equipment audit history</h1><p>{id ? `Equipment #${id} · Oldest events first. Records are preserved as actions happen.` : 'Open an equipment record to view its history.'}</p></div><Button variant="outline" onClick={() => void query.refetch()} disabled={workspace.preview || !id}><RefreshCw size={15} />Refresh</Button></div><EquipmentLookup />
    {!workspace.preview && !!id && query.isPending ? <LoadingState /> : query.error ? <ErrorState error={query.error} retry={() => void query.refetch()} /> : !entries.length ? <EmptyState title="No history loaded" description="Choose an equipment record above to read its audit history." />
      : <section className="surface audit-timeline">{entries.map(entry => <article className="audit-entry" key={entry.id}><span className="audit-dot">{entry.action === 'REPAIR_VERIFIED' ? <Check size={17} /> : entry.action === 'FAULT_REPORTED' ? <ClipboardList size={17} /> : <History size={17} />}</span><div><h3>{actionLabels[entry.action] || entry.action}</h3><p>{entry.details}</p><small>{entry.actorRole.replaceAll('_', ' ').toLowerCase()} · {formatTime(entry.occurredAt)}</small></div></article>)}</section>}
    {!workspace.preview && (query.data?.totalPages || 0) > 1 && <div className="pagination-controls"><Button variant="outline" onClick={() => setPage(value => value - 1)} disabled={!page}>Previous</Button><span>Page {page + 1} of {query.data?.totalPages}</span><Button variant="outline" onClick={() => setPage(value => value + 1)} disabled={page + 1 >= (query.data?.totalPages || 0)}>Next</Button></div>}
    <div className="care-notice workspace-section"><ShieldCheck size={18} /><p>Business changes and their audit entries are saved together. History cannot be edited through this application.</p></div>
  </>
}
