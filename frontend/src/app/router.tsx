import { lazy, Suspense } from 'react'
import { createBrowserRouter, Link, RouterProvider } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { EmptyState, LoadingState } from '@/components/shared/page-state'
import { RequireRole } from '@/features/auth/components/require-role'

const PublicLayout = lazy(() => import('./layouts/public-layout'))
const WorkspaceLayout = lazy(() => import('./layouts/workspace-layout'))
const SearchPage = lazy(() => import('@/features/availability/pages/search-page'))
const HospitalPage = lazy(() => import('@/features/availability/pages/hospital-page'))
const SignInPage = lazy(() => import('@/features/auth/pages/sign-in-page'))
const OverviewPage = lazy(() => import('@/features/equipment/pages/overview-page'))
const EquipmentPage = lazy(() => import('@/features/equipment/pages/equipment-page'))
const RepairsPage = lazy(() => import('@/features/maintenance/pages/repairs-page'))
const HistoryPage = lazy(() => import('@/features/maintenance/pages/history-page'))
const DepartmentsPage = lazy(() => import('@/features/organization/pages/departments-page'))
const AdministrationPage = lazy(() => import('@/features/organization/pages/administration-page'))
const MinistryPage = lazy(() => import('@/features/ministry/pages/ministry-page'))
const ProfilePage = lazy(() => import('@/features/staff/pages/profile-page'))
const hospitalRoles = ['HOSPITAL_MANAGER', 'MAINTENANCE_TECHNICIAN', 'RADIOLOGY_OPERATOR'] as const

function NotFound() { return <EmptyState title="Let’s get you back on track" description="This page isn’t available. Start again from public search."><Button asChild><Link to="/">Find imaging</Link></Button></EmptyState> }
function RouteError() { return <EmptyState title="Something interrupted this page" description="Your changes may not have been saved. Reload and check the current record before trying again."><Button onClick={() => window.location.reload()}>Reload page</Button></EmptyState> }

const router = createBrowserRouter([
  { element: <PublicLayout />, errorElement: <RouteError />, children: [
    { path: '/', element: <SearchPage /> }, { path: '/hospitals/:hospitalId', element: <HospitalPage /> },
    { path: '/staff', element: <SignInPage /> }, { path: '*', element: <NotFound /> },
  ] },
  { path: '/workspace', element: <WorkspaceLayout />, errorElement: <RouteError />, children: [
    { index: true, element: <OverviewPage /> },
    { path: 'equipment', element: <RequireRole roles={[...hospitalRoles]}><EquipmentPage /></RequireRole> },
    { path: 'repairs', element: <RequireRole roles={[...hospitalRoles]}><RepairsPage /></RequireRole> },
    { path: 'history', element: <RequireRole roles={[...hospitalRoles]}><HistoryPage /></RequireRole> },
    { path: 'department', element: <RequireRole roles={['HOSPITAL_MANAGER']}><DepartmentsPage /></RequireRole> },
    { path: 'organization', element: <RequireRole roles={['PLATFORM_ADMIN']}><AdministrationPage /></RequireRole> },
    { path: 'ministry', element: <RequireRole roles={['MINISTRY_OFFICER']}><MinistryPage /></RequireRole> },
    { path: 'profile', element: <ProfilePage /> },
  ] },
])

export function AppRouter() { return <Suspense fallback={<LoadingState label="Opening HealthLink…" />}><RouterProvider router={router} /></Suspense> }
