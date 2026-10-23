import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Info, Leaf, Recycle, Wind } from 'lucide-react';
import { analyticsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { ErrorState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import {
  DateRangeSelector,
  defaultRange,
  type DateRange,
} from '@/components/common/DateRangeSelector';
import { formatNumber } from '@/lib/format';

export function EnvironmentalPage() {
  const [range, setRange] = useState<DateRange>(defaultRange());

  const query = useQuery({
    queryKey: ['environmental', range.from, range.to],
    queryFn: () => analyticsApi.environmental({ from: range.from, to: range.to }),
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <DateRangeSelector value={range} onChange={setRange} />
        <StatusBadge />
      </div>

      {query.isError ? (
        <ErrorState
          title="The environmental summary could not be loaded"
          description={(query.error as Error)?.message}
          onRetry={() => void query.refetch()}
        />
      ) : query.isLoading || !query.data ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
        </div>
      ) : (
        <>
          <section className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <ImpactCard
              icon={<Recycle className="h-4 w-4" />}
              label="Waste diverted from disposal"
              value={`${formatNumber(query.data.wasteDivertedKg)} kg`}
              hint="estimated from the weight of recovered goods"
              accent="cerulean"
            />
            <ImpactCard
              icon={<Wind className="h-4 w-4" />}
              label="Emissions avoided"
              value={`${formatNumber(query.data.carbonAvoidedKg)} kg CO₂e`}
              hint="estimated using published category factors"
              accent="rose"
            />
            <ImpactCard
              icon={<Leaf className="h-4 w-4" />}
              label="Items assessed"
              value={formatNumber(query.data.itemsAssessed)}
              hint="recovery recommendations evaluated"
              accent="cerulean"
            />
          </section>

          <Card className="reveal">
            <CardHeader
              title="How these figures are calculated"
              description="Every environmental number in ReturnLoop is an estimate with a documented basis"
            />
            <CardBody className="space-y-3">
              <ol className="space-y-2.5">
                {query.data.methodology.map((line, index) => (
                  <li key={index} className="flex gap-2.5 text-xs leading-relaxed text-ink-secondary">
                    <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-cerulean/35 bg-cerulean/12 text-[10px] font-bold text-cerulean-light">
                      {index + 1}
                    </span>
                    {line}
                  </li>
                ))}
              </ol>
              <div className="flex items-start gap-2 rounded-lg border border-warn/30 bg-warn/[0.06] px-3.5 py-3">
                <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-warn" aria-hidden />
                <p className="text-[11px] leading-relaxed text-ink-secondary">
                  ReturnLoop does not measure emissions. These figures are modelled from product
                  weights and published embodied-carbon factors, and they should be read as an
                  indication of scale rather than an audited claim. Disposal and recycling pathways
                  contribute no avoided emissions because the baseline is disposal.
                </p>
              </div>
            </CardBody>
          </Card>
        </>
      )}
    </div>
  );
}

function ImpactCard({
  icon,
  label,
  value,
  hint,
  accent,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  hint: string;
  accent: 'cerulean' | 'rose';
}) {
  return (
    <div className="reveal panel lift p-5">
      <span
        className={
          accent === 'rose'
            ? 'flex h-9 w-9 items-center justify-center rounded-xl border border-rose/30 bg-rose/10 text-rose-light'
            : 'flex h-9 w-9 items-center justify-center rounded-xl border border-cerulean/30 bg-cerulean/10 text-cerulean-light'
        }
        aria-hidden
      >
        {icon}
      </span>
      <p className="mt-4 text-[11px] font-medium uppercase tracking-wide text-ink-muted">{label}</p>
      <p className="mt-1 text-2xl font-bold tracking-tight tabular-nums text-ink-primary">{value}</p>
      <p className="mt-1.5 text-[11px] text-ink-muted">{hint}</p>
    </div>
  );
}

function StatusBadge() {
  return (
    <span className="inline-flex items-center gap-2 rounded-lg border border-warn/30 bg-warn/[0.07] px-3 py-1.5 text-[11px] font-medium text-warn">
      <Info className="h-3.5 w-3.5" aria-hidden />
      Estimates based on documented assumptions
    </span>
  );
}
