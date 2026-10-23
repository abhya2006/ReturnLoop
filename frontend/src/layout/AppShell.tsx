import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { Sidebar, useSidebarCollapsed } from './Sidebar';
import { TopHeader } from './TopHeader';
import { GlobalSearch } from './GlobalSearch';
import { navigation } from './navigation';
import { notificationApi } from '@/api/client';
import { Logo } from './Logo';
import { useReveal } from '@/lib/useReveal';

export function AppShell() {
  const { collapsed, toggle } = useSidebarCollapsed();
  const [searchOpen, setSearchOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const location = useLocation();

  // Rebuild the observer on every navigation so new page content animates in.
  useReveal(location.pathname);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [location.pathname]);

  const { data: unread } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => notificationApi.unreadCount(),
    refetchInterval: 60_000,
  });

  const current = navigation
    .flatMap((group) => group.items)
    .find((item) =>
      item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to),
    );

  return (
    <div className="app-backdrop flex h-screen overflow-hidden bg-jet">
      <Sidebar
        collapsed={collapsed}
        onToggle={toggle}
        notificationCount={unread ?? 0}
      />

      {mobileMenuOpen ? (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="absolute inset-0 bg-jet/80 backdrop-blur-sm"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden
          />
          <nav
            aria-label="Mobile navigation"
            className="relative h-full w-[270px] animate-fade-up overflow-y-auto border-r border-edge bg-surface-base px-3 py-4"
          >
            <div className="mb-4 flex items-center justify-between px-1">
              <span className="flex items-center gap-2.5">
                <Logo size={28} />
                <span className="text-[15px] font-bold tracking-tight text-ink-primary">ReturnLoop</span>
              </span>
              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                aria-label="Close navigation menu"
                className="rounded-lg p-1.5 text-ink-muted hover:bg-surface-hover hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            {navigation.map((group) => (
              <div key={group.title} className="mb-4">
                <p className="px-3 pb-1.5 text-[10px] font-semibold uppercase tracking-widest text-ink-muted/70">
                  {group.title}
                </p>
                {group.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      onClick={() => setMobileMenuOpen(false)}
                      className={({ isActive }) =>
                        `flex items-center gap-2.5 rounded-lg px-3 py-2 text-[13px] font-medium transition-colors ${
                          isActive
                            ? 'bg-cerulean/12 text-ink-primary'
                            : 'text-ink-secondary hover:bg-surface-hover hover:text-ink-primary'
                        }`
                      }
                    >
                      <Icon className="h-4 w-4 text-ink-muted" aria-hidden />
                      {item.label}
                    </NavLink>
                  );
                })}
              </div>
            ))}
          </nav>
        </div>
      ) : null}

      <div className="flex min-w-0 flex-1 flex-col">
        <TopHeader
          onOpenMobileMenu={() => setMobileMenuOpen(true)}
          onOpenSearch={() => setSearchOpen(true)}
        />
        <main key={location.pathname} className="page-enter flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-[1600px] px-4 py-5 lg:px-6 lg:py-6">
            {current ? (
              <div className="mb-4 flex flex-wrap items-end justify-between gap-2">
                <div>
                  <h1 className="text-lg font-semibold tracking-tight text-ink-primary">
                    {current.label}
                  </h1>
                  <p className="mt-0.5 text-xs text-ink-muted">{current.description}</p>
                </div>
                <span
                  className="hidden items-center gap-1.5 rounded-full border border-ok/30 bg-ok/10 px-2.5 py-1 text-[10px] font-medium text-ok sm:inline-flex"
                  aria-hidden
                >
                  <span className="h-1.5 w-1.5 animate-pulse-soft rounded-full bg-ok" />
                  Live
                </span>
              </div>
            ) : null}
            <Outlet />
          </div>
        </main>
      </div>

      <GlobalSearch open={searchOpen} onClose={() => setSearchOpen(false)} />
    </div>
  );
}
