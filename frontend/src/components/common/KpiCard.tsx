import { useRef, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { TrendingDown, TrendingUp } from 'lucide-react';
import { cn } from '@/lib/cn';

export interface KpiCardProps {
  label: string;
  value: ReactNode;
  icon: ReactNode;
  trend?: { value: number; label: string } | null;
  to?: string;
  accent?: 'cerulean' | 'rose';
  footer?: ReactNode;
}

export function KpiCard({
  label,
  value,
  icon,
  trend,
  to,
  accent = 'cerulean',
  footer,
}: KpiCardProps) {
  const ref = useRef<HTMLDivElement>(null);

  // Tracks the pointer so the hover glow follows it across the tile.
  const trackPointer = (event: React.PointerEvent<HTMLElement>) => {
    const node = ref.current;
    if (!node) return;
    const bounds = node.getBoundingClientRect();
    node.style.setProperty('--spot-x', `${event.clientX - bounds.left}px`);
    node.style.setProperty('--spot-y', `${event.clientY - bounds.top}px`);
  };

  const body = (
    <>
      <div className="relative flex items-start justify-between gap-3">
        <span
          className={cn(
            'flex h-9 w-9 items-center justify-center rounded-xl border transition-transform duration-300 group-hover:scale-110',
            accent === 'cerulean'
              ? 'border-cerulean/30 bg-cerulean/10 text-cerulean-light'
              : 'border-rose/30 bg-rose/10 text-rose-light',
          )}
          aria-hidden
        >
          {icon}
        </span>
        {trend && Number.isFinite(trend.value) ? (
          <span
            className={cn(
              'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[11px] font-medium tabular-nums',
              trend.value >= 0
                ? 'border-ok/30 bg-ok/10 text-ok'
                : 'border-danger/30 bg-danger/10 text-danger',
            )}
          >
            {trend.value >= 0 ? (
              <TrendingUp className="h-3 w-3" aria-hidden />
            ) : (
              <TrendingDown className="h-3 w-3" aria-hidden />
            )}
            {trend.value >= 0 ? '+' : ''}
            {trend.value.toFixed(1)}%
          </span>
        ) : null}
      </div>
      <p className="relative mt-4 text-[11px] font-medium uppercase tracking-wide text-ink-muted">{label}</p>
      <p className="relative mt-1 text-2xl font-bold tracking-tight tabular-nums text-ink-primary">{value}</p>
      {footer ? <div className="relative mt-2">{footer}</div> : null}
    </>
  );

  const className = cn(
    'panel kpi-tile group relative overflow-hidden p-5',
    'focus-visible:ring-2 focus-visible:ring-cerulean',
  );

  if (to) {
    return (
      <Link
        to={to}
        ref={ref as unknown as React.RefObject<HTMLAnchorElement>}
        onPointerMove={trackPointer}
        className={className}
      >
        <span
          className={cn(
            'pointer-events-none absolute inset-x-0 top-0 h-px opacity-0 transition-opacity duration-300 group-hover:opacity-100',
            accent === 'cerulean'
              ? 'bg-gradient-to-r from-transparent via-cerulean to-transparent'
              : 'bg-gradient-to-r from-transparent via-rose to-transparent',
          )}
          aria-hidden
        />
        {body}
      </Link>
    );
  }

  return (
    <div ref={ref} onPointerMove={trackPointer} className={className}>
      {body}
    </div>
  );
}
