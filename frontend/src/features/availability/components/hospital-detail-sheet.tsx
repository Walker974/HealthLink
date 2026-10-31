import { ArrowUpRight, Building2, Clock3, Info, MapPin } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { StatusBadge } from '@/components/shared/status-badge'
import { usePreview } from '@/lib/use-preview'
import { formatTime } from '@/lib/utils'
import type { HospitalListing, ImagingCode } from '../types'

export function HospitalDetailSheet({ hospital, type, onClose }: {
  hospital: HospitalListing | null; type: ImagingCode; onClose: () => void;
}) {
  const preview = usePreview()
  return <Sheet open={!!hospital} onOpenChange={open => { if (!open) onClose() }}><SheetContent className="hospital-detail-sheet">
    {hospital && <><SheetHeader><span className="detail-hospital-icon"><Building2 size={30} /></span>
      <SheetTitle>{hospital.name}</SheetTitle><SheetDescription>Public hospital · {hospital.city}</SheetDescription>
    </SheetHeader><div className="detail-sheet-body"><StatusBadge status={hospital.status} />
      <div className="detail-service"><span>{type}</span><div><strong>{hospital.equipmentCount} imaging {hospital.equipmentCount === 1 ? 'machine' : 'machines'}</strong>
        <p><Clock3 size={13} />Updated {formatTime(hospital.updatedAt)}</p></div></div>
      <div className="detail-address"><MapPin size={19} /><div><strong>Hospital address</strong><p>{hospital.address}</p></div></div>
      <div className="care-notice"><Info size={18} /><p>“Available” means the equipment is working. Contact the hospital before travelling to confirm access to the service.</p></div>
      <Button asChild className="w-full"><Link to={preview.href(`/hospitals/${hospital.id}`)}>View hospital details<ArrowUpRight size={16} /></Link></Button>
    </div></>}
  </SheetContent></Sheet>
}
