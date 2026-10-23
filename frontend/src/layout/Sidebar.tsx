import { useEffect, useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { navigation } from './navigation';
import { Wordmark } from './Logo';
import { cn } from '@/lib/cn';

const COLLAPSE_KEY = 'returnloop.sidebar.collapsed';

export function Sidebar({
  collapsed,
  onToggle,
  notificationCount,
}: {
  collapsed: boolean;
  onToggle: () => void;
  notificationCount: number;
}) {
  const location = useLocation();
  const [hovered, setHovered] = useState<string | null>(null);

  useEffect(() => {
    setHovered(null);
  }, [location.pathname]);

  return (
    <aside
      className={cn(
        'relative z-20 hidden shrink-0 flex-col border-r border-edge bg-surface-base transition-[width] duration-200 lg:flex',
        collapsed ? 'w-[68px]' : 'w-[248px]',
      )}
    >
      <div
        className={cn(
          'flex h-14 items-center border-b border-edge px-4',
          collapsed ? 'justify-center' : 'justify-between',
        )}
      >
        <Wordmark collapsed={collapsed} />
        {!collapsed ? (
          <button
            type="button"
            onClick={onToggle}
            aria-label="Collapse sidebar"
            className="rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <PanelLeftClose className="h-4 w-4" />
          </button>
        ) : null}
      </div>

      <nav className="flex-1 overflow-y-auto px-2 py-3" aria-label="Primary">
        {navigation.map((group) => (
          <div key={group.title} className="mb-4 last:mb-0">
            {!collapsed ? (
              <p className="px-3 pb-1.5 text-[10px] font-semibold uppercase tracking-widest text-ink-muted/70">
                {group.title}
              </p>
            ) : (
              <div className="mx-3 mb-2 h-px bg-edge" aria-hidden />
            )}
            <ul className="space-y-0.5">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive =
                  item.to === '/'
                    ? location.pathname === '/'
                    : location.pathname === item.to ||
                      location.pathname.startsWith(`${item.to}/`);
                return (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onMouseEnter={() => setHovered(item.to)}
                      onMouseLeave={() => setHovered(null)}
                      aria-current={isActive ? 'page' : undefined}
                      className={cn(
                        'nav-item group relative flex items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] font-medium',
                        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cerulean',
                        isActive
                          ? 'bg-cerulean/12 text-ink-primary'
                          : 'text-ink-secondary hover:bg-surface-hover hover:text-ink-primary',
                        collapsed && 'justify-center px-0',
                      )}
                    >
                      {isActive ? (
                        <span
                          className="absolute left-0 top-1/2 h-5 w-[3px] -translate-y-1/2 rounded-r bg-rose shadow-[0_0_10px_rgba(255,45,149,0.7)]"
                          aria-hidden
                        />
                      ) : null}
                      <Icon
                        className={cn(
                          'h-4 w-4 shrink-0 transition-all duration-200 group-hover:scale-110',
                          isActive ? 'text-cerulean-light' : 'text-ink-muted group-hover:text-ink-secondary',
                        )}
                        aria-hidden
                      />
                      {!collapsed ? <span className="truncate">{item.label}</span> : null}
                      {!collapsed && item.badge === 'notifications' && notificationCount > 0 ? (
                        <span className="nav-badge-pop ml-auto rounded-full bg-rose px-1.5 py-0.5 text-[10px] font-bold text-white">
                          {notificationCount > 99 ? '99+' : notificationCount}
                        </span>
                      ) : null}
                    </NavLink>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      {collapsed ? (
        <div className="border-t border-edge p-2">
          <button
            type="button"
            onClick={onToggle}
            aria-label="Expand sidebar"
            className="flex w-full items-center justify-center rounded-lg py-2 text-ink-muted transition-colors hover:bg-surface-hover hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <PanelLeftOpen className="h-4 w-4" />
          </button>
        </div>
      ) : null}

      {collapsed && hovered ? (
        <div
          className="pointer-events-none fixed left-[76px] top-20 z-50 max-w-[260px] rounded-lg border border-edge bg-surface-elevated px-3 py-2 shadow-panel"
          role="tooltip"
        >
          <p className="text-[13px] font-medium text-ink-primary">
            {navigation.flatMap((g) => g.items).find((i) => i.to === hovered)?.label}
          </p>
          <p className="mt-0.5 text-[11px] text-ink-muted">
            {navigation.flatMap((g) => g.items).find((i) => i.to === hovered)?.description}
          </p>
        </div>
      ) : null}
    </aside>
  );
}

export function useSidebarCollapsed() {
  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem(COLLAPSE_KEY) === 'true',
  );
  const toggle = () => {
    setCollapsed((current) => {
      localStorage.setItem(COLLAPSE_KEY, String(!current));
      return !current;
    });
  };
  return { collapsed, toggle };
}
