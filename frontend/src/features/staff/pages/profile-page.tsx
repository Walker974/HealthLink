import { KeyRound, LogOut, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { useAuth } from '@/features/auth/auth-provider'
import { roleLabels } from '@/features/auth/types'

export default function ProfilePage() {
  const workspace = useWorkspace()
  const auth = useAuth()
  return <><div className="page-heading"><div><span className="section-kicker">YOUR ACCOUNT & ASSIGNMENT</span><h1>My profile</h1><p>Your identity comes from HealthLink ID. Your assignment determines your workspace.</p></div></div>
    <div className="profile-grid"><section className="surface profile-card"><span className="avatar">{workspace.name.split(' ').slice(0, 2).map(value => value[0]).join('')}</span><h2>{workspace.name}</h2><p className="muted-copy">{roleLabels[workspace.role]}</p><dl>
      <div><dt>Email</dt><dd>{workspace.profile?.email || 'Contact your platform administrator'}</dd></div>
      <div><dt>Hospital assignment</dt><dd>{workspace.profile?.hospitalId ? workspace.hospitalName : 'No hospital assignment for this role'}</dd></div>
      <div><dt>Department assignment</dt><dd>{workspace.profile?.departmentId ? `Department #${workspace.profile.departmentId}` : 'Not required for this role'}</dd></div>
      <div><dt>Local staff profile</dt><dd>{workspace.profile ? `#${workspace.profile.id}` : 'Not linked yet; platform provisioning remains available'}</dd></div>
    </dl></section><aside className="surface profile-card"><span className="dialog-symbol"><ShieldCheck size={25} /></span><h2>Access with a clear boundary.</h2><p className="muted-copy">Your Keycloak role and local staff assignment must agree. Hospital and department checks run on the server for every protected operation.</p>
      <div className="care-notice"><KeyRound size={18} /><p>Your password is managed by Keycloak. HealthLink keeps access tokens in memory and does not save them for offline use.</p></div>
      {!workspace.preview && <Button variant="outline" className="mt-6" onClick={() => void auth.logout()}><LogOut size={16} />Sign out</Button>}
    </aside></div>
  </>
}
