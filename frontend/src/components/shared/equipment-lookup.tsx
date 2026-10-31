import { useState, type FormEvent } from 'react'
import { Search, ShieldCheck } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useWorkspace } from '@/app/providers/workspace-provider'

export function EquipmentLookup() {
  const workspace = useWorkspace()
  const [id, setId] = useState(workspace.selectedId?.toString() || '')
  if (workspace.preview) return null
  function submit(event: FormEvent) { event.preventDefault(); const value = Number(id); if (Number.isSafeInteger(value) && value > 0) workspace.selectEquipment(value) }
  return <div className="equipment-lookup surface"><form onSubmit={submit}><div><label htmlFor="equipment-id">Open an equipment record</label><p>Enter its ID to view equipment and repair history within your assignment.</p></div>
    <Input id="equipment-id" type="number" min={1} step={1} required placeholder="Equipment ID" value={id} onChange={event => setId(event.target.value)} />
    <Button type="submit"><Search size={16} />Open record</Button></form><span><ShieldCheck size={13} />Your hospital and department permissions apply.</span></div>
}
