import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarPlus, MapPin, Truck } from 'lucide-react';
import { logisticsApi } from '@/api/client';
import { Card, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Field, Input, Select, Textarea } from '@/components/ui/Form';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill, toneForStatus } from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { formatCurrency, formatDate, formatNumber } from '@/lib/format';
import type { PickupStatus } from '@/lib/types';

const STATUSES: PickupStatus[] = [
  'REQUESTED',
  'SCHEDULED',
  'IN_TRANSIT',
  'COLLECTED',
  'DELIVERED',
  'FAILED',
  'CANCELLED',
];

export function PickupsPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [status, setStatus] = useState<PickupStatus | ''>('');
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [schedule, setSchedule] = useState<{ id: string; number: string } | null>(null);

  const query = useQuery({
    queryKey: ['pickups', status, page],
    queryFn: () => logisticsApi.pickups({ status: status ? [status] : undefined, page, size: 15 }),
  });

  const locations = useQuery({
    queryKey: ['locations'],
    queryFn: () => logisticsApi.locations(),
  });

  const vehicles = useQuery({
    queryKey: ['vehicles'],
    queryFn: () => logisticsApi.vehicles(),
  });

  const drivers = useQuery({
    queryKey: ['drivers'],
    queryFn: () => logisticsApi.drivers(),
  });

  const advance = useMutation({
    mutationFn: ({ id, next, note }: { id: string; next: PickupStatus; note?: string }) =>
      logisticsApi.updatePickupStatus(id, next, note),
    onSuccess: (pickup) => {
      toast.success(`Pickup ${pickup.pickupNumber} is now ${pickup.statusLabel.toLowerCase()}`);
      void queryClient.invalidateQueries({ queryKey: ['pickups'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
    onError: (error) =>
      toast.error('The pickup could not be updated', error instanceof ApiError ? error.message : undefined),
  });

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="reveal lg:col-span-2">
          <div className="flex flex-wrap items-center gap-2 border-b border-edge px-4 py-3">
            <Select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as PickupStatus | '');
                setPage(0);
              }}
              className="w-52"
              aria-label="Filter pickups by status"
            >
              <option value="">All statuses</option>
              {STATUSES.map((entry) => (
                <option key={entry} value={entry}>
                  {entry.replace(/_/g, ' ').toLowerCase()}
                </option>
              ))}
            </Select>
            <Button
              className="ml-auto"
              size="md"
              onClick={() => setCreateOpen(true)}
              icon={<CalendarPlus className="h-4 w-4" />}
            >
              Schedule pickup
            </Button>
          </div>

          {query.isError ? (
            <div className="p-4">
              <ErrorState
                title="Pickups could not be loaded"
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
                      <th>Pickup</th>
                      <th>Status</th>
                      <th>Origin</th>
                      <th>Scheduled</th>
                      <th>Vehicle</th>
                      <th className="text-right">Units</th>
                      <th className="text-right">Est. cost</th>
                      <th className="text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {query.data.content.map((pickup) => (
                      <tr key={pickup.id}>
                        <td className="font-mono text-[12px] text-cerulean-light">{pickup.pickupNumber}</td>
                        <td>
                          <StatusPill
                            label={pickup.statusLabel}
                            tone={toneForStatus(pickup.status)}
                            dot
                          />
                        </td>
                        <td className="max-w-[220px] truncate text-ink-secondary">
                          {pickup.originAddress ?? '—'}
                        </td>
                        <td className="whitespace-nowrap text-ink-muted">
                          {pickup.scheduledDate ? formatDate(pickup.scheduledDate) : 'Unscheduled'}
                        </td>
                        <td className="text-ink-muted">
                          {vehicles.data?.find((vehicle) => vehicle.id === pickup.vehicleId)?.name ?? '—'}
                        </td>
                        <td className="text-right tabular-nums">{formatNumber(pickup.unitCount)}</td>
                        <td className="text-right tabular-nums">
                          {pickup.estimatedCost !== null ? formatCurrency(pickup.estimatedCost) : '—'}
                        </td>
                        <td className="text-right">
                          <div className="flex justify-end gap-1.5">
                            {pickup.status === 'REQUESTED' || pickup.status === 'FAILED' ? (
                              <Button
                                size="sm"
                                variant="secondary"
                                onClick={() => setSchedule({ id: pickup.id, number: pickup.pickupNumber })}
                              >
                                Schedule
                              </Button>
                            ) : null}
                            {pickup.status === 'SCHEDULED' ? (
                              <Button
                                size="sm"
                                variant="secondary"
                                loading={advance.isPending}
                                onClick={() => advance.mutate({ id: pickup.id, next: 'IN_TRANSIT' })}
                              >
                                Dispatch
                              </Button>
                            ) : null}
                            {pickup.status === 'IN_TRANSIT' ? (
                              <Button
                                size="sm"
                                loading={advance.isPending}
                                onClick={() => advance.mutate({ id: pickup.id, next: 'COLLECTED' })}
                              >
                                Collected
                              </Button>
                            ) : null}
                            {pickup.status === 'COLLECTED' ? (
                              <Button
                                size="sm"
                                loading={advance.isPending}
                                onClick={() => advance.mutate({ id: pickup.id, next: 'DELIVERED' })}
                              >
                                Deliver
                              </Button>
                            ) : null}
                          </div>
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
              icon={<Truck className="h-5 w-5" />}
              title="No pickups scheduled"
              description="Schedule a collection so the goods can be consolidated into a route."
              action={
                <Button size="sm" onClick={() => setCreateOpen(true)}>
                  Schedule pickup
                </Button>
              }
            />
          )}
        </Card>

        <div className="space-y-4">
          <Card className="reveal">
            <CardHeader title="Fleet" description="Vehicles available for collection" />
            <div className="divide-y divide-edge">
              {vehicles.data?.map((vehicle) => (
                <div key={vehicle.id} className="px-4 py-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-[13px] font-medium text-ink-primary">{vehicle.name}</p>
                    <StatusPill
                      label={vehicle.active ? 'Active' : 'Inactive'}
                      tone={vehicle.active ? 'success' : 'neutral'}
                    />
                  </div>
                  <p className="mt-0.5 text-[11px] text-ink-muted">
                    {vehicle.registration} · {vehicle.vehicleType} · {vehicle.capacityUnits} unit capacity
                    {vehicle.capacityKg ? ` · ${vehicle.capacityKg} kg` : ''}
                  </p>
                </div>
              ))}
            </div>
          </Card>

          <Card className="reveal">
            <CardHeader title="Drivers" description="Available for assignment" />
            <div className="divide-y divide-edge">
              {drivers.data?.map((driver) => (
                <div key={driver.id} className="flex items-center justify-between gap-2 px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-[13px] font-medium text-ink-primary">{driver.name}</p>
                    <p className="mt-0.5 truncate text-[11px] text-ink-muted">{driver.phone ?? driver.email}</p>
                  </div>
                  <StatusPill label="Available" tone="success" dot />
                </div>
              ))}
            </div>
          </Card>

          <Card className="reveal">
            <CardHeader title="Destinations" description="Warehouses and partner facilities" />
            <div className="divide-y divide-edge">
              {locations.data?.map((location) => (
                <div key={location.id} className="px-4 py-3">
                  <div className="flex items-start justify-between gap-2">
                    <p className="text-[13px] font-medium text-ink-primary">{location.name}</p>
                    <StatusPill label={location.locationTypeLabel} tone="info" />
                  </div>
                  <p className="mt-0.5 flex items-start gap-1 text-[11px] text-ink-muted">
                    <MapPin className="mt-0.5 h-3 w-3 shrink-0" aria-hidden />
                    {location.address}
                  </p>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>

      <CreatePickupModal
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        locations={locations.data ?? []}
        onCreated={() => {
          setCreateOpen(false);
          void queryClient.invalidateQueries({ queryKey: ['pickups'] });
        }}
      />

      {schedule ? (
        <SchedulePickupModal
          pickup={schedule}
          vehicles={vehicles.data ?? []}
          drivers={drivers.data ?? []}
          onClose={() => setSchedule(null)}
          onScheduled={() => {
            setSchedule(null);
            void queryClient.invalidateQueries({ queryKey: ['pickups'] });
          }}
        />
      ) : null}
    </div>
  );
}

function CreatePickupModal({
  open,
  onClose,
  locations,
  onCreated,
}: {
  open: boolean;
  onClose: () => void;
  locations: { id: string; name: string }[];
  onCreated: () => void;
}) {
  const toast = useToast();
  const [form, setForm] = useState({
    originAddress: '',
    originLatitude: '',
    originLongitude: '',
    destinationLocationId: '',
    scheduledDate: new Date().toISOString().slice(0, 10),
    windowStart: '09:00',
    windowEnd: '12:00',
    priority: 5,
    unitCount: 1,
    totalWeightKg: '',
    notes: '',
  });

  const create = useMutation({
    mutationFn: () =>
      logisticsApi.createPickup({
        originAddress: form.originAddress,
        originLatitude: form.originLatitude ? Number(form.originLatitude) : undefined,
        originLongitude: form.originLongitude ? Number(form.originLongitude) : undefined,
        destinationLocationId: form.destinationLocationId || undefined,
        scheduledDate: form.scheduledDate,
        windowStart: form.windowStart,
        windowEnd: form.windowEnd,
        priority: form.priority,
        unitCount: form.unitCount,
        totalWeightKg: form.totalWeightKg ? Number(form.totalWeightKg) : undefined,
        notes: form.notes || undefined,
      }),
    onSuccess: (pickup) => {
      toast.success('Pickup scheduled', `${pickup.pickupNumber} is queued for collection.`);
      onCreated();
    },
    onError: (error) =>
      toast.error('The pickup could not be scheduled', error instanceof ApiError ? error.message : undefined),
  });

  const update = (key: keyof typeof form, value: string | number) =>
    setForm((current) => ({ ...current, [key]: value }));

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Schedule a pickup"
      description="Give the collection a window, an origin and a destination warehouse"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            loading={create.isPending}
            disabled={!form.originAddress || !form.scheduledDate}
            onClick={() => create.mutate()}
          >
            Schedule pickup
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Origin address" required>
          <Input
            value={form.originAddress}
            onChange={(event) => update('originAddress', event.target.value)}
            placeholder="e.g. 220 Riverside Dr, Newark, NJ"
          />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Origin latitude" hint="Used for route distance estimates">
            <Input
              type="number"
              step="0.0000001"
              value={form.originLatitude}
              onChange={(event) => update('originLatitude', event.target.value)}
              placeholder="40.7357"
            />
          </Field>
          <Field label="Origin longitude" hint="Used for route distance estimates">
            <Input
              type="number"
              step="0.0000001"
              value={form.originLongitude}
              onChange={(event) => update('originLongitude', event.target.value)}
              placeholder="-74.1724"
            />
          </Field>
        </div>
        <Field label="Destination">
          <Select
            value={form.destinationLocationId}
            onChange={(event) => update('destinationLocationId', event.target.value)}
          >
            <option value="">Select a warehouse…</option>
            {locations.map((location) => (
              <option key={location.id} value={location.id}>
                {location.name}
              </option>
            ))}
          </Select>
        </Field>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Date" required>
            <Input
              type="date"
              value={form.scheduledDate}
              onChange={(event) => update('scheduledDate', event.target.value)}
            />
          </Field>
          <Field label="Window start">
            <Input
              type="time"
              value={form.windowStart}
              onChange={(event) => update('windowStart', event.target.value)}
            />
          </Field>
          <Field label="Window end">
            <Input
              type="time"
              value={form.windowEnd}
              onChange={(event) => update('windowEnd', event.target.value)}
            />
          </Field>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <Field label="Priority" hint="1 is highest">
            <Input
              type="number"
              min={1}
              max={9}
              value={form.priority}
              onChange={(event) => update('priority', Number(event.target.value))}
            />
          </Field>
          <Field label="Units">
            <Input
              type="number"
              min={1}
              value={form.unitCount}
              onChange={(event) => update('unitCount', Number(event.target.value))}
            />
          </Field>
          <Field label="Weight (kg)">
            <Input
              type="number"
              step="0.1"
              value={form.totalWeightKg}
              onChange={(event) => update('totalWeightKg', event.target.value)}
            />
          </Field>
        </div>
        <Field label="Notes">
          <Textarea
            rows={2}
            value={form.notes}
            onChange={(event) => update('notes', event.target.value)}
          />
        </Field>
      </div>
    </Modal>
  );
}

function SchedulePickupModal({
  pickup,
  vehicles,
  drivers,
  onClose,
  onScheduled,
}: {
  pickup: { id: string; number: string };
  vehicles: { id: string; name: string; capacityUnits: number }[];
  drivers: { id: string; name: string }[];
  onClose: () => void;
  onScheduled: () => void;
}) {
  const toast = useToast();
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [vehicleId, setVehicleId] = useState('');
  const [driverId, setDriverId] = useState('');

  const schedule = useMutation({
    mutationFn: () =>
      logisticsApi.schedule(pickup.id, {
        scheduledDate: date,
        vehicleId: vehicleId || undefined,
        driverId: driverId || undefined,
      }),
    onSuccess: () => {
      toast.success('Pickup scheduled', `${pickup.number} has a date, vehicle and driver.`);
      onScheduled();
    },
    onError: (error) =>
      toast.error('The pickup could not be scheduled', error instanceof ApiError ? error.message : undefined),
  });

  return (
    <Modal
      open
      onClose={onClose}
      title={`Schedule ${pickup.number}`}
      description="Assign a date, vehicle and driver. Capacity is checked before the assignment is accepted."
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={schedule.isPending} onClick={() => schedule.mutate()}>
            Confirm
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        <Field label="Collection date" required>
          <Input type="date" value={date} onChange={(event) => setDate(event.target.value)} />
        </Field>
        <Field label="Vehicle">
          <Select value={vehicleId} onChange={(event) => setVehicleId(event.target.value)}>
            <option value="">Assign later</option>
            {vehicles.map((vehicle) => (
              <option key={vehicle.id} value={vehicle.id}>
                {vehicle.name} · {vehicle.capacityUnits} units
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Driver">
          <Select value={driverId} onChange={(event) => setDriverId(event.target.value)}>
            <option value="">Assign later</option>
            {drivers.map((driver) => (
              <option key={driver.id} value={driver.id}>
                {driver.name}
              </option>
            ))}
          </Select>
        </Field>
      </div>
    </Modal>
  );
}
