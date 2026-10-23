import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, Save } from 'lucide-react';
import { organizationApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { ErrorState } from '@/components/ui/EmptyState';
import { Field, Input } from '@/components/ui/Form';
import { Skeleton } from '@/components/ui/Skeleton';
import { StatusPill } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';

export function OrganizationsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [form, setForm] = useState({
    name: '',
    contactEmail: '',
    country: '',
    holdingThresholdDays: 30,
  });

  const query = useQuery({
    queryKey: ['organization'],
    queryFn: () => organizationApi.current(),
  });

  useEffect(() => {
    if (query.data) {
      setForm({
        name: query.data.name,
        contactEmail: query.data.contactEmail ?? '',
        country: query.data.country ?? '',
        holdingThresholdDays: query.data.holdingThresholdDays,
      });
    }
  }, [query.data]);

  const save = useMutation({
    mutationFn: () => organizationApi.update(form),
    onSuccess: () => {
      toast.success('Organisation updated');
      void queryClient.invalidateQueries({ queryKey: ['organization'] });
    },
    onError: (error) =>
      toast.error('The organisation could not be updated', error instanceof ApiError ? error.message : undefined),
  });

  const update = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((current) => ({ ...current, [key]: value }));

  if (query.isError) {
    return (
      <ErrorState
        title="The organisation could not be loaded"
        description={(query.error as Error)?.message}
        onRetry={() => void query.refetch()}
      />
    );
  }

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="reveal">
        <CardHeader title="Organisation" description="Tenant-level settings for this workspace" />
        <CardBody className="space-y-3">
          {query.isLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : (
            <>
              <Field label="Name" required>
                <Input value={form.name} onChange={(event) => update('name', event.target.value)} />
              </Field>
              <Field label="Operations contact email">
                <Input
                  type="email"
                  value={form.contactEmail}
                  onChange={(event) => update('contactEmail', event.target.value)}
                />
              </Field>
              <Field label="Country">
                <Input value={form.country} onChange={(event) => update('country', event.target.value)} />
              </Field>
              <Field
                label="Holding threshold (days)"
                hint="Stock held longer than this is flagged in the inventory monitor"
              >
                <Input
                  type="number"
                  min={1}
                  value={form.holdingThresholdDays}
                  onChange={(event) => update('holdingThresholdDays', Number(event.target.value) || 1)}
                />
              </Field>
              <Button loading={save.isPending} onClick={() => save.mutate()} icon={<Save className="h-4 w-4" />}>
                Save changes
              </Button>
            </>
          )}
        </CardBody>
      </Card>

      <Card className="reveal">
        <CardHeader title="Tenant isolation" description="How data separation is enforced" />
        <CardBody className="space-y-3 text-xs leading-relaxed text-ink-secondary">
          <p>
            Every operational record carries an <code className="font-mono text-ink-primary">organization_id</code>.
            Reads and writes are scoped to the organisation of the authenticated user, and a request that
            reaches for a record belonging to another tenant is rejected with{' '}
            <code className="font-mono text-ink-primary">CROSS_TENANT_ACCESS</code>.
          </p>
          <p>
            The organisation is part of the signed access token, so the server — not the browser —
            decides which tenant a request belongs to. Changing the organisation in the browser has no
            effect on the data returned.
          </p>
          {query.data ? (
            <div className="rounded-lg border border-edge bg-surface-base px-3.5 py-3">
              <p className="text-[10px] font-semibold uppercase tracking-wide text-ink-muted">
                Current tenant
              </p>
              <p className="mt-1 flex items-center gap-2 text-[13px] font-medium text-ink-primary">
                <Building2 className="h-3.5 w-3.5 text-cerulean-light" aria-hidden />
                {query.data.name}
              </p>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <StatusPill label={query.data.slug} tone="info" />
                <StatusPill label={query.data.defaultCurrency} tone="neutral" />
                <StatusPill
                  label={query.data.active ? 'Active' : 'Suspended'}
                  tone={query.data.active ? 'success' : 'danger'}
                  dot
                />
              </div>
            </div>
          ) : null}
        </CardBody>
      </Card>
    </div>
  );
}
