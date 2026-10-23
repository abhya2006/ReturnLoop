import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import { chartTheme, recoveryPathColors, tooltipProps } from './chartTheme';
import type { DistributionSlice } from '@/lib/types';
import { formatCurrency, formatNumber } from '@/lib/format';

export function RecoveryDonutChart({ data }: { data: DistributionSlice[] }) {
  const active = data.filter((slice) => slice.count > 0);
  const totalCount = active.reduce((sum, slice) => sum + slice.count, 0);
  const totalValue = active.reduce((sum, slice) => sum + slice.value, 0);

  if (totalCount === 0) {
    return (
      <div className="flex h-[280px] items-center justify-center text-xs text-ink-muted">
        No recovery recommendations in this period yet.
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-center">
      <div className="relative h-[240px] w-full sm:w-[240px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={active}
              dataKey="count"
              nameKey="label"
              innerRadius={62}
              outerRadius={94}
              paddingAngle={2}
              stroke="none"
              animationBegin={140}
              animationDuration={900}
              animationEasing="ease-out"
            >
              {active.map((slice) => (
                <Cell key={slice.path} fill={recoveryPathColors[slice.path] ?? chartTheme.cerulean} />
              ))}
            </Pie>
            <Tooltip
              {...tooltipProps}
              formatter={(value: number, name: string, entry) => [
                `${formatNumber(value)} item(s) · ${formatCurrency(
                  (entry.payload as DistributionSlice).value,
                )}`,
                name,
              ]}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
          <span className="text-2xl font-bold tracking-tight text-ink-primary">
            {formatNumber(totalCount)}
          </span>
          <span className="text-[11px] uppercase tracking-wide text-ink-muted">items</span>
        </div>
      </div>

      <ul className="w-full min-w-0 flex-1 space-y-1.5">
        {active
          .slice()
          .sort((a, b) => b.count - a.count)
          .map((slice) => (
            <li key={slice.path} className="flex items-center gap-2.5">
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-sm"
                style={{ background: recoveryPathColors[slice.path] ?? chartTheme.cerulean }}
                aria-hidden
              />
              <span className="min-w-0 flex-1 truncate text-[12px] text-ink-secondary">
                {slice.label}
              </span>
              <span className="shrink-0 text-[12px] font-medium tabular-nums text-ink-primary">
                {slice.percentage.toFixed(0)}%
              </span>
              <span className="w-20 shrink-0 text-right text-[11px] tabular-nums text-ink-muted">
                {formatCurrency(slice.value, 'USD', true)}
              </span>
            </li>
          ))}
        <li className="mt-2 flex items-center justify-between border-t border-edge pt-2 text-[12px]">
          <span className="text-ink-muted">Total value routed</span>
          <span className="font-semibold tabular-nums text-ink-primary">
            {formatCurrency(totalValue)}
          </span>
        </li>
      </ul>
    </div>
  );
}
