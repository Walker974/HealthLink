import { useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Copy, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { organizationApi } from '../api'
import type { Department } from '../types'

export default function DepartmentsPage() {
  const workspace = useWorkspace()
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [sample, setSample] = useState<Department[]>([{ id: 10, code: 'RADIOLOGY', name: 'Radiology', hospitalId: 101 }, { id: 11, code: 'EMERGENCY', name: 'Emergency imaging', hospitalId: 101 }])
  const query = useQuery({ queryKey: ['departments', workspace.profile?.hospitalId], enabled: !workspace.preview && !!workspace.profile?.hospitalId,
    queryFn: () => organizationApi.departments(workspace.profile!.hospitalId!),
  })
  const departments = workspace.preview ? sample : query.data?.items || []
  async function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setBusy(true); setError('')
    const data = new FormData(event.currentTarget)
    const code = String(data.get('code')).trim().toUpperCase(), name = String(data.get('name')).trim()
    try {
      if (!/^[A-Z0-9_-]{1,30}$/.test(code) || !name || name.length > 120) throw new Error('Enter a valid code and department name.')
      const body = { code, name, hospitalId: workspace.profile!.hospitalId! }
      if (workspace.preview) setSample(items => [...items, { ...body, id: Math.max(...items.map(item => item.id)) + 1 }])
      else { await organizationApi.createDepartment(body); await queryClient.invalidateQueries({ queryKey: ['departments'] }) }
      toast.success(workspace.preview ? 'Sample department created' : 'Department created'); setOpen(false)
    } catch (failure) { setError(failure instanceof Error ? failure.message : 'Unable to create department.') }
    finally { setBusy(false) }
  }
  return <><div className="page-heading"><div><span className="section-kicker">ORGANIZED AROUND YOUR TEAM</span><h1>Hospital departments</h1><p>Departments define equipment ownership and operator assignments.</p></div><Button onClick={() => { setError(''); setOpen(true) }}><Plus size={16} />Add department</Button></div>
    {!workspace.preview && query.isPending ? <LoadingState /> : query.error ? <ErrorState error={query.error} /> : !departments.length ? <EmptyState title="Create your first department" description="Departments are needed before registering equipment or assigning operators." /> : <div className="department-list">{departments.map(department => <article className="surface department-card" key={department.id}><span className="equipment-symbol"><Building2 size={21} /></span><div><h3>{department.name}</h3><p>{department.code} · Department #{department.id}</p></div><Button variant="ghost" size="icon" aria-label={`Copy department ${department.id}`} onClick={() => void navigator.clipboard.writeText(String(department.id)).then(() => toast.success('Department ID copied')).catch(() => toast.error('Clipboard unavailable'))}><Copy size={15} /></Button></article>)}</div>}
    <Dialog open={open} onOpenChange={value => { if (!value && !busy) setOpen(false) }}><DialogContent><DialogHeader><DialogTitle>Add a department</DialogTitle><DialogDescription>The department belongs to your assigned hospital.</DialogDescription></DialogHeader><form className="dialog-form" onSubmit={event => void create(event)}><div className="form-field"><label htmlFor="department-code">Department code</label><Input id="department-code" name="code" placeholder="RADIOLOGY" maxLength={30} required /></div><div className="form-field"><label htmlFor="department-name">Name</label><Input id="department-name" name="name" placeholder="Radiology department" maxLength={120} required /></div>{error && <div className="form-alert" role="alert">{error}</div>}<div className="form-actions"><Button variant="outline" type="button" disabled={busy} onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" disabled={busy || !workspace.online}>Create department</Button></div></form></DialogContent></Dialog>
  </>
}
