import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Briefcase, Search } from 'lucide-react';
import { jobApi } from '@/api/client';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Input, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import type { JobStatus, RecoveryJob } from '@/lib/types';

const STATUSES: JobStatus[] = [
  'PROPOSED',
  'ASSIGNED',
  'ACCEPTED',
  'PROCESSING',
  'AWAITING_REVIEW',
  'COMPLETED',
  'REJECTED',
  'CANCELLED',
  'FAILED',
];

const NEXT_ACTION: Partial<Record<JobStatus, { status: JobStatus; label: string }>> = {
  ASSIGNED: { status: 'ACCEPTED', label: 'Accept' },
  ACCEPTED: { status: 'PROCESSING', label: 'Start processing' },
  PROCESSING: { status: 'AWAITING_REVIEW', label: 'Send for review' },
  AWAITING_REVIEW: { status: 'COMPLETED', label: 'Mark completed' },
};

export function PartnerJobsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<JobStatus | ''>('');
  const [page, setPage] = useState(0);
  const [active, setActive] = useState<RecoveryJob | null>(null);

  const query = useQuery({
    queryKey: ['partner-jobs', search, status, page],
    queryFn: () =>
      jobApi.search({
        search,
        status: status ? [status] : undefined,
        page,
        size: 15,
      }),
  });

  const advance = useMutation({
    mutationFn: ({
      id,
      next,
      note,
      realizedValue,
    }: {
      id: string;
      next: JobStatus;
      note?: string;
      realizedValue?: number;
    }) => jobApi.transition(id, next, note, realizedValue),
    onSuccess: (job) => {
      toast.success(`Job ${job.jobNumber} is now ${job.statusLabel.toLowerCase()}`);
      setActive(null);
      void queryClient.invalidateQueries({ queryKey: ['partner-jobs'] });
      void queryClient.invalidateQueries({ queryKey: ['partners'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (error) =>
      toast.error('The job could not be updated', error instanceof ApiError ? error.message : undefined),
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
              placeholder="Search job number, SKU or partner…"
              className="pl-9"
              aria-label="Search partner jobs"
            />
          </div>
          <Select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as JobStatus | '');
              setPage(0);
            }}
            className="w-52"
            aria-label="Filter by job status"
          >
            <option value="">All statuses</option>
            {STATUSES.map((entry) => (
              <option key={entry} value={entry}>
                {entry.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </Select>
        </div>

        {query.isError ? (
          <div className="p-4">
            <ErrorState
              title="Partner jobs could not be loaded"
              description={(query.error as Error)?.message}
              onRetry={() => void query.refetch()}
            />
          </div>
        ) : query.isLoading ? (
          <SkeletonTable rows={8} columns={7} />
        ) : query.data && query.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="data-table rows-enter">
                <thead>
                  <tr>
                    <th>Job</th>
                    <th>Path</th>
                    <th>Status</th>
                    <th>Partner</th>
                    <th className="text-right">Expected value</th>
                    <th className="text-right">Cost</th>
                    <th className="text-right">Realised</th>
                    <th>Deadline</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.content.map((job) => {
                    const next = NEXT_ACTION[job.status];
                    return (
                      <tr key={job.id}>
                        <td className="font-mono text-[12px] text-cerulean-light">{job.jobNumber}</td>
                        <td>
                          <span className="text-ink-primary">{job.recoveryPathLabel}</span>
                          <span className="ml-1.5 text-[11px] text-ink-muted">{job.sku}</span>
                        </td>
                        <td>
                          <StatusPill label={job.statusLabel} tone={toneForStatus(job.status)} dot />
                        </td>
                        <td className="text-ink-secondary">{job.partnerName ?? '—'}</td>
                        <td className="text-right tabular-nums">
                          {formatCurrency(job.expectedRecoveryValue ?? 0)}
                        </td>
                        <td className="text-right tabular-nums text-ink-muted">
                          {formatCurrency(job.costEstimate ?? 0)}
                        </td>
                        <td className="text-right tabular-nums">
                          {job.realizedValue !== null ? (
                            <span className="text-ok">{formatCurrency(job.realizedValue)}</span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td className="whitespace-nowrap text-ink-muted">
                          {job.serviceDeadline ? formatDate(job.serviceDeadline) : '—'}
                        </td>
                        <td className="text-right">
                          <div className="flex justify-end gap-1.5">
                            {next ? (
                              <Button
                                size="sm"
                                variant="secondary"
                                onClick={() =>
                                  advance.mutate({ id: job.id, next: next.status })
                                }
                                loading={advance.isPending}
                              >
                                {next.label}
                              </Button>
                            ) : null}
                            <Button size="sm" variant="ghost" onClick={() => setActive(job)}>
                              Details
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
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
            icon={<Briefcase className="h-5 w-5" />}
            title="No recovery jobs yet"
            description="Approve a recovery recommendation and assign a partner to create the first job."
          />
        )}
      </Card>

      {active ? (
        <JobDetailModal
          job={active}
          onClose={() => setActive(null)}
          onAdvance={(next, note, realizedValue) =>
            advance.mutate({ id: active.id, next, note, realizedValue })
          }
          pending={advance.isPending}
        />
      ) : null}
    </div>
  );
}

function JobDetailModal({
  job,
  onClose,
  onAdvance,
  pending,
}: {
  job: RecoveryJob;
  onClose: () => void;
  onAdvance: (next: JobStatus, note?: string, realizedValue?: number) => void;
  pending: boolean;
}) {
  const [note, setNote] = useState('');
  const [realized, setRealized] = useState('');
  const next = NEXT_ACTION[job.status];

  return (
    <Modal
      open
      onClose={onClose}
      title={`${job.jobNumber} — ${job.recoveryPathLabel}`}
      description={`${job.quantity} unit(s) of ${job.sku ?? 'goods'} with ${job.partnerName ?? 'no partner assigned'}`}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
          {next ? (
            <Button
              loading={pending}
              onClick={() =>
                onAdvance(
                  next.status,
                  note || undefined,
                  next.status === 'COMPLETED' && realized ? Number(realized) : undefined,
                )
              }
            >
              {next.label}
            </Button>
          ) : null}
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <Metric label="Status" value={job.statusLabel} />
          <Metric label="Units" value={formatNumber(job.quantity)} />
          <Metric label="Expected value" value={formatCurrency(job.expectedRecoveryValue ?? 0)} />
          <Metric label="Cost estimate" value={formatCurrency(job.costEstimate ?? 0)} />
        </div>

        <dl className="space-y-2 border-t border-edge pt-3 text-xs">
          <Row label="Return" value={job.returnId ? job.returnId.slice(0, 8) : '—'} />
          <Row label="Service deadline" value={job.serviceDeadline ? formatDate(job.serviceDeadline) : '—'} />
          <Row label="Assigned" value={job.assignedAt ? formatDate(job.assignedAt) : '—'} />
          <Row label="Accepted" value={job.acceptedAt ? formatDate(job.acceptedAt) : '—'} />
          <Row label="Completed" value={job.completedAt ? formatDate(job.completedAt) : '—'} />
        </dl>

        {job.rejectionReason ? (
          <p className="rounded-lg border border-danger/30 bg-danger/[0.06] px-3 py-2 text-[11px] text-danger">
            Rejected: {job.rejectionReason}
          </p>
        ) : null}
        {job.exceptionNote ? (
          <p className="rounded-lg border border-warn/30 bg-warn/[0.06] px-3 py-2 text-[11px] text-warn">
            Exception: {job.exceptionNote}
          </p>
        ) : null}

        {next ? (
          <div className="space-y-3 border-t border-edge pt-3">
            {next.status === 'COMPLETED' ? (
              <label className="block space-y-1.5">
                <span className="text-xs font-medium text-ink-secondary">Realised recovery value</span>
                <Input
                  type="number"
                  step="0.01"
                  min={0}
                  value={realized}
                  onChange={(event) => setRealized(event.target.value)}
                  placeholder="0.00"
                />
              </label>
            ) : null}
            <label className="block space-y-1.5">
              <span className="text-xs font-medium text-ink-secondary">
                {next.status === 'REJECTED' ? 'Rejection reason' : 'Note (optional)'}
              </span>
              <Textarea
                rows={2}
                value={note}
                onChange={(event) => setNote(event.target.value)}
                placeholder="Recorded against the job history"
              />
            </label>
          </div>
        ) : null}
      </div>
    </Modal>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-edge bg-surface-base px-3 py-2">
      <p className="text-[10px] uppercase tracking-wide text-ink-muted">{label}</p>
      <p className="mt-0.5 truncate text-[13px] font-semibold tabular-nums text-ink-primary">{value}</p>
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
