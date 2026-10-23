import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ScrollText } from 'lucide-react';
import { auditApi } from '@/api/client';
import { Card } from '@/components/ui/Card';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Select } from '@/components/ui/Form';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { StatusPill } from '@/components/ui/StatusPill';
import { formatDateTime } from '@/lib/format';

const ENTITY_TYPES = [
  'ReturnRequest',
  'ReturnItem',
  'Inspection',
  'RecoveryRecommendation',
  'RecoveryJob',
  'PickupJob',
  'Route',
  'Partner',
  'InventoryItem',
  'UserAccount',
];

export function AuditPage() {
  const [entityType, setEntityType] = useState('');
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['audit', entityType, page],
    queryFn: () => auditApi.search({ entityType: entityType || undefined, page, size: 25 }),
  });

  return (
    <Card>
      <div className="flex flex-wrap items-center gap-2 border-b border-edge px-4 py-3">
        <Select
          value={entityType}
          onChange={(event) => {
            setEntityType(event.target.value);
            setPage(0);
          }}
          className="w-56"
          aria-label="Filter audit events by entity type"
        >
          <option value="">All entity types</option>
          {ENTITY_TYPES.map((entry) => (
            <option key={entry} value={entry}>
              {entry}
            </option>
          ))}
        </Select>
      </div>

      {query.isError ? (
        <div className="p-4">
          <ErrorState
            title="The audit trail could not be loaded"
            description={(query.error as Error)?.message}
            onRetry={() => void query.refetch()}
          />
        </div>
      ) : query.isLoading ? (
        <SkeletonTable rows={10} columns={5} />
      ) : query.data && query.data.content.length > 0 ? (
        <>
          <div className="overflow-x-auto">
            <table className="data-table rows-enter">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Entity</th>
                  <th>Summary</th>
                </tr>
              </thead>
              <tbody>
                {query.data.content.map((event) => (
                  <tr key={event.id}>
                    <td className="whitespace-nowrap text-ink-muted">{formatDateTime(event.occurredAt)}</td>
                    <td>
                      <span className="font-medium text-ink-primary">{event.actor}</span>
                      {event.actorRole ? (
                        <span className="ml-1.5 text-[11px] text-ink-muted">
                          {event.actorRole.replace(/_/g, ' ').toLowerCase()}
                        </span>
                      ) : null}
                    </td>
                    <td>
                      <StatusPill label={event.action.replace(/_/g, ' ').toLowerCase()} tone="info" />
                    </td>
                    <td className="text-ink-secondary">{event.entityType}</td>
                    <td className="max-w-[380px] truncate text-ink-muted">{event.summary ?? '—'}</td>
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
          icon={<ScrollText className="h-5 w-5" />}
          title="No audit events yet"
          description="Privileged actions are recorded here as they happen."
        />
      )}
    </Card>
  );
}
