import type { ReactNode } from 'react';
import { cn } from '@/lib/cn';

export function EmptyState({
  icon,
  title,
  description,
  action,
  className,
}: {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 px-6 py-14 text-center',
        className,
      )}
    >
      {icon ? (
        <div
          className="flex h-12 w-12 items-center justify-center rounded-2xl border border-edge bg-surface-elevated text-ink-muted"
          aria-hidden
        >
          {icon}
        </div>
      ) : null}
      <div>
        <p className="text-sm font-semibold text-ink-primary">{title}</p>
        {description ? (
          <p className="mx-auto mt-1 max-w-sm text-xs leading-relaxed text-ink-muted">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  );
}

export function ErrorState({
  title = 'Something went wrong',
  description,
  onRetry,
  className,
}: {
  title?: string;
  description?: string;
  onRetry?: () => void;
  className?: string;
}) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center gap-3 rounded-card border border-danger/30 bg-danger/[0.06] px-6 py-10 text-center',
        className,
      )}
      role="alert"
    >
      <p className="text-sm font-semibold text-danger">{title}</p>
      {description ? <p className="max-w-md text-xs text-ink-secondary">{description}</p> : null}
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="rounded-lg border border-danger/40 px-3 py-1.5 text-xs font-medium text-danger transition-colors hover:bg-danger/10 focus-visible:ring-2 focus-visible:ring-cerulean"
        >
          Try again
        </button>
      ) : null}
    </div>
  );
}
