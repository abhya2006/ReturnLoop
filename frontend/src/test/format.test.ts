import { describe, expect, it } from 'vitest';
import { formatCurrency, formatNumber, formatPercent, initials, relativeTime } from '@/lib/format';

describe('format helpers', () => {
  it('formats currency', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50');
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('renders a dash for missing values', () => {
    expect(formatCurrency(null)).toBe('—');
    expect(formatCurrency(undefined)).toBe('—');
    expect(formatNumber(null)).toBe('—');
    expect(formatPercent(null)).toBe('—');
  });

  it('formats numbers and percentages', () => {
    expect(formatNumber(1234)).toBe('1,234');
    expect(formatPercent(42.345)).toBe('42.3%');
  });

  it('builds initials from a name', () => {
    expect(initials('Amara Okafor')).toBe('AO');
    expect(initials('Priya')).toBe('P');
    expect(initials('')).toBe('');
  });

  it('describes recent timestamps relatively', () => {
    const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();
    expect(relativeTime(twoHoursAgo)).toContain('2 hours ago');
  });
});
