import { useState } from 'react';
import { CalendarDays, ChevronDown } from 'lucide-react';
import { cn } from '@/lib/cn';

export type DateRangePreset = '7d' | '30d' | '90d' | 'custom';

export interface DateRange {
  from: string;
  to: string;
  preset: DateRangePreset;
}

export function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

export function rangeForPreset(preset: Exclude<DateRangePreset, 'custom'>): { from: string; to: string } {
  const to = new Date();
  const days = preset === '7d' ? 7 : preset === '30d' ? 30 : 90;
  const from = new Date(to);
  from.setDate(from.getDate() - days + 1);
  return { from: isoDate(from), to: isoDate(to) };
}

export function defaultRange(): DateRange {
  const { from, to } = rangeForPreset('30d');
  return { from, to, preset: '30d' };
}

const presets: { value: Exclude<DateRangePreset, 'custom'>; label: string }[] = [
  { value: '7d', label: 'Last 7 days' },
  { value: '30d', label: 'Last 30 days' },
  { value: '90d', label: 'Last 90 days' },
];

export function DateRangeSelector({
  value,
  onChange,
  className,
}: {
  value: DateRange;
  onChange: (range: DateRange) => void;
  className?: string;
}) {
  const [open, setOpen] = useState(false);
  const [customFrom, setCustomFrom] = useState(value.from);
  const [customTo, setCustomTo] = useState(value.to);

  const label =
    value.preset === 'custom'
      ? `${value.from} → ${value.to}`
      : presets.find((preset) => preset.value === value.preset)?.label ?? 'Custom range';

  const choose = (preset: Exclude<DateRangePreset, 'custom'>) => {
    const range = rangeForPreset(preset);
    onChange({ ...range, preset });
    setOpen(false);
  };

  const applyCustom = () => {
    onChange({ from: customFrom, to: customTo, preset: 'custom' });
    setOpen(false);
  };

  return (
    <div className={cn('relative', className)}>
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        className="inline-flex items-center gap-2 rounded-lg border border-edge bg-surface-base px-3 py-2 text-[12px] font-medium text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
      >
        <CalendarDays className="h-3.5 w-3.5 text-cerulean-light" aria-hidden />
        {label}
        <ChevronDown className="h-3 w-3 text-ink-muted" aria-hidden />
      </button>

      {open ? (
        <>
          <div className="fixed inset-0 z-30" onClick={() => setOpen(false)} aria-hidden />
          <div className="absolute right-0 top-[calc(100%+6px)] z-40 w-64 animate-fade-up rounded-card border border-edge bg-surface-elevated p-3 shadow-panel">
            <p className="mb-2 text-[10px] font-semibold uppercase tracking-wide text-ink-muted">
              Quick ranges
            </p>
            <div className="space-y-1">
              {presets.map((preset) => (
                <button
                  key={preset.value}
                  type="button"
                  onClick={() => choose(preset.value)}
                  className={cn(
                    'w-full rounded-lg px-2.5 py-1.5 text-left text-[12px] transition-colors',
                    value.preset === preset.value
                      ? 'bg-cerulean/15 text-cerulean-light'
                      : 'text-ink-secondary hover:bg-surface-hover hover:text-ink-primary',
                  )}
                >
                  {preset.label}
                </button>
              ))}
            </div>
            <p className="mb-2 mt-3 text-[10px] font-semibold uppercase tracking-wide text-ink-muted">
              Custom range
            </p>
            <div className="space-y-2">
              <label className="block">
                <span className="sr-only">From date</span>
                <input
                  type="date"
                  value={customFrom}
                  max={customTo}
                  onChange={(event) => setCustomFrom(event.target.value)}
                  className="w-full rounded-lg border border-edge bg-surface-base px-2.5 py-1.5 text-[12px] text-ink-primary focus:border-cerulean focus:outline-none focus:ring-1 focus:ring-cerulean"
                />
              </label>
              <label className="block">
                <span className="sr-only">To date</span>
                <input
                  type="date"
                  value={customTo}
                  min={customFrom}
                  onChange={(event) => setCustomTo(event.target.value)}
                  className="w-full rounded-lg border border-edge bg-surface-base px-2.5 py-1.5 text-[12px] text-ink-primary focus:border-cerulean focus:outline-none focus:ring-1 focus:ring-cerulean"
                />
              </label>
              <button
                type="button"
                onClick={applyCustom}
                className="w-full rounded-lg bg-cerulean px-3 py-1.5 text-[12px] font-medium text-white transition-colors hover:bg-cerulean-light focus-visible:ring-2 focus-visible:ring-cerulean"
              >
                Apply range
              </button>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
