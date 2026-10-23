import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell, CheckCheck } from 'lucide-react';
import { notificationApi } from '@/api/client';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState } from '@/components/ui/EmptyState';
import { Pagination } from '@/components/ui/Pagination';
import { SkeletonTable } from '@/components/ui/Skeleton';
import { cn } from '@/lib/cn';
import { formatDateTime, relativeTime } from '@/lib/format';

const SEVERITY_TONE: Record<string, string> = {
  INFO: 'bg-cerulean',
  WARNING: 'bg-warn',
  ERROR: 'bg-danger',
  SUCCESS: 'bg-ok',
};

export function NotificationsPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ['notifications', 'list', page],
    queryFn: () => notificationApi.list({ page, size: 20 }),
  });

  const markRead = useMutation({
    mutationFn: (id: string) => notificationApi.markRead(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  const markAllRead = useMutation({
    mutationFn: () => notificationApi.markAllRead(),
    onSuccess: (count) => {
      void queryClient.invalidateQueries({ queryKey: ['notifications'] });
      void queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      return count;
    },
  });

  return (
    <Card>
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-edge px-4 py-3">
        <p className="text-xs text-ink-muted">
          Domain events raised by the platform, newest first.
        </p>
        <Button
          variant="secondary"
          size="sm"
          loading={markAllRead.isPending}
          onClick={() => markAllRead.mutate()}
          icon={<CheckCheck className="h-3.5 w-3.5" />}
        >
          Mark all read
        </Button>
      </div>

      {query.isError ? (
        <div className="p-4">
          <ErrorState
            title="Notifications could not be loaded"
            description={(query.error as Error)?.message}
            onRetry={() => void query.refetch()}
          />
        </div>
      ) : query.isLoading ? (
        <SkeletonTable rows={8} columns={3} />
      ) : query.data && query.data.content.length > 0 ? (
        <>
          <ul className="divide-y divide-edge">
            {query.data.content.map((notification) => (
              <li
                key={notification.id}
                className={cn(
                  'flex items-start gap-3 px-4 py-3 transition-colors hover:bg-surface-elevated/40',
                  !notification.read && 'bg-cerulean/[0.04]',
                )}
              >
                <span
                  className={cn(
                    'mt-1.5 h-2 w-2 shrink-0 rounded-full',
                    SEVERITY_TONE[notification.severity] ?? 'bg-ink-muted',
                  )}
                  aria-hidden
                />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-[13px] font-medium text-ink-primary">{notification.title}</p>
                    <span className="rounded-full border border-edge px-2 py-0.5 text-[10px] uppercase tracking-wide text-ink-muted">
                      {notification.category}
                    </span>
                    {!notification.read ? (
                      <span className="rounded-full border border-rose/35 bg-rose/12 px-2 py-0.5 text-[10px] font-medium text-rose-light">
                        New
                      </span>
                    ) : null}
                  </div>
                  {notification.body ? (
                    <p className="mt-1 text-[12px] leading-relaxed text-ink-secondary">
                      {notification.body}
                    </p>
                  ) : null}
                  <p className="mt-1.5 text-[10px] text-ink-muted">
                    {formatDateTime(notification.createdAt)} · {relativeTime(notification.createdAt)}
                  </p>
                </div>
                <div className="flex shrink-0 flex-col gap-1.5">
                  {!notification.read ? (
                    <Button
                      size="sm"
                      variant="ghost"
                      loading={markRead.isPending}
                      onClick={() => markRead.mutate(notification.id)}
                    >
                      Mark read
                    </Button>
                  ) : null}
                  {notification.link ? (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => navigate(notification.link!)}
                    >
                      Open
                    </Button>
                  ) : null}
                </div>
              </li>
            ))}
          </ul>
          <Pagination
            page={query.data.page}
            totalPages={query.data.totalPages}
            totalElements={query.data.totalElements}
            onPageChange={setPage}
          />
        </>
      ) : (
        <EmptyState
          icon={<Bell className="h-5 w-5" />}
          title="You are all caught up"
          description="New returns, pickups, inspections and recovery decisions will appear here."
        />
      )}
    </Card>
  );
}
