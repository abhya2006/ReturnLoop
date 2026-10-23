import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Bell,
  Building2,
  ChevronDown,
  Command,
  LogOut,
  Menu,
  Search,
  Settings,
  UserRound,
} from 'lucide-react';
import { notificationApi } from '@/api/client';
import { useAuth } from '@/auth/AuthContext';
import { initials } from '@/lib/format';
import { ThemeToggle } from '@/components/common/ThemeToggle';

export function TopHeader({
  onOpenMobileMenu,
  onOpenSearch,
}: {
  onOpenMobileMenu: () => void;
  onOpenSearch: () => void;
}) {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const { data: unread } = useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: () => notificationApi.unreadCount(),
    refetchInterval: 60_000,
  });

  useEffect(() => {
    const onClick = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault();
        onOpenSearch();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onOpenSearch]);

  const unreadCount = unread ?? 0;

  return (
    <header className="sticky top-0 z-30 flex h-14 items-center gap-3 border-b border-edge bg-jet/90 px-4 backdrop-blur-md lg:px-6">
      <button
        type="button"
        onClick={onOpenMobileMenu}
        aria-label="Open navigation menu"
        className="rounded-lg p-2 text-ink-secondary transition-colors hover:bg-surface-hover hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean lg:hidden"
      >
        <Menu className="h-4 w-4" />
      </button>

      <button
        type="button"
        onClick={onOpenSearch}
        className="group flex h-9 flex-1 items-center gap-2 rounded-lg border border-edge bg-surface-base px-3 text-left transition-colors hover:border-cerulean/50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cerulean sm:max-w-md"
      >
        <Search className="h-4 w-4 shrink-0 text-ink-muted group-hover:text-ink-secondary" aria-hidden />
        <span className="flex-1 truncate text-[13px] text-ink-muted">Search returns, SKUs, partners…</span>
        <kbd className="hidden items-center gap-0.5 rounded border border-edge bg-surface-elevated px-1.5 py-0.5 font-mono text-[10px] text-ink-muted sm:flex">
          <Command className="h-2.5 w-2.5" aria-hidden />K
        </kbd>
      </button>

      <div className="ml-auto flex items-center gap-1.5">
        <ThemeToggle className="hidden h-9 w-9 sm:inline-flex" />
        <div className="hidden items-center gap-2 rounded-lg border border-edge bg-surface-base px-2.5 py-1.5 md:flex">
          <span className={`h-1.5 w-1.5 rounded-full ${unread === undefined ? 'bg-warn animate-pulse-soft' : 'bg-ok shadow-[0_0_6px_rgba(0,214,163,0.9)]'}`} aria-hidden />
          <span className="text-[11px] font-medium text-ink-secondary">{unread === undefined ? 'Connecting…' : 'API connected'}</span>
        </div>

        <div className="hidden items-center gap-1.5 rounded-lg border border-edge bg-surface-base px-2.5 py-1.5 lg:flex">
          <Building2 className="h-3.5 w-3.5 text-cerulean-light" aria-hidden />
          <span className="max-w-[180px] truncate text-[11px] font-medium text-ink-secondary">
            {user?.organizationId ? 'Northwind Retail Group' : 'Organisation'}
          </span>
        </div>

        <Link
          to="/notifications"
          aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ''}`}
          className="relative rounded-lg p-2 text-ink-secondary transition-colors hover:bg-surface-hover hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
        >
          <Bell className="h-4 w-4" />
          {unreadCount > 0 ? (
            <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose px-1 text-[9px] font-bold text-white">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          ) : null}
        </Link>

        <div className="relative" ref={menuRef}>
          <button
            type="button"
            onClick={() => setMenuOpen((open) => !open)}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            className="flex items-center gap-2 rounded-lg border border-edge bg-surface-base py-1 pl-1 pr-2 transition-colors hover:border-cerulean/50 focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <span className="flex h-6 w-6 items-center justify-center rounded-md bg-brand-gradient text-[10px] font-bold text-white">
              {initials(user?.fullName ?? 'RL')}
            </span>
            <span className="hidden text-left sm:block">
              <span className="block max-w-[140px] truncate text-[12px] font-medium leading-tight text-ink-primary">
                {user?.fullName ?? 'Signed in'}
              </span>
              <span className="block text-[10px] uppercase leading-tight tracking-wide text-ink-muted">
                {user?.role?.replace('_', ' ') ?? ''}
              </span>
            </span>
            <ChevronDown className="h-3.5 w-3.5 text-ink-muted" aria-hidden />
          </button>

          {menuOpen ? (
            <div
              role="menu"
              className="absolute right-0 top-[calc(100%+8px)] z-40 w-60 animate-fade-up rounded-card border border-edge bg-surface-elevated p-1.5 shadow-panel"
            >
              <div className="border-b border-edge px-3 py-2.5">
                <p className="truncate text-[13px] font-semibold text-ink-primary">
                  {user?.fullName}
                </p>
                <p className="truncate text-[11px] text-ink-muted">{user?.email}</p>
                {user?.jobTitle ? (
                  <p className="mt-1.5 inline-flex rounded-full border border-cerulean/35 bg-cerulean/12 px-2 py-0.5 text-[10px] font-medium text-cerulean-light">
                    {user.jobTitle}
                  </p>
                ) : null}
              </div>
              <Link
                role="menuitem"
                to="/settings"
                onClick={() => setMenuOpen(false)}
                className="flex items-center gap-2 rounded-lg px-3 py-2 text-[13px] text-ink-secondary transition-colors hover:bg-surface-hover hover:text-ink-primary"
              >
                <Settings className="h-3.5 w-3.5" aria-hidden />
                Settings
              </Link>
              <Link
                role="menuitem"
                to="/organizations"
                onClick={() => setMenuOpen(false)}
                className="flex items-center gap-2 rounded-lg px-3 py-2 text-[13px] text-ink-secondary transition-colors hover:bg-surface-hover hover:text-ink-primary"
              >
                <UserRound className="h-3.5 w-3.5" aria-hidden />
                My organisation
              </Link>
              <button
                role="menuitem"
                type="button"
                onClick={() => {
                  setMenuOpen(false);
                  signOut();
                  navigate('/login');
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-[13px] text-danger transition-colors hover:bg-danger/10"
              >
                <LogOut className="h-3.5 w-3.5" aria-hidden />
                Sign out
              </button>
            </div>
          ) : null}
        </div>
      </div>
    </header>
  );
}
