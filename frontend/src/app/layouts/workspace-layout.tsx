import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, Outlet, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowUpRight, Building2, ChevronRight, CircleHelp, ClipboardList, FlaskConical, History, LayoutDashboard, LogOut, Menu, MonitorCog, ShieldCheck, UserCircle, Users, Wifi, WifiOff } from 'lucide-react'
import { Brand } from '@/components/shared/brand'
import { PreviewBanner } from '@/components/shared/preview-banner'
import { ErrorState, LoadingState } from '@/components/shared/page-state'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { useAuth } from '@/features/auth/auth-provider'
import { useStaffProfile } from '@/features/auth/api'
import { roleLabels, staffRoles, type StaffRole } from '@/features/auth/types'
import { organizationApi } from '@/features/organization/api'
import { useOnline } from '@/lib/use-online'
import { usePreview } from '@/lib/use-preview'
import { WorkspaceProvider, type WorkspaceIdentity } from '@/app/providers/workspace-provider'

const previewNames: Record<StaffRole, string> = { RADIOLOGY_OPERATOR: 'Salma Bennani', MAINTENANCE_TECHNICIAN: 'Youssef Alaoui', HOSPITAL_MANAGER: 'Meryem Amrani', MINISTRY_OFFICER: 'Nour Benali', PLATFORM_ADMIN: 'Amine El Idrissi' }
const hospitalRoles: StaffRole[] = ['HOSPITAL_MANAGER', 'RADIOLOGY_OPERATOR', 'MAINTENANCE_TECHNICIAN']

export default function WorkspaceLayout() {
  const auth = useAuth()
  const preview = usePreview()
  const profile = useStaffProfile()
  const [authError, setAuthError] = useState<unknown>(null)
  useEffect(() => { if (!preview.enabled) void auth.initialize().catch(setAuthError) }, [preview.enabled, auth.initialize])
  const role = preview.enabled ? preview.role : profile.data?.role || (auth.roles.includes('PLATFORM_ADMIN') ? 'PLATFORM_ADMIN' : undefined)
  const hospital = useQuery({ queryKey: ['workspace-hospital', profile.data?.hospitalId],
    enabled: !preview.enabled && !!profile.data?.hospitalId,
    queryFn: () => organizationApi.hospital(profile.data!.hospitalId!),
  })
  if (!preview.enabled) {
    if (authError) return <ErrorState error={authError} retry={() => window.location.reload()} />
    if (!auth.ready || (auth.authenticated && profile.isPending && !auth.roles.includes('PLATFORM_ADMIN'))) return <LoadingState />
    if (!auth.authenticated) return <Navigate to="/staff" replace />
    if (!role || !auth.roles.includes(role)) return <div className="signed-out-state"><ErrorState error={profile.error || new Error('Your account needs an active staff profile matching its Keycloak role. Please contact your administrator.')} /><Button asChild><Link to="/staff">Return to sign in</Link></Button></div>
  }
  const identity: WorkspaceIdentity = { role: role!, name: preview.enabled ? previewNames[role!] : profile.data?.fullName || auth.name,
    hospitalName: preview.enabled ? hospitalRoles.includes(role!) ? 'Hay Riad Regional Hospital' : 'HealthLink administration'
      : hospital.data?.name || (role === 'PLATFORM_ADMIN' ? 'Platform administration' : role === 'MINISTRY_OFFICER' ? 'Ministry workspace' : 'Your assigned hospital'),
    profile: preview.enabled ? { id: role === 'MAINTENANCE_TECHNICIAN' ? 2 : role === 'HOSPITAL_MANAGER' ? 3 : 1,
      keycloakSubject: 'sample-profile', fullName: previewNames[role!], email: 'demo@example.test', role: role!, hospitalId: hospitalRoles.includes(role!) ? 101 : null, departmentId: role === 'RADIOLOGY_OPERATOR' ? 10 : null } : profile.data,
  }
  return <WorkspaceProvider identity={identity}><WorkspaceShell identity={identity} /></WorkspaceProvider>
}

function WorkspaceShell({ identity }: { identity: WorkspaceIdentity }) {
  const preview = usePreview()
  const auth = useAuth()
  const online = useOnline()
  const [params] = useSearchParams()
  const [menu, setMenu] = useState(false)
  const role = identity.role
  function href(path: string) {
    const url = new URL(preview.href(path), window.location.origin)
    if (params.has('equipment')) url.searchParams.set('equipment', params.get('equipment')!)
    return url.pathname + url.search
  }
  const links = [
    { path: '/workspace', label: 'Overview', Icon: LayoutDashboard, show: true },
    { path: '/workspace/equipment', label: 'Equipment', Icon: MonitorCog, show: hospitalRoles.includes(role) },
    { path: '/workspace/repairs', label: 'Repairs', Icon: ClipboardList, show: hospitalRoles.includes(role) },
    { path: '/workspace/history', label: 'Audit history', Icon: History, show: hospitalRoles.includes(role) },
    { path: '/workspace/department', label: 'Departments', Icon: Building2, show: role === 'HOSPITAL_MANAGER' },
    { path: '/workspace/organization', label: 'Hospitals & staff', Icon: Users, show: role === 'PLATFORM_ADMIN' },
    { path: '/workspace/ministry', label: 'Availability reports', Icon: Building2, show: role === 'MINISTRY_OFFICER' },
    { path: '/workspace/profile', label: 'My profile', Icon: UserCircle, show: true },
  ].filter(link => link.show)
  const navigation = <><div className="workspace-brand"><Brand light /></div><div className="sidebar-scope"><span>YOUR WORKSPACE</span><strong>{roleLabels[role]}</strong></div>
    <nav aria-label="Staff workspace">{links.map(({ path, label, Icon }) => <NavLink key={path} to={href(path)} end={path === '/workspace'} onClick={() => setMenu(false)}><Icon size={18} strokeWidth={1.7} />{label}</NavLink>)}</nav>
    <div className="sidebar-bottom"><div className="sidebar-reassurance"><ShieldCheck size={20} /><strong>Accountable by design.</strong><p>Every repair is verified before equipment returns to service.</p></div>
      <Link className="sidebar-public-link" to={preview.href('/')}><ArrowUpRight size={16} />Open public search</Link>
      <div className="sidebar-profile"><span className="avatar">{identity.name.split(' ').slice(0, 2).map(part => part[0]).join('')}</span><div><strong>{identity.name}</strong><span>{roleLabels[role]}</span></div>
        {!preview.enabled && <Button variant="ghost" size="icon" aria-label="Sign out" onClick={() => void auth.logout()}><LogOut size={16} /></Button>}</div>
    </div></>
  return <div className="workspace-app"><a href="#workspace-content" className="skip-link">Skip to workspace</a><PreviewBanner />
    <div className="workspace-frame"><aside className="workspace-sidebar">{navigation}</aside><div className="workspace-main">
      <header className="workspace-topbar"><div><Button variant="ghost" size="icon" className="workspace-menu-button" aria-label="Open workspace navigation" onClick={() => setMenu(true)}><Menu size={21} /></Button><Building2 size={18} className="topbar-hospital-icon" /><span>{identity.hospitalName}</span></div>
        <div><span className={`workspace-online ${online ? '' : 'is-offline'}`}>{online ? <Wifi size={14} /> : <WifiOff size={14} />}{online ? 'Online' : 'Offline'}</span><span className="topbar-divider" /><Link to={href('/workspace/profile')} className="topbar-user">{identity.name.split(' ')[0]}<ChevronRight size={14} /></Link></div>
      </header>
      {preview.enabled && <div className="preview-role-selector"><FlaskConical size={14} /><label htmlFor="preview-role">Preview workspace</label><select id="preview-role" value={role} onChange={event => { window.location.href = `/workspace?preview=1&role=${event.target.value}` }}>
        {staffRoles.map(item => <option key={item} value={item}>{roleLabels[item]}</option>)}
      </select><span>Synthetic data · local preview only</span></div>}
      {!online && <div className="workspace-offline-notice" role="status"><WifiOff size={17} />Reconnect before submitting staff actions. Changes are never queued offline.</div>}
      <main id="workspace-content" className="workspace-content"><Outlet /></main>
      <footer className="workspace-footer"><span>HealthLink · Care, with clarity.</span><span><CircleHelp size={13} />Need help? Contact your hospital administrator.</span></footer>
    </div></div>
    <Sheet open={menu} onOpenChange={setMenu}><SheetContent side="left" className="workspace-mobile-sidebar"><SheetHeader className="sr-only"><SheetTitle>Workspace navigation</SheetTitle></SheetHeader>{navigation}</SheetContent></Sheet>
  </div>
}

