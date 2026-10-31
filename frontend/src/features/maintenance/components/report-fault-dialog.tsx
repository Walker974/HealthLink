import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation } from '@tanstack/react-query'
import { AlertTriangle, LoaderCircle } from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { Button } from '@/components/ui/button'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { ApiError } from '@/lib/api/client'
import type { Equipment } from '@/features/equipment/types'

export const faultSchema = z.object({ description: z.string().trim().min(1, 'Describe the equipment fault.').max(2000, 'Keep the description within 2,000 characters.') })

export function ReportFaultDialog({ equipment, onClose }: { equipment: Equipment | null; onClose: () => void }) {
  const workspace = useWorkspace()
  const form = useForm<z.infer<typeof faultSchema>>({ resolver: zodResolver(faultSchema), defaultValues: { description: '' } })
  const mutation = useMutation({ mutationFn: (values: z.infer<typeof faultSchema>) => workspace.reportFault(equipment!, values.description),
    onSuccess: () => { toast.success(workspace.preview ? 'Sample fault reported' : 'Fault reported. Equipment is now out of service.'); form.reset(); onClose() },
    onError: error => { if (error instanceof ApiError && error.status === 409) void workspace.refresh() },
  })
  const close = () => { if (!mutation.isPending) { form.reset(); mutation.reset(); onClose() } }
  return <Dialog open={!!equipment} onOpenChange={open => { if (!open) close() }}><DialogContent><DialogHeader>
    <span className="dialog-symbol"><AlertTriangle size={25} /></span><DialogTitle>Report an equipment fault</DialogTitle><DialogDescription>{equipment?.model}<br />{equipment?.serialNumber}</DialogDescription>
  </DialogHeader><form className="dialog-form" onSubmit={form.handleSubmit(values => mutation.mutate(values))}>
    <div className="form-field"><label htmlFor="fault-description">What’s wrong?</label><Textarea id="fault-description" placeholder="Describe the issue for the technician…" rows={5} maxLength={2000} {...form.register('description')} aria-invalid={!!form.formState.errors.description} aria-describedby="fault-description-help" />
      <p id="fault-description-help" className={form.formState.errors.description ? 'field-error' : ''}>{form.formState.errors.description?.message || 'Technical equipment details only. Do not include patient information.'}</p></div>
    <div className="form-notice">A work order will be opened and this machine will leave public availability. It returns only after repair and independent manager verification.</div>
    {mutation.error && <p className="form-alert" role="alert">{mutation.error.message}</p>}
    <div className="form-actions"><Button type="button" variant="outline" onClick={close} disabled={mutation.isPending}>Cancel</Button><Button type="submit" disabled={mutation.isPending || !workspace.online}>{mutation.isPending && <LoaderCircle size={15} className="animate-spin" />}Report fault</Button></div>
  </form></DialogContent></Dialog>
}
