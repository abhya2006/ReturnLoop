import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Download, Filter, Package, Plus, Search, X } from 'lucide-react';
import { catalogApi, returnsApi, type CreateReturnInput } from '@/api/client';
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
import { cn } from '@/lib/cn';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import type { ReturnReason, ReturnStatus } from '@/lib/types';

const REASONS: { value: ReturnReason; label: string }[] = [
  { value: 'DEFECTIVE', label: 'Defective or faulty' },
  { value: 'DAMAGED_IN_TRANSIT', label: 'Damaged in transit' },
  { value: 'WRONG_ITEM', label: 'Wrong item delivered' },
  { value: 'NOT_AS_DESCRIBED', label: 'Not as described' },
  { value: 'CHANGED_MIND', label: 'Customer changed mind' },
  { value: 'OVERPURCHASE', label: 'Over-purchase / unneeded' },
  { value: 'DUPLICATE_ORDER', label: 'Duplicate order' },
  { value: 'NO_LONGER_NEEDED', label: 'No longer needed' },
  { value: 'RECALL', label: 'Recall or safety recall' },
  { value: 'SURPLUS_UNSOLD', label: 'Unsold surplus / inventory' },
  { value: 'END_OF_LIFE', label: 'End of life / obsolete' },
  { value: 'OTHER', label: 'Other' },
];

const STATUSES: ReturnStatus[] = [
  'RETURN_CREATED',
  'PICKUP_SCHEDULED',
  'COLLECTED',
  'WAREHOUSE_RECEIVED',
  'INSPECTED',
  'RECOVERY_DECISION',
  'PARTNER_ASSIGNED',
  'PROCESSING',
  'RECOVERED',
  'COMPLETED',
  'CANCELLED',
];

const itemSchema = z.object({
  sku: z.string().min(1, 'SKU is required'),
  quantity: z.coerce.number().int().min(1, 'Quantity must be at least 1'),
  condition: z.string().min(1),
});

const formSchema = z.object({
  reason: z.string().min(1, 'A reason is required'),
  customerName: z.string().optional(),
  sourceReference: z.string().optional(),
  originAddress: z.string().optional(),
  notes: z.string().optional(),
  items: z.array(itemSchema).min(1, 'Add at least one item'),
});

type FormValues = z.infer<typeof formSchema>;

export function ReturnsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const toast = useToast();

  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ReturnStatus[]>([]);
  const [reason, setReason] = useState<ReturnReason | ''>('');
  const [page, setPage] = useState(0);
  const [showCreate, setShowCreate] = useState(searchParams.get('new') === '1');
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    if (searchParams.get('new') === '1') {
      setShowCreate(true);
      searchParams.delete('new');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const query = useQuery({
    queryKey: ['returns', search, status, reason, page],
    queryFn: () =>
      returnsApi.search({
        search,
        status: status.length ? status : undefined,
        reason: reason || undefined,
        page,
        size: 15,
      }),
  });

  const createMutation = useMutation({
    mutationFn: (input: CreateReturnInput) =>
      returnsApi.create(input, `return-${crypto.randomUUID()}`),
    onSuccess: (created) => {
      toast.success(
        `Return ${created.returnNumber} created`,
        `${created.itemCount} item(s) added and queued for pickup.`,
      );
      void queryClient.invalidateQueries({ queryKey: ['returns'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      setShowCreate(false);
      setPage(0);
    },
    onError: (error) => {
      toast.error(
        'The return could not be created',
        error instanceof ApiError ? error.message : 'Unexpected error',
      );
    },
  });

  const hasFilters = search.length > 0 || status.length > 0 || reason.length > 0;

  const exportCsv = () => {
    const rows = query.data?.content ?? [];
    if (rows.length === 0) return;
    const header = ['Return', 'Status', 'Reason', 'Items', 'Estimated value', 'Realized value', 'Created'];
    const lines = rows.map((row) =>
      [
        row.returnNumber,
        row.status,
        row.reason,
        row.itemCount,
        row.estimatedRecoveryValue ?? 0,
        row.realizedRecoveryValue ?? 0,
        row.createdAt,
      ].join(','),
    );
    const blob = new Blob([[header.join(','), ...lines].join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `returnloop-returns-${new Date().toISOString().slice(0, 10)}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

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
              placeholder="Search return number, customer or reference…"
              className="pl-9"
              aria-label="Search returns"
            />
          </div>

          <Button
            variant={showFilters || hasFilters ? 'primary' : 'secondary'}
            size="md"
            onClick={() => setShowFilters((current) => !current)}
            icon={<Filter className="h-4 w-4" />}
          >
            Filters
            {hasFilters ? (
              <span className="ml-1 rounded-full bg-rose px-1.5 text-[10px] font-bold text-white">
                {status.length + (reason ? 1 : 0) + (search ? 1 : 0)}
              </span>
            ) : null}
          </Button>

          <Button variant="secondary" size="md" onClick={exportCsv} icon={<Download className="h-4 w-4" />}>
            Export
          </Button>

          <Button size="md" onClick={() => setShowCreate(true)} icon={<Plus className="h-4 w-4" />}>
            Create return
          </Button>
        </div>

        {showFilters ? (
          <div className="flex flex-wrap items-end gap-3 border-b border-edge bg-surface-elevated/40 px-4 py-3">
            <Field label="Reason" className="w-56">
              <Select
                value={reason}
                onChange={(event) => {
                  setReason(event.target.value as ReturnReason | '');
                  setPage(0);
                }}
              >
                <option value="">All reasons</option>
                {REASONS.map((entry) => (
                  <option key={entry.value} value={entry.value}>
                    {entry.label}
                  </option>
                ))}
              </Select>
            </Field>

            <Field label="Status" className="min-w-[280px] flex-1">
              <div className="flex flex-wrap gap-1.5">
                {STATUSES.map((entry) => {
                  const active = status.includes(entry);
                  return (
                    <button
                      key={entry}
                      type="button"
                      onClick={() => {
                        setStatus((current) =>
                          current.includes(entry)
                            ? current.filter((value) => value !== entry)
                            : [...current, entry],
                        );
                        setPage(0);
                      }}
                      className={cn(
                        'rounded-full border px-2.5 py-1 text-[11px] font-medium transition-colors focus-visible:ring-2 focus-visible:ring-cerulean',
                        active
                          ? 'border-cerulean bg-cerulean/15 text-cerulean-light'
                          : 'border-edge text-ink-muted hover:border-cerulean/50 hover:text-ink-secondary',
                      )}
                    >
                      {entry.replace(/_/g, ' ').toLowerCase()}
                    </button>
                  );
                })}
              </div>
            </Field>

            {hasFilters ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setSearch('');
                  setStatus([]);
                  setReason('');
                  setPage(0);
                }}
                icon={<X className="h-3.5 w-3.5" />}
              >
                Clear
              </Button>
            ) : null}
          </div>
        ) : null}

        {query.isError ? (
          <div className="p-4">
            <ErrorState
              title="Returns could not be loaded"
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
                    <th>Return</th>
                    <th>Status</th>
                    <th>Reason</th>
                    <th>Items</th>
                    <th className="text-right">Est. recovery</th>
                    <th className="text-right">Realised</th>
                    <th>Created</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.content.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <Link
                          to={`/returns/${row.id}`}
                          className="font-medium text-cerulean-light transition-colors hover:text-cerulean hover:underline"
                        >
                          {row.returnNumber}
                        </Link>
                        {row.sourceReference ? (
                          <span className="ml-2 text-[11px] text-ink-muted">{row.sourceReference}</span>
                        ) : null}
                      </td>
                      <td>
                        <StatusPill label={row.statusLabel} tone={toneForStatus(row.status)} dot />
                      </td>
                      <td className="text-ink-muted">{row.reason.replace(/_/g, ' ').toLowerCase()}</td>
                      <td className="tabular-nums">{formatNumber(row.itemCount)}</td>
                      <td className="text-right tabular-nums">
                        {formatCurrency(row.estimatedRecoveryValue ?? 0)}
                      </td>
                      <td className="text-right tabular-nums">
                        {row.realizedRecoveryValue !== null
                          ? formatCurrency(row.realizedRecoveryValue)
                          : '—'}
                      </td>
                      <td className="whitespace-nowrap text-ink-muted">{formatDate(row.createdAt)}</td>
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
            icon={<Package className="h-5 w-5" />}
            title={hasFilters ? 'No returns match these filters' : 'No returns yet'}
            description={
              hasFilters
                ? 'Try widening the status or reason filters, or clear them to see everything.'
                : 'Create your first return to start tracking recovered value.'
            }
            action={
              hasFilters ? (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setSearch('');
                    setStatus([]);
                    setReason('');
                  }}
                >
                  Clear filters
                </Button>
              ) : (
                <Button size="sm" onClick={() => setShowCreate(true)}>
                  Create return
                </Button>
              )
            }
          />
        )}
      </Card>

      <CreateReturnModal
        open={showCreate}
        onClose={() => setShowCreate(false)}
        onSubmit={(values) => createMutation.mutate(values as unknown as CreateReturnInput)}
        submitting={createMutation.isPending}
        error={createMutation.error instanceof ApiError ? createMutation.error.message : null}
      />
    </div>
  );
}

function CreateReturnModal({
  open,
  onClose,
  onSubmit,
  submitting,
  error,
}: {
  open: boolean;
  onClose: () => void;
  onSubmit: (values: CreateReturnInput) => void;
  submitting: boolean;
  error: string | null;
}) {
  const { data: products } = useQuery({
    queryKey: ['products', 'picker'],
    queryFn: () => catalogApi.search({ size: 200 }),
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      reason: 'DEFECTIVE',
      customerName: '',
      sourceReference: '',
      originAddress: '',
      notes: '',
      items: [{ sku: '', quantity: 1, condition: 'GOOD' }],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'items' });

  useEffect(() => {
    if (open) {
      reset({
        reason: 'DEFECTIVE',
        customerName: '',
        sourceReference: '',
        originAddress: '',
        notes: '',
        items: [{ sku: '', quantity: 1, condition: 'GOOD' }],
      });
    }
  }, [open, reset]);

  const skuOptions = useMemo(
    () => (products?.content ?? []).map((product) => product.sku),
    [products],
  );

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Create a return"
      description="Register a return with one or more items. The SKU must exist in the product catalogue."
      size="lg"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button form="create-return-form" type="submit" loading={submitting} disabled={!skuOptions.length}>
            Create return
          </Button>
        </>
      }
    >
      {products && products.content.length === 0 ? (
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-warn/30 bg-warn/10 p-3 text-xs text-ink-secondary">
          <span>This workspace has no product SKUs yet. Add one before creating a return.</span>
          <Link to="/inventory" onClick={onClose} className="font-semibold text-cerulean-light hover:text-cerulean">Open inventory</Link>
        </div>
      ) : null}
      <form
        id="create-return-form"
        onSubmit={handleSubmit((values) => onSubmit(values as unknown as CreateReturnInput))}
        className="space-y-4"
        noValidate
      >
        {error ? (
          <div
            role="alert"
            className="rounded-lg border border-danger/40 bg-danger/[0.08] px-3 py-2.5 text-xs text-danger"
          >
            {error}
          </div>
        ) : null}

        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <Field label="Return reason" error={errors.reason?.message} required>
            <Select {...register('reason')}>
              {REASONS.map((entry) => (
                <option key={entry.value} value={entry.value}>
                  {entry.label}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Customer name">
            <Input placeholder="e.g. Jordan Alvarez" {...register('customerName')} />
          </Field>
          <Field label="Source reference" hint="Order number or external reference">
            <Input placeholder="ORD-88213" {...register('sourceReference')} />
          </Field>
          <Field label="Origin address">
            <Input placeholder="Street, city, state" {...register('originAddress')} />
          </Field>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <p className="text-xs font-medium text-ink-secondary">Items</p>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => append({ sku: '', quantity: 1, condition: 'GOOD' })}
              icon={<Plus className="h-3.5 w-3.5" />}
            >
              Add item
            </Button>
          </div>

          <div className="space-y-2">
            {fields.map((field, index) => (
              <div
                key={field.id}
                className="grid grid-cols-1 items-end gap-2 rounded-lg border border-edge bg-surface-base p-3 sm:grid-cols-[2fr_100px_1fr_auto]"
              >
                <Field label="SKU" error={errors.items?.[index]?.sku?.message} required>
                  <Input
                    list="return-skus"
                    placeholder="NWR-PH-0142"
                    {...register(`items.${index}.sku`)}
                  />
                </Field>
                <Field label="Qty" error={errors.items?.[index]?.quantity?.message}>
                  <Input type="number" min={1} {...register(`items.${index}.quantity`)} />
                </Field>
                <Field label="Condition">
                  <Select {...register(`items.${index}.condition`)}>
                    {['NEW', 'LIKE_NEW', 'GOOD', 'FAIR', 'DAMAGED', 'UNSELLABLE', 'UNSAFE'].map(
                      (condition) => (
                        <option key={condition} value={condition}>
                          {condition.replace(/_/g, ' ').toLowerCase()}
                        </option>
                      ),
                    )}
                  </Select>
                </Field>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => remove(index)}
                  disabled={fields.length === 1}
                  aria-label="Remove item"
                  icon={<X className="h-3.5 w-3.5" />}
                />
              </div>
            ))}
          </div>
          <datalist id="return-skus">
            {skuOptions.map((sku) => (
              <option key={sku} value={sku} />
            ))}
          </datalist>
          {errors.items?.message ? (
            <p className="mt-1.5 text-[11px] text-danger">{errors.items.message}</p>
          ) : null}
        </div>

        <Field label="Notes">
          <Textarea rows={2} placeholder="Anything the warehouse should know…" {...register('notes')} />
        </Field>
      </form>
    </Modal>
  );
}
