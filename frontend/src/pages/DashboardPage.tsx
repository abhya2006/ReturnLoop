import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Activity,
  ArrowRight,
  ClipboardCheck,
  Coins,
  Leaf,
  Package,
  PackageCheck,
  Route,
  Sparkles,
  Timer,
  Truck,
  Upload,
} from 'lucide-react';
import { analyticsApi, notificationApi } from '@/api/client';
import { useAuth } from '@/auth/AuthContext';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { SkeletonCards, Skeleton } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { KpiCard } from '@/components/common/KpiCard';
import {
  DateRangeSelector,
  defaultRange,
  type DateRange,
} from '@/components/common/DateRangeSelector';
import { ReturnTrendChart } from '@/components/charts/ReturnTrendChart';
import { RecoveryDonutChart } from '@/components/charts/RecoveryDonutChart';
import { OperationsMap } from '@/components/map/OperationsMap';
import { CountUp } from '@/components/common/CountUp';
import { formatCurrency, formatNumber, relativeTime } from '@/lib/format';

export function DashboardPage() {
  const { user } = useAuth();
  const [range, setRange] = useState<DateRange>(defaultRange());

  const dashboard = useQuery({
    queryKey: ['dashboard', range.from, range.to],
    queryFn: () => analyticsApi.dashboard({ from: range.from, to: range.to }),
  });

  const notifications = useQuery({
    queryKey: ['notifications', 'recent'],
    queryFn: () => notificationApi.list({ size: 8 }),
  });

  const summary = dashboard.data;

  const firstName = user?.fullName?.split(' ')[0] ?? 'there';

  const quickActions = useMemo(
    () => [
      {
        to: '/returns?new=1',
        label: 'Create return',
        description: 'Log a return with one or more items',
        icon: Package,
        accent: 'cerulean' as const,
      },
      {
        to: '/pickups',
        label: 'Schedule pickup',
        description: 'Book a collection window and vehicle',
        icon: Truck,
        accent: 'cerulean' as const,
      },
      {
        to: '/recovery',
        label: 'Generate recommendation',
        description: 'Compare recovery paths and net value',
        icon: Sparkles,
        accent: 'rose' as const,
      },
      {
        to: '/imports',
        label: 'Import inventory CSV',
        description: 'Bulk ingest returns and surplus stock',
        icon: Upload,
        accent: 'cerulean' as const,
      },
    ],
    [],
  );

  return (
    <div className="dashboard-stagger space-y-5">
      {/* ---------------------------------------------------------- welcome */}
      <section className="reveal panel lift relative overflow-hidden">
        <div className="pointer-events-none absolute inset-0 bg-hero-sheen" aria-hidden />
        <div className="pointer-events-none absolute inset-0 grid-noise opacity-50" aria-hidden />
        <div
          className="pointer-events-none absolute -right-16 -top-24 h-56 w-56 rounded-full bg-cerulean/25 blur-[80px] glow-breathe"
          aria-hidden
        />
        <div className="relative flex flex-wrap items-start justify-between gap-4 px-5 py-5">
          <div>
            <h2 className="text-xl font-semibold tracking-tight text-ink-primary">
              Welcome back, {firstName}!
            </h2>
            <p className="mt-1 text-sm text-ink-secondary">
              Here’s what’s happening with your returns and recovery today.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <div className="inline-flex items-center gap-2 rounded-lg border border-ok/30 bg-ok/10 px-3 py-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-ok shadow-[0_0_6px_rgba(0,214,163,0.9)]" aria-hidden />
              <span className="text-[11px] font-medium text-ok">All systems operational</span>
            </div>
            <DateRangeSelector value={range} onChange={setRange} />
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------- KPIs */}
      {dashboard.isError ? (
        <ErrorState
          title="The dashboard could not be loaded"
          description={(dashboard.error as Error)?.message}
          onRetry={() => void dashboard.refetch()}
        />
      ) : dashboard.isLoading || !summary ? (
        <SkeletonCards count={5} />
      ) : (
        <section className="reveal grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <KpiCard
            label={summary.totalReturns.label}
            value={<CountUp value={summary.totalReturns.value} format={(n) => formatNumber(Math.round(n))} />}
            icon={<Package className="h-4 w-4" />}
            to="/returns"
            trend={
              summary.totalReturns.previousValue !== null
                ? {
                    value:
                      summary.totalReturns.previousValue === 0
                        ? 0
                        : ((summary.totalReturns.value - summary.totalReturns.previousValue) /
                            summary.totalReturns.previousValue) *
                          100,
                    label: 'vs previous period',
                  }
                : null
            }
          />
          <KpiCard
            label={summary.awaitingInspection.label}
            value={<CountUp value={summary.awaitingInspection.value} format={(n) => formatNumber(Math.round(n))} />}
            icon={<ClipboardCheck className="h-4 w-4" />}
            to="/inspections"
            accent="rose"
          />
          <KpiCard
            label={summary.recoveredValue.label}
            value={<CountUp value={summary.recoveredValue.value} format={(n) => formatCurrency(n, 'USD', true)} />}
            icon={<Coins className="h-4 w-4" />}
            to="/reports"
            trend={
              summary.recoveredValue.previousValue !== null
                ? {
                    value:
                      summary.recoveredValue.previousValue === 0
                        ? 0
                        : ((summary.recoveredValue.value - summary.recoveredValue.previousValue) /
                            summary.recoveredValue.previousValue) *
                          100,
                    label: 'vs previous period',
                  }
                : null
            }
          />
          <KpiCard
            label={summary.wasteAvoided.label}
            value={<CountUp value={summary.wasteAvoided.value} format={(n) => formatCurrency(n, 'USD', true)} />}
            icon={<Leaf className="h-4 w-4" />}
            to="/environmental"
          />
          <KpiCard
            label={summary.averageTurnaround.label}
            value={<CountUp value={summary.averageTurnaround.value} format={(n) => `${n.toFixed(1)} d`} />}
            icon={<Timer className="h-4 w-4" />}
            to="/reports"
          />
        </section>
      )}

      {/* ---------------------------------------------------- quick actions */}
      <section className="stagger grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
        {quickActions.map((action) => {
          const Icon = action.icon;
          return (
            <Link
              key={action.to + action.label}
              to={action.to}
              className="panel lift group flex items-start gap-3 p-4 hover:border-cerulean/40 hover:shadow-glow focus-visible:ring-2 focus-visible:ring-cerulean"
            >
              <span
                className={
                  action.accent === 'rose'
                    ? 'flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-rose/30 bg-rose/10 text-rose-light'
                    : 'flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border border-cerulean/30 bg-cerulean/10 text-cerulean-light'
                }
                aria-hidden
              >
                <Icon className="h-4 w-4" />
              </span>
              <span className="min-w-0 flex-1">
                <span className="flex items-center gap-1 text-[13px] font-semibold text-ink-primary">
                  {action.label}
                  <ArrowRight className="h-3 w-3 opacity-0 transition-opacity group-hover:opacity-100" />
                </span>
                <span className="mt-0.5 block text-[11px] leading-relaxed text-ink-muted">
                  {action.description}
                </span>
              </span>
            </Link>
          );
        })}
      </section>

      {/* -------------------------------------------------------- analytics */}
      <div className="reveal grid grid-cols-1 gap-5 xl:grid-cols-3">
        <Card className="reveal lift xl:col-span-2">
          <CardHeader
            title="Return trends"
            description="Returns created against items routed to a recovery path"
            action={
              <Link
                to="/reports"
                className="inline-flex items-center gap-1 text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean"
              >
                Full report <ArrowRight className="h-3 w-3" />
              </Link>
            }
          />
          <CardBody>
            {dashboard.isLoading ? (
              <Skeleton className="h-[280px] w-full" />
            ) : summary && summary.trend.length > 0 ? (
              <div className="chart-enter"><ReturnTrendChart data={summary.trend} /></div>
            ) : (
              <EmptyState
                title="No return activity in this period"
                description="Adjust the date range or create a return to start populating the trend."
              />
            )}
          </CardBody>
        </Card>

        <Card className="reveal lift">
          <CardHeader title="Recovery distribution" description="Where goods are being routed" />
          <CardBody>
            {dashboard.isLoading ? (
              <Skeleton className="h-[280px] w-full" />
            ) : summary ? (
              <div className="chart-enter"><RecoveryDonutChart data={summary.distribution} /></div>
            ) : null}
          </CardBody>
        </Card>
      </div>

      {/* ------------------------------------------------------------- map */}
      <Card className="reveal">
        <CardHeader
          title="Pickup locations"
          description="Warehouses, partner facilities and scheduled collections"
          action={
            <Link
              to="/routes"
              className="inline-flex items-center gap-1 text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean"
            >
              <Route className="h-3 w-3" /> Plan routes
            </Link>
          }
        />
        <CardBody>
          {dashboard.isLoading ? (
            <Skeleton className="h-[360px] w-full" />
          ) : summary && summary.markers.length > 0 ? (
            <div className="chart-enter"><OperationsMap markers={summary.markers} /></div>
          ) : (
            <EmptyState
              icon={<Truck className="h-5 w-5" />}
              title="No pickup locations to show"
              description="Schedule a collection and the pickup will appear on the operations map."
            />
          )}
        </CardBody>
      </Card>

      <div className="reveal grid grid-cols-1 gap-5 xl:grid-cols-3">
        {/* ------------------------------------------------ recent returns */}
        <Card className="reveal lift xl:col-span-2">
          <CardHeader
            title="Recent returns"
            description="The latest movement through the reverse-logistics lifecycle"
            action={
              <Link
                to="/returns"
                className="inline-flex items-center gap-1 text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean"
              >
                All returns <ArrowRight className="h-3 w-3" />
              </Link>
            }
          />
          {dashboard.isLoading ? (
            <CardBody className="space-y-2">
              {Array.from({ length: 5 }).map((_, index) => (
                <Skeleton key={index} className="h-10 w-full" />
              ))}
            </CardBody>
          ) : summary && summary.recentReturns.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="data-table rows-enter">
                <thead>
                  <tr>
                    <th>Return</th>
                    <th>Status</th>
                    <th>Customer</th>
                    <th className="text-right">Est. value</th>
                    <th className="text-right">Created</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.recentReturns.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <Link
                          to={`/returns/${item.id}`}
                          className="font-medium text-cerulean-light transition-colors hover:text-cerulean hover:underline"
                        >
                          {item.returnNumber}
                        </Link>
                      </td>
                      <td>
                        <StatusPill label={item.statusLabel} tone={toneForStatus(item.status)} dot />
                      </td>
                      <td className="max-w-[180px] truncate">{item.customerName ?? '—'}</td>
                      <td className="text-right tabular-nums">
                        {formatCurrency(item.estimatedRecoveryValue ?? 0)}
                      </td>
                      <td className="text-right text-ink-muted">{relativeTime(item.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              icon={<Package className="h-5 w-5" />}
              title="No returns yet"
              description="Create your first return to see it tracked through the lifecycle."
              action={
                <Link to="/returns">
                  <Button size="sm">Create a return</Button>
                </Link>
              }
            />
          )}
        </Card>

        {/* ------------------------------------------------ recent activity */}
        <Card className="reveal">
          <CardHeader
            title="Recent activity"
            description="Domain events across the platform"
            action={
              <Link
                to="/notifications"
                className="inline-flex items-center gap-1 text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean"
              >
                All <ArrowRight className="h-3 w-3" />
              </Link>
            }
          />
          <CardBody className="list-enter space-y-2.5">
            {notifications.isLoading ? (
              Array.from({ length: 6 }).map((_, index) => <Skeleton key={index} className="h-12 w-full" />)
            ) : notifications.data && notifications.data.content.length > 0 ? (
              notifications.data.content.map((notification) => (
                <div
                  key={notification.id}
                  className="flex items-start gap-2.5 rounded-lg border border-transparent px-1 py-1 transition-colors hover:border-edge hover:bg-surface-elevated/60"
                >
                  <span
                    className={
                      notification.severity === 'ERROR'
                        ? 'mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-danger'
                        : notification.severity === 'WARNING'
                          ? 'mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-warn'
                          : 'mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-cerulean'
                    }
                    aria-hidden
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-[12px] font-medium text-ink-primary">
                      {notification.title}
                    </p>
                    <p className="mt-0.5 line-clamp-2 text-[11px] leading-relaxed text-ink-muted">
                      {notification.body}
                    </p>
                    <p className="mt-1 text-[10px] text-ink-muted/80">
                      {relativeTime(notification.createdAt)}
                    </p>
                  </div>
                </div>
              ))
            ) : (
              <EmptyState
                icon={<Activity className="h-5 w-5" />}
                title="No activity yet"
                description="Events appear here as returns, pickups and recovery jobs move forward."
              />
            )}
          </CardBody>
        </Card>
      </div>

      <section className="reveal panel lift flex flex-wrap items-center justify-between gap-4 px-5 py-4">
        <div className="flex items-center gap-3">
          <span className="flex h-9 w-9 items-center justify-center rounded-xl border border-cerulean/30 bg-cerulean/10 text-cerulean-light">
            <PackageCheck className="h-4 w-4" />
          </span>
          <div>
            <p className="text-[13px] font-semibold text-ink-primary">
              Recovery decisions are explained, not guessed
            </p>
            <p className="mt-0.5 text-[11px] text-ink-muted">
              The engine ranks eight recovery paths using a documented cost model. Every option shows
              its gross value, costs, net recovery value and the reasons it was ruled out.
            </p>
          </div>
        </div>
        <Link to="/recovery">
          <Button variant="secondary" size="sm">
            Open recovery engine
            <ArrowRight className="h-3.5 w-3.5" />
          </Button>
        </Link>
      </section>
    </div>
  );
}
