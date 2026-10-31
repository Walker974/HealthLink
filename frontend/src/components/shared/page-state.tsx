import { AlertCircle, LoaderCircle, SearchX } from 'lucide-react'
import { Button } from '@/components/ui/button'

export function LoadingState({ label = 'Loading your workspace…' }: { label?: string }) {
  return <div className="page-state" role="status"><LoaderCircle className="animate-spin" size={26} /><p>{label}</p></div>
}
export function ErrorState({ error, retry }: { error: unknown; retry?: () => void }) {
  return <div className="page-state error-state" role="alert"><AlertCircle size={28} />
    <h3>We couldn’t load this information</h3><p>{error instanceof Error ? error.message : 'Please reconnect and try again.'}</p>
    {retry && <Button variant="outline" onClick={retry}>Try again</Button>}
  </div>
}
export function EmptyState({ title, description, children }: { title: string; description: string; children?: React.ReactNode }) {
  return <div className="page-state"><span className="empty-icon"><SearchX size={27} /></span>
    <h3>{title}</h3><p>{description}</p>{children}</div>
}
