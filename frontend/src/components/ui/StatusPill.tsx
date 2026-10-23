import { cn } from '@/lib/cn';

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'accent';

const tones: Record<Tone, string> = {
  neutral: 'bg-surface-elevated text-ink-secondary border-edge-strong',
  info: 'bg-cerulean/12 text-cerulean-light border-cerulean/35',
  success: 'bg-ok/12 text-ok border-ok/35',
  warning: 'bg-warn/12 text-warn border-warn/35',
  danger: 'bg-danger/12 text-danger border-danger/35',
  accent: 'bg-rose/12 text-rose-light border-rose/35',
};

export interface StatusPillProps {
  label: string;
  tone?: Tone;
  className?: string;
  dot?: boolean;
}

export function StatusPill({ label, tone = 'neutral', className, dot }: StatusPillProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-[11px] font-medium leading-5',
        tones[tone],
        className,
      )}
    >
      {dot ? <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden /> : null}
      {label}
    </span>
  );
}

/**
 * Maps a domain status string to a pill tone, so the same status always looks
 * the same across the application.
 */
export function toneForStatus(status: string): Tone {
  switch (status) {
    case 'RETURN_CREATED':
    case 'REQUESTED':
    case 'PROPOSED':
    case 'AWAITING_INSPECTION':
    case 'AWAITING_REVIEW':
      return 'neutral';
    case 'PICKUP_SCHEDULED':
    case 'SCHEDULED':
    case 'ASSIGNED':
    case 'PLANNED':
    case 'IN_PROGRESS':
    case 'ACCEPTED':
    case 'PROCESSING':
    case 'IN_TRANSIT':
      return 'info';
    case 'COLLECTED':
    case 'WAREHOUSE_RECEIVED':
    case 'INSPECTED':
    case 'RECOVERY_DECISION':
    case 'PARTNER_ASSIGNED':
    case 'PASSED':
    case 'AVAILABLE':
    case 'RESERVED':
      return 'accent';
    case 'RECOVERED':
    case 'COMPLETED':
    case 'DELIVERED':
      return 'success';
    case 'FAILED':
    case 'DAMAGED':
    case 'UNSAFE':
    case 'REJECTED':
    case 'CANCELLED':
      return 'danger';
    case 'DELAYED':
    case 'INCOMPLETE':
    case 'QUARANTINED':
      return 'warning';
    default:
      return 'neutral';
  }
}
