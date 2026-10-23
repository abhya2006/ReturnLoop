import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ArrowLeft,
  Ban,
  ClipboardCheck,
  Coins,
  History,
  MapPin,
  Package,
  Sparkles,
  Truck,
} from 'lucide-react';
import { recoveryApi, returnsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { ErrorState } from '@/components/ui/EmptyState';
import { Skeleton, SkeletonText } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { Modal } from '@/components/ui/Modal';
import { Field, Textarea } from '@/components/ui/Form';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatCurrency, formatDateTime, relativeTime } from '@/lib/format';
import type { ReturnStatus } from '@/lib/types';

const NEXT_STATUS: Partial<Record<ReturnStatus, { status: ReturnStatus; label: string }>> = {
  RETURN_CREATED: { status: 'PICKUP_SCHEDULED', label: 'Mark pickup scheduled' },
  PICKUP_SCHEDULED: { status: 'COLLECTED', label: 'Mark collected' },
  COLLECTED: { status: 'WAREHOUSE_RECEIVED', label: 'Mark warehouse received' },
  WAREHOUSE_RECEIVED: { status: 'INSPECTED', label: 'Mark inspected' },
  INSPECTED: { status: 'RECOVERY_DECISION', label: 'Move to recovery decision' },
  RECOVERY_DECISION: { status: 'PARTNER_ASSIGNED', label: 'Mark partner assigned' },
  PARTNER_ASSIGNED: { status: 'PROCESSING', label: 'Mark processing' },
  PROCESSING: { status: 'RECOVERED', label: 'Mark recovered' },
  RECOVERED: { status: 'COMPLETED', label: 'Close return' },
};

export function ReturnDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const detail = useQuery({
    queryKey: ['return', id],
    queryFn: () => returnsApi.get(id),
    enabled: Boolean(id),
  });

  const recommendations = useQuery({
    queryKey: ['return-recommendations', id],
    queryFn: () => recoveryApi.recommendations(id),
    enabled: Boolean(id),
  });

  const transition = useMutation({
    mutationFn: (status: ReturnStatus) => returnsApi.transition(id, status),
    onSuccess: (updated) => {
      toast.success(`Return moved to ${updated.statusLabel}`);
      void queryClient.invalidateQueries({ queryKey: ['return', id] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (error) =>
      toast.error('The status could not be changed', error instanceof ApiError ? error.message : undefined),
  });

  const cancel = useMutation({
    mutationFn: (reason: string) => returnsApi.cancel(id, reason),
    onSuccess: (updated) => {
      toast.success('Return cancelled', `Reason recorded against ${updated.returnNumber}.`);
      setCancelOpen(false);
      setCancelReason('');
      void queryClient.invalidateQueries({ queryKey: ['return', id] });
    },
    onError: (error) =>
      toast.error('The return could not be cancelled', error instanceof ApiError ? error.message : undefined),
  });

  if (detail.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-24 w-full" />
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <Skeleton className="h-72 lg:col-span-2" />
          <Skeleton className="h-72" />
        </div>
      </div>
    );
  }

  if (detail.isError || !detail.data) {
    return (
      <ErrorState
        title="This return could not be loaded"
        description={(detail.error as Error)?.message}
        onRetry={() => void detail.refetch()}
      />
    );
  }

  const data = detail.data;
  const next = NEXT_STATUS[data.status];
  const canCancel = !['COMPLETED', 'CANCELLED'].includes(data.status);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-3">
          <button
            type="button"
            onClick={() => navigate('/returns')}
            aria-label="Back to returns"
            className="mt-0.5 rounded-lg border border-edge p-2 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <ArrowLeft className="h-4 w-4" />
          </button>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-semibold tracking-tight text-ink-primary">
                {data.returnNumber}
              </h2>
              <StatusPill label={data.statusLabel} tone={toneForStatus(data.status)} dot />
            </div>
            <p className="mt-0.5 text-xs text-ink-muted">
              Created {formatDateTime(data.createdAt)} · {data.itemCount} item(s) · {data.reasonLabel}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Link to={`/recovery/${data.id}`}>
            <Button variant="secondary" size="md" icon={<Sparkles className="h-4 w-4" />}>
              Recovery options
            </Button>
          </Link>
          <Link to={`/inspections?returnId=${data.id}`}>
            <Button variant="secondary" size="md" icon={<ClipboardCheck className="h-4 w-4" />}>
              Inspections
            </Button>
          </Link>
          {canCancel ? (
            <Button variant="danger" size="md" onClick={() => setCancelOpen(true)} icon={<Ban className="h-4 w-4" />}>
              Cancel
            </Button>
          ) : null}
          {next ? (
            <Button
              size="md"
              loading={transition.isPending}
              onClick={() => transition.mutate(next.status)}
            >
              {next.label}
            </Button>
          ) : null}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="space-y-4 lg:col-span-2">
          {/* ------------------------------------------------------- items */}
          <Card className="reveal">
            <CardHeader title="Items" description="Products included in this return" />
            <div className="overflow-x-auto">
              <table className="data-table rows-enter">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>SKU</th>
                    <th>Qty</th>
                    <th>Condition</th>
                    <th className="text-right">Est. value</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((item) => (
                    <tr key={item.id}>
                      <td className="font-medium text-ink-primary">{item.productName}</td>
                      <td className="font-mono text-[12px] text-ink-muted">{item.sku}</td>
                      <td className="tabular-nums">{item.quantity}</td>
                      <td>
                        <StatusPill
                          label={item.conditionLabel}
                          tone={
                            item.condition === 'NEW' || item.condition === 'LIKE_NEW'
                              ? 'success'
                              : item.condition === 'GOOD'
                                ? 'info'
                                : item.condition === 'FAIR'
                                  ? 'warning'
                                  : 'danger'
                          }
                        />
                      </td>
                      <td className="text-right tabular-nums">
                        {formatCurrency(item.estimatedValue ?? 0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>

          {/* -------------------------------------------------- timeline */}
          <Card className="reveal">
            <CardHeader
              title="Lifecycle timeline"
              description="Every recorded event, in order"
              action={<History className="h-4 w-4 text-ink-muted" aria-hidden />}
            />
            <CardBody>
              {data.timeline.length === 0 ? (
                <SkeletonText lines={2} />
              ) : (
                <ol className="relative space-y-4 border-l border-edge pl-5">
                  {data.timeline.map((event) => (
                    <li key={event.id} className="relative">
                      <span
                        className="absolute -left-[26px] top-1 h-2.5 w-2.5 rounded-full border-2 border-jet bg-cerulean"
                        aria-hidden
                      />
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <p className="text-[13px] font-medium text-ink-primary">
                          {event.description}
                        </p>
                        <span className="text-[11px] text-ink-muted">
                          {relativeTime(event.occurredAt)}
                        </span>
                      </div>
                      <p className="mt-0.5 text-[11px] text-ink-muted">
                        {event.eventType.replace(/_/g, ' ').toLowerCase()}
                        {event.actor ? ` · ${event.actor}` : ''}
                      </p>
                    </li>
                  ))}
                </ol>
              )}
            </CardBody>
          </Card>

          {/* -------------------------------------------- recommendations */}
          <Card className="reveal">
            <CardHeader
              title="Recovery recommendations"
              description="Options calculated by the recovery engine"
              action={
                <Link
                  to={`/recovery/${data.id}`}
                  className="text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean"
                >
                  Open comparison
                </Link>
              }
            />
            <CardBody className="space-y-2">
              {recommendations.isLoading ? (
                <SkeletonText lines={3} />
              ) : recommendations.data && recommendations.data.length > 0 ? (
                recommendations.data.slice(0, 5).map((recommendation) => (
                  <div
                    key={recommendation.id}
                    className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-edge bg-surface-base px-3 py-2.5"
                  >
                    <div className="min-w-0">
                      <p className="text-[12px] font-medium text-ink-primary">
                        {recommendation.sku} → {recommendation.recommendedPathLabel}
                      </p>
                      <p className="mt-0.5 text-[11px] text-ink-muted">
                        {recommendation.approved
                          ? `Approved by ${recommendation.decidedBy}`
                          : recommendation.rejected
                            ? `Rejected: ${recommendation.decisionReason}`
                            : 'Awaiting a decision'}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-[12px] font-semibold tabular-nums text-ink-primary">
                        {formatCurrency(recommendation.recommendedNetValue ?? 0)}
                      </span>
                      <StatusPill
                        label={recommendation.approved ? 'Approved' : recommendation.rejected ? 'Rejected' : 'Pending'}
                        tone={
                          recommendation.approved
                            ? 'success'
                            : recommendation.rejected
                              ? 'danger'
                              : 'warning'
                        }
                      />
                    </div>
                  </div>
                ))
              ) : (
                <p className="py-4 text-center text-xs text-ink-muted">
                  No recommendations yet. Run the recovery engine to calculate them.
                </p>
              )}
            </CardBody>
          </Card>
        </div>

        {/* -------------------------------------------------------- side */}
        <div className="space-y-4">
          <Card className="reveal">
            <CardHeader title="Recovery economics" />
            <CardBody className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-ink-muted">Estimated recovery value</span>
                <span className="text-sm font-semibold tabular-nums text-ink-primary">
                  {formatCurrency(data.estimatedRecoveryValue ?? 0)}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-ink-muted">Realised recovery value</span>
                <span className="text-sm font-semibold tabular-nums text-ok">
                  {data.realizedRecoveryValue !== null
                    ? formatCurrency(data.realizedRecoveryValue)
                    : 'Not yet realised'}
                </span>
              </div>
              {data.estimatedRecoveryValue && data.realizedRecoveryValue !== null ? (
                <div className="flex items-center justify-between border-t border-edge pt-3">
                  <span className="text-xs text-ink-muted">Realisation rate</span>
                  <span className="text-sm font-semibold tabular-nums text-ink-primary">
                    {((data.realizedRecoveryValue / data.estimatedRecoveryValue) * 100).toFixed(0)}%
                  </span>
                </div>
              ) : null}
              {data.cancelledReason ? (
                <p className="rounded-lg border border-danger/30 bg-danger/[0.06] px-3 py-2 text-[11px] text-danger">
                  Cancelled: {data.cancelledReason}
                </p>
              ) : null}
            </CardBody>
          </Card>

          <Card className="reveal">
            <CardHeader title="Collection & origin" />
            <CardBody className="space-y-3 text-xs">
              <div>
                <p className="text-ink-muted">Customer</p>
                <p className="mt-0.5 text-ink-primary">{data.customerName ?? '—'}</p>
              </div>
              <div>
                <p className="text-ink-muted">Source reference</p>
                <p className="mt-0.5 font-mono text-ink-primary">{data.sourceReference ?? '—'}</p>
              </div>
              <div>
                <p className="text-ink-muted">Origin address</p>
                <p className="mt-0.5 text-ink-primary">{data.originAddress ?? '—'}</p>
              </div>
              <div className="grid grid-cols-2 gap-2 border-t border-edge pt-3">
                <div>
                  <p className="text-ink-muted">Latitude</p>
                  <p className="mt-0.5 tabular-nums text-ink-primary">
                    {data.originLatitude != null ? data.originLatitude.toFixed(4) : '—'}
                  </p>
                </div>
                <div>
                  <p className="text-ink-muted">Longitude</p>
                  <p className="mt-0.5 tabular-nums text-ink-primary">
                    {data.originLongitude != null ? data.originLongitude.toFixed(4) : '—'}
                  </p>
                </div>
              </div>
              {data.notes ? (
                <div className="border-t border-edge pt-3">
                  <p className="text-ink-muted">Notes</p>
                  <p className="mt-0.5 leading-relaxed text-ink-primary">{data.notes}</p>
                </div>
              ) : null}
            </CardBody>
          </Card>

          <Card className="reveal">
            <CardHeader title="Related" />
            <CardBody className="space-y-2">
              <Link
                to="/pickups"
                className="flex items-center gap-2.5 rounded-lg border border-edge px-3 py-2.5 text-[12px] text-ink-secondary transition-colors hover:border-cerulean/50 hover:text-ink-primary"
              >
                <Truck className="h-4 w-4 text-cerulean-light" aria-hidden />
                Pickup scheduling
              </Link>
              <Link
                to="/inventory"
                className="flex items-center gap-2.5 rounded-lg border border-edge px-3 py-2.5 text-[12px] text-ink-secondary transition-colors hover:border-cerulean/50 hover:text-ink-primary"
              >
                <Package className="h-4 w-4 text-cerulean-light" aria-hidden />
                Inventory tracking
              </Link>
              <Link
                to="/audit"
                className="flex items-center gap-2.5 rounded-lg border border-edge px-3 py-2.5 text-[12px] text-ink-secondary transition-colors hover:border-cerulean/50 hover:text-ink-primary"
              >
                <MapPin className="h-4 w-4 text-cerulean-light" aria-hidden />
                Audit history
              </Link>
              <Link
                to="/partner-jobs"
                className="flex items-center gap-2.5 rounded-lg border border-edge px-3 py-2.5 text-[12px] text-ink-secondary transition-colors hover:border-cerulean/50 hover:text-ink-primary"
              >
                <Coins className="h-4 w-4 text-cerulean-light" aria-hidden />
                Partner jobs
              </Link>
            </CardBody>
          </Card>
        </div>
      </div>

      <Modal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        title="Cancel this return"
        description="The reason is recorded on the return and in the audit trail. This cannot be undone."
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setCancelOpen(false)}>
              Keep return
            </Button>
            <Button
              variant="danger"
              loading={cancel.isPending}
              disabled={!cancelReason.trim()}
              onClick={() => cancel.mutate(cancelReason)}
            >
              Cancel return
            </Button>
          </>
        }
      >
        <Field label="Reason for cancellation" required>
          <Textarea
            rows={3}
            value={cancelReason}
            onChange={(event) => setCancelReason(event.target.value)}
            placeholder="e.g. Customer provided a replacement instead"
          />
        </Field>
      </Modal>
    </div>
  );
}
