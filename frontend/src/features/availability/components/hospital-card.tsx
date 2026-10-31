import { ArrowUpRight, Building2, Clock3, MapPin } from 'lucide-react'
import { StatusBadge } from '@/components/shared/status-badge'
import { cn, formatTime } from '@/lib/utils'
import type { HospitalListing } from '../types'

export function HospitalCard({ hospital, selected, onSelect }: {
  hospital: HospitalListing; selected: boolean; onSelect: () => void;
}) {
  return <button className={cn('hospital-result', selected && 'hospital-result-selected')} onClick={onSelect}
    aria-pressed={selected} aria-label={`View ${hospital.name}`}>
    <div className="result-topline"><span className="hospital-symbol"><Building2 size={21} strokeWidth={1.7} /></span>
      <StatusBadge status={hospital.status} /></div>
    <h3>{hospital.name}</h3>
    <p className="result-area"><MapPin size={13} />{hospital.area || hospital.city}<span>·</span>Public hospital</p>
    <div className="result-bottom"><span><Clock3 size={13} />Updated {formatTime(hospital.updatedAt)}</span><ArrowUpRight size={17} /></div>
  </button>
}
