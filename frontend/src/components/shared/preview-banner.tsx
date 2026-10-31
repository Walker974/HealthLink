import { FlaskConical, ArrowUpRight } from 'lucide-react'
import { Link, useLocation } from 'react-router-dom'
import { usePreview } from '@/lib/use-preview'

export function PreviewBanner() {
  const preview = usePreview()
  const location = useLocation()
  if (!preview.enabled) return null
  return <div className="preview-banner"><div className="site-width">
    <span><FlaskConical size={14} aria-hidden="true" /><strong>Design preview</strong><span className="preview-copy">Sample data. Changes affect examples only.</span></span>
    <Link to={location.pathname}>Use live application <ArrowUpRight size={14} /></Link>
  </div></div>
}
