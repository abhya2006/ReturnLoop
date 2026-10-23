import { cn } from '@/lib/cn';

/**
 * The ReturnLoop infinity-style mark: two interlocking loops in the brand
 * gradient, echoing the circular flow of goods.
 */
export function Logo({ className, size = 32 }: { className?: string; size?: number }) {
  return (
    <span
      className={cn('relative inline-flex shrink-0 items-center justify-center', className)}
      style={{ width: size, height: size }}
      aria-hidden
    >
      <svg viewBox="0 0 40 40" width={size} height={size} fill="none">
        <defs>
          <linearGradient id="returnloop-mark" x1="0" y1="0" x2="40" y2="40">
            <stop offset="0%" stopColor="#007BA7" />
            <stop offset="100%" stopColor="#FF2D95" />
          </linearGradient>
        </defs>
        <path
          d="M13.5 25.5c-3.3 0-6-2.4-6-5.4 0-3 2.7-5.4 6-5.4h16c3.3 0 6 2.4 6 5.4 0 3-2.7 5.4-6 5.4"
          stroke="url(#returnloop-mark)"
          strokeWidth="3.2"
          strokeLinecap="round"
        />
        <path
          d="M26.5 14.5c3.3 0 6 2.4 6 5.4 0 3-2.7 5.4-6 5.4h-16c-3.3 0-6-2.4-6-5.4 0-3 2.7-5.4 6-5.4"
          stroke="url(#returnloop-mark)"
          strokeWidth="3.2"
          strokeLinecap="round"
          opacity="0.72"
        />
      </svg>
    </span>
  );
}

export function Wordmark({ collapsed }: { collapsed?: boolean }) {
  if (collapsed) {
    return <Logo className="mx-auto" size={30} />;
  }
  return (
    <span className="flex items-center gap-2.5">
      <Logo size={30} />
      <span className="flex flex-col leading-none">
        <span className="text-[15px] font-bold tracking-tight text-ink-primary">
          Return<span className="text-cerulean-light">Loop</span>
        </span>
        <span className="mt-0.5 text-[10px] font-medium tracking-wide text-ink-muted">
          Less Waste. More Value.
        </span>
      </span>
    </span>
  );
}
