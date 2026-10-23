import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/cn';

export interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
  className,
}: PaginationProps) {
  if (totalPages <= 1) {
    return (
      <div className={cn('px-4 py-2.5 text-xs text-ink-muted', className)}>
        {totalElements} record{totalElements === 1 ? '' : 's'}
      </div>
    );
  }

  const pages = pageWindow(page, totalPages);

  return (
    <div
      className={cn(
        'flex flex-wrap items-center justify-between gap-3 border-t border-edge px-4 py-3',
        className,
      )}
    >
      <p className="text-xs text-ink-muted">
        {totalElements} record{totalElements === 1 ? '' : 's'} · page {page + 1} of {totalPages}
      </p>
      <div className="flex items-center gap-1">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Previous page"
          className="rounded-lg border border-edge p-1.5 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary disabled:opacity-40 focus-visible:ring-2 focus-visible:ring-cerulean"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        {pages.map((entry, index) =>
          entry === null ? (
            <span key={`gap-${index}`} className="px-1 text-xs text-ink-muted">
              …
            </span>
          ) : (
            <button
              key={entry}
              type="button"
              onClick={() => onPageChange(entry)}
              aria-current={entry === page ? 'page' : undefined}
              className={cn(
                'min-w-[32px] rounded-lg border px-2 py-1 text-xs font-medium transition-colors focus-visible:ring-2 focus-visible:ring-cerulean',
                entry === page
                  ? 'border-cerulean bg-cerulean/15 text-cerulean-light'
                  : 'border-edge text-ink-secondary hover:border-cerulean/60 hover:text-ink-primary',
              )}
            >
              {entry + 1}
            </button>
          ),
        )}
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Next page"
          className="rounded-lg border border-edge p-1.5 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary disabled:opacity-40 focus-visible:ring-2 focus-visible:ring-cerulean"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

function pageWindow(page: number, totalPages: number): (number | null)[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }
  const pages: (number | null)[] = [0];
  const start = Math.max(1, page - 1);
  const end = Math.min(totalPages - 2, page + 1);
  if (start > 1) pages.push(null);
  for (let index = start; index <= end; index += 1) pages.push(index);
  if (end < totalPages - 2) pages.push(null);
  pages.push(totalPages - 1);
  return pages;
}
