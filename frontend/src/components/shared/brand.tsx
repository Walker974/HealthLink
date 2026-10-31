import { Activity } from 'lucide-react'
import { Link } from 'react-router-dom'
import { usePreview } from '@/lib/use-preview'
import { cn } from '@/lib/utils'

export function Brand({ light = false }: { light?: boolean }) {
  const preview = usePreview()
  return <Link to={preview.href('/')} className={cn('brand', light && 'brand-light')} aria-label="HealthLink home">
    <span className="brand-icon"><Activity size={23} strokeWidth={2.2} aria-hidden="true" /></span>
    <span>Health<span className="brand-link">Link</span><span className="brand-country">MOROCCO</span></span>
  </Link>
}
