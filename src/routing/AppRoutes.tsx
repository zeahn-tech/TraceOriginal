import { Suspense } from 'react'
import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { type Alert, type Report, type Tip, type User, type Wanted } from '../domain'
import { Protected } from '../shared/Protected'
import type { Toast } from '../shared/types'
import { Splash } from '../features/auth/Splash'
import { Welcome } from '../features/auth/Welcome'
import {
  AboutPage,
  AdminPage,
  AnalyticsPage,
  AuthPage,
  CitizenDashboard,
  ContactsPage,
  MapPage,
  OperationsPage,
  ProfilePage,
  PublicPortal,
  ReportForm,
  SignUpPage,
  SOSPage,
  TipForm,
  WantedForm,
} from './lazyRoutes'
import { RouteErrorBoundary } from './ErrorBoundary'
import { RouteFallback } from './RouteFallback'

type AppData = {
  user?: User
  setUser: (u: User) => void
  loading: boolean
  reports: Report[]
  tips: Tip[]
  alerts: Alert[]
  wanted: Wanted[]
  users: User[]
  publicReports: Report[]
  publicWanted: Wanted[]
}

export function AppRoutes({ data, setToast }: { data: AppData; setToast: (t: Toast) => void }) {
  // Keying the boundary by pathname gives every route navigation a fresh
  // error boundary automatically: if a screen crashed, moving to any other
  // route (nav back, a Link, the bottom nav) remounts a clean boundary
  // rather than continuing to show the previous route's crash fallback.
  const location = useLocation()
  return (
    <RouteErrorBoundary key={location.pathname}>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route path="/" element={<Splash user={data.user} loading={data.loading} />} />
          <Route path="/welcome" element={<Welcome />} />
          <Route path="/sign-in" element={<AuthPage setToast={setToast} />} />
          <Route path="/sign-up" element={<SignUpPage setToast={setToast} />} />
          <Route path="/public" element={<PublicPortal alerts={data.alerts} reports={data.publicReports} wanted={data.publicWanted} />} />
          <Route path="/dashboard" element={<Protected user={data.user}><CitizenDashboard {...data} user={data.user!} setToast={setToast} /></Protected>} />
          <Route path="/reports/new" element={<Protected user={data.user}><ReportForm user={data.user!} setToast={setToast} /></Protected>} />
          <Route path="/tips/new" element={<Protected user={data.user}><TipForm user={data.user!} reports={data.publicReports} setToast={setToast} /></Protected>} />
          <Route path="/sos" element={<Protected user={data.user}><SOSPage user={data.user!} setToast={setToast} /></Protected>} />
          <Route path="/map" element={<MapPage reports={data.reports} />} />
          <Route path="/contacts" element={<ContactsPage />} />
          <Route path="/analytics" element={<AnalyticsPage reports={data.reports} wanted={data.wanted} />} />
          <Route path="/profile" element={<Protected user={data.user}><ProfilePage user={data.user!} setUser={data.setUser} setToast={setToast} /></Protected>} />
          <Route path="/about" element={<AboutPage />} />
          <Route path="/operations" element={<Protected user={data.user} role="LAW_ENFORCER"><OperationsPage {...data} user={data.user!} setToast={setToast} /></Protected>} />
          <Route path="/operations/wanted/new" element={<Protected user={data.user} role="LAW_ENFORCER"><WantedForm setToast={setToast} /></Protected>} />
          <Route path="/admin" element={<Protected user={data.user} role="ADMIN"><AdminPage {...data} setToast={setToast} /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </RouteErrorBoundary>
  )
}
