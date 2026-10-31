import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery } from '@tanstack/react-query'
import { CheckCircle2, LoaderCircle, MonitorCog } from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { useWorkspace } from '@/app/providers/workspace-provider'
import { organizationApi } from '@/features/organization/api'

const schema = z.object({ serialNumber: z.string().trim().min(1, 'Enter a serial number.').max(60),
  model: z.string().trim().min(1, 'Enter the equipment model.').max(120),
  departmentId: z.number().int().positive('Choose a department.'),
  imagingTypeCode: z.string().trim().min(1, 'Choose an imaging type.').max(30),
})
type Values = z.infer<typeof schema>

export function RegisterEquipmentDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const workspace = useWorkspace()
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { serialNumber: '', model: '', imagingTypeCode: 'MRI', departmentId: workspace.preview ? 10 : undefined } })
  const departments = useQuery({ queryKey: ['departments', workspace.profile?.hospitalId], enabled: open && !workspace.preview && !!workspace.profile?.hospitalId,
    queryFn: () => organizationApi.departments(workspace.profile!.hospitalId!),
  })
  const options = workspace.preview ? [{ id: 10, name: 'Radiology' }, { id: 11, name: 'Emergency imaging' }] : departments.data?.items || []
  const mutation = useMutation({ mutationFn: workspace.registerEquipment, onSuccess: () => {
    toast.success(workspace.preview ? 'Sample equipment registered' : 'Equipment registered and available')
    form.reset(); onClose()
  } })
  return <Dialog open={open} onOpenChange={value => { if (!value && !mutation.isPending) onClose() }}><DialogContent className="max-h-[90dvh] overflow-y-auto"><DialogHeader>
    <span className="dialog-symbol"><MonitorCog size={25} /></span><DialogTitle>Register equipment</DialogTitle><DialogDescription>Add a machine to a department in your assigned hospital.</DialogDescription>
  </DialogHeader><form className="dialog-form" onSubmit={form.handleSubmit(values => mutation.mutate(values))}>
    <div className="form-grid"><div className="form-field"><label htmlFor="register-type">Imaging type code</label><Input id="register-type" list="imaging-codes" maxLength={30} {...form.register('imagingTypeCode')} aria-invalid={!!form.formState.errors.imagingTypeCode} /><datalist id="imaging-codes">{['MRI', 'CT', 'XRAY', 'US'].map(code => <option key={code} value={code} />)}</datalist><p>Use an existing imaging catalog code.</p></div>
      <div className="form-field"><label htmlFor="register-department">Department</label><select id="register-department" className="plain-select" {...form.register('departmentId', { valueAsNumber: true })} aria-invalid={!!form.formState.errors.departmentId}><option value="">Choose department</option>{options.map(department => <option key={department.id} value={department.id}>{department.name}</option>)}</select><p>{form.formState.errors.departmentId?.message || 'Only your hospital’s departments are listed.'}</p></div></div>
    <div className="form-field"><label htmlFor="register-serial">Serial number</label><Input id="register-serial" placeholder="e.g. MRI-2026-0042" maxLength={60} {...form.register('serialNumber')} aria-invalid={!!form.formState.errors.serialNumber} /><p className={form.formState.errors.serialNumber ? 'field-error' : ''}>{form.formState.errors.serialNumber?.message || 'Must be unique. Up to 60 characters.'}</p></div>
    <div className="form-field"><label htmlFor="register-model">Model</label><Input id="register-model" placeholder="Equipment manufacturer and model" maxLength={120} {...form.register('model')} aria-invalid={!!form.formState.errors.model} /><p className="field-error">{form.formState.errors.model?.message}</p></div>
    <div className="care-notice"><CheckCircle2 size={17} /><p>New equipment starts as Available and can appear in public search after registration.</p></div>
    {(mutation.error || departments.error) && <p className="form-alert" role="alert">{(mutation.error || departments.error)?.message}</p>}
    <div className="form-actions"><Button variant="outline" type="button" onClick={onClose} disabled={mutation.isPending}>Cancel</Button><Button type="submit" disabled={mutation.isPending || !workspace.online || !options.length}>{mutation.isPending && <LoaderCircle className="animate-spin" size={15} />}Register equipment</Button></div>
  </form></DialogContent></Dialog>
}
