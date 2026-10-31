import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useRegisterSW } from 'virtual:pwa-register/react'
import { Download, RefreshCw, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'

interface InstallPrompt extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}
const InstallContext = createContext<() => void>(() => undefined)

export function PwaProvider({ children }: { children: ReactNode }) {
  const [prompt, setPrompt] = useState<InstallPrompt | null>(null)
  const [help, setHelp] = useState(false)
  const { needRefresh: [needRefresh, setNeedRefresh], updateServiceWorker } = useRegisterSW()
  useEffect(() => {
    const capture = (event: Event) => { event.preventDefault(); setPrompt(event as InstallPrompt) }
    const installed = () => setPrompt(null)
    window.addEventListener('beforeinstallprompt', capture)
    window.addEventListener('appinstalled', installed)
    return () => { window.removeEventListener('beforeinstallprompt', capture); window.removeEventListener('appinstalled', installed) }
  }, [])
  async function install() {
    if (!prompt) { setHelp(true); return }
    await prompt.prompt()
    await prompt.userChoice
    setPrompt(null)
  }
  return <InstallContext.Provider value={() => void install()}>
    {children}
    {needRefresh && <div className="pwa-update" role="status"><RefreshCw size={20} />
      <div><strong>A fresh version is ready</strong><p>Save your work before updating HealthLink.</p></div>
      <Button onClick={() => void updateServiceWorker(true)}>Update</Button>
      <Button variant="ghost" size="icon" aria-label="Dismiss update notice" onClick={() => setNeedRefresh(false)}><X size={16} /></Button>
    </div>}
    <Dialog open={help} onOpenChange={setHelp}><DialogContent><DialogHeader>
      <span className="dialog-symbol"><Download size={24} /></span>
      <DialogTitle>Keep HealthLink close</DialogTitle>
      <DialogDescription>Open HealthLink from your home screen, just like an app.</DialogDescription>
    </DialogHeader><div className="install-instructions"><p><strong>On iPhone or iPad</strong><br />Open the browser’s Share menu and choose “Add to Home Screen”.</p>
      <p><strong>On Android or desktop</strong><br />Look for “Install app” or “Add to Home Screen” in your browser menu.</p>
      <p className="muted-copy">Availability depends on your browser and installation support. Offline search shows only previously saved public results, with their original timestamp.</p>
    </div></DialogContent></Dialog>
  </InstallContext.Provider>
}

export function InstallButton({ compact = false }: { compact?: boolean }) {
  const install = useContext(InstallContext)
  return <Button variant="outline" onClick={install} className={compact ? 'install-compact' : ''}>
    <Download size={16} />{compact ? 'Install app' : 'Install HealthLink'}
  </Button>
}
