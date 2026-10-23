import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Info, MapPinned, Route as RouteIcon, Truck, Zap } from 'lucide-react';
import { logisticsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Input, Select } from '@/components/ui/Form';
import { Skeleton } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { cn } from '@/lib/cn';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import type { Route } from '@/lib/types';

export function RoutesPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [depotId, setDepotId] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);

  const locations = useQuery({
    queryKey: ['locations'],
    queryFn: () => logisticsApi.locations(),
  });

  const routes = useQuery({
    queryKey: ['routes', date],
    queryFn: () => logisticsApi.routes({ date, size: 50 }),
  });

  const detail = useQuery({
    queryKey: ['route', expanded],
    queryFn: () => logisticsApi.route(expanded!),
    enabled: Boolean(expanded),
  });

  const optimize = useMutation({
    mutationFn: () => logisticsApi.optimize(date, depotId || undefined),
    onSuccess: (created) => {
      toast.success(
        'Routes recalculated',
        `${created.length} route(s) planned with the capacity-constrained optimiser.`,
      );
      void queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
    onError: (error) =>
      toast.error('Routes could not be planned', error instanceof ApiError ? error.message : undefined),
  });

  const completeStop = useMutation({
    mutationFn: ({ routeId, stopId }: { routeId: string; stopId: string }) =>
      logisticsApi.completeStop(routeId, stopId, 'Collected by the assigned driver'),
    onSuccess: () => {
      toast.success('Stop completed');
      void queryClient.invalidateQueries({ queryKey: ['route', expanded] });
      void queryClient.invalidateQueries({ queryKey: ['routes'] });
    },
    onError: (error) =>
      toast.error('The stop could not be completed', error instanceof ApiError ? error.message : undefined),
  });

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-end gap-3 px-4 py-3">
          <label className="flex flex-col gap-1.5">
            <span className="text-xs font-medium text-ink-secondary">Route date</span>
            <Input
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              className="w-40"
            />
          </label>
          <label className="flex min-w-[220px] flex-col gap-1.5">
            <span className="text-xs font-medium text-ink-secondary">Depot</span>
            <Select value={depotId} onChange={(event) => setDepotId(event.target.value)}>
              <option value="">Default warehouse</option>
              {locations.data
                ?.filter((location) => location.locationType === 'WAREHOUSE')
                .map((location) => (
                  <option key={location.id} value={location.id}>
                    {location.name}
                  </option>
                ))}
            </Select>
          </label>
          <Button
            className="ml-auto"
            onClick={() => optimize.mutate()}
            loading={optimize.isPending}
            icon={<Zap className="h-4 w-4" />}
          >
            Optimise routes
          </Button>
        </div>
        <div className="flex items-start gap-2 border-t border-edge bg-surface-elevated/40 px-4 py-2.5">
          <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-cerulean-light" aria-hidden />
          <p className="text-[11px] leading-relaxed text-ink-muted">
            The planner uses a capacity-constrained nearest-neighbour sweep with a 2-opt improvement
            pass over great-circle distances. Distances and durations are estimates, not live
            navigation data.
          </p>
        </div>
      </Card>

      {routes.isError ? (
        <ErrorState
          title="Routes could not be loaded"
          description={(routes.error as Error)?.message}
          onRetry={() => void routes.refetch()}
        />
      ) : routes.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : routes.data && routes.data.content.length > 0 ? (
        <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
          {routes.data.content.map((route) => (
            <RouteCard
              key={route.id}
              route={route}
              expanded={expanded === route.id}
              onToggle={() => setExpanded(expanded === route.id ? null : route.id)}
            />
          ))}
        </div>
      ) : (
        <Card>
          <EmptyState
            icon={<RouteIcon className="h-5 w-5" />}
            title="No routes planned for this date"
            description="Schedule pickups for the date, then run the optimiser to consolidate them into vehicle routes."
            action={
              <Button size="sm" onClick={() => optimize.mutate()} loading={optimize.isPending}>
                Optimise routes
              </Button>
            }
          />
        </Card>
      )}

      {expanded && detail.data ? (
        <Card className="reveal">
          <CardHeader
            title={`${detail.data.routeNumber} — stop sequence`}
            description={`${detail.data.vehicleName ?? 'Unassigned vehicle'} · ${detail.data.originName ?? 'Depot'}`}
            action={
              <StatusPill label={detail.data.statusLabel} tone={toneForStatus(detail.data.status)} dot />
            }
          />
          <CardBody>
            <ol className="space-y-2">
              {detail.data.stops?.map((stop) => (
                <li
                  key={stop.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-edge bg-surface-base px-3.5 py-3"
                >
                  <div className="flex min-w-0 items-start gap-3">
                    <span
                      className={cn(
                        'flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold',
                        stop.completed
                          ? 'bg-ok/20 text-ok'
                          : 'bg-cerulean/20 text-cerulean-light',
                      )}
                    >
                      {stop.sequence}
                    </span>
                    <div className="min-w-0">
                      <p className="truncate text-[12px] font-medium text-ink-primary">
                        {stop.locationName}
                      </p>
                      <p className="mt-0.5 text-[11px] text-ink-muted">
                        {stop.unitCount} unit(s) · leg {stop.legDistanceKm?.toFixed(1) ?? '—'} km ·{' '}
                        {stop.legDurationMinutes ?? 0} min
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {stop.completed ? (
                      <StatusPill label="Collected" tone="success" dot />
                    ) : (
                      <Button
                        size="sm"
                        variant="secondary"
                        loading={completeStop.isPending}
                        onClick={() =>
                          completeStop.mutate({ routeId: detail.data!.id, stopId: stop.id })
                        }
                      >
                        Mark collected
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ol>
            <div className="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-edge pt-3 text-[11px] text-ink-muted">
              <span>
                Algorithm: <code className="font-mono text-ink-secondary">{detail.data.algorithm}</code>
              </span>
              <span>Distances are estimates, not live navigation data.</span>
            </div>
          </CardBody>
        </Card>
      ) : null}
    </div>
  );
}

function RouteCard({
  route,
  expanded,
  onToggle,
}: {
  route: Route;
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <Card className="reveal">
      <CardHeader
        title={route.routeNumber}
        description={`${route.vehicleName ?? 'Unassigned'} · ${formatDate(route.routeDate)}`}
        action={<StatusPill label={route.statusLabel} tone={toneForStatus(route.status)} dot />}
      />
      <CardBody className="space-y-3">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Metric label="Stops" value={formatNumber(route.stopCount)} />
          <Metric label="Units" value={formatNumber(route.totalUnits)} />
          <Metric
            label="Est. distance"
            value={`${route.estimatedDistanceKm?.toFixed(1) ?? '—'} km`}
          />
          <Metric
            label="Est. duration"
            value={`${route.estimatedDurationMinutes ?? 0} min`}
          />
        </div>

        {route.notes ? (
          <p className="rounded-lg border border-edge bg-surface-base px-3 py-2 text-[11px] leading-relaxed text-ink-muted">
            {route.notes}
          </p>
        ) : null}

        <div className="flex items-center justify-between gap-2">
          <span className="inline-flex items-center gap-1.5 text-[11px] text-ink-muted">
            <Truck className="h-3.5 w-3.5" aria-hidden />
            Est. transport cost {formatCurrency(route.estimatedCost ?? 0)}
          </span>
          <Button variant="secondary" size="sm" onClick={onToggle} icon={<MapPinned className="h-3.5 w-3.5" />}>
            {expanded ? 'Hide stops' : 'Show stops'}
          </Button>
        </div>
      </CardBody>
    </Card>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-edge bg-surface-base px-3 py-2">
      <p className="text-[10px] uppercase tracking-wide text-ink-muted">{label}</p>
      <p className="mt-0.5 text-[13px] font-semibold tabular-nums text-ink-primary">{value}</p>
    </div>
  );
}
