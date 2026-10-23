import { useState, type ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, ArrowRight, Building2, CheckCircle2, KeyRound, LockKeyhole, Mail, UserRound } from 'lucide-react';
import { authApi } from '@/api/client';
import { useAuth } from '@/auth/AuthContext';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { Button } from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Form';
import { Logo } from '@/layout/Logo';
import { ApiError } from '@/lib/api';

const registerSchema = z.object({ organizationName: z.string().trim().min(2, 'Enter your organisation name').max(160), fullName: z.string().trim().min(2, 'Enter your name').max(160), email: z.string().email('Enter a valid email'), password: z.string().min(10, 'Use at least 10 characters'), confirmPassword: z.string() }).refine(v => v.password === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });
const emailSchema = z.object({ email: z.string().email('Enter a valid email') });
const resetCodeSchema = z.object({ email: z.string().email(), code: z.string().trim().regex(/^\d{6}$/, 'Enter the 6-digit code from your email') });
const resetSchema = z.object({ email: z.string().email(), code: z.string().trim().min(6, 'Enter the reset code'), newPassword: z.string().min(10, 'Use at least 10 characters'), confirmPassword: z.string() }).refine(v => v.newPassword === v.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });
type RegisterValues = z.infer<typeof registerSchema>;
type EmailValues = z.infer<typeof emailSchema>;
type ResetValues = z.infer<typeof resetSchema>;

export function AccountPage({ mode }: { mode: 'register' | 'forgot' }) {
  return mode === 'register' ? <RegisterPage /> : <ForgotPasswordPage />;
}

function AuthFrame({ children, title, subtitle }: { children: ReactNode; title: string; subtitle: string }) {
  return <main className="auth-page relative flex min-h-screen items-center justify-center overflow-hidden px-4 py-12"><div className="auth-glow pointer-events-none absolute -left-40 top-0 h-[500px] w-[500px] rounded-full bg-cerulean/15 blur-[120px]" /><div className="auth-glow auth-glow-rose pointer-events-none absolute -bottom-48 -right-24 h-[460px] w-[460px] rounded-full bg-rose/10 blur-[110px]" /><div className="relative z-10 w-full max-w-[460px] animate-fade-up"><div className="mb-7 flex items-center justify-between"><Link to="/" className="flex items-center gap-2.5"><Logo size={34} /><span className="text-base font-bold text-ink-primary">Return<span className="text-cerulean-light">Loop</span></span></Link><ThemeToggle /></div><section className="auth-card rounded-[24px] border border-edge bg-surface-elevated/90 p-6 shadow-panel backdrop-blur-xl sm:p-8"><div className="mb-6"><h1 className="text-2xl font-semibold tracking-tight text-ink-primary">{title}</h1><p className="mt-2 text-sm leading-6 text-ink-secondary">{subtitle}</p></div>{children}</section><p className="mt-6 text-center text-[11px] text-ink-muted">By continuing, you agree to use ReturnLoop responsibly for your organisation.</p></div></main>;
}

function RegisterPage() {
  const { register: createAccount } = useAuth();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const form = useForm<RegisterValues>({ resolver: zodResolver(registerSchema) });
  const submit = async (values: RegisterValues) => {
    setServerError('');
    try { await createAccount({ organizationName: values.organizationName, fullName: values.fullName, email: values.email, password: values.password }); navigate('/', { replace: true }); }
    catch (error) { setServerError(error instanceof ApiError ? error.message : 'Could not reach the server. Start the backend and try again.'); }
  };
  return <AuthFrame title="Create your workspace" subtitle="Set up your organisation and invite your team when you’re ready.">
    {serverError && <p role="alert" className="mb-4 rounded-xl border border-danger/30 bg-danger/10 p-3 text-sm text-danger">{serverError}</p>}
    <form onSubmit={form.handleSubmit(submit)} className="space-y-4" noValidate>
      <Field label="Organisation name" error={form.formState.errors.organizationName?.message} required><div className="relative"><Building2 className="auth-input-icon" /><Input autoComplete="organization" className="auth-input" placeholder="Northwind Retail" {...form.register('organizationName')} /></div></Field>
      <Field label="Your name" error={form.formState.errors.fullName?.message} required><div className="relative"><UserRound className="auth-input-icon" /><Input autoComplete="name" className="auth-input" placeholder="Amara Okafor" {...form.register('fullName')} /></div></Field>
      <Field label="Work email" error={form.formState.errors.email?.message} required><div className="relative"><Mail className="auth-input-icon" /><Input type="email" autoComplete="email" className="auth-input" placeholder="you@company.com" {...form.register('email')} /></div></Field>
      <Field label="Password" error={form.formState.errors.password?.message} required><div className="relative"><LockKeyhole className="auth-input-icon" /><Input type="password" autoComplete="new-password" className="auth-input" placeholder="At least 10 characters" {...form.register('password')} /></div></Field>
      <Field label="Confirm password" error={form.formState.errors.confirmPassword?.message} required><Input type="password" autoComplete="new-password" placeholder="Enter password again" {...form.register('confirmPassword')} /></Field>
      <Button type="submit" size="lg" loading={form.formState.isSubmitting} className="mt-2 w-full">Create workspace <ArrowRight className="h-4 w-4" /></Button>
    </form>
    <div className="mt-6 border-t border-edge pt-5 text-center text-sm text-ink-secondary">Already have an account? <Link to="/login" className="font-semibold text-cerulean-light hover:text-cerulean">Sign in</Link></div>
  </AuthFrame>;
}

function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [verified, setVerified] = useState(false);
  const [complete, setComplete] = useState(false);
  const [error, setError] = useState('');
  const emailForm = useForm<EmailValues>({ resolver: zodResolver(emailSchema) });
  const codeForm = useForm<{ email: string; code: string }>({ resolver: zodResolver(resetCodeSchema), defaultValues: { email: '', code: '' } });
  const resetForm = useForm<ResetValues>({ resolver: zodResolver(resetSchema) });

  const requestReset = async (values: EmailValues) => {
    setError('');
    try {
      await authApi.requestPasswordReset(values.email);
      setEmail(values.email);
      codeForm.reset({ email: values.email, code: '' });
      setSent(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not reach the server. Try again shortly.');
    }
  };

  const verifyCode = async (values: { email: string; code: string }) => {
    setError('');
    try {
      await authApi.verifyPasswordResetCode(values.email, values.code);
      resetForm.reset({ email: values.email, code: values.code, newPassword: '', confirmPassword: '' });
      setVerified(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not reach the server. Try again shortly.');
    }
  };

  const changePassword = async (values: ResetValues) => {
    setError('');
    try {
      await authApi.resetPassword(values.email, values.code, values.newPassword);
      setComplete(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not reach the server. Try again shortly.');
    }
  };

  const title = complete ? 'Password updated' : verified ? 'Choose a new password' : sent ? 'Verify your email' : 'Reset your password';
  const subtitle = complete
    ? 'Your password has been changed. You can sign in now.'
    : verified
      ? 'Your email code is verified. Set a new password for your workspace.'
      : sent
        ? `Enter the 6-digit code sent to ${email}. It expires in 15 minutes.`
        : 'Enter your work email and we’ll send you a one-time verification code.';

  return <AuthFrame title={title} subtitle={subtitle}>
    {error && <p role="alert" className="mb-4 rounded-xl border border-danger/30 bg-danger/10 p-3 text-sm text-danger">{error}</p>}
    {complete ? <div className="text-center"><span className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-ok/10 text-ok"><CheckCircle2 className="h-6 w-6" /></span><Link className="mt-6 inline-flex items-center gap-2 rounded-xl bg-cerulean px-5 py-3 text-sm font-semibold text-white" to="/login">Go to sign in <ArrowRight className="h-4 w-4" /></Link></div>
      : !sent ? <form onSubmit={emailForm.handleSubmit(requestReset)} className="space-y-4" noValidate><Field label="Work email" error={emailForm.formState.errors.email?.message} required><div className="relative"><Mail className="auth-input-icon" /><Input type="email" autoComplete="email" className="auth-input" placeholder="you@company.com" {...emailForm.register('email')} /></div></Field><Button type="submit" size="lg" loading={emailForm.formState.isSubmitting} className="w-full">Send verification code <ArrowRight className="h-4 w-4" /></Button></form>
      : !verified ? <form onSubmit={codeForm.handleSubmit(verifyCode)} className="space-y-4" noValidate><input type="hidden" {...codeForm.register('email')} /><Field label="6-digit email code" error={codeForm.formState.errors.code?.message} required><div className="relative"><KeyRound className="auth-input-icon" /><Input inputMode="numeric" autoComplete="one-time-code" maxLength={6} className="auth-input font-mono tracking-[0.22em]" placeholder="000000" {...codeForm.register('code', { required: 'Enter the code from your email', pattern: { value: /^\d{6}$/, message: 'Enter the 6-digit code' } })} /></div></Field><Button type="submit" size="lg" loading={codeForm.formState.isSubmitting} className="w-full">Verify code <ArrowRight className="h-4 w-4" /></Button><button type="button" onClick={() => { setSent(false); setError(''); }} className="w-full text-sm text-ink-secondary hover:text-ink-primary">Use a different email</button></form>
      : <form onSubmit={resetForm.handleSubmit(changePassword)} className="space-y-4" noValidate><Field label="New password" error={resetForm.formState.errors.newPassword?.message} required><Input type="password" autoComplete="new-password" {...resetForm.register('newPassword')} /></Field><Field label="Confirm new password" error={resetForm.formState.errors.confirmPassword?.message} required><Input type="password" autoComplete="new-password" {...resetForm.register('confirmPassword')} /></Field><Button type="submit" size="lg" loading={resetForm.formState.isSubmitting} className="w-full">Update password <ArrowRight className="h-4 w-4" /></Button></form>}
    <div className="mt-6 border-t border-edge pt-5 text-center"><Link to="/login" className="inline-flex items-center gap-2 text-sm font-medium text-ink-secondary hover:text-ink-primary"><ArrowLeft className="h-4 w-4" /> Back to sign in</Link></div>
  </AuthFrame>;
}
