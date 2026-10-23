import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, Download, Info, TrendingUp } from 'lucide-react';
import { analyticsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { ErrorState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  DateRangeSelector,
  defaultRange,
  type DateRange,
} from '@/components/common/DateRangeSelector';
import { RecoveryDonutChart } from '@/components/charts/RecoveryDonutChart';
import { ReturnTrendChart } from '@/components/charts/ReturnTrendChart';
import { formatCurrency, formatNumber, formatPercent } from '@/lib/format';

export function ReportsPage() {
  const [range, setRange] = useState<DateRange>(defaultRange());

  const report = useQuery({
    queryKey: ['report', range.from, range.to],
    queryFn: () => analyticsApi.report({ from: range.from, to: range.to }),
  });

  const trend = useQuery({
    queryKey: ['trend', range.from, range.to],
    queryFn: () => analyticsApi.dashboard({ from: range.from, to: range.to }),
  });

  const exportReport = () => {
    if (!report.data) return;
    const lines = [
      'Metric,Value',
      `Period,${report.data.from} to ${report.data.to}`,
      `Total returns,${report.data.totalReturns}`,
      `Recovery success rate,${report.data.recoverySuccessRate}%`,
      `Estimated value,${report.data.estimatedValue}`,
      `Realised value,${report.data.realizedValue}`,
      `Realisation rate,${report.data.realizationRate}%`,
      `Average turnaround days,${report.data.averageTurnaroundDays ?? ''}`,
      `Consolidated pickups,${report.data.consolidatedPickups}`,
      '',
      'Recovery path,Items,Percentage,Value',
      ...report.data.recoveryDistribution.map(
        (slice) => `${slice.label},${slice.count},${slice.percentage},${slice.value}`,
      ),
    ];
    const blob = new Blob([lines.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `returnloop-report-${range.from}-to-${range.to}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <DateRangeSelector value={range} onChange={setRange} />
        <Button
          variant="secondary"
          size="md"
          onClick={exportReport}
          disabled={!report.data}
          icon={<Download className="h-4 w-4" />}
        >
          Export report
        </Button>
      </div>

      {report.isError ? (
        <ErrorState
          title="The report could not be generated"
          description={(report.error as Error)?.message}
          onRetry={() => void report.refetch()}
        />
      ) : report.isLoading || !report.data ? (
        <div className="space-y-4">
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-72 w-full" />
        </div>
      ) : (
        <>
          <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Return volume"
              value={formatNumber(report.data.totalReturns)}
              hint="returns created in the period"
              icon={<BarChart3 className="h-4 w-4" />}
            />
            <StatCard
              label="Recovery success rate"
              value={formatPercent(report.data.recoverySuccessRate)}
              hint="reached recovered or completed"
              icon={<TrendingUp className="h-4 w-4" />}
            />
            <StatCard
              label="Estimated vs realised"
              value={`${formatCurrency(report.data.estimatedValue, 'USD', true)} → ${formatCurrency(
                report.data.realizedValue,
                'USD',
                true,
              )}`}
              hint={`${formatPercent(report.data.realizationRate)} of estimate realised`}
              icon={<TrendingUp className="h-4 w-4" />}
            />
            <StatCard
              label="Average turnaround"
              value={
                report.data.averageTurnaroundDays !== null
                  ? `${report.data.averageTurnaroundDays.toFixed(1)} d`
                  : '—'
              }
              hint="mean days from creation to recovery"
              icon={<BarChart3 className="h-4 w-4" />}
            />
          </section>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
            <Card className="reveal xl:col-span-2">
              <CardHeader title="Return volume trend" description="Daily returns created against items recovered" />
              <CardBody>
                {trend.data ? (
                  <ReturnTrendChart data={trend.data.trend} />
                ) : (
                  <Skeleton className="h-[280px] w-full" />
                )}
              </CardBody>
            </Card>

            <Card className="reveal">
              <CardHeader title="Recovery path mix" description="How returns were routed" />
              <CardBody>
                <RecoveryDonutChart data={report.data.recoveryDistribution} />
              </CardBody>
            </Card>
          </div>

          <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
            <Card className="reveal">
              <CardHeader
                title="Partner performance"
                description="Completed jobs, derived from the job records"
              />
              <CardBody>
                {report.data.partnerPerformance.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="data-table rows-enter">
                      <thead>
                        <tr>
                          <th>Partner</th>
                          <th className="text-right">Completed jobs</th>
                          <th className="text-right">On-time rate</th>
                          <th className="text-right">Avg. turnaround</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.data.partnerPerformance.map((partner) => (
                          <tr key={partner.partnerName}>
                            <td className="font-medium text-ink-primary">{partner.partnerName}</td>
                            <td className="text-right tabular-nums">{partner.completedJobs}</td>
                            <td className="text-right tabular-nums">
                              {partner.onTimeRate !== null ? formatPercent(partner.onTimeRate) : '—'}
                            </td>
                            <td className="text-right tabular-nums">
                              {partner.averageProcessingDays !== null
                                ? `${partner.averageProcessingDays.toFixed(1)} d`
                                : '—'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="py-8 text-center text-xs text-ink-muted">
                    No jobs have been completed in this period.
                  </p>
                )}
              </CardBody>
            </Card>

            <Card className="reveal">
              <CardHeader title="Logistics efficiency" description="Consolidation and avoided transport" />
              <CardBody className="space-y-3">
                <div className="flex items-center justify-between rounded-lg border border-edge bg-surface-base px-3.5 py-3">
                  <span className="text-xs text-ink-muted">Pickups consolidated into routes</span>
                  <span className="text-sm font-semibold tabular-nums text-ink-primary">
                    {formatNumber(report.data.consolidatedPickups)}
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-edge bg-surface-base px-3.5 py-3">
                  <span className="text-xs text-ink-muted">Gross value kept in circulation</span>
                  <span className="text-sm font-semibold tabular-nums text-ok">
                    {formatCurrency(report.data.wasteAvoidedValue)}
                  </span>
                </div>
                <div className="flex items-start gap-2 rounded-lg border border-cerulean/25 bg-cerulean/[0.05] px-3.5 py-3">
                  <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-cerulean-light" aria-hidden />
                  <p className="text-[11px] leading-relaxed text-ink-secondary">
                    Transport costs shown on routes are estimates derived from great-circle distance,
                    payload weight and a per-kilometre rate. They are not carrier invoices.
                  </p>
                </div>
              </CardBody>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}

function StatCard({
  label,
  value,
  hint,
  icon,
}: {
  label: string;
  value: string;
  hint: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="reveal panel lift p-5">
      <div className="flex items-start justify-between">
        <p className="text-[11px] font-medium uppercase tracking-wide text-ink-muted">{label}</p>
        <span className="flex h-8 w-8 items-center justify-center rounded-lg border border-cerulean/30 bg-cerulean/10 text-cerulean-light">
          {icon}
        </span>
      </div>
      <p className="mt-3 text-xl font-bold tracking-tight tabular-nums text-ink-primary">{value}</p>
      <p className="mt-1 text-[11px] text-ink-muted">{hint}</p>
    </div>
  );
}
