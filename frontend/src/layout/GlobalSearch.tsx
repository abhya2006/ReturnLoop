import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Command, Package, Search, Building2, ArrowRight } from 'lucide-react';
import { returnsApi, partnerApi, catalogApi } from '@/api/client';
import { Modal } from '@/components/ui/Modal';
import { cn } from '@/lib/cn';
import { formatCurrency } from '@/lib/format';

interface SearchResult {
  id: string;
  group: string;
  title: string;
  subtitle: string;
  to: string;
  icon: typeof Package;
}

/**
 * Global search across returns, products and partners, opened with Ctrl+K.
 */
export function GlobalSearch({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [term, setTerm] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) setTerm('');
  }, [open]);

  const { data: returns } = useQuery({
    queryKey: ['search', 'returns', term],
    queryFn: () => returnsApi.search({ search: term, size: 5 }),
    enabled: open && term.trim().length >= 2,
  });

  const { data: products } = useQuery({
    queryKey: ['search', 'products', term],
    queryFn: () => catalogApi.search({ search: term, size: 5 }),
    enabled: open && term.trim().length >= 2,
  });

  const { data: partners } = useQuery({
    queryKey: ['search', 'partners', term],
    queryFn: () => partnerApi.search({ search: term, size: 5 }),
    enabled: open && term.trim().length >= 2,
  });

  const results = useMemo<SearchResult[]>(() => {
    const items: SearchResult[] = [];
    returns?.content.forEach((item) =>
      items.push({
        id: item.id,
        group: 'Returns',
        title: item.returnNumber,
        subtitle: `${item.statusLabel} · ${item.itemCount} item(s)`,
        to: `/returns/${item.id}`,
        icon: Package,
      }),
    );
    products?.content.forEach((item) =>
      items.push({
        id: item.id,
        group: 'Catalog',
        title: `${item.sku} — ${item.name}`,
        subtitle: `${item.category} · resale ${formatCurrency(item.resaleValue ?? 0)}`,
        to: '/inventory',
        icon: Package,
      }),
    );
    partners?.content.forEach((item) =>
      items.push({
        id: item.id,
        group: 'Partners',
        title: item.name,
        subtitle: `${item.partnerTypeLabel} · ${item.completedJobs} completed job(s)`,
        to: `/partners/${item.id}`,
        icon: Building2,
      }),
    );
    return items;
  }, [returns, products, partners]);

  const go = (result: SearchResult) => {
    navigate(result.to);
    onClose();
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Global search"
      description="Search returns, products, SKUs, partners and tracking references"
      size="lg"
    >
      <div className="flex items-center gap-2 rounded-lg border border-edge bg-surface-base px-3">
        <Search className="h-4 w-4 text-ink-muted" aria-hidden />
        <input
          autoFocus
          value={term}
          onChange={(event) => setTerm(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && results[0]) {
              go(results[0]);
            }
          }}
          placeholder="Type at least two characters to search…"
          className="w-full bg-transparent py-3 text-sm text-ink-primary placeholder:text-ink-muted/70 focus:outline-none"
        />
        <kbd className="flex items-center gap-0.5 rounded border border-edge bg-surface-elevated px-1.5 py-0.5 font-mono text-[10px] text-ink-muted">
          <Command className="h-2.5 w-2.5" aria-hidden />K
        </kbd>
      </div>

      <div className="mt-3 max-h-[46vh] overflow-y-auto">
        {term.trim().length < 2 ? (
          <p className="px-2 py-8 text-center text-xs text-ink-muted">
            Start typing to search across the platform.
          </p>
        ) : results.length === 0 ? (
          <p className="px-2 py-8 text-center text-xs text-ink-muted">
            No matches for “{term}”.
          </p>
        ) : (
          <ul className="space-y-1">
            {results.map((result) => {
              const Icon = result.icon;
              return (
                <li key={`${result.group}-${result.id}`}>
                  <button
                    type="button"
                    onClick={() => go(result)}
                    className={cn(
                      'flex w-full items-center gap-3 rounded-lg border border-transparent px-3 py-2.5 text-left transition-colors',
                      'hover:border-cerulean/40 hover:bg-cerulean/[0.07] focus-visible:ring-2 focus-visible:ring-cerulean',
                    )}
                  >
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg border border-edge bg-surface-elevated text-cerulean-light">
                      <Icon className="h-4 w-4" aria-hidden />
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[13px] font-medium text-ink-primary">
                        {result.title}
                      </span>
                      <span className="block truncate text-[11px] text-ink-muted">
                        {result.group} · {result.subtitle}
                      </span>
                    </span>
                    <ArrowRight className="h-3.5 w-3.5 shrink-0 text-ink-muted" aria-hidden />
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </Modal>
  );
}
