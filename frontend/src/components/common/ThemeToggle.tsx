import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/theme/ThemeContext';

export function ThemeToggle({ className = '' }: { className?: string }) {
  const { theme, toggleTheme } = useTheme();
  const next = theme === 'dark' ? 'light' : 'dark';
  return (
    <button type="button" onClick={toggleTheme} aria-label={`Switch to ${next} mode`} title={`Switch to ${next} mode`}
      className={`theme-toggle inline-flex h-10 w-10 items-center justify-center rounded-xl border border-edge bg-surface-base text-ink-secondary transition hover:-translate-y-0.5 hover:text-ink-primary ${className}`}>
      {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </button>
  );
}
