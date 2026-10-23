import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/cn';

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'rose';
type Size = 'sm' | 'md' | 'lg';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  icon?: ReactNode;
}

const variants: Record<Variant, string> = {
  primary:
    'bg-cerulean text-white hover:bg-cerulean-light active:bg-cerulean-dark shadow-[0_0_0_1px_rgba(0,123,167,0.5),0_8px_24px_-12px_rgba(0,123,167,0.9)]',
  secondary:
    'bg-surface-elevated text-ink-primary border border-edge hover:border-cerulean/60 hover:bg-surface-hover',
  ghost: 'text-ink-secondary hover:text-ink-primary hover:bg-surface-hover border border-transparent',
  danger: 'bg-danger/15 text-danger border border-danger/40 hover:bg-danger/25',
  rose: 'bg-rose text-white hover:bg-rose-light shadow-[0_0_0_1px_rgba(255,45,149,0.5),0_8px_24px_-12px_rgba(255,45,149,0.9)]',
};

const sizes: Record<Size, string> = {
  sm: 'h-9 px-3.5 text-sm gap-1.5 rounded-lg',
  md: 'h-11 px-4.5 text-[15px] gap-2 rounded-lg',
  lg: 'h-12 px-5 text-[15px] gap-2 rounded-xl',
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { className, variant = 'primary', size = 'md', loading, icon, children, disabled, ...props },
  ref,
) {
  return (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        'inline-flex items-center justify-center font-medium transition-all duration-200 hover:-translate-y-px active:translate-y-0 active:scale-[0.98]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cerulean focus-visible:ring-offset-2 focus-visible:ring-offset-jet',
        'disabled:cursor-not-allowed disabled:opacity-50',
        variants[variant],
        sizes[size],
        className,
      )}
      {...props}
    >
      {loading ? <Loader2 className="h-4 w-4 animate-spin" aria-hidden /> : icon}
      {children}
    </button>
  );
});
