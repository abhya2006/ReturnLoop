import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Cpu, Info, ShieldCheck } from 'lucide-react';
import { analyticsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { StatusPill } from '@/components/ui/StatusPill';
import { formatNumber } from '@/lib/format';

export function SettingsPage() {
  const [range] = useState(() => {
    const to = new Date();
    const from = new Date(to);
    from.setDate(from.getDate() - 29);
    return {
      from: from.toISOString().slice(0, 10),
      to: to.toISOString().slice(0, 10),
    };
  });

  const report = useQuery({
    queryKey: ['settings', 'report', range.from, range.to],
    queryFn: () => analyticsApi.report({ from: range.from, to: range.to }),
  });

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="reveal">
        <CardHeader
          title="Recovery engine"
          description="How recovery decisions are produced"
          action={<Cpu className="h-4 w-4 text-ink-muted" aria-hidden />}
        />
        <CardBody className="space-y-3 text-xs leading-relaxed text-ink-secondary">
          <p>
            The recovery engine is a <strong className="text-ink-primary">rule-based, explainable
            system</strong>. It is not a trained machine-learning model and does not present itself as
            one.
          </p>
          <p>
            For each item it applies a documented cost model to eight recovery destinations and
            computes:
          </p>
          <pre className="overflow-x-auto rounded-lg border border-edge bg-surface-base px-3.5 py-3 font-mono text-[11px] leading-relaxed text-cerulean-light">
            netRecoveryValue = expectedResaleValue
            {'  '}- repairCost
            {'  '}- inspectionCost
            {'  '}- handlingCost
            {'  '}- transportationCost
            {'  '}- partnerFees
          </pre>
          <p>
            The feasible options are then ranked by a weighted score across net value, value yield,
            turnaround time, circularity benefit and data quality. Every option in the comparison view
            shows its full economics, its eligibility restrictions and the reasons it was ruled out.
          </p>
          <p>
            The ranking strategy sits behind an interface, so a learned model can be introduced later
            without rewriting the rest of the application.
          </p>
        </CardBody>
      </Card>

      <Card className="reveal">
        <CardHeader
          title="Platform capabilities"
          description="What this deployment has enabled"
          action={<ShieldCheck className="h-4 w-4 text-ink-muted" aria-hidden />}
        />
        <CardBody className="space-y-2.5">
          {[
            ['Recovery strategy', 'rule-based-explainable', 'info'],
            ['Route algorithm', 'nearest-neighbor-2opt', 'info'],
            ['Distance model', 'great-circle estimate', 'warning'],
            ['Map rendering', 'simulated projection', 'warning'],
            ['Messaging transport', 'in-process events', 'neutral'],
            ['Authentication', 'JWT bearer tokens', 'success'],
            ['Tenant isolation', 'organisation scoped', 'success'],
          ].map(([label, value, tone]) => (
            <div
              key={label}
              className="flex items-center justify-between gap-3 rounded-lg border border-edge bg-surface-base px-3.5 py-2.5"
            >
              <span className="text-xs text-ink-secondary">{label}</span>
              <StatusPill label={value} tone={tone as 'info'} />
            </div>
          ))}
          <div className="flex items-start gap-2 rounded-lg border border-edge bg-surface-base px-3.5 py-3">
            <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-cerulean-light" aria-hidden />
            <p className="text-[11px] leading-relaxed text-ink-muted">
              Simulated map rendering and great-circle distance estimates are used because no map
              provider credentials are configured. Set{' '}
              <code className="font-mono text-ink-secondary">RETURNLOOP_MAP_API_KEY</code> to enable a
              real tile provider.
            </p>
          </div>
        </CardBody>
      </Card>

      <Card className="reveal lg:col-span-2">
        <CardHeader title="Workspace summary" description="A snapshot of the current period" />
        <CardBody>
          {report.data ? (
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              {[
                ['Returns handled', formatNumber(report.data.totalReturns)],
                ['Recovery success', `${report.data.recoverySuccessRate.toFixed(0)}%`],
                ['Value recovered', report.data.realizedValue.toFixed(2)],
                ['Pickups consolidated', formatNumber(report.data.consolidatedPickups)],
              ].map(([label, value]) => (
                <div key={label} className="rounded-lg border border-edge bg-surface-base px-3.5 py-3">
                  <p className="text-[10px] uppercase tracking-wide text-ink-muted">{label}</p>
                  <p className="mt-1 text-lg font-bold tabular-nums text-ink-primary">{value}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-ink-muted">Loading the current period…</p>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
