import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AlertCircle, ArrowRight, Eye, EyeOff, Lock, Mail, Recycle, Route, ShieldCheck, Warehouse } from 'lucide-react';
import { useAuth } from '@/auth/AuthContext';
import { Logo } from '@/layout/Logo';
import { Button } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';
import { ApiError } from '@/lib/api';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Link } from 'react-router-dom';

const loginSchema = z.object({
  email: z.string().min(1, 'Email is required').email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  const onSubmit = async (values: LoginForm) => {
    setServerError(null);
    try {
      await signIn(values.email, values.password);
      navigate('/', { replace: true });
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError(error.message);
      } else {
        setServerError('Unable to reach the ReturnLoop API. Is the backend running?');
      }
    }
  };

  return (
    <div className="auth-page relative flex min-h-screen items-center justify-center overflow-hidden bg-jet px-4 py-10">
      <div className="pointer-events-none absolute inset-0 bg-hero-sheen" aria-hidden />
      <div className="pointer-events-none absolute inset-0 grid-noise opacity-60" aria-hidden />

      <div className="login-layout relative z-10 grid w-full max-w-6xl items-center gap-8 lg:grid-cols-[1.05fr_.95fr]">
       <aside className="login-story relative hidden overflow-hidden rounded-[30px] border border-edge p-8 sm:block sm:p-10 lg:min-h-[640px]">
        <div className="login-story-orb" aria-hidden="true" />
        <div className="login-animated-loop" aria-hidden="true"><span className="login-loop-track"/><span className="login-loop-core"><Logo size={25}/></span><span className="login-loop-parcel">RL</span><span className="login-loop-beam"/></div>
        <div className="relative z-10 flex h-full flex-col">
         <Link to="/" className="inline-flex w-fit items-center gap-3"><Logo size={40}/><span className="text-xl font-bold tracking-tight text-ink-primary">Return<span className="text-cerulean-light">Loop</span></span></Link>
         <p className="mt-16 inline-flex w-fit items-center gap-2 rounded-full border border-cerulean/25 bg-cerulean/10 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[.16em] text-cerulean-light"><span className="h-1.5 w-1.5 animate-pulse-soft rounded-full bg-ok"/> Reverse logistics, connected</p>
         <h1 className="mt-5 max-w-lg text-4xl font-semibold leading-tight tracking-[-.04em] text-ink-primary lg:text-5xl">Make every return a smarter next move.</h1>
         <p className="mt-4 max-w-lg text-base leading-7 text-ink-secondary">One workspace for return intake, warehouse inspection, recovery decisions, pickups and partner handoffs.</p>
         <div className="mt-9 grid gap-3 sm:grid-cols-2">
          <div className="login-story-feature"><Warehouse/><span><b>Know what is in the warehouse</b><small>Inventory, returns and inspections</small></span></div>
          <div className="login-story-feature"><Recycle/><span><b>Find the next best outcome</b><small>Restock, resale, repair or recycle</small></span></div>
          <div className="login-story-feature"><Route/><span><b>Keep pickups on track</b><small>Routes, partners and status updates</small></span></div>
          <div className="login-story-feature"><ShieldCheck/><span><b>See value with context</b><small>Explainable recovery and reports</small></span></div>
         </div>
         <div className="login-story-foot mt-auto flex items-center gap-3 pt-10"><span className="login-story-pulse"/><span><b>One clear loop, from return to recovery</b><small>Less waste. More value.</small></span></div>
        </div>
       </aside>
       <div className="relative mx-auto w-full max-w-md">
        <div className="absolute right-0 top-0"><ThemeToggle /></div>
        <div className="mb-8 flex flex-col items-center text-center">
          <Logo size={44} />
          <h1 className="mt-4 text-2xl font-bold tracking-tight text-ink-primary sm:hidden">
            Return<span className="text-cerulean-light">Loop</span>
          </h1>
          <p className="mt-1.5 text-sm text-ink-secondary sm:hidden">Less Waste. More Value.</p>
          <p className="mt-4 max-w-xs text-xs leading-relaxed text-ink-muted sm:hidden">
            Reverse-logistics intelligence for recovering the maximum value from returns and
            unsold inventory.
          </p>
        </div>

        <div className="auth-card panel-elevated rounded-[24px] p-6 shadow-panel sm:p-8">
          <h2 className="text-xl font-semibold text-ink-primary">Welcome back</h2>
          <p className="mt-1 text-sm text-ink-muted">
            Use your organisation credentials to continue.
          </p>

          {serverError ? (
            <div
              role="alert"
              className="mt-4 flex items-start gap-2 rounded-lg border border-danger/40 bg-danger/[0.08] px-3 py-2.5"
            >
              <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-danger" aria-hidden />
              <p className="text-xs leading-relaxed text-danger">{serverError}</p>
            </div>
          ) : null}

          <form onSubmit={handleSubmit(onSubmit)} className="mt-5 space-y-4" noValidate>
            <Field label="Work email" htmlFor="email" error={errors.email?.message} required>
              <div className="relative">
                <Mail
                  className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted"
                  aria-hidden
                />
                <Input
                  id="email"
                  type="email"
                  autoComplete="username"
                  className="pl-9"
                  aria-invalid={Boolean(errors.email)}
                  {...register('email')}
                />
              </div>
            </Field>

            <div className="-mt-2 flex justify-end"><Link to="/forgot-password" className="text-xs font-medium text-cerulean-light transition hover:text-cerulean">Forgot password?</Link></div>

            <Field label="Password" htmlFor="password" error={errors.password?.message} required>
              <div className="relative">
                <Lock
                  className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted"
                  aria-hidden
                />
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className="pl-9 pr-11"
                  aria-invalid={Boolean(errors.password)}
                  {...register('password')}
                />
                <button type="button" onClick={() => setShowPassword((visible) => !visible)} aria-label={showPassword ? 'Hide password' : 'Show password'} className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-md p-1.5 text-ink-muted transition hover:bg-surface-hover hover:text-ink-primary"><span className="sr-only">{showPassword ? 'Hide password' : 'Show password'}</span>{showPassword ? <EyeOff className="h-4 w-4"/> : <Eye className="h-4 w-4"/>}</button>
              </div>
            </Field>

            <Button type="submit" size="lg" loading={isSubmitting} className="w-full">
              Sign in
              <ArrowRight className="h-4 w-4" aria-hidden />
            </Button>
          </form>

          <p className="mt-5 text-center text-sm text-ink-secondary">New to ReturnLoop? <Link to="/register" className="font-semibold text-cerulean-light hover:text-cerulean">Create a workspace</Link></p>
        </div>
       </div>
      </div>
    </div>
  );
}
