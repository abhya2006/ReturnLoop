import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Building2, Search, Star } from 'lucide-react';
import { partnerApi } from '@/api/client';
import { Card } from '@/components/ui/Card';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Input, Select } from '@/components/ui/Form';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill } from '@/components/ui/StatusPill';
import { formatCurrency, formatNumber, formatPercent } from '@/lib/format';

const PARTNER_TYPES = [
  'REPAIR',
  'REFURBISHMENT',
  'RESALE',
  'LIQUIDATION',
  'RECYCLING',
  'LOGISTICS',
];

export function PartnersPage() {
  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const [region, setRegion] = useState('');
  const [availableOnly, setAvailableOnly] = useState(false);
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['partners', search, type, region, availableOnly, page],
    queryFn: () =>
      partnerApi.search({
        search,
        type: type || undefined,
        region: region || undefined,
        availableOnly,
        page,
        size: 12,
      }),
  });

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-edge px-4 py-3">
          <div className="relative min-w-[220px] flex-1">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted"
              aria-hidden
            />
            <Input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
              placeholder="Search partners…"
              className="pl-9"
              aria-label="Search partners"
            />
          </div>
          <Select
            value={type}
            onChange={(event) => {
              setType(event.target.value);
              setPage(0);
            }}
            className="w-48"
            aria-label="Filter by partner type"
          >
            <option value="">All partner types</option>
            {PARTNER_TYPES.map((entry) => (
              <option key={entry} value={entry}>
                {entry.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </Select>
          <Input
            value={region}
            onChange={(event) => {
              setRegion(event.target.value);
              setPage(0);
            }}
            placeholder="Region, e.g. US-NORTHEAST"
            className="w-52"
            aria-label="Filter by serviceable region"
          />
          <label className="inline-flex cursor-pointer items-center gap-2 text-xs text-ink-secondary">
            <input
              type="checkbox"
              checked={availableOnly}
              onChange={(event) => {
                setAvailableOnly(event.target.checked);
                setPage(0);
              }}
              className="h-4 w-4 rounded border-edge bg-surface-elevated text-cerulean focus:ring-cerulean focus:ring-offset-jet"
            />
            Available now
          </label>
        </div>

        {query.isError ? (
          <div className="p-4">
            <ErrorState
              title="Partners could not be loaded"
              description={(query.error as Error)?.message}
              onRetry={() => void query.refetch()}
            />
          </div>
        ) : query.isLoading ? (
          <SkeletonTable rows={6} columns={5} />
        ) : query.data && query.data.content.length > 0 ? (
          <>
            <div className="grid grid-cols-1 gap-4 p-4 md:grid-cols-2 xl:grid-cols-3">
              {query.data.content.map((partner) => (
                <Link
                  key={partner.id}
                  to={`/partners/${partner.id}`}
                  className="group flex flex-col rounded-card border border-edge bg-surface-base p-4 transition-all duration-200 hover:-translate-y-0.5 hover:border-cerulean/40 hover:shadow-glow focus-visible:ring-2 focus-visible:ring-cerulean"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="min-w-0">
                      <p className="truncate text-[13px] font-semibold text-ink-primary">
                        {partner.name}
                      </p>
                      <p className="mt-0.5 text-[11px] text-ink-muted">{partner.partnerTypeLabel}</p>
                    </div>
                    {partner.verified ? (
                      <Star
                        className="h-3.5 w-3.5 shrink-0 text-cerulean-light"
                        aria-label="Verified partner"
                      />
                    ) : null}
                  </div>

                  <p className="mt-2.5 line-clamp-2 text-[11px] leading-relaxed text-ink-secondary">
                    {partner.description ?? 'No description provided.'}
                  </p>

                  <div className="mt-3 flex flex-wrap gap-1.5">
                    <StatusPill
                      label={partner.currentlyAvailable ? 'Accepting work' : 'Unavailable'}
                      tone={partner.currentlyAvailable ? 'success' : 'neutral'}
                      dot
                    />
                    <StatusPill
                      label={`${partner.processingCapacityPerDay}/day`}
                      tone="info"
                    />
                  </div>

                  <dl className="mt-3 grid grid-cols-2 gap-x-3 gap-y-1 border-t border-edge pt-3 text-[11px]">
                    <dt className="text-ink-muted">Completed jobs</dt>
                    <dd className="text-right tabular-nums text-ink-secondary">
                      {formatNumber(partner.completedJobs)}
                    </dd>
                    <dt className="text-ink-muted">On-time rate</dt>
                    <dd className="text-right tabular-nums text-ink-secondary">
                      {partner.onTimeRate !== null ? formatPercent(partner.onTimeRate) : 'No data yet'}
                    </dd>
                    <dt className="text-ink-muted">Avg. turnaround</dt>
                    <dd className="text-right tabular-nums text-ink-secondary">
                      {partner.averageProcessingDays !== null
                        ? `${partner.averageProcessingDays.toFixed(1)} d`
                        : 'No data yet'}
                    </dd>
                    <dt className="text-ink-muted">Fee</dt>
                    <dd className="text-right tabular-nums text-ink-secondary">
                      {partner.feePerUnit !== null
                        ? `${formatCurrency(partner.feePerUnit)}/unit`
                        : partner.feePercentage !== null
                          ? `${partner.feePercentage}%`
                          : 'On request'}
                    </dd>
                  </dl>
                </Link>
              ))}
            </div>
            <Pagination
              page={query.data.page}
              totalPages={query.data.totalPages}
              totalElements={query.data.totalElements}
              onPageChange={setPage}
            />
          </>
        ) : (
          <EmptyState
            icon={<Building2 className="h-5 w-5" />}
            title="No partners match these filters"
            description="Widen the type or region filter, or clear the search to see the full directory."
          />
        )}
      </Card>
    </div>
  );
}
