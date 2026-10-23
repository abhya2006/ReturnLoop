import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { AppShell } from '@/layout/AppShell';
import { useAuth } from '@/auth/AuthContext';
import { Loader2 } from 'lucide-react';
import { ErrorBoundary } from '@/components/common/ErrorBoundary';

const LoginPage = lazy(() => import('@/pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));
const ReturnsPage = lazy(() => import('@/pages/ReturnsPage').then((module) => ({ default: module.ReturnsPage })));
const ReturnDetailPage = lazy(() => import('@/pages/ReturnDetailPage').then((module) => ({ default: module.ReturnDetailPage })));
const ImportPage = lazy(() => import('@/pages/ImportPage').then((module) => ({ default: module.ImportPage })));
const InventoryPage = lazy(() => import('@/pages/InventoryPage').then((module) => ({ default: module.InventoryPage })));
const InspectionsPage = lazy(() => import('@/pages/InspectionsPage').then((module) => ({ default: module.InspectionsPage })));
const RecoveryPage = lazy(() => import('@/pages/RecoveryPage').then((module) => ({ default: module.RecoveryPage })));
const PickupsPage = lazy(() => import('@/pages/PickupsPage').then((module) => ({ default: module.PickupsPage })));
const RoutesPage = lazy(() => import('@/pages/RoutesPage').then((module) => ({ default: module.RoutesPage })));
const PartnersPage = lazy(() => import('@/pages/PartnersPage').then((module) => ({ default: module.PartnersPage })));
const PartnerDetailPage = lazy(() => import('@/pages/PartnerDetailPage').then((module) => ({ default: module.PartnerDetailPage })));
const PartnerJobsPage = lazy(() => import('@/pages/PartnerJobsPage').then((module) => ({ default: module.PartnerJobsPage })));
const ReportsPage = lazy(() => import('@/pages/ReportsPage').then((module) => ({ default: module.ReportsPage })));
const EnvironmentalPage = lazy(() => import('@/pages/EnvironmentalPage').then((module) => ({ default: module.EnvironmentalPage })));
const NotificationsPage = lazy(() => import('@/pages/NotificationsPage').then((module) => ({ default: module.NotificationsPage })));
const UsersPage = lazy(() => import('@/pages/UsersPage').then((module) => ({ default: module.UsersPage })));
const OrganizationsPage = lazy(() => import('@/pages/OrganizationsPage').then((module) => ({ default: module.OrganizationsPage })));
const AuditPage = lazy(() => import('@/pages/AuditPage').then((module) => ({ default: module.AuditPage })));
const SettingsPage = lazy(() => import('@/pages/SettingsPage').then((module) => ({ default: module.SettingsPage })));
const LandingPage = lazy(() => import('@/pages/LandingPage').then((module) => ({ default: module.LandingPage })));
const AccountPage = lazy(() => import('@/pages/AccountPage').then((module) => ({ default: module.AccountPage })));

function FullPageSpinner() {
  return (
    <div className="flex h-screen w-screen items-center justify-center bg-jet">
      <div className="flex flex-col items-center gap-3">
        <Loader2 className="h-6 w-6 animate-spin text-cerulean" />
        <p className="text-xs tracking-wide text-ink-muted">Loading ReturnLoop…</p>
      </div>
    </div>
  );
}

export default function App() {
  const { authenticated, loading } = useAuth();

  if (loading) {
    return <FullPageSpinner />;
  }

  if (!authenticated) {
    return (
      <ErrorBoundary><Suspense fallback={<FullPageSpinner />}><Routes>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<AccountPage mode="register" />} />
        <Route path="/forgot-password" element={<AccountPage mode="forgot" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes></Suspense></ErrorBoundary>
    );
  }

  return (
    <ErrorBoundary><Suspense fallback={<FullPageSpinner />}><Routes>
      <Route path="/login" element={<Navigate to="/dashboard" replace />} />
      <Route path="/register" element={<Navigate to="/dashboard" replace />} />
      <Route path="/forgot-password" element={<Navigate to="/dashboard" replace />} />
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="dashboard" element={<DashboardPage />} />
        <Route path="returns" element={<ReturnsPage />} />
        <Route path="returns/:id" element={<ReturnDetailPage />} />
        <Route path="imports" element={<ImportPage />} />
        <Route path="inventory" element={<InventoryPage />} />
        <Route path="inspections" element={<InspectionsPage />} />
        <Route path="recovery" element={<RecoveryPage />} />
        <Route path="recovery/:returnId" element={<RecoveryPage />} />
        <Route path="pickups" element={<PickupsPage />} />
        <Route path="routes" element={<RoutesPage />} />
        <Route path="partners" element={<PartnersPage />} />
        <Route path="partners/:id" element={<PartnerDetailPage />} />
        <Route path="partner-jobs" element={<PartnerJobsPage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="environmental" element={<EnvironmentalPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="organizations" element={<OrganizationsPage />} />
        <Route path="audit" element={<AuditPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes></Suspense></ErrorBoundary>
  );
}
