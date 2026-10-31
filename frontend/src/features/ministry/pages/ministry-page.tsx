import { BarChart, Bar, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Building2, CheckCircle2, Clock3, Download, Wrench } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { ColumnDef } from '@tanstack/react-table'
import { Button } from '@/components/ui/button'
import { DataTable } from '@/components/shared/data-table'
import { EmptyState } from '@/components/shared/page-state'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { usePreview } from '@/lib/use-preview'

const regions = [
  { name: 'Rabat-Salé-Kénitra', short: 'Rabat', hospitals: 8, working: 18, total: 22 },
  { name: 'Casablanca-Settat', short: 'Casablanca', hospitals: 10, working: 25, total: 30 },
  { name: 'Fès-Meknès', short: 'Fès', hospitals: 6, working: 14, total: 19 },
  { name: 'Marrakech-Safi', short: 'Marrakech', hospitals: 7, working: 15, total: 20 },
  { name: 'Tanger-Tétouan-Al Hoceïma', short: 'Tanger', hospitals: 5, working: 12, total: 15 },
  { name: 'Souss-Massa', short: 'Agadir', hospitals: 4, working: 9, total: 13 },
].map(region => ({ ...region, availability: Math.round(region.working / region.total * 100) }))
type RegionalSummary = typeof regions[number]

export default function MinistryPage() {
  const workspace = useWorkspace()
  const preview = usePreview()
  if (!workspace.preview) return <><div className="page-heading"><div><span className="section-kicker">MINISTRY WORKSPACE</span><h1>Availability reporting</h1><p>Regional and national summaries will appear here when reporting is connected.</p></div></div><EmptyState title="No reports available yet" description="National figures are not published in this version. Public availability search is available now."><Button asChild><Link to="/">Open public search</Link></Button></EmptyState></>
  const working = regions.reduce((sum, item) => sum + item.working, 0)
  const total = regions.reduce((sum, item) => sum + item.total, 0)
  const columns: ColumnDef<RegionalSummary>[] = [{ accessorKey: 'name', header: 'REGION' }, { accessorKey: 'hospitals', header: 'HOSPITALS' },
    { id: 'working', header: 'WORKING', cell: ({ row }) => `${row.original.working} of ${row.original.total}` },
    { accessorKey: 'availability', header: 'AVAILABILITY', cell: ({ row }) => <div className="flex items-center gap-3"><span className="h-1.5 w-16 rounded-full bg-[#edf1e8]"><span className="block h-full rounded-full bg-[#7fa574]" style={{ width: `${row.original.availability}%` }} /></span>{row.original.availability}%</div> },
  ]
  function download() {
    const csv = ['Sample data only - HealthLink design preview', 'Region,Hospitals,Working,Total,Availability', ...regions.map(item => `${item.name},${item.hospitals},${item.working},${item.total},${item.availability}%`)].join('\n')
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'healthlink-sample-report.csv'; anchor.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  return <><div className="page-heading"><div><span className="section-kicker">A SHARED VIEW OF PUBLIC CAPACITY</span><h1>Availability overview</h1><p>Illustrative regional summaries · Sample data for design review.</p></div><Button variant="outline" onClick={download}><Download size={15} />Export sample CSV</Button></div>
    <div className="stat-grid"><div className="surface stat-card"><div><span>Working machines</span><strong>{Math.round(working / total * 100)}%</strong><p>{working} of {total} sample machines</p></div><span className="stat-icon"><CheckCircle2 size={19} /></span></div><div className="surface stat-card terra"><div><span>Out of service</span><strong>{total - working}</strong><p>in this synthetic dataset</p></div><span className="stat-icon"><Wrench size={19} /></span></div><div className="surface stat-card amber"><div><span>Hospitals represented</span><strong>{regions.reduce((sum, item) => sum + item.hospitals, 0)}</strong><p>across 6 example regions</p></div><span className="stat-icon"><Building2 size={19} /></span></div></div>
    <section className="surface ministry-card"><div className="panel-heading"><div><span className="section-kicker">REGIONAL COMPARISON · SAMPLE</span><h2>Imaging equipment availability</h2></div></div><div className="ministry-chart" role="img" aria-label="Sample regional equipment availability from 69 to 83 percent"><ResponsiveContainer width="100%" height="100%"><BarChart data={regions} margin={{ top: 10, right: 12, left: -20, bottom: 0 }}><CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e6ecdf" /><XAxis dataKey="short" tick={{ fill: '#849776', fontSize: 10 }} axisLine={false} tickLine={false} /><YAxis domain={[0, 100]} tickFormatter={value => `${value}%`} tick={{ fill: '#9aaa8d', fontSize: 10 }} axisLine={false} tickLine={false} /><Tooltip cursor={{ fill: '#f0f5e9' }} contentStyle={{ borderRadius: 10, border: '1px solid #dfe8d5', fontSize: 12 }} formatter={value => [`${value}%`, 'Sample availability']} /><Bar dataKey="availability" fill="#7ca472" radius={[5, 5, 0, 0]} maxBarSize={43} /></BarChart></ResponsiveContainer></div></section>
    <section className="surface table-surface"><div className="panel-heading"><div><h2>By region</h2><span className="section-kicker">ILLUSTRATIVE FIGURES</span></div><Link to={preview.href('/')}>Public search</Link></div><DataTable data={regions} columns={columns} /></section>
  </>
}
