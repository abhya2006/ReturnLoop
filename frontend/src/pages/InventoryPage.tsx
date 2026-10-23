import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Boxes, Plus, Search, Timer } from 'lucide-react';
import { catalogApi, inventoryApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Button } from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatCurrency, formatDate } from '@/lib/format';
import type { InventoryStatus } from '@/lib/types';

const STATUSES: InventoryStatus[] = [
  'AVAILABLE',
  'RESERVED',
  'DAMAGED',
  'PROCESSING',
  'DISPATCHED',
  'COMPLETED',
  'QUARANTINED',
];

export function InventoryPage() {
  const [addProductOpen, setAddProductOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<InventoryStatus | ''>('');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [threshold, setThreshold] = useState(30);
  const queryClient = useQueryClient();
  const toast = useToast();
  const createProduct = useMutation({
    mutationFn: catalogApi.create,
    onSuccess: async (product) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['products', 'picker'] }),
        queryClient.invalidateQueries({ queryKey: ['search', 'products'] }),
      ]);
      toast.success('Product added', `${product.sku} is ready to use on returns.`);
      setAddProductOpen(false);
    },
    onError: (error) => toast.error('Could not add product', error instanceof ApiError ? error.message : 'Check the details and try again.'),
  });

  const submitProduct = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const numberOrNull = (key: string) => {
      const raw = String(form.get(key) ?? '').trim();
      return raw ? Number(raw) : null;
    };
    const sku = String(form.get('sku') ?? '').trim();
    const name = String(form.get('name') ?? '').trim();
    if (!sku || !name) {
      toast.error('SKU and product name are required');
      return;
    }
    createProduct.mutate({
      sku,
      name,
      brand: String(form.get('brand') ?? '').trim() || undefined,
      category: String(form.get('category') ?? 'OTHER'),
      originalPrice: numberOrNull('originalPrice') ?? undefined,
      resaleValue: numberOrNull('resaleValue') ?? undefined,
      weightKg: numberOrNull('weightKg') ?? undefined,
    });
  };

  const query = useQuery({
    queryKey: ['inventory', search, status, category, page],
    queryFn: () =>
      inventoryApi.search({
        search,
        status: status || undefined,
        category: category || undefined,
        page,
        size: 15,
      }),
  });

  const aging = useQuery({
    queryKey: ['inventory', 'aging', threshold],
    queryFn: () => inventoryApi.aging(threshold),
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
              placeholder="Search SKU or product name…"
              className="pl-9"
              aria-label="Search inventory"
            />
          </div>
          <Select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as InventoryStatus | '');
              setPage(0);
            }}
            className="w-44"
            aria-label="Filter by status"
          >
            <option value="">All statuses</option>
            {STATUSES.map((entry) => (
              <option key={entry} value={entry}>
                {entry.replace(/_/g, ' ').toLowerCase()}
              </option>
            ))}
          </Select>
          <Button size="sm" onClick={() => setAddProductOpen(true)} icon={<Plus className="h-3.5 w-3.5" />}>
            Add product
          </Button>
          <Select
            value={category}
            onChange={(event) => {
              setCategory(event.target.value);
              setPage(0);
            }}
            className="w-48"
            aria-label="Filter by category"
          >
            <option value="">All categories</option>
            {['ELECTRONICS', 'MOBILE_PHONES', 'LAPTOPS', 'APPLIANCES', 'FURNITURE', 'APPAREL', 'SPORTS', 'TOYS', 'BOOKS', 'GARDEN', 'AUTOMOTIVE', 'OTHER'].map(
              (entry) => (
                <option key={entry} value={entry}>
                  {entry.replace(/_/g, ' ').toLowerCase()}
                </option>
              ),
            )}
          </Select>
        </div>

        {query.isError ? (
          <div className="p-4">
            <ErrorState
              title="Inventory could not be loaded"
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
                    <th>SKU</th>
                    <th>Product</th>
                    <th>Category</th>
                    <th className="text-right">Qty</th>
                    <th className="text-right">Available</th>
                    <th>Status</th>
                    <th>Location</th>
                    <th className="text-right">Value</th>
                    <th>Received</th>
                  </tr>
                </thead>
                <tbody>
                  {query.data.content.map((item) => (
                    <tr key={item.id}>
                      <td className="font-mono text-[12px] text-cerulean-light">{item.sku}</td>
                      <td className="font-medium text-ink-primary">{item.productName}</td>
                      <td className="text-ink-muted">
                        {item.category?.replace(/_/g, ' ').toLowerCase() ?? '—'}
                      </td>
                      <td className="text-right tabular-nums">{item.quantity}</td>
                      <td className="text-right tabular-nums">
                        <span
                          className={
                            item.availableQuantity === 0 ? 'text-danger' : 'text-ink-secondary'
                          }
                        >
                          {item.availableQuantity}
                        </span>
                      </td>
                      <td>
                        <StatusPill label={item.statusLabel} tone={toneForStatus(item.status)} dot />
                      </td>
                      <td className="text-ink-muted">{item.locationCode ?? '—'}</td>
                      <td className="text-right tabular-nums">
                        {formatCurrency(item.estimatedValue ?? 0)}
                      </td>
                      <td className="whitespace-nowrap text-ink-muted">
                        {formatDate(item.receivedAt)}
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
            icon={<Boxes className="h-5 w-5" />}
            title="No inventory matches these filters"
            description="Create a product SKU, import unsold stock, or receive items from a return."
            action={<Button size="sm" onClick={() => setAddProductOpen(true)} icon={<Plus className="h-3.5 w-3.5" />}>Add your first product</Button>}
          />
        )}
      </Card>

      <Card className="reveal">
        <CardHeader
          title="Holding-time monitor"
          description="Stock that has been sitting beyond the configured threshold"
          action={
            <label className="flex items-center gap-2 text-[11px] text-ink-muted">
              Threshold (days)
              <Input
                type="number"
                min={1}
                value={threshold}
                onChange={(event) => {
                  setThreshold(Math.max(1, Number(event.target.value) || 1));
                }}
                className="w-20"
              />
            </label>
          }
        />
        <CardBody>
          {aging.isLoading ? (
            <SkeletonTable rows={4} columns={5} />
          ) : aging.data && aging.data.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="data-table rows-enter">
                <thead>
                  <tr>
                    <th>SKU</th>
                    <th>Product</th>
                    <th className="text-right">Units</th>
                    <th>Status</th>
                    <th className="text-right">Days held</th>
                    <th className="text-right">Value at risk</th>
                  </tr>
                </thead>
                <tbody>
                  {aging.data.map((item) => (
                    <tr key={item.id}>
                      <td className="font-mono text-[12px] text-cerulean-light">{item.sku}</td>
                      <td className="text-ink-primary">{item.productName}</td>
                      <td className="text-right tabular-nums">{item.quantity}</td>
                      <td>
                        <StatusPill label={item.status.replace(/_/g, ' ')} tone={toneForStatus(item.status)} />
                      </td>
                      <td className="text-right tabular-nums">
                        <span
                          className={
                            item.daysHeld > item.thresholdDays * 1.5
                              ? 'font-semibold text-danger'
                              : item.daysHeld > item.thresholdDays
                                ? 'text-warn'
                                : 'text-ink-secondary'
                          }
                        >
                          {item.daysHeld}
                        </span>
                      </td>
                      <td className="text-right tabular-nums">
                        {formatCurrency(item.estimatedValue ?? 0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              icon={<Timer className="h-5 w-5" />}
              title="Nothing is ageing out"
              description={`No stock has been held for more than ${threshold} days.`}
            />
          )}
        </CardBody>
      </Card>

      <Modal
        open={addProductOpen}
        onClose={() => setAddProductOpen(false)}
        title="Add a product to your catalogue"
        description="Create a SKU first so your team can register returns and recovery values."
        footer={<><Button variant="ghost" onClick={() => setAddProductOpen(false)}>Cancel</Button><Button form="add-product-form" type="submit" loading={createProduct.isPending}>Save product</Button></>}
      >
        <form id="add-product-form" onSubmit={submitProduct} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="SKU" required><Input name="sku" autoFocus maxLength={64} placeholder="SKU-10001" required /></Field>
          <Field label="Product name" required><Input name="name" maxLength={240} placeholder="Wireless headphones" required /></Field>
          <Field label="Category"><Select name="category" defaultValue="OTHER">{['ELECTRONICS','MOBILE_PHONES','LAPTOPS','APPLIANCES','FURNITURE','APPAREL','SPORTS','TOYS','BOOKS','GARDEN','AUTOMOTIVE','OTHER'].map((value) => <option key={value} value={value}>{value.replace(/_/g, ' ').toLowerCase()}</option>)}</Select></Field>
          <Field label="Brand"><Input name="brand" maxLength={120} placeholder="Optional" /></Field>
          <Field label="Original price"><Input name="originalPrice" type="number" min="0" step="0.01" placeholder="0.00" /></Field>
          <Field label="Expected resale value"><Input name="resaleValue" type="number" min="0" step="0.01" placeholder="Uses original price if blank" /></Field>
          <Field label="Weight (kg)"><Input name="weightKg" type="number" min="0" step="0.001" placeholder="Optional" /></Field>
        </form>
      </Modal>
    </div>
  );
}
