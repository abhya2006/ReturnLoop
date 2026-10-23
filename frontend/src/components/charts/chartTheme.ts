/**
 * Chart tokens shared by every Recharts surface so the visual language stays
 * consistent with the rest of the product.
 */
export const chartTheme = {
  cerulean: 'rgb(var(--cerulean))',
  ceruleanLight: 'rgb(var(--cerulean-light))',
  rose: 'rgb(var(--rose))',
  roseLight: 'rgb(var(--rose-light))',
  ok: 'rgb(var(--ok))',
  warn: 'rgb(var(--warn))',
  danger: 'rgb(var(--danger))',
  grid: 'rgb(var(--chart-grid))',
  axis: 'rgb(var(--chart-axis))',
  text: 'rgb(var(--chart-text))',
} as const;

export const recoveryPathColors: Record<string, string> = {
  RESTOCK_AS_NEW: chartTheme.ok,
  OPEN_BOX_RESALE: chartTheme.ceruleanLight,
  REPAIR: chartTheme.cerulean,
  REFURBISHMENT: chartTheme.rose,
  LIQUIDATION: chartTheme.warn,
  SPARE_PARTS: '#8B7CF6',
  RECYCLING: '#4ADE80',
  SAFE_DISPOSAL: chartTheme.danger,
};

export const axisProps = {
  stroke: chartTheme.axis,
  tick: { fill: chartTheme.axis, fontSize: 16 },
  tickLine: false,
  axisLine: { stroke: chartTheme.grid },
} as const;

export const tooltipProps = {
  cursor: { fill: 'rgba(0,123,167,0.08)' },
  contentStyle: {
    background: 'rgb(var(--chart-tooltip))',
    border: '1px solid rgb(var(--chart-tooltip-edge))',
    borderRadius: 10,
    fontSize: 16,
    color: 'rgb(var(--chart-tooltip-text))',
    boxShadow: '0 18px 40px -24px rgba(0,0,0,0.9)',
  },
  labelStyle: { color: 'rgb(var(--chart-tooltip-muted))', marginBottom: 4 },
  itemStyle: { padding: '2px 0', fontSize: 16 },
} as const;
