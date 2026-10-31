import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { CheckCheck, Clock3, LoaderCircle, ShieldCheck } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { ApiError } from '@/lib/api/client'
import type { RepairAction } from '../api'
import type { WorkOrder } from '../types'

const actionLabels: Record<RepairAction, string> = { accept: 'Accept repair', complete: 'Submit for verification', verify: 'Verify & publish', reject: 'Return for repair' }

export function RepairActions({ order }: { order: WorkOrder }) {
  const workspace = useWorkspace()
  const [action, setAction] = useState<RepairAction | null>(null)
  const technician = workspace.role === 'MAINTENANCE_TECHNICIAN'
  const manager = workspace.role === 'HOSPITAL_MANAGER'
  const assigned = order.repairerId === workspace.profile?.id
  const actions: RepairAction[] = technician && order.status === 'OPEN' ? ['accept']
    : technician && assigned && order.status === 'IN_PROGRESS' ? ['complete']
      : manager && order.status === 'AWAITING_VERIFICATION' && !assigned ? ['reject', 'verify'] : []
  return <>
    {actions.length ? <div>{actions.map(item => <Button key={item} variant={item === 'reject' ? 'outline' : 'default'} disabled={!workspace.online} onClick={() => setAction(item)}>
      {item === 'verify' && <CheckCheck size={14} />}{actionLabels[item]}
    </Button>)}</div> : <span>{order.status === 'VERIFIED' ? <><ShieldCheck size={14} />Independently verified</> : manager && assigned ? 'Another manager must review your repair.' : order.status === 'AWAITING_VERIFICATION' ? <><Clock3 size={14} />Waiting for independent verification</> : technician && order.status === 'IN_PROGRESS' ? 'Assigned to another technician' : 'Repair workflow in progress'}</span>}
    {action && <RepairActionDialog order={order} action={action} onClose={() => setAction(null)} />}
  </>
}

function RepairActionDialog({ order, action, onClose }: { order: WorkOrder; action: RepairAction; onClose: () => void }) {
  const workspace = useWorkspace()
  const needsNotes = action !== 'accept'
  const schema = z.object({ notes: needsNotes ? z.string().trim().min(1, 'Add your findings before continuing.').max(2000) : z.string() })
  const form = useForm<{ notes: string }>({ resolver: zodResolver(schema), defaultValues: { notes: '' } })
  const mutation = useMutation({ mutationFn: ({ notes }: { notes: string }) => workspace.repairAction(order, action, notes),
    onSuccess: () => { toast.success(workspace.preview ? 'Sample repair updated' : action === 'verify' ? 'Repair verified. Equipment is available again.' : 'Repair updated'); onClose() },
    onError: error => { if (error instanceof ApiError && error.status === 409) void workspace.refresh() },
  })
  return <Dialog open onOpenChange={open => { if (!open && !mutation.isPending) onClose() }}><DialogContent><DialogHeader><span className="dialog-symbol"><ShieldCheck size={25} /></span>
    <DialogTitle>{actionLabels[action]}</DialogTitle><DialogDescription>Work order #{order.id} · Equipment #{order.equipmentId}</DialogDescription>
  </DialogHeader><form className="dialog-form" onSubmit={form.handleSubmit(values => mutation.mutate(values))}>
    <div className="work-order-description">{order.description}</div>
    {needsNotes ? <div className="form-field"><label htmlFor="repair-notes">{action === 'complete' ? 'Repair summary' : action === 'verify' ? 'Independent verification findings' : 'Reason for returning the repair'}</label><Textarea id="repair-notes" rows={5} maxLength={2000} placeholder={action === 'complete' ? 'What was repaired, and which checks were completed?' : action === 'verify' ? 'Describe the independent checks and their results.' : 'Explain what needs further work.'} {...form.register('notes')} aria-invalid={!!form.formState.errors.notes} /><p className={form.formState.errors.notes ? 'field-error' : ''}>{form.formState.errors.notes?.message || 'Required. Technical details only, without patient information.'}</p></div>
      : <p className="muted-copy">This work order will be assigned to you and the equipment will enter maintenance.</p>}
    {action === 'verify' && <div className="care-notice"><ShieldCheck size={18} /><p>Confirm that you independently checked the repair. Verification restores this equipment to public availability.</p></div>}
    {mutation.error && <div className="form-alert" role="alert">{mutation.error.message}</div>}
    <div className="form-actions"><Button type="button" variant="outline" onClick={onClose} disabled={mutation.isPending}>Cancel</Button><Button type="submit" disabled={mutation.isPending || !workspace.online}>{mutation.isPending && <LoaderCircle className="animate-spin" size={15} />}{actionLabels[action]}</Button></div>
  </form></DialogContent></Dialog>
}
