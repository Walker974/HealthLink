import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, ArrowUpRight, Building2, Clock3, Info, MapPin, Navigation, RefreshCw, Share2, ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { StatusBadge } from '@/components/shared/status-badge'
import { EmptyState, ErrorState, LoadingState } from '@/components/shared/page-state'
import { organizationApi } from '@/features/organization/api'
import { usePreview } from '@/lib/use-preview'
import { formatTime } from '@/lib/utils'
import { imagingTypes } from '../types'
import { previewAvailability, sampleHospitals } from '../preview-data'
import { searchAvailability } from '../api'

export default function HospitalPage() {
  const { hospitalId } = useParams()
  const id = Number(hospitalId)
  const preview = usePreview()
  const valid = Number.isSafeInteger(id) && id > 0
  const hospitalQuery = useQuery({ queryKey: ['hospital', preview.enabled, id], enabled: valid,
    queryFn: async () => {
      if (!preview.enabled) return organizationApi.hospital(id)
      const sample = sampleHospitals.find(h => h.id === id)
      if (!sample) throw new Error('This sample hospital was not found.')
      return { id, code: 'DEMO', name: sample.name, city: sample.city, address: sample.address, regionId: 1 }
    },
  })
  const hospital = hospitalQuery.data
  const services = useQuery({ queryKey: ['hospital-services', preview.enabled, id, hospital?.city], enabled: !!hospital,
    queryFn: () => Promise.all(imagingTypes.map(async type => {
      const result = preview.enabled ? { hospitals: previewAvailability(hospital!.city, type.code), source: 'preview' }
        : await searchAvailability(hospital!.city, type.code, false)
      return { ...type, listing: result.hospitals.find(h => h.id === id), saved: result.source === 'offline' }
    })),
  })
  if (!valid) return <EmptyState title="Hospital not found" description="Please return to search and choose a hospital." />
  if (hospitalQuery.isPending) return <LoadingState label="Opening hospital details…" />
  if (hospitalQuery.isError) return <ErrorState error={hospitalQuery.error} retry={() => void hospitalQuery.refetch()} />
  async function share() {
    try {
      if (navigator.share) await navigator.share({ title: hospital!.name, url: window.location.href })
      else { await navigator.clipboard.writeText(window.location.href); toast.success('Hospital link copied') }
    } catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) toast.error('Sharing is unavailable. You can copy the page address instead.') }
  }
  return <div className="site-width hospital-page"><Link className="back-link" to={preview.href('/')}><ArrowLeft size={16} />Back to search</Link>
    <div className="hospital-page-heading"><span className="detail-hospital-icon"><Building2 size={34} /></span><div><span className="section-kicker">PUBLIC HOSPITAL</span><h1>{hospital!.name}</h1><p><MapPin size={15} />{hospital!.city}, Morocco</p></div></div>
    <div className="hospital-page-grid"><section className="surface hospital-services"><div className="panel-heading"><div><span className="section-kicker">PLAN YOUR NEXT STEP</span><h2>Imaging services</h2></div><Button variant="ghost" size="icon" aria-label="Refresh hospital services" disabled={services.isFetching} onClick={() => void services.refetch()}><RefreshCw size={17} className={services.isFetching ? 'animate-spin' : ''} /></Button></div>
      {services.isPending ? <LoadingState label="Checking imaging availability…" /> : services.isError ? <ErrorState error={services.error} retry={() => void services.refetch()} /> : services.data.map(service => <div className="hospital-service-row" key={service.code}>
        <span className="service-code">{service.label}</span><div><h3>{service.name}</h3><p>{service.listing ? `${service.listing.equipmentCount} machines · ${service.saved ? 'saved' : 'updated'} ${formatTime(service.listing.updatedAt)}` : 'No available equipment is currently listed.'}</p></div>
        {service.listing ? <StatusBadge status={service.listing.status} /> : <Badge variant="outline">Not listed</Badge>}
      </div>)}
      <div className="care-notice"><Info size={19} /><p>Equipment availability does not confirm an appointment or same-day scan. Please contact the hospital before travelling.</p></div>
    </section><aside><div className="surface visit-panel"><span className="section-kicker">BEFORE YOU GO</span><h2>A little preparation helps.</h2><div className="detail-address"><MapPin size={19} /><div><strong>Address</strong><p>{hospital!.address}</p></div></div>
      <p className="muted-copy">Confirm reception hours, referral requirements, and service access directly with the hospital.</p>
      {!preview.enabled && <Button asChild className="w-full"><a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${hospital!.name} ${hospital!.address} ${hospital!.city}`)}`} target="_blank" rel="noreferrer"><Navigation size={17} />Open directions<ArrowUpRight size={15} /></a></Button>}
      <Button variant="outline" className="w-full" onClick={() => void share()}><Share2 size={16} />Share hospital</Button>
      <div className="visit-security"><ShieldCheck size={17} />Public information, no account required.</div>
      {preview.enabled && <p className="sample-note"><Clock3 size={14} />Sample hospital and address for design review.</p>}
    </div></aside></div>
  </div>
}
