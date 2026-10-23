import { forwardRef, type InputHTMLAttributes, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { cn } from '@/lib/cn';

const baseField =
  'w-full rounded-lg border border-edge bg-surface-elevated px-3 py-2 text-sm text-ink-primary placeholder:text-ink-muted/70 transition-colors focus:border-cerulean focus:outline-none focus:ring-1 focus:ring-cerulean disabled:cursor-not-allowed disabled:opacity-50';

export interface FieldProps {
  label?: string;
  hint?: string;
  error?: string;
  required?: boolean;
  children: React.ReactNode;
  htmlFor?: string;
  className?: string;
}

export function Field({ label, hint, error, required, children, htmlFor, className }: FieldProps) {
  return (
    <div className={cn('space-y-1.5', className)}>
      {label ? (
        <label htmlFor={htmlFor} className="block text-sm font-medium text-ink-secondary">
          {label}
          {required ? <span className="ml-0.5 text-rose">*</span> : null}
        </label>
      ) : null}
      {children}
      {error ? (
        <p className="text-[11px] text-danger" role="alert">
          {error}
        </p>
      ) : hint ? (
        <p className="text-[11px] text-ink-muted">{hint}</p>
      ) : null}
    </div>
  );
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  function Input({ className, ...props }, ref) {
    return <input ref={ref} className={cn(baseField, className)} {...props} />;
  },
);

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...props }, ref) {
    return <textarea ref={ref} className={cn(baseField, 'min-h-[80px] resize-y', className)} {...props} />;
  },
);

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(
  function Select({ className, children, ...props }, ref) {
    return (
      <select ref={ref} className={cn(baseField, 'appearance-none pr-8', className)} {...props}>
        {children}
      </select>
    );
  },
);

export function Checkbox({
  label,
  className,
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: React.ReactNode }) {
  return (
    <label className={cn('inline-flex cursor-pointer items-center gap-2 text-xs text-ink-secondary', className)}>
      <input
        type="checkbox"
        className="h-4 w-4 rounded border-edge bg-surface-elevated text-cerulean focus:ring-cerulean focus:ring-offset-jet"
        {...props}
      />
      {label}
    </label>
  );
}
