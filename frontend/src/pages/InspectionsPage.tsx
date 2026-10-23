import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, CheckCircle2, ClipboardCheck, Play, ShieldAlert } from 'lucide-react';
import { inspectionApi, returnsApi } from '@/api/client';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatCurrency, formatDateTime } from '@/lib/format';
import type { Inspection, InspectionStatus, ProductCondition } from '@/lib/types';

const CONDITIONS: ProductCondition[] = [
  'NEW',
  'LIKE_NEW',
  'GOOD',
  'FAIR',
  'DAMAGED',
  'UNSELLABLE',
  'UNSAFE',
];

export function InspectionsPage() {
  const [searchParams] = useSearchParams();
  const returnId = searchParams.get('returnId') ?? undefined;
  const queryClient = useQueryClient();
  const toast = useToast();

  const [status, setStatus] = useState<InspectionStatus | ''>('');
  const [page, setPage] = useState(0);
  const [active, setActive] = useState<Inspection | null>(null);

  const query = useQuery({
    queryKey: ['inspections', status, returnId, page],
    queryFn: () =>
      inspectionApi.search({
        status: status || undefined,
        returnId,
        page,
        size: 15,
      }),
  });

  const openMutation = useMutation({
    mutationFn: (returnItemId: string) => inspectionApi.open(returnItemId),
    onSuccess: (inspection) => {
      toast.success('Inspection opened', `${inspection.sku} is ready for assessment.`);
      void queryClient.invalidateQueries({ queryKey: ['inspections'] });
    },
    onError: (error) =>
      toast.error('The inspection could not be opened', error instanceof ApiError ? error.message : undefined),
  });

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex flex-wrap items-center gap-2 border-b border-edge px-4 py-3">
          <Select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as InspectionStatus | '');
              setPage(0);
            }}
            className="w-52"
            aria-label="Filter by inspection status"
          >
            <option value="">All inspection states</option>
            {(
              [
                'AWAITING_INSPECTION',
                'IN_PROGRESS',
                'PASSED',
                'DAMAGED',
                'INCOMPLETE',
                'UNSAFE',
              ] as InspectionStatus[]
            ).map((entry) => (
              <option key={entry} value={entry}>
                {entry.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </Select>
          {returnId ? (
            <span className="rounded-lg border border-cerulean/30 bg-cerulean/10 px-2.5 py-1.5 text-[11px] text-cerulean-light">
              Filtered to a single return
            </span>
          ) : null}
        </div>

        {query.isError ? (
          <div className="p-4">
            <ErrorState
              title="Inspections could not be loaded"
              description={(query.error as Error)?.message}
              onRetry={() => void query.refetch()}
            />
          </div>
        ) : query.isLoading ? (
          <SkeletonTable rows={8} columns={6} />
        ) : query.data && query.data.content.length > 0 ? (
          <>
            <div className="overflow-x-auto">
              <table className="data-table rows-enter">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Product</th>
                    <th>Status</th>
                    <th>Functional</th>
                    <th>Accessories</th>
                    <th className="text-right">Repair cost</th>
                    <th>Inspected</th>
                    <th className="text-right">Action</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.content.map((inspection) => (
                    <tr key={inspection.id}>
                      <td className="font-mono text-[12px] text-cerulean-light">{inspection.sku}</td>
                      <td className="font-medium text-ink-primary">{inspection.productName}</td>
                      <td>
                        <StatusPill
                          label={inspection.statusLabel}
                          tone={toneForStatus(inspection.status)}
                          dot
                        />
                      </td>
                      <td>
                        {inspection.functional ? (
                          <CheckCircle2 className="h-4 w-4 text-ok" aria-label="Functional" />
                        ) : (
                          <AlertTriangle className="h-4 w-4 text-warn" aria-label="Not functional" />
                        )}
                      </td>
                      <td className="text-xs">
                        {inspection.accessoriesComplete ? (
                          <span className="text-ok">Complete</span>
                        ) : (
                          <span className="text-danger">Missing</span>
                        )}
                      </td>
                      <td className="text-right tabular-nums">
                        {inspection.estimatedRepairCost !== null
                          ? formatCurrency(inspection.estimatedRepairCost)
                          : '—'}
                      </td>
                      <td className="whitespace-nowrap text-ink-muted">
                        {inspection.inspectedAt ? formatDateTime(inspection.inspectedAt) : '—'}
                      </td>
                      <td className="text-right">
                        {inspection.status === 'AWAITING_INSPECTION' ? (
                          <Button
                            size="sm"
                            variant="secondary"
                            onClick={() => openMutation.mutate(inspection.returnItemId)}
                            loading={openMutation.isPending}
                            icon={<Play className="h-3.5 w-3.5" />}
                          >
                            Start
                          </Button>
                        ) : (
                          <Button size="sm" variant="secondary" onClick={() => setActive(inspection)}>
                            Review
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
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
            icon={<ClipboardCheck className="h-5 w-5" />}
            title="No inspections in the queue"
            description="Open an inspection from a return item once the goods have been received at the warehouse."
          />
        )}
      </Card>

      <StartInspectionPanel
        onOpen={(itemId) => openMutation.mutate(itemId)}
        pending={openMutation.isPending}
      />

      {active ? (
        <InspectionWorkspace inspection={active} onClose={() => setActive(null)} />
      ) : null}
    </div>
  );
}

function StartInspectionPanel({
  onOpen,
  pending,
}: {
  onOpen: (returnItemId: string) => void;
  pending: boolean;
}) {
  const [returnId, setReturnId] = useState('');

  const returns = useQuery({
    queryKey: ['returns', 'inspection-picker'],
    queryFn: () => returnsApi.search({ size: 50, sort: 'createdAt,desc' }),
  });

  const detail = useQuery({
    queryKey: ['return', returnId],
    queryFn: () => returnsApi.get(returnId),
    enabled: Boolean(returnId),
  });

  return (
    <Card>
      <div className="border-b border-edge px-5 py-4">
        <h2 className="text-sm font-semibold tracking-tight text-ink-primary">Open an inspection</h2>
        <p className="mt-0.5 text-xs text-ink-muted">
          Select a return, then open an inspection for one of its items.
        </p>
      </div>
      <div className="space-y-3 px-5 py-4">
        <Field label="Return">
          <Select value={returnId} onChange={(event) => setReturnId(event.target.value)}>
            <option value="">Select a return…</option>
            {returns.data?.content.map((item) => (
              <option key={item.id} value={item.id}>
                {item.returnNumber} — {item.statusLabel} ({item.itemCount} item(s))
              </option>
            ))}
          </Select>
        </Field>

        {detail.data ? (
          <div className="space-y-2">
            {detail.data.items.map((item) => (
              <div
                key={item.id}
                className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-edge bg-surface-base px-3 py-2.5"
              >
                <div className="min-w-0">
                  <p className="text-[12px] font-medium text-ink-primary">{item.productName}</p>
                  <p className="mt-0.5 font-mono text-[11px] text-ink-muted">
                    {item.sku} · {item.conditionLabel}
                  </p>
                </div>
                <Button
                  size="sm"
                  variant="secondary"
                  disabled={pending}
                  onClick={() => onOpen(item.id)}
                >
                  Open inspection
                </Button>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </Card>
  );
}

function InspectionWorkspace({
  inspection,
  onClose,
}: {
  inspection: Inspection;
  onClose: () => void;
}) {
  const queryClient = useQueryClient();
  const toast = useToast();

  const [packaging, setPackaging] = useState<ProductCondition | ''>('');
  const [functionality, setFunctionality] = useState<ProductCondition | ''>('');
  const [functional, setFunctional] = useState(false);
  const [accessories, setAccessories] = useState(true);
  const [damage, setDamage] = useState('');
  const [missing, setMissing] = useState('');
  const [repairCost, setRepairCost] = useState('');
  const [notes, setNotes] = useState('');
  const [unsafe, setUnsafe] = useState(false);

  const record = useMutation({
    mutationFn: () =>
      inspectionApi.record(inspection.id, {
        packagingCondition: packaging || undefined,
        functionalityResult: functionality || undefined,
        functional,
        accessoriesComplete: accessories,
        damageDescription: damage || undefined,
        missingAccessories: missing || undefined,
        estimatedRepairCost: repairCost ? Number(repairCost) : undefined,
        notes: notes || undefined,
        flaggedUnsafe: unsafe,
      }),
    onSuccess: () => {
      toast.success('Findings recorded');
      void queryClient.invalidateQueries({ queryKey: ['inspections'] });
    },
    onError: (error) =>
      toast.error('The findings could not be saved', error instanceof ApiError ? error.message : undefined),
  });

  const complete = useMutation({
    mutationFn: () => inspectionApi.complete(inspection.id),
    onSuccess: (updated) => {
      toast.success('Inspection completed', `${updated.sku} recorded as ${updated.statusLabel}.`);
      void queryClient.invalidateQueries({ queryKey: ['inspections'] });
      void queryClient.invalidateQueries({ queryKey: ['return'] });
      onClose();
    },
    onError: (error) =>
      toast.error('The inspection could not be completed', error instanceof ApiError ? error.message : undefined),
  });

  const locked = inspection.status !== 'AWAITING_INSPECTION' && inspection.status !== 'IN_PROGRESS';

  return (
    <Modal
      open
      onClose={onClose}
      title={`Inspect ${inspection.sku}`}
      description={inspection.productName}
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
          {!locked ? (
            <>
              <Button
                variant="secondary"
                loading={record.isPending}
                onClick={() => record.mutate()}
                disabled={!packaging && !functionality && !notes}
              >
                Save findings
              </Button>
              <Button
                loading={complete.isPending}
                onClick={() => complete.mutate()}
                disabled={!packaging || !functionality}
              >
                Complete inspection
              </Button>
            </>
          ) : null}
        </>
      }
    >
      {locked ? (
        <div className="mb-4 flex items-start gap-2 rounded-lg border border-warn/30 bg-warn/[0.07] px-3 py-2.5">
          <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-warn" aria-hidden />
          <p className="text-xs leading-relaxed text-ink-secondary">
            This inspection is complete and locked. Completed inspections cannot be edited, which keeps
            the record of what the warehouse actually saw.
          </p>
        </div>
      ) : null}

      <div className="space-y-4">
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Packaging condition" required>
            <Select
              value={packaging}
              onChange={(event) => setPackaging(event.target.value as ProductCondition)}
              disabled={locked}
            >
              <option value="">Select…</option>
              {CONDITIONS.map((condition) => (
                <option key={condition} value={condition}>
                  {condition.replace(/_/g, ' ').toLowerCase()}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Functionality result" required>
            <Select
              value={functionality}
              onChange={(event) => setFunctionality(event.target.value as ProductCondition)}
              disabled={locked}
            >
              <option value="">Select…</option>
              {CONDITIONS.map((condition) => (
                <option key={condition} value={condition}>
                  {condition.replace(/_/g, ' ').toLowerCase()}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="flex flex-wrap items-center gap-4">
          <label className="inline-flex cursor-pointer items-center gap-2 text-xs text-ink-secondary">
            <input
              type="checkbox"
              checked={functional}
              onChange={(event) => setFunctional(event.target.checked)}
              disabled={locked}
              className="h-4 w-4 rounded border-edge bg-surface-elevated text-cerulean focus:ring-cerulean focus:ring-offset-jet"
            />
            Passes the functionality test
          </label>
          <label className="inline-flex cursor-pointer items-center gap-2 text-xs text-ink-secondary">
            <input
              type="checkbox"
              checked={accessories}
              onChange={(event) => setAccessories(event.target.checked)}
              disabled={locked}
              className="h-4 w-4 rounded border-edge bg-surface-elevated text-cerulean focus:ring-cerulean focus:ring-offset-jet"
            />
            All accessories present
          </label>
          <label className="inline-flex cursor-pointer items-center gap-2 text-xs text-danger">
            <input
              type="checkbox"
              checked={unsafe}
              onChange={(event) => setUnsafe(event.target.checked)}
              disabled={locked}
              className="h-4 w-4 rounded border-edge bg-surface-elevated text-danger focus:ring-rose focus:ring-offset-jet"
            />
            Flag as unsafe
          </label>
        </div>

        <Field label="Damage description">
          <Textarea
            rows={2}
            value={damage}
            onChange={(event) => setDamage(event.target.value)}
            disabled={locked}
            placeholder="e.g. Cracked rear housing, display scuffed"
          />
        </Field>

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Missing accessories">
            <Input
              value={missing}
              onChange={(event) => setMissing(event.target.value)}
              disabled={locked}
              placeholder="e.g. Charger, case"
            />
          </Field>
          <Field label="Estimated repair cost">
            <Input
              type="number"
              min={0}
              step="0.01"
              value={repairCost}
              onChange={(event) => setRepairCost(event.target.value)}
              disabled={locked}
              placeholder="0.00"
            />
          </Field>
        </div>

        <Field label="Inspector notes">
          <Textarea
            rows={2}
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
            disabled={locked}
            placeholder="Anything else worth recording…"
          />
        </Field>
      </div>
    </Modal>
  );
}
