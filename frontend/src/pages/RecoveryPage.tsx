import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, Info, Scale, Sparkles, ThumbsDown, ThumbsUp, X } from 'lucide-react';
import { recoveryApi, returnsApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Field, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/Skeleton';
import { StatusPill } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { cn } from '@/lib/cn';
import { formatCurrency, formatPercent } from '@/lib/format';
import type { RecoveryOption } from '@/lib/types';

export function RecoveryPage() {
  const { returnId: routeReturnId } = useParams();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();

  const [selectedId, setSelectedId] = useState<string>(routeReturnId ?? '');
  const [decision, setDecision] = useState<{ id: string; approve: boolean } | null>(null);
  const [reason, setReason] = useState('');

  const returns = useQuery({
    queryKey: ['returns', 'recovery-picker'],
    queryFn: () => returnsApi.search({ size: 50, sort: 'createdAt,desc' }),
  });

  useEffect(() => {
    if (routeReturnId) {
      setSelectedId(routeReturnId);
    }
  }, [routeReturnId]);

  useEffect(() => {
    if (!selectedId && returns.data?.content.length) {
      setSelectedId(returns.data.content[0].id);
    }
  }, [returns.data, selectedId]);

  const evaluation = useQuery({
    queryKey: ['recovery', selectedId],
    queryFn: () => recoveryApi.preview(selectedId),
    enabled: Boolean(selectedId),
  });

  const recommendations = useQuery({
    queryKey: ['recovery-recommendations', selectedId],
    queryFn: () => recoveryApi.recommendations(selectedId),
    enabled: Boolean(selectedId),
  });

  const recommend = useMutation({
    mutationFn: () => recoveryApi.recommend(selectedId),
    onSuccess: (result) => {
      toast.success(
        'Recovery options calculated',
        `${result.items.length} item(s) evaluated with the ${result.strategy} strategy.`,
      );
      void queryClient.invalidateQueries({ queryKey: ['recovery', selectedId] });
      void queryClient.invalidateQueries({ queryKey: ['recovery-recommendations', selectedId] });
    },
    onError: (error) =>
      toast.error('The options could not be calculated', error instanceof ApiError ? error.message : undefined),
  });

  const decide = useMutation({
    mutationFn: () => recoveryApi.decide(decision!.id, decision!.approve, reason),
    onSuccess: () => {
      toast.success(
        decision?.approve ? 'Recommendation approved' : 'Recommendation rejected',
        'The decision and its reason were recorded.',
      );
      setDecision(null);
      setReason('');
      void queryClient.invalidateQueries({ queryKey: ['recovery-recommendations', selectedId] });
    },
    onError: (error) =>
      toast.error('The decision could not be recorded', error instanceof ApiError ? error.message : undefined),
  });

  const pendingRecommendation = recommendations.data?.find((item) => !item.approved && !item.rejected);

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-end gap-3 px-4 py-3">
          <Field label="Return" className="min-w-[280px] flex-1">
            <Select
              value={selectedId}
              onChange={(event) => {
                setSelectedId(event.target.value);
                navigate(`/recovery/${event.target.value}`, { replace: true });
              }}
            >
              <option value="">Select a return…</option>
              {returns.data?.content.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.returnNumber} — {item.statusLabel}
                </option>
              ))}
            </Select>
          </Field>
          <Button
            onClick={() => recommend.mutate()}
            disabled={!selectedId}
            loading={recommend.isPending}
            icon={<Sparkles className="h-4 w-4" />}
          >
            Generate recommendation
          </Button>
        </div>
      </Card>

      {selectedId && recommendations.data && pendingRecommendation ? (
        <Card className="reveal border-rose/30">
          <CardBody className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-[13px] font-semibold text-ink-primary">
                A recommendation is waiting for your decision
              </p>
              <p className="mt-0.5 text-[11px] text-ink-muted">
                {pendingRecommendation.sku} → {pendingRecommendation.recommendedPathLabel}, net value{' '}
                {formatCurrency(pendingRecommendation.recommendedNetValue ?? 0)}
              </p>
            </div>
            <div className="flex gap-2">
              <Button
                variant="secondary"
                size="sm"
                icon={<ThumbsDown className="h-3.5 w-3.5" />}
                onClick={() => setDecision({ id: pendingRecommendation.id, approve: false })}
              >
                Reject
              </Button>
              <Button
                size="sm"
                icon={<ThumbsUp className="h-3.5 w-3.5" />}
                onClick={() => setDecision({ id: pendingRecommendation.id, approve: true })}
              >
                Approve
              </Button>
            </div>
          </CardBody>
        </Card>
      ) : null}

      {!selectedId ? (
        <Card>
          <EmptyState
            icon={<Sparkles className="h-5 w-5" />}
            title="Select a return to compare recovery paths"
            description="The engine evaluates eight recovery destinations against the inspected condition and shows the full economics of each."
          />
        </Card>
      ) : evaluation.isError ? (
        <ErrorState
          title="The recovery options could not be loaded"
          description={(evaluation.error as Error)?.message}
          onRetry={() => void evaluation.refetch()}
        />
      ) : evaluation.isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-28 w-full" />
          <Skeleton className="h-56 w-full" />
        </div>
      ) : evaluation.data && evaluation.data.items.length > 0 ? (
        evaluation.data.items.map((item) => (
          <Card key={item.returnItemId}>
            <CardHeader
              title={`${item.sku} — ${item.productName}`}
              description={`${item.quantity} unit(s) · assessed condition ${item.condition.replace(/_/g, ' ').toLowerCase()}`}
              action={
                item.recommended ? (
                  <StatusPill
                    label={`Recommended: ${item.recommended.label}`}
                    tone="accent"
                    dot
                  />
                ) : null
              }
            />
            <CardBody>
              <div className="overflow-x-auto">
                <table className="data-table rows-enter">
                  <thead>
                    <tr>
                      <th>Recovery path</th>
                      <th className="text-right">Gross value</th>
                      <th className="text-right">Work</th>
                      <th className="text-right">Transport</th>
                      <th className="text-right">Partner fees</th>
                      <th className="text-right">Net value</th>
                      <th className="text-right">Turnaround</th>
                      <th className="text-right">Data quality</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {item.options.map((option) => (
                      <OptionRow key={option.path} option={option} recommended={item.recommended?.path === option.path} />
                    ))}
                  </tbody>
                </table>
              </div>

              {item.recommended ? (
                <div className="mt-4 rounded-lg border border-cerulean/25 bg-cerulean/[0.05] p-3.5">
                  <div className="flex items-start gap-2">
                    <Scale className="mt-0.5 h-4 w-4 shrink-0 text-cerulean-light" aria-hidden />
                    <div className="min-w-0">
                      <p className="text-[12px] font-semibold text-ink-primary">
                        Why {item.recommended.label} is recommended
                      </p>
                      <p className="mt-1 text-[11px] leading-relaxed text-ink-secondary">
                        {item.recommended.explanation}
                      </p>
                      <p className="mt-2 text-[11px] text-ink-muted">
                        Net recovery value{' '}
                        <span className="font-semibold text-ink-primary">
                          {formatCurrency(item.recommended.netRecoveryValue)}
                        </span>{' '}
                        after {formatCurrency(item.recommended.processingCost)} of work,{' '}
                        {formatCurrency(item.recommended.transportationCost)} of transport,{' '}
                        {formatCurrency(item.recommended.partnerFees)} of partner fees and{' '}
                        {formatCurrency(item.recommended.inspectionCost)} of inspection.
                      </p>
                      {item.recommended.carbonAvoidedKg > 0 ? (
                        <p className="mt-1.5 text-[11px] text-ink-muted">
                          Estimated environmental benefit:{' '}
                          {item.recommended.wasteDivertedKg.toFixed(2)} kg diverted,{' '}
                          {item.recommended.carbonAvoidedKg.toFixed(2)} kg CO₂e avoided — an estimate
                          based on documented weight and emissions factors.
                        </p>
                      ) : null}
                    </div>
                  </div>
                </div>
              ) : null}

              {item.options.some((option) => !option.eligible) ? (
                <details className="mt-3 rounded-lg border border-edge bg-surface-base px-3.5 py-2.5">
                  <summary className="cursor-pointer text-[12px] font-medium text-ink-secondary">
                    Why some options were ruled out
                  </summary>
                  <ul className="mt-2 space-y-1.5">
                    {item.options
                      .filter((option) => !option.eligible)
                      .map((option) => (
                        <li key={option.path} className="text-[11px] leading-relaxed text-ink-muted">
                          <span className="font-medium text-ink-secondary">{option.label}:</span>{' '}
                          {option.restrictions.join('; ')}
                        </li>
                      ))}
                  </ul>
                </details>
              ) : null}
            </CardBody>
          </Card>
        ))
      ) : (
        <Card>
          <EmptyState
            icon={<Sparkles className="h-5 w-5" />}
            title="This return has no items to evaluate"
            description="Add items to the return before generating recovery options."
          />
        </Card>
      )}

      <Modal
        open={decision !== null}
        onClose={() => setDecision(null)}
        title={decision?.approve ? 'Approve this recommendation' : 'Reject this recommendation'}
        description="Your decision and its reason are stored on the recovery record."
        size="sm"
        footer={
          <>
            <Button variant="ghost" onClick={() => setDecision(null)}>
              Cancel
            </Button>
            <Button
              variant={decision?.approve ? 'primary' : 'danger'}
              loading={decide.isPending}
              disabled={!decision?.approve && !reason.trim()}
              onClick={() => decide.mutate()}
            >
              {decision?.approve ? 'Approve' : 'Reject'}
            </Button>
          </>
        }
      >
        <Field
          label="Reason"
          required={!decision?.approve}
          hint={decision?.approve ? 'Optional context for the audit trail' : undefined}
        >
          <Textarea
            rows={3}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            placeholder={
              decision?.approve
                ? 'e.g. Repair cost confirmed with the partner'
                : 'e.g. We prefer open-box resale to protect margin'
            }
          />
        </Field>
      </Modal>
    </div>
  );
}

function OptionRow({ option, recommended }: { option: RecoveryOption; recommended: boolean }) {
  return (
    <tr className={cn(recommended && 'bg-rose/[0.05]')}>
      <td>
        <span className="flex items-center gap-1.5 font-medium text-ink-primary">
          {recommended ? <Check className="h-3.5 w-3.5 text-rose" aria-hidden /> : null}
          {option.label}
        </span>
      </td>
      <td className="text-right tabular-nums">{formatCurrency(option.expectedResaleValue)}</td>
      <td className="text-right tabular-nums">{formatCurrency(option.processingCost)}</td>
      <td className="text-right tabular-nums">{formatCurrency(option.transportationCost)}</td>
      <td className="text-right tabular-nums">{formatCurrency(option.partnerFees)}</td>
      <td
        className={cn(
          'text-right font-semibold tabular-nums',
          option.netRecoveryValue > 0 ? 'text-ok' : 'text-danger',
        )}
      >
        {formatCurrency(option.netRecoveryValue)}
      </td>
      <td className="text-right tabular-nums text-ink-muted">{option.estimatedProcessingDays} d</td>
      <td className="text-right tabular-nums text-ink-muted">
        {formatPercent(option.dataQualityScore, 0)}
      </td>
      <td>
        {option.eligible ? (
          <StatusPill label="Eligible" tone="success" dot />
        ) : (
          <span
            title={option.restrictions.join('; ')}
            className="inline-flex items-center gap-1 text-[11px] text-ink-muted"
          >
            <X className="h-3 w-3 text-danger" aria-hidden />
            Ruled out
            <Info className="h-3 w-3" aria-hidden />
          </span>
        )}
      </td>
    </tr>
  );
}
