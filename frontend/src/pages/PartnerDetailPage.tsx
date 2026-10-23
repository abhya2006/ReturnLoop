import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Building2, Mail, MapPin, Phone, Star } from 'lucide-react';
import { partnerApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Skeleton } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { formatCurrency, formatDate, formatNumber, formatPercent } from '@/lib/format';

export function PartnerDetailPage() {
  const { id = '' } = useParams();
  const [tab, setTab] = useState<'profile' | 'jobs'>('profile');

  const query = useQuery({
    queryKey: ['partner', id],
    queryFn: () => partnerApi.get(id),
    enabled: Boolean(id),
  });

  if (query.isLoading) {
    return <Skeleton className="h-96 w-full" />;
  }

  if (query.isError || !query.data) {
    return (
      <ErrorState
        title="This partner could not be loaded"
        description={(query.error as Error)?.message}
        onRetry={() => void query.refetch()}
      />
    );
  }

  const partner = query.data;

  return (
    <div className="space-y-4">
      <div className="flex items-start gap-3">
        <Link
          to="/partners"
          aria-label="Back to partners"
          className="mt-0.5 rounded-lg border border-edge p-2 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
        >
          <ArrowLeft className="h-4 w-4" />
        </Link>
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-lg font-semibold tracking-tight text-ink-primary">{partner.name}</h2>
            <StatusPill label={partner.partnerTypeLabel} tone="info" />
            {partner.verified ? (
              <StatusPill label="Verified" tone="success" dot />
            ) : (
              <StatusPill label="Unverified" tone="warning" dot />
            )}
          </div>
          <p className="mt-0.5 text-xs text-ink-muted">
            {partner.currentlyAvailable
              ? 'Currently accepting work'
              : 'Not currently accepting work'}
          </p>
        </div>
      </div>

      <div className="flex gap-1 border-b border-edge">
        {(['profile', 'jobs'] as const).map((entry) => (
          <button
            key={entry}
            type="button"
            onClick={() => setTab(entry)}
            className={
              tab === entry
                ? 'border-b-2 border-rose px-3.5 py-2 text-[13px] font-medium text-ink-primary'
                : 'border-b-2 border-transparent px-3.5 py-2 text-[13px] font-medium text-ink-muted transition-colors hover:text-ink-secondary'
            }
          >
            {entry === 'profile' ? 'Profile' : `Job history (${partner.recentJobs.length})`}
          </button>
        ))}
      </div>

      {tab === 'profile' ? (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <div className="space-y-4 lg:col-span-2">
            <Card className="reveal">
              <CardHeader title="About" />
              <CardBody className="space-y-3 text-xs">
                <p className="leading-relaxed text-ink-secondary">
                  {partner.description ?? 'No description provided.'}
                </p>
                {partner.acceptanceCriteria ? (
                  <div className="rounded-lg border border-edge bg-surface-base px-3.5 py-3">
                    <p className="text-[10px] font-semibold uppercase tracking-wide text-ink-muted">
                      Acceptance criteria
                    </p>
                    <p className="mt-1.5 leading-relaxed text-ink-secondary">
                      {partner.acceptanceCriteria}
                    </p>
                  </div>
                ) : null}
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <div>
                    <p className="text-ink-muted">Serviceable regions</p>
                    <p className="mt-0.5 text-ink-primary">
                      {partner.serviceableRegions?.split(',').map((region) => region.trim()).join(', ') ??
                        'Any region'}
                    </p>
                  </div>
                  <div>
                    <p className="text-ink-muted">Supported categories</p>
                    <p className="mt-0.5 text-ink-primary">
                      {partner.supportedCategories?.split(',').map((entry) => entry.trim()).join(', ') ??
                        'Any category'}
                    </p>
                  </div>
                </div>
              </CardBody>
            </Card>

            <Card className="reveal">
              <CardHeader
                title="Recorded performance"
                description="Derived from completed jobs — never entered by hand"
              />
              <CardBody>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <Metric label="Completed jobs" value={formatNumber(partner.completedJobs)} />
                  <Metric label="Failed jobs" value={formatNumber(partner.failedJobs)} />
                  <Metric
                    label="On-time rate"
                    value={partner.onTimeRate !== null ? formatPercent(partner.onTimeRate) : '—'}
                  />
                  <Metric
                    label="Avg. turnaround"
                    value={
                      partner.averageProcessingDays !== null
                        ? `${partner.averageProcessingDays.toFixed(1)} d`
                        : '—'
                    }
                  />
                </div>
                <p className="mt-3 text-[11px] leading-relaxed text-ink-muted">
                  Acceptance rate:{' '}
                  {partner.acceptanceRate !== null ? formatPercent(partner.acceptanceRate) : 'no decided jobs yet'}.
                  Metrics appear once this partner has completed and accepted work in your organisation.
                </p>
              </CardBody>
            </Card>
          </div>

          <div className="space-y-4">
            <Card className="reveal">
              <CardHeader title="Contact" />
              <CardBody className="space-y-2.5 text-xs">
                <p className="flex items-center gap-2 text-ink-secondary">
                  <Building2 className="h-3.5 w-3.5 text-ink-muted" aria-hidden />
                  {partner.contactName ?? '—'}
                </p>
                {partner.contactEmail ? (
                  <p className="flex items-center gap-2 text-ink-secondary">
                    <Mail className="h-3.5 w-3.5 text-ink-muted" aria-hidden />
                    {partner.contactEmail}
                  </p>
                ) : null}
                {partner.contactPhone ? (
                  <p className="flex items-center gap-2 text-ink-secondary">
                    <Phone className="h-3.5 w-3.5 text-ink-muted" aria-hidden />
                    {partner.contactPhone}
                  </p>
                ) : null}
                {partner.address ? (
                  <p className="flex items-start gap-2 text-ink-secondary">
                    <MapPin className="mt-0.5 h-3.5 w-3.5 shrink-0 text-ink-muted" aria-hidden />
                    {partner.address}
                  </p>
                ) : null}
              </CardBody>
            </Card>

            <Card className="reveal">
              <CardHeader title="Commercial terms" />
              <CardBody className="space-y-2.5 text-xs">
                <Row label="Fee per unit" value={partner.feePerUnit !== null ? formatCurrency(partner.feePerUnit) : '—'} />
                <Row label="Fee percentage" value={partner.feePercentage !== null ? `${partner.feePercentage}%` : '—'} />
                <Row label="Daily capacity" value={`${partner.processingCapacityPerDay} units`} />
                <Row label="Verified" value={partner.verified ? 'Yes' : 'No'} />
              </CardBody>
            </Card>
          </div>
        </div>
      ) : partner.recentJobs.length > 0 ? (
        <Card>
          <div className="overflow-x-auto">
            <table className="data-table rows-enter">
              <thead>
                <tr>
                  <th>Job</th>
                  <th>Path</th>
                  <th>Status</th>
                  <th>Partner</th>
                  <th className="text-right">Expected value</th>
                  <th className="text-right">Realised</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {partner.recentJobs.map((job) => (
                  <tr key={job.id}>
                    <td className="font-mono text-[12px] text-cerulean-light">{job.jobNumber}</td>
                    <td>{job.recoveryPathLabel}</td>
                    <td>
                      <StatusPill label={job.statusLabel} tone={toneForStatus(job.status)} dot />
                    </td>
                    <td className="text-ink-muted">{job.partnerName ?? '—'}</td>
                    <td className="text-right tabular-nums">
                      {formatCurrency(job.expectedRecoveryValue ?? 0)}
                    </td>
                    <td className="text-right tabular-nums">
                      {job.realizedValue !== null ? formatCurrency(job.realizedValue) : '—'}
                    </td>
                    <td className="whitespace-nowrap text-ink-muted">{formatDate(job.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      ) : (
        <Card>
          <EmptyState
            icon={<Star className="h-5 w-5" />}
            title="No job history yet"
            description="Jobs assigned to this partner will appear here with their outcomes."
          />
        </Card>
      )}
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-edge bg-surface-base px-3 py-2.5">
      <p className="text-[10px] uppercase tracking-wide text-ink-muted">{label}</p>
      <p className="mt-0.5 text-sm font-semibold tabular-nums text-ink-primary">{value}</p>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <span className="text-ink-muted">{label}</span>
      <span className="tabular-nums text-ink-primary">{value}</span>
    </div>
  );
}
