import { lazy } from 'react'

// Each entry below is its own async chunk, fetched only when its route is
// visited. Grouped by feature (not by string similarity) so that, for
// example, opening OPERATIONS never pulls in the ADMIN command center's
// code, and a CITIZEN who never opens /admin never downloads it at all.

// Auth
export const AuthPage = lazy(() => import('../features/auth/AuthPage').then(m => ({ default: m.AuthPage })))
export const SignUpPage = lazy(() => import('../features/auth/SignUpPage').then(m => ({ default: m.SignUpPage })))

// Public / unauthenticated
export const PublicPortal = lazy(() => import('../features/public/PublicPortal').then(m => ({ default: m.PublicPortal })))
export const AboutPage = lazy(() => import('../features/public/AboutPage').then(m => ({ default: m.AboutPage })))

// Citizen dashboard
export const CitizenDashboard = lazy(() => import('../features/dashboard/CitizenDashboard').then(m => ({ default: m.CitizenDashboard })))

// Reports
export const ReportForm = lazy(() => import('../features/reports/ReportForm').then(m => ({ default: m.ReportForm })))

// Tips
export const TipForm = lazy(() => import('../features/tips/TipForm').then(m => ({ default: m.TipForm })))

// SOS
export const SOSPage = lazy(() => import('../features/sos/SOSPage').then(m => ({ default: m.SOSPage })))

// Maps
export const MapPage = lazy(() => import('../features/maps/MapPage').then(m => ({ default: m.MapPage })))

// Emergency contacts
export const ContactsPage = lazy(() => import('../features/contacts/ContactsPage').then(m => ({ default: m.ContactsPage })))

// Analytics
export const AnalyticsPage = lazy(() => import('../features/analytics/AnalyticsPage').then(m => ({ default: m.AnalyticsPage })))

// Profile
export const ProfilePage = lazy(() => import('../features/profile/ProfilePage').then(m => ({ default: m.ProfilePage })))

// Law-enforcement operations
export const OperationsPage = lazy(() => import('../features/operations/OperationsPage').then(m => ({ default: m.OperationsPage })))
export const WantedForm = lazy(() => import('../features/operations/WantedForm').then(m => ({ default: m.WantedForm })))

// Admin
export const AdminPage = lazy(() => import('../features/admin/AdminPage').then(m => ({ default: m.AdminPage })))
