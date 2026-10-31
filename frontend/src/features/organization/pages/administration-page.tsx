import { useEffect, useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef } from '@tanstack/react-table'
import { Link } from 'react-router-dom'
import { z } from 'zod'
import { ArrowUpRight, Building2, KeyRound, LoaderCircle, Plus, ShieldCheck, Users } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { DataTable } from '@/components/shared/data-table'
import { ErrorState, LoadingState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { usePreview } from '@/lib/use-preview'
import { organizationApi } from '../api'
import type { Hospital, Region } from '../types'
import { staffApi } from '@/features/staff/api'
import { staffRoles, roleLabels, type StaffProfile, type StaffRole } from '@/features/auth/types'
import { sampleHospitals } from '@/features/availability/preview-data'

const codeSchema = z.string().trim().min(1).max(30).toUpperCase().regex(/^[A-Z0-9_-]+$/, 'Use letters, digits, underscores or hyphens for the code.')
const regionSchema = z.object({ code: codeSchema, name: z.string().trim().min(1).max(120) })
const hospitalSchema = z.object({ code: codeSchema, name: z.string().trim().min(1).max(160), city: z.string().trim().min(1).max(100), address: z.string().trim().min(1).max(250), regionId: z.number().int().positive() })
const staffSchema = z.object({ keycloakSubject: z.string().trim().min(1).max(255), fullName: z.string().trim().min(1).max(120), email: z.string().trim().email().max(254), role: z.enum(staffRoles), hospitalId: z.number().int().positive().nullable(), departmentId: z.number().int().positive().nullable() }).superRefine((value, context) => {
  if (['HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN'].includes(value.role) && !value.hospitalId) context.addIssue({ code: 'custom', message: 'Choose a hospital for this role.', path: ['hospitalId'] })
  if (value.role === 'RADIOLOGY_OPERATOR' && !value.departmentId) context.addIssue({ code: 'custom', message: 'Provide the operator’s department ID.', path: ['departmentId'] })
})

export default function AdministrationPage() {
  const workspace = useWorkspace()
  const preview = usePreview()
  const queryClient = useQueryClient()
  const [tab, setTab] = useState('hospitals')
  const [regionId, setRegionId] = useState(0)
  const [page, setPage] = useState(0)
  const [modal, setModal] = useState<'hospital' | 'region' | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [staffRole, setStaffRole] = useState<StaffRole>('HOSPITAL_MANAGER')
  const [linkedStaff, setLinkedStaff] = useState<StaffProfile[]>([])
  const [demoRegions, setDemoRegions] = useState<Region[]>([{ id: 1, code: 'RABAT_SALE_KENITRA', name: 'Rabat-Salé-Kénitra' }])
  const [demoHospitals, setDemoHospitals] = useState<Hospital[]>(sampleHospitals.map((item, index) => ({ id: item.id, code: `DEMO_${index + 1}`, name: item.name, city: item.city, address: item.address, regionId: 1 })))
  const regionsQuery = useQuery({ queryKey: ['admin-regions'], queryFn: () => organizationApi.regions(), enabled: !workspace.preview })
  const regions = workspace.preview ? demoRegions : regionsQuery.data?.items || []
  useEffect(() => { if (!regionId && regions.length) setRegionId(regions[0].id) }, [regionId, regions])
  const hospitalsQuery = useQuery({ queryKey: ['admin-hospitals', regionId, page], queryFn: () => organizationApi.hospitals(regionId, page), enabled: !workspace.preview && regionId > 0 })
  const hospitals = workspace.preview ? demoHospitals.filter(item => item.regionId === regionId) : hospitalsQuery.data?.items || []
  const needsHospital = ['HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN'].includes(staffRole)
  const columns: ColumnDef<Hospital>[] = [
    { accessorKey: 'code', header: 'CODE', cell: ({ row }) => <span className="font-mono text-[10px]">{row.original.code}</span> },
    { accessorKey: 'name', header: 'HOSPITAL', cell: ({ row }) => <Link className="inline-flex items-center gap-2 font-medium" to={preview.href(`/hospitals/${row.original.id}`)}>{row.original.name}<ArrowUpRight size={12} /></Link> },
    { accessorKey: 'city', header: 'CITY' },
    { accessorKey: 'address', header: 'ADDRESS' },
    { id: 'registered', header: 'DIRECTORY', cell: () => <Badge variant="outline" className="status-good">Registered</Badge> },
  ]
  async function createOrganization(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    try {
      if (modal === 'region') {
        const body = regionSchema.parse(values)
        if (workspace.preview) { const id = Math.max(...demoRegions.map(item => item.id)) + 1; setDemoRegions(items => [...items, { ...body, id }]); setRegionId(id) }
        else { const created = await organizationApi.createRegion(body); await queryClient.invalidateQueries({ queryKey: ['admin-regions'] }); setRegionId(created.id) }
      } else {
        const body = hospitalSchema.parse({ ...values, regionId: Number(values.regionId) })
        if (workspace.preview) setDemoHospitals(items => [...items, { ...body, id: Math.max(100, ...items.map(item => item.id)) + 1 }])
        else { await organizationApi.createHospital(body); await queryClient.invalidateQueries({ queryKey: ['admin-hospitals'] }) }
      }
      toast.success(workspace.preview ? 'Sample directory updated' : 'Organization record created'); setModal(null)
    } catch (failure) { setError(failure instanceof z.ZodError ? failure.issues[0].message : failure instanceof Error ? failure.message : 'Unable to save this record.') }
    finally { setBusy(false) }
  }
  async function linkStaff(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(''); setBusy(true)
    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))
    try {
      const body = staffSchema.parse({ ...values, role: staffRole, hospitalId: needsHospital ? Number(values.hospitalId) || null : null, departmentId: staffRole === 'RADIOLOGY_OPERATOR' ? Number(values.departmentId) || null : null })
      const created = workspace.preview ? { ...body, id: 900 + linkedStaff.length } : await staffApi.create(body)
      setLinkedStaff(items => [...items, created]); form.reset(); setStaffRole('HOSPITAL_MANAGER')
      toast.success(workspace.preview ? 'Sample staff profile linked' : 'Staff profile linked')
    } catch (failure) { setError(failure instanceof z.ZodError ? failure.issues[0].message : failure instanceof Error ? failure.message : 'Unable to link this profile.') }
    finally { setBusy(false) }
  }
  return <><div className="page-heading"><div><span className="section-kicker">BUILDING THE PUBLIC HOSPITAL NETWORK</span><h1>Hospitals & staff</h1><p>Onboard organizations and link staff to the right assignment.</p></div><div><Button variant="outline" onClick={() => { setError(''); setModal('region') }}><Plus size={15} />Add region</Button><Button onClick={() => { setError(''); setModal('hospital') }} disabled={!regions.length}><Plus size={15} />Add hospital</Button></div></div>
    <div className="care-notice mb-6"><ShieldCheck size={18} /><p>Platform administration manages organizations and staff provisioning. Equipment access belongs to assigned hospital teams.</p></div>
    <div className="workspace-tabs"><Button variant={tab === 'hospitals' ? 'secondary' : 'ghost'} onClick={() => { setError(''); setTab('hospitals') }}><Building2 size={15} />Hospital directory</Button><Button variant={tab === 'staff' ? 'secondary' : 'ghost'} onClick={() => { setError(''); setTab('staff') }}><Users size={15} />Link staff accounts</Button></div>
    <div className="table-toolbar"><label className="sr-only" htmlFor="admin-region">Region</label><select id="admin-region" className="plain-select max-w-[300px]" value={regionId || ''} onChange={event => { setRegionId(Number(event.target.value)); setPage(0) }}><option value="">Choose a region</option>{regions.map(region => <option key={region.id} value={region.id}>{region.name}</option>)}</select><span>{workspace.preview ? 'Sample directory' : 'Organization records'}</span></div>
    {tab === 'hospitals' ? regionsQuery.error || hospitalsQuery.error ? <ErrorState error={regionsQuery.error || hospitalsQuery.error} /> : !workspace.preview && (regionsQuery.isPending || (regionId > 0 && hospitalsQuery.isPending)) ? <LoadingState /> : <section className="surface table-surface"><DataTable data={hospitals} columns={columns} serverPage={workspace.preview ? undefined : { page, size: 100, totalPages: hospitalsQuery.data?.totalPages || 1, change: setPage }} /></section>
      : <div className="admin-grid"><section className="surface admin-panel"><span className="dialog-symbol"><KeyRound size={24} /></span><h2>Link an existing Keycloak user</h2><p>The account and realm role must already exist in Keycloak. This creates the local staff assignment.</p><form onSubmit={event => void linkStaff(event)}>
        <div className="form-field"><label htmlFor="staff-subject">Keycloak user ID</label><Input id="staff-subject" name="keycloakSubject" required maxLength={255} placeholder="Immutable user ID from Keycloak" /></div>
        <div className="form-field"><label htmlFor="staff-fullname">Full name</label><Input id="staff-fullname" name="fullName" required maxLength={120} autoComplete="off" /></div>
        <div className="form-field"><label htmlFor="staff-email">Email</label><Input id="staff-email" name="email" required type="email" maxLength={254} autoComplete="off" /></div>
        <div className="form-field"><label htmlFor="staff-role">Assigned role</label><select id="staff-role" className="plain-select" value={staffRole} onChange={event => setStaffRole(event.target.value as StaffRole)}>{staffRoles.map(role => <option key={role} value={role}>{roleLabels[role]}</option>)}</select></div>
        {needsHospital && <div className="form-field"><label htmlFor="staff-hospital">Hospital</label><select id="staff-hospital" name="hospitalId" className="plain-select" required><option value="">Choose a hospital in this region</option>{hospitals.map(hospital => <option key={hospital.id} value={hospital.id}>{hospital.name}</option>)}</select></div>}
        {staffRole === 'RADIOLOGY_OPERATOR' && <div className="form-field"><label htmlFor="staff-department">Department ID</label><Input id="staff-department" name="departmentId" type="number" min={1} step={1} required /><p>Use the department ID provided by the hospital manager.</p></div>}
        {error && <div className="form-alert" role="alert">{error}</div>}<Button type="submit" disabled={busy || !workspace.online}>{busy && <LoaderCircle className="animate-spin" size={15} />}Link staff profile</Button>
      </form></section><aside className="surface admin-panel"><h2>Linked in this session</h2><p>Newly linked staff profiles appear here. Full staff directory management is a later feature.</p>{linkedStaff.length ? linkedStaff.map(profile => <div className="mini-equipment-item" key={profile.id}><div><span className="equipment-symbol"><Users size={17} /></span><div><strong>{profile.fullName}</strong><p>{roleLabels[profile.role]}</p></div></div><Badge variant="outline">Linked</Badge></div>) : <div className="page-state"><Users size={27} /><p>No profiles linked during this session yet.</p></div>}</aside></div>}
    <Dialog open={!!modal} onOpenChange={open => { if (!open && !busy) setModal(null) }}><DialogContent><DialogHeader><DialogTitle>{modal === 'region' ? 'Add a region' : 'Add a public hospital'}</DialogTitle><DialogDescription>{modal === 'region' ? 'Create organizational reference data.' : 'Register a hospital in the public directory.'}</DialogDescription></DialogHeader><form className="dialog-form" onSubmit={event => void createOrganization(event)}>
      <div className="form-grid"><div className="form-field"><label htmlFor="org-code">Code</label><Input id="org-code" name="code" required maxLength={30} placeholder={modal === 'region' ? 'SOUSS_MASSA' : 'HOSPITAL_CODE'} /></div><div className="form-field"><label htmlFor="org-name">Name</label><Input id="org-name" name="name" required maxLength={modal === 'region' ? 120 : 160} /></div></div>
      {modal === 'hospital' && <><div className="form-grid"><div className="form-field"><label htmlFor="org-city">City</label><Input id="org-city" name="city" required maxLength={100} /></div><div className="form-field"><label htmlFor="org-region">Region</label><select id="org-region" name="regionId" className="plain-select" defaultValue={regionId} required>{regions.map(region => <option key={region.id} value={region.id}>{region.name}</option>)}</select></div></div><div className="form-field"><label htmlFor="org-address">Address</label><Input id="org-address" name="address" required maxLength={250} /></div></>}
      {error && <div className="form-alert" role="alert">{error}</div>}<div className="form-actions"><Button variant="outline" type="button" disabled={busy} onClick={() => setModal(null)}>Cancel</Button><Button type="submit" disabled={busy || !workspace.online}>{busy && <LoaderCircle className="animate-spin" size={15} />}Create {modal}</Button></div>
    </form></DialogContent></Dialog>
  </>
}
