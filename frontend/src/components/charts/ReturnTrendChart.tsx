import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { axisProps, chartTheme, tooltipProps } from './chartTheme';
import type { TrendPoint } from '@/lib/types';
import { formatDate } from '@/lib/format';

export function ReturnTrendChart({ data }: { data: TrendPoint[] }) {
  return (
    <div className="h-[280px] w-full">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 12, bottom: 4, left: -18 }}>
          <CartesianGrid stroke={chartTheme.grid} strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="date"
            {...axisProps}
            minTickGap={28}
            tickFormatter={(value: string) =>
              formatDate(value).replace(/, \d{4}$/, '')
            }
          />
          <YAxis {...axisProps} allowDecimals={false} width={56} />
          <Tooltip
            {...tooltipProps}
            formatter={(value: number, name: string) => [
              value,
              name === 'returnsCreated' ? 'Returns created' : 'Items recovered',
            ]}
            labelFormatter={(value: string) => formatDate(value)}
          />
          <Legend
            verticalAlign="top"
            align="right"
            height={28}
            iconType="plainline"
            wrapperStyle={{ fontSize: 16, color: chartTheme.text }}
          />
          <Line
            type="monotone"
            dataKey="returnsCreated"
            name="returnsCreated"
            stroke={chartTheme.ceruleanLight}
            strokeWidth={2}
            dot={false}
            activeDot={{ r: 4, fill: chartTheme.ceruleanLight, stroke: chartTheme.cerulean }}
            animationDuration={1100}
            animationEasing="ease-out"
          />
          <Line
            type="monotone"
            dataKey="itemsRecovered"
            name="itemsRecovered"
            stroke={chartTheme.rose}
            strokeWidth={2}
            strokeDasharray="5 4"
            dot={false}
            activeDot={{ r: 4, fill: chartTheme.rose }}
            animationDuration={1400}
            animationEasing="ease-out"
            animationBegin={220}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
