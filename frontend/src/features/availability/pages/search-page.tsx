import { lazy, Suspense, useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Activity, ArrowRight, ArrowUpRight, Check, CircleCheck, Clock3, Grid2X2, HeartHandshake, Info, List, Map, MapPin, Maximize2, Navigation, ScanLine, Search, ShieldCheck, SlidersHorizontal, Waves, WifiOff } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { EmptyState, ErrorState } from '@/components/shared/page-state'
import { usePreview } from '@/lib/use-preview'
import { useOnline } from '@/lib/use-online'
import { env } from '@/lib/env'
import { cn, formatTime } from '@/lib/utils'
import { imagingTypes, type HospitalListing, type ImagingCode } from '../types'
import { useAvailability } from '../api'
import { HospitalCard } from '../components/hospital-card'
import { HospitalDetailSheet } from '../components/hospital-detail-sheet'

const HospitalMap = lazy(() => import('../components/hospital-map'))
const typeIcons = { MRI: ScanLine, CT: Activity, XRAY: Navigation, US: Waves }
const statusOrder = { AVAILABLE: 0, UNDER_MAINTENANCE: 1, OUT_OF_SERVICE: 2 }

export default function SearchPage() {
  const preview = usePreview()
  const online = useOnline()
  const [params, setParams] = useSearchParams()
  const city = params.get('city')?.trim() || 'Rabat'
  const type = (imagingTypes.find(item => item.code === params.get('type'))?.code || 'MRI') as ImagingCode
  const [cityDraft, setCityDraft] = useState(city)
  const [availableOnly, setAvailableOnly] = useState(false)
  const [sort, setSort] = useState('availability')
  const [view, setView] = useState<'split' | 'list' | 'map'>('split')
  const [selected, setSelected] = useState<HospitalListing | null>(null)
  const query = useAvailability(city, type, preview.enabled)
  useEffect(() => { setCityDraft(city); setSelected(null) }, [city, type])
  const hospitals = useMemo(() => {
    const result = [...(query.data?.hospitals || [])].filter(h => !availableOnly || h.status === 'AVAILABLE')
    return result.sort((a, b) => sort === 'recent' ? b.updatedAt.localeCompare(a.updatedAt)
      : sort === 'name' ? a.name.localeCompare(b.name)
        : statusOrder[a.status] - statusOrder[b.status] || a.name.localeCompare(b.name))
  }, [query.data, availableOnly, sort])
  const availableCount = query.data?.hospitals.filter(h => h.status === 'AVAILABLE').length || 0
  const saved = query.data?.source === 'offline' || (!online && !!query.data && !preview.enabled)
  function updateSearch(nextCity = city, nextType = type) {
    const next = new URLSearchParams(params)
    next.set('city', nextCity); next.set('type', nextType)
    setParams(next)
  }
  function submit(event: FormEvent) { event.preventDefault(); if (cityDraft.trim()) updateSearch(cityDraft.trim()) }

  return <>
    <section className="search-hero site-width">
      <span className="eyebrow"><span className="eyebrow-dot" />PUBLIC HOSPITAL IMAGING, MADE CLEAR</span>
      <h1>Find the right scan.<br /><span>Know where to go.</span></h1>
      <p>Find working imaging services in Morocco’s public hospitals.<br className="desktop-break" /> A little clarity, when it matters most.</p>
      <div className="hero-reassurance"><span><ShieldCheck size={15} />No account needed</span><span className="reassurance-divider" /><span><HeartHandshake size={16} />Updated by hospital teams</span></div>
    </section>

    <section className="site-width search-section" aria-label="Find imaging services">
      <div className="search-toolbar">
        <form className="city-search" onSubmit={submit}><MapPin size={20} aria-hidden="true" />
          <div><label htmlFor="city-search">Where are you looking?</label><Input id="city-search" name="city" list="city-options" value={cityDraft}
            onChange={e => setCityDraft(e.target.value)} placeholder="Enter a city" maxLength={100} required autoComplete="address-level2" /></div>
          <datalist id="city-options">{['Rabat', 'Fes', 'Casablanca', 'Marrakech', 'Tanger', 'Agadir', 'Meknes', 'Oujda'].map(item => <option key={item} value={item} />)}</datalist>
          <Button type="submit" size="icon" aria-label="Search this city"><Search size={19} /></Button>
        </form>
        <div className="imaging-filters" aria-label="Imaging type">{imagingTypes.map(item => {
          const Icon = typeIcons[item.code]
          return <button key={item.code} className={cn('imaging-filter', item.code === type && 'imaging-filter-active')}
            onClick={() => updateSearch(city, item.code)} aria-pressed={item.code === type}>
            <Icon size={19} strokeWidth={1.6} aria-hidden="true" /><span>{item.label}</span>{item.code === type && <Check size={13} className="type-check" />}
          </button>
        })}</div>
      </div>
      <div className="search-results-heading"><div><div className="section-kicker">YOUR NEXT STEP</div><h2>{imagingTypes.find(item => item.code === type)?.label} services in {city}</h2>
        <p>{query.isPending ? 'Finding available services…' : query.isError ? 'Search results are currently unavailable.' : <><strong>{availableCount} {availableCount === 1 ? 'hospital' : 'hospitals'} with available equipment</strong><span className="dot-separator">·</span>{query.data?.hospitals.length || 0} results{preview.enabled ? ' in this preview' : ''}</>}</p></div>
        <div className="result-view-switch" aria-label="Results layout">
          <Button variant={view === 'split' ? 'secondary' : 'ghost'} size="icon" className="split-view-button" aria-label="Show list and map" aria-pressed={view === 'split'} onClick={() => setView('split')}><Grid2X2 size={17} /></Button>
          <Button variant={view === 'list' ? 'secondary' : 'ghost'} size="icon" aria-label="Show list" aria-pressed={view === 'list'} onClick={() => setView('list')}><List size={18} /></Button>
          <Button variant={view === 'map' ? 'secondary' : 'ghost'} size="icon" aria-label="Show map" aria-pressed={view === 'map'} onClick={() => setView('map')}><Map size={18} /></Button>
        </div>
      </div>
      {saved && <div className="offline-notice" role="status"><WifiOff size={19} /><div><strong>Showing your saved search</strong><p>Saved {formatTime(query.data!.fetchedAt)}. Equipment availability may have changed.</p></div><Button variant="outline" onClick={() => void query.refetch()}>Retry</Button></div>}
      <div className="result-controls"><Button variant={availableOnly ? 'secondary' : 'outline'} size="sm" aria-pressed={availableOnly} onClick={() => setAvailableOnly(!availableOnly)}><CircleCheck size={14} />Available now</Button>
        <div className="sort-control"><SlidersHorizontal size={14} /><label htmlFor="result-sort" className="sr-only">Sort hospitals</label><select id="result-sort" value={sort} onChange={e => setSort(e.target.value)}>
          <option value="availability">Availability first</option><option value="name">Hospital name</option><option value="recent">Recently updated</option>
        </select></div>
        {query.data && !saved && <span className="search-timestamp"><Clock3 size={12} />{preview.enabled ? 'Sample results' : `Checked ${formatTime(query.data.fetchedAt)}`}</span>}
      </div>

      <div className={cn('results-layout', `results-view-${view}`)}>
        <div className="results-list" aria-label="Hospital results" aria-busy={query.isFetching}>
          {query.isPending ? Array.from({ length: 3 }, (_, i) => <div className="result-skeleton" key={i}><Skeleton className="h-10 w-10 rounded-xl" /><Skeleton className="mt-4 h-5 w-3/4" /><Skeleton className="mt-3 h-4 w-1/2" /></div>)
            : query.isError ? <ErrorState error={query.error} retry={() => void query.refetch()} />
              : hospitals.length ? hospitals.map(hospital => <HospitalCard key={hospital.id} hospital={hospital} selected={selected?.id === hospital.id} onSelect={() => setSelected(hospital)} />)
                : <EmptyState title="No matching services yet" description={availableOnly ? 'Try including all statuses, another imaging type, or a different city.' : `No matching equipment is listed for ${city}. Try another imaging type or city.`}>
                  {availableOnly && <Button variant="outline" onClick={() => setAvailableOnly(false)}>Show all results</Button>}
                  {!preview.enabled && env.designPreview && <Button variant="outline" asChild><Link to="/?preview=1&city=Rabat&type=MRI">Explore sample results</Link></Button>}
                </EmptyState>}
        </div>
        <div className="map-panel" aria-label="Hospital map">
          {preview.enabled && hospitals.some(h => h.position) ? <Suspense fallback={<Skeleton className="h-full w-full" />}><HospitalMap hospitals={hospitals} selectedId={selected?.id} onSelect={setSelected} /></Suspense>
            : <div className="map-placeholder"><div className="map-placeholder-river" /><div className="map-placeholder-block one" /><div className="map-placeholder-block two" />
              <div className="map-placeholder-message"><span><MapPin size={29} /></span><h3>A clearer picture of nearby care</h3><p>{query.data?.hospitals.length ? 'Hospital names and addresses are ready in the list. Map pins will appear when verified coordinates are available.' : 'Choose a city and imaging type to explore available public hospital services.'}</p></div>
            </div>}
          <div className="map-top-label"><span><MapPin size={14} />{city}{preview.enabled ? ' · illustrative locations' : ''}</span><Button variant="outline" size="icon" aria-label={view === 'map' ? 'Restore list and map' : 'Expand map'} onClick={() => setView(view === 'map' ? 'split' : 'map')}><Maximize2 size={15} /></Button></div>
          <div className="map-legend"><span><i className="legend-good" />Available</span><span><i className="legend-pending" />In repair</span><span><i className="legend-bad" />Out of service</span></div>
        </div>
      </div>
      <div className="availability-footnote"><Info size={16} /><p>Working equipment is a starting point, not an appointment. Contact the hospital before you travel.</p></div>
    </section>

    <section className="how-section site-width" id="how-it-works"><div className="how-heading"><div><span className="section-kicker">LESS UNCERTAINTY. MORE CONFIDENCE.</span><h2>A simpler path to the care you need.</h2></div><p>Clear information helps you plan your next step.</p></div>
      <div className="how-grid">{[
        { number: '01', Icon: Search, title: 'Find your service', text: 'Choose an imaging type and a city. No account, no extra steps.' },
        { number: '02', Icon: ShieldCheck, title: 'Check availability', text: 'See equipment status and when the hospital team last updated it.' },
        { number: '03', Icon: Navigation, title: 'Plan your visit', text: 'Check the hospital’s address and contact reception before travelling.' },
      ].map(({ number, Icon, title, text }) => <article key={number} className="how-card"><span className="how-number">{number}</span><Icon size={23} strokeWidth={1.5} /><h3>{title}</h3><p>{text}</p></article>)}</div>
    </section>
    <section className="site-width"><div className="teams-cta"><div><span className="section-kicker">FOR THE PEOPLE BEHIND THE CARE</span><h2>Working together.<br />Keeping services available.</h2><p>A shared workspace for reporting faults, coordinating repairs,<br className="desktop-break" /> and independently verifying equipment before it returns to service.</p><Button asChild><Link to={preview.href('/staff')}>Explore the staff workspace<ArrowUpRight size={17} /></Link></Button></div><div className="teams-art" aria-hidden="true"><span className="teams-art-circle" /><Activity size={110} strokeWidth={1} /><HeartHandshake size={28} /></div></div></section>
    {env.designPreview && !preview.enabled && <div className="preview-entry site-width"><span>Exploring the interface?</span><Link to="/?preview=1&city=Rabat&type=MRI">Open the design preview <ArrowRight size={14} /></Link></div>}
    <HospitalDetailSheet hospital={selected} type={type} onClose={() => setSelected(null)} />
  </>
}
