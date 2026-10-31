import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Activity, ArrowRight, ArrowUpRight, Building2, CheckCircle2, FlaskConical, KeyRound, LoaderCircle, LockKeyhole, ShieldCheck } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { useAuth } from '../auth-provider'
import { roleLabels, staffRoles } from '../types'
import { env } from '@/lib/env'
import { useOnline } from '@/lib/use-online'

export default function SignInPage() {
  const auth = useAuth()
  const navigate = useNavigate()
  const online = useOnline()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  useEffect(() => {
    void auth.initialize().then(authenticated => { if (authenticated) navigate('/workspace', { replace: true }) })
      .catch(() => setError('The sign-in service is currently unavailable. You can still use public search.'))
  }, [auth.initialize, navigate])
  async function signIn() {
    setBusy(true); setError('')
    try { await auth.login() }
    catch { setError('We couldn’t open secure sign-in. Please check your connection and try again.'); setBusy(false) }
  }
  return <section className="site-width signin-page">
    <div className="signin-story"><span className="eyebrow">THE PEOPLE BEHIND EVERY UPDATE</span>
      <h1>Good care takes<br /><span>a connected team.</span></h1><p>One shared place to keep equipment working and the public informed.</p>
      <div className="signin-story-art" aria-hidden="true"><Activity size={130} strokeWidth={1} /><span><CheckCircle2 size={24} /></span><span><Building2 size={24} /></span></div>
      <div className="signin-promises"><span><ShieldCheck size={18} />Access follows your hospital assignment</span><span><CheckCircle2 size={18} />Every repair has a traceable history</span></div>
    </div>
    <div className="signin-form"><span className="dialog-symbol"><LockKeyhole size={25} /></span><span className="section-kicker">HEALTHLINK FOR HOSPITAL TEAMS</span>
      <h2>Welcome back.</h2><p>Sign in with your assigned HealthLink account<br className="desktop-break" /> to open your staff workspace.</p>
      {(!online || error) && <div className="form-alert" role="alert">{!online ? 'Reconnect to sign in. Public saved searches remain available offline.' : error}</div>}
      <Button className="signin-primary" disabled={busy || !online} onClick={() => void signIn()}>{busy ? <LoaderCircle size={18} className="animate-spin" /> : <KeyRound size={18} />}Continue with HealthLink ID<ArrowRight size={17} /></Button>
      <div className="signin-security"><ShieldCheck size={15} /><span>Secure sign-in is handled by Keycloak.<br />Your password is never entered into HealthLink.</span></div>
      <div className="signin-help"><strong>Need access?</strong><p>Your administrator creates your account and links it to your hospital and role.</p><Link to="/">Back to public search <ArrowUpRight size={13} /></Link></div>
      {env.designPreview && <details className="workspace-previews"><summary><FlaskConical size={15} />Explore sample workspaces</summary><p>These are labelled design previews with synthetic data. They do not grant access to the live API.</p>
        {staffRoles.map(role => <Link key={role} to={`/workspace?preview=1&role=${role}`}>{roleLabels[role]}<ArrowUpRight size={14} /></Link>)}
      </details>}
    </div>
  </section>
}
