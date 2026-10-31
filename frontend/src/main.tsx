import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { AuthProvider } from '@/features/auth/auth-provider'
import { TooltipProvider } from '@/components/ui/tooltip'
import { PwaProvider } from '@/app/providers/pwa-provider'
import { queryClient } from '@/lib/query-client'
import { AppRouter } from '@/app/router'
import './index.css'
import './app/styles.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><QueryClientProvider client={queryClient}><TooltipProvider><AuthProvider><PwaProvider>
    <AppRouter /><Toaster richColors position="bottom-right" closeButton />
  </PwaProvider></AuthProvider></TooltipProvider></QueryClientProvider></React.StrictMode>,
)
