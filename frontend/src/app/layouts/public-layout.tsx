import { ArrowUpRight, HeartHandshake, Menu, ShieldCheck, WifiOff } from 'lucide-react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { useState } from 'react'
import { Brand } from '@/components/shared/brand'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { PreviewBanner } from '@/components/shared/preview-banner'
import { InstallButton } from '@/app/providers/pwa-provider'
import { usePreview } from '@/lib/use-preview'
import { useOnline } from '@/lib/use-online'
import { useAuth } from '@/features/auth/auth-provider'

export default function PublicLayout() {
  const preview = usePreview()
  const online = useOnline()
  const auth = useAuth()
  const [menu, setMenu] = useState(false)
  return <div className="public-app">
    <a href="#main-content" className="skip-link">Skip to content</a>
    <PreviewBanner />
    <header className="public-header"><div className="site-width header-inner">
      <Brand />
      <nav className="desktop-nav" aria-label="Main navigation">
        <NavLink to={preview.href('/')} end>Find imaging</NavLink>
        <Link to={preview.href('/#how-it-works')}>How it works</Link>
        <Link to={preview.href('/staff')}>For hospital teams</Link>
      </nav>
      <div className="header-actions">{!online && <span className="connection-label"><WifiOff size={14} /> Offline</span>}
        <Button asChild variant="outline" className="staff-signin"><Link to={preview.href(auth.authenticated ? '/workspace' : '/staff')}>
          {auth.authenticated ? 'My workspace' : 'Staff sign in'}<ArrowUpRight size={15} />
        </Link></Button>
        <Button variant="ghost" size="icon" className="mobile-menu-button" onClick={() => setMenu(true)} aria-label="Open navigation"><Menu size={22} /></Button>
      </div>
    </div></header>
    <main id="main-content"><Outlet /></main>
    <footer className="public-footer"><div className="site-width footer-main"><div><Brand /><p>Care starts with clear information.</p></div>
      <div className="footer-note"><ShieldCheck size={18} /><span>No account needed to search.<br /><strong>Your location stays yours.</strong></span></div>
      <InstallButton />
    </div><div className="site-width footer-bottom"><span><HeartHandshake size={14} /> Built for Morocco. Designed for people.</span><span>Equipment availability is not an appointment.</span></div></footer>
    <Sheet open={menu} onOpenChange={setMenu}><SheetContent side="right" className="mobile-nav-sheet"><SheetHeader><SheetTitle>Explore HealthLink</SheetTitle></SheetHeader>
      <nav aria-label="Mobile navigation"><Link onClick={() => setMenu(false)} to={preview.href('/')}>Find imaging</Link>
        <Link onClick={() => setMenu(false)} to={preview.href('/#how-it-works')}>How it works</Link>
        <Link onClick={() => setMenu(false)} to={preview.href('/staff')}>Hospital staff</Link></nav><InstallButton />
    </SheetContent></Sheet>
  </div>
}
