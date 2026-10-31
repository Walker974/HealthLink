import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReportFaultDialog } from './report-fault-dialog'
import { sampleEquipment } from '@/features/availability/preview-data'
import { ApiError } from '@/lib/api/client'

const mocks = vi.hoisted(() => ({ reportFault: vi.fn(), refresh: vi.fn(), online: true }))
vi.mock('@/app/providers/workspace-provider', () => ({ useWorkspace: () => ({ ...mocks, preview: false }) }))
beforeEach(() => { mocks.reportFault.mockReset(); mocks.refresh.mockReset(); mocks.online = true })
function mount() {
  const close = vi.fn()
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><ReportFaultDialog equipment={sampleEquipment[0]} onClose={close} /></QueryClientProvider>)
  return close
}

describe('fault reporting form', () => {
  it('requires technical notes before opening a work order', async () => {
    mount(); const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Report fault' }))
    expect(await screen.findByText('Describe the equipment fault.')).toBeInTheDocument()
    expect(mocks.reportFault).not.toHaveBeenCalled()
  })

  it('keeps notes and the dialog open when a concurrent change returns 409', async () => {
    mocks.reportFault.mockRejectedValue(new ApiError(409, 'This record changed. Review the latest version.'))
    const close = mount(); const user = userEvent.setup()
    await user.type(screen.getByRole('textbox', { name: /what.*wrong/i }), 'The power supply fails its self-test.')
    await user.click(screen.getByRole('button', { name: 'Report fault' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('This record changed.')
    expect(screen.getByRole('textbox')).toHaveValue('The power supply fails its self-test.')
    await waitFor(() => expect(mocks.refresh).toHaveBeenCalledTimes(1))
    expect(close).not.toHaveBeenCalled()
  })

  it('disables fault submission while offline', () => {
    mocks.online = false; mount()
    expect(screen.getByRole('button', { name: 'Report fault' })).toBeDisabled()
  })
})
