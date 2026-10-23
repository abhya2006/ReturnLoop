import { useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowDown,
  ArrowRight,
  ArrowUpRight,
  BarChart3,
  CircleCheck,
  ClipboardCheck,
  Coins,
  Leaf,
  MapPinned,
  MoveRight,
  PackageCheck,
  Recycle,
  Route,
  ShieldCheck,
  Sparkles,
  Truck,
  Warehouse,
} from 'lucide-react';
import { Logo } from '@/layout/Logo';
import { ThemeToggle } from '@/components/common/ThemeToggle';
import { HeroScene3D } from '@/components/3d/HeroScene3D';
import { useReveal } from '@/lib/useReveal';

const features = [
  { icon: PackageCheck, title: 'One clear return journey', text: 'Track every item from the customer doorstep through inspection, recovery and closeout.' },
  { icon: Sparkles, title: 'Explainable recovery', text: 'Compare eight recovery paths with the costs, assumptions and net value behind each choice.' },
  { icon: Route, title: 'Smarter collections', text: 'Group nearby pickups into capacity-aware routes and keep teams moving together.' },
  { icon: Leaf, title: 'Value with less waste', text: 'See recovered value and estimated waste diversion in the same operational view.' },
];

const journey = [
  { icon: PackageCheck, step: '01', title: 'Capture every return', text: 'Log returned and surplus goods, keep customer and item details together, and see each case move forward.' },
  { icon: Warehouse, step: '02', title: 'Receive & inspect', text: 'Coordinate warehouse intake, condition checks and inventory so good stock does not get lost in the pile.' },
  { icon: Sparkles, step: '03', title: 'Choose the next best path', text: 'Compare restock, resale, repair, refurbishment, donation and recycling with estimated costs and recovery value.' },
  { icon: Coins, step: '04', title: 'Recover value', text: 'Route suitable goods to resale or restock, track partner work and measure recovered value. Outcomes depend on condition and demand.' },
];

export function LandingPage() {
  // Same reveal system as the rest of the product — the marketing page has no
  // reason to behave differently from the app.
  useReveal('landing');

  useEffect(() => {
    if (!window.matchMedia('(pointer: fine)').matches) return;
    let frame = 0;
    let targetX = window.innerWidth / 2;
    let targetY = window.innerHeight / 3;
    let glowX = targetX;
    let glowY = targetY;
    const trackGlow = () => {
      glowX += (targetX - glowX) * 0.085;
      glowY += (targetY - glowY) * 0.085;
      document.documentElement.style.setProperty('--glow-x', `${glowX}px`);
      document.documentElement.style.setProperty('--glow-y', `${glowY}px`);
      if (Math.abs(targetX - glowX) > 0.5 || Math.abs(targetY - glowY) > 0.5) frame = requestAnimationFrame(trackGlow);
      else frame = 0;
    };
    const move = (event: PointerEvent) => {
      targetX = event.clientX;
      targetY = event.clientY;
      if (!frame) frame = requestAnimationFrame(trackGlow);
    };
    window.addEventListener('pointermove', move, { passive: true });
    return () => {
      if (frame) cancelAnimationFrame(frame);
      window.removeEventListener('pointermove', move);
    };
  }, []);

  return (
    <div className="min-h-screen overflow-x-hidden bg-jet text-ink-primary">
      <header className="relative z-20 mx-auto flex max-w-7xl items-center justify-between px-5 py-5 lg:px-8">
        <Link to="/" className="flex items-center gap-3">
          <Logo size={36} />
          <span className="text-lg font-bold tracking-tight">
            Return<span className="bg-gradient-to-r from-cyan to-rose bg-clip-text text-transparent">Loop</span>
          </span>
        </Link>
        <nav className="hidden items-center gap-8 text-sm text-ink-secondary md:flex">
          <a href="#features" className="transition hover:text-cyan">Features</a>
          <a href="#how" className="transition hover:text-cyan">How it works</a>
          <a href="#impact" className="transition hover:text-cyan">Impact</a>
        </nav>
        <div className="flex items-center gap-2">
          <ThemeToggle />
          <Link to="/login" className="hidden rounded-xl px-4 py-2.5 text-sm font-semibold text-ink-secondary transition hover:text-ink-primary sm:inline-flex">
            Sign in
          </Link>
          <Link
            to="/register"
            className="button-sheen inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-cyan via-cerulean to-rose px-5 py-2.5 text-sm font-semibold text-white shadow-glow transition hover:-translate-y-0.5"
          >
            Get Started <ArrowUpRight className="h-4 w-4" />
          </Link>
        </div>
      </header>

      <main>
        {/* HERO */}
        <section className="relative mx-auto grid max-w-7xl items-center gap-12 px-5 pb-20 pt-8 lg:grid-cols-2 lg:gap-8 lg:px-8 lg:pb-28 lg:pt-10">
          <div className="hero-orb hero-orb-cyan pointer-events-none absolute -left-40 top-0 h-[460px] w-[460px] rounded-full bg-cyan/20 blur-[140px]" />
          <div className="hero-orb hero-orb-rose pointer-events-none absolute -right-40 top-24 h-[380px] w-[380px] rounded-full bg-rose/20 blur-[140px]" />
          <div className="hero-orb hero-orb-violet pointer-events-none absolute bottom-0 left-1/3 h-[300px] w-[300px] rounded-full bg-violet/15 blur-[130px]" />

          <div className="relative z-10 landing-enter">
            <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-cyan/40 bg-gradient-to-r from-cyan/10 to-rose/10 px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.2em] text-cyan">
              <span className="h-2 w-2 animate-pulse rounded-full bg-cyan shadow-[0_0_15px_rgba(0,230,255,0.9)]" />
              Next-Gen Reverse Logistics
            </div>
            <h1 className="text-5xl font-bold leading-[1.02] tracking-[-0.06em] sm:text-6xl lg:text-[80px]">
              Give Every Return{' '}
              <span className="bg-gradient-to-r from-cyan via-violet to-rose bg-clip-text text-transparent">
                A Better Life
              </span>
            </h1>
            <p className="mt-6 max-w-xl text-base leading-8 text-ink-secondary sm:text-lg">
              An intelligent reverse-logistics platform that maximises recovery value, reduces waste, and turns
              returned goods into revenue with explainable, data-backed decisions.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <Link
                to="/register"
                className="button-sheen group inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-cyan to-rose px-7 py-3.5 text-sm font-semibold text-white shadow-glow transition hover:-translate-y-1 hover:scale-[1.03]"
              >
                Launch your workspace
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
              </Link>
              <a
                href="#features"
                className="inline-flex items-center gap-2 rounded-xl border border-cyan/40 bg-cyan/5 px-7 py-3.5 text-sm font-semibold text-cyan transition hover:-translate-y-1 hover:border-cyan hover:bg-cyan/10"
              >
                See it in action <ArrowDown className="h-4 w-4" />
              </a>
            </div>

            <div className="mt-10 flex flex-wrap gap-x-7 gap-y-3 text-xs text-ink-secondary">
              <span className="inline-flex items-center gap-2"><CircleCheck className="h-4 w-4 text-ok" /> Explainable decisions</span>
              <span className="inline-flex items-center gap-2"><ShieldCheck className="h-4 w-4 text-cyan" /> Organisation-scoped data</span>
              <span className="inline-flex items-center gap-2"><Recycle className="h-4 w-4 text-rose" /> Waste-aware by design</span>
            </div>

            <div className="mt-9 grid max-w-md grid-cols-3 gap-4 border-t border-edge/70 pt-7">
              <div>
                <p className="bg-gradient-to-r from-cyan to-rose bg-clip-text text-3xl font-bold text-transparent">8</p>
                <p className="mt-1 text-[11px] text-ink-muted">Recovery paths</p>
              </div>
              <div>
                <p className="bg-gradient-to-r from-violet to-cyan bg-clip-text text-3xl font-bold text-transparent">100%</p>
                <p className="mt-1 text-[11px] text-ink-muted">Traceable items</p>
              </div>
              <div>
                <p className="bg-gradient-to-r from-rose to-violet bg-clip-text text-3xl font-bold text-transparent">24/7</p>
                <p className="mt-1 text-[11px] text-ink-muted">Live visibility</p>
              </div>
            </div>
          </div>

          <div className="landing-enter landing-enter-visual relative h-[480px] sm:h-[540px] lg:h-[620px]">
            <HeroScene3D />
          </div>
        </section>

        {/* FEATURES */}
        <section id="features" className="relative mx-auto max-w-7xl px-5 py-20 lg:px-8">
          <div className="reveal mx-auto mb-12 max-w-3xl text-center">
            <p className="mb-4 text-xs font-semibold uppercase tracking-[0.2em] text-cyan">One connected operation</p>
            <h2 className="mb-4 text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">From first scan to final outcome</h2>
            <p className="text-base text-ink-secondary sm:text-lg">
              Keep every team aligned across the entire return lifecycle with real-time visibility and intelligent automation.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {features.map(({ icon: Icon, title, text }, i) => (
              <article
                key={title}
                className="reveal group relative overflow-hidden rounded-2xl border border-edge bg-gradient-to-br from-surface-base to-surface-elevated p-6 transition-all duration-300 hover:-translate-y-2 hover:border-cyan/50 hover:shadow-glow"
                style={{ transitionDelay: `${i * 90}ms` }}
              >
                <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-cyan/[0.07] to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100" />
                <div className="relative">
                  <span className="mb-6 inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-cyan/20 to-cerulean/20 text-cyan transition-transform duration-300 group-hover:rotate-3 group-hover:scale-110">
                    <Icon className="h-6 w-6" />
                  </span>
                  <h3 className="mb-2 text-base font-semibold text-ink-primary">{title}</h3>
                  <p className="text-sm leading-6 text-ink-muted">{text}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* HOW IT WORKS */}
        <section id="how" className="mx-auto max-w-7xl px-5 pb-20 lg:px-8">
          <div className="reveal mx-auto mb-12 max-w-3xl text-center">
            <p className="mb-4 text-xs font-semibold uppercase tracking-[0.2em] text-rose">The complete return loop</p>
            <h2 className="mb-4 text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">One journey. Maximum recovery.</h2>
            <p className="text-base text-ink-secondary sm:text-lg">
              Follow every handoff from return creation through to a completed recovery outcome.
            </p>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {journey.map(({ icon: Icon, step, title, text }, i) => (
              <article
                key={step}
                className="reveal group relative overflow-hidden rounded-2xl border border-edge bg-gradient-to-br from-surface-base to-surface-elevated p-6 transition-all duration-300 hover:-translate-y-2 hover:border-rose/50"
                style={{ transitionDelay: `${i * 90}ms` }}
              >
                <div className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-rose/10 opacity-0 blur-2xl transition-opacity duration-300 group-hover:opacity-100" />
                <div className="relative">
                  <div className="mb-6 flex items-center justify-between">
                    <span className="inline-flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-rose/20 to-cerulean/20 text-rose-light transition-transform duration-300 group-hover:scale-110">
                      <Icon className="h-6 w-6" />
                    </span>
                    <span className="bg-gradient-to-br from-cyan to-rose bg-clip-text text-4xl font-bold text-transparent opacity-25">
                      {step}
                    </span>
                  </div>
                  <h3 className="mb-2 text-base font-semibold text-ink-primary">{title}</h3>
                  <p className="text-sm leading-6 text-ink-muted">{text}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        {/* LIVE PREVIEW BAND */}
        <section className="mx-auto max-w-7xl px-5 pb-20 lg:px-8">
          <div className="reveal relative overflow-hidden rounded-[28px] border border-cerulean/25 bg-gradient-to-br from-cerulean/[0.12] via-surface-base to-violet/[0.10] p-7 sm:p-10 lg:p-12">
            <div className="pointer-events-none absolute -right-24 -top-24 h-72 w-72 rounded-full bg-cyan/15 blur-[90px]" />
            <div className="relative z-10 grid items-center gap-10 lg:grid-cols-[1fr_0.9fr]">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan">A calmer operation</p>
                <h2 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">
                  Every number has a source. Every decision has a reason.
                </h2>
                <p className="mt-4 max-w-xl text-sm leading-7 text-ink-secondary">
                  Recovery value is computed from a documented cost model, partner performance comes from completed
                  jobs, and environmental impact shows its methodology beside every figure.
                </p>
                <div className="mt-6 flex flex-wrap gap-3 text-xs text-ink-secondary">
                  <span className="inline-flex items-center gap-2 rounded-full border border-edge bg-surface-base/70 px-3 py-2">
                    <Warehouse className="h-4 w-4 text-cyan" /> Warehouse visibility
                  </span>
                  <span className="inline-flex items-center gap-2 rounded-full border border-edge bg-surface-base/70 px-3 py-2">
                    <MapPinned className="h-4 w-4 text-rose" /> Pickup & partner coordination
                  </span>
                  <span className="inline-flex items-center gap-2 rounded-full border border-edge bg-surface-base/70 px-3 py-2">
                    <ClipboardCheck className="h-4 w-4 text-ok" /> Inspection tracking
                  </span>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="rounded-2xl border border-edge bg-surface-base/80 p-4 backdrop-blur">
                  <p className="text-[10px] uppercase tracking-wider text-ink-muted">Value recovered</p>
                  <p className="mt-2 text-2xl font-bold text-ink-primary">$48.2K</p>
                  <p className="mt-1 text-[11px] text-ok">↑ 12.8% this month</p>
                </div>
                <div className="rounded-2xl border border-edge bg-surface-base/80 p-4 backdrop-blur">
                  <p className="text-[10px] uppercase tracking-wider text-ink-muted">Items in motion</p>
                  <p className="mt-2 text-2xl font-bold text-ink-primary">1,284</p>
                  <p className="mt-1 text-[11px] text-cyan">Across 6 stages</p>
                </div>
                <div className="col-span-2 rounded-2xl border border-cyan/25 bg-cyan/[0.08] p-4">
                  <div className="flex items-center gap-3">
                    <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan/15 text-cyan">
                      <Truck className="h-5 w-5" />
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-semibold text-ink-primary">Route RL-204 is on track</p>
                      <p className="mt-0.5 truncate text-[11px] text-ink-muted">8 stops · 42 km estimated · 2h 18m</p>
                    </div>
                    <MoveRight className="h-4 w-4 text-cyan" />
                  </div>
                </div>
                <div className="col-span-2 rounded-2xl border border-edge bg-surface-base/80 p-4 backdrop-blur">
                  <div className="mb-2 flex items-center justify-between text-[11px]">
                    <span className="flex items-center gap-2 text-ink-secondary"><i className="h-2 w-2 rounded-full bg-cyan" /> Recovery mix</span>
                    <span className="text-ink-muted">Last 30 days</span>
                  </div>
                  <div className="flex h-2 w-full overflow-hidden rounded-full bg-surface-hover">
                    <span className="h-full w-[52%] bg-cyan" />
                    <span className="h-full w-[26%] bg-rose" />
                    <span className="h-full w-[14%] bg-ok" />
                    <span className="h-full w-[8%] bg-violet" />
                  </div>
                  <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[10px] text-ink-muted">
                    <span className="flex items-center gap-1.5"><i className="h-1.5 w-1.5 rounded-full bg-cyan" /> Resale</span>
                    <span className="flex items-center gap-1.5"><i className="h-1.5 w-1.5 rounded-full bg-rose" /> Repair</span>
                    <span className="flex items-center gap-1.5"><i className="h-1.5 w-1.5 rounded-full bg-ok" /> Recycled</span>
                    <span className="flex items-center gap-1.5"><i className="h-1.5 w-1.5 rounded-full bg-violet" /> Other</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* IMPACT CTA */}
        <section id="impact" className="mx-auto max-w-7xl px-5 pb-24 lg:px-8">
          <div className="reveal relative overflow-hidden rounded-[32px] border border-cyan/30 bg-gradient-to-br from-cyan/[0.15] via-surface-base to-rose/[0.15] p-8 sm:p-12 lg:p-16">
            <div className="pointer-events-none absolute -right-32 -top-32 h-96 w-96 animate-pulse rounded-full bg-cyan/20 blur-[100px]" />
            <div className="pointer-events-none absolute -bottom-32 -left-32 h-96 w-96 animate-pulse rounded-full bg-rose/20 blur-[100px]" style={{ animationDelay: '1s' }} />
            <div className="relative z-10 grid items-center gap-10 lg:grid-cols-[1fr_auto]">
              <div>
                <p className="mb-4 text-xs font-semibold uppercase tracking-[0.2em] text-cyan">Start with a clearer picture</p>
                <h2 className="mb-4 text-3xl font-bold tracking-tight sm:text-4xl lg:text-5xl">Make the next move count.</h2>
                <p className="max-w-2xl text-base text-ink-secondary sm:text-lg">
                  Create a workspace for your team and bring returns, recovery choices and collection planning into one view.
                </p>
                <div className="mt-8 flex flex-wrap gap-3">
                  <span className="inline-flex items-center gap-2 rounded-full border border-ok/40 bg-ok/10 px-4 py-2 text-xs text-ok">
                    <BarChart3 className="h-4 w-4" /> Real-time analytics
                  </span>
                  <span className="inline-flex items-center gap-2 rounded-full border border-cyan/40 bg-cyan/10 px-4 py-2 text-xs text-cyan">
                    <Route className="h-4 w-4" /> Route optimisation
                  </span>
                  <span className="inline-flex items-center gap-2 rounded-full border border-rose/40 bg-rose/10 px-4 py-2 text-xs text-rose-light">
                    <Recycle className="h-4 w-4" /> Waste diversion
                  </span>
                </div>
              </div>
              <Link
                to="/register"
                className="button-sheen group inline-flex items-center justify-center gap-3 rounded-2xl bg-gradient-to-r from-cyan to-rose px-9 py-4 text-base font-semibold text-white shadow-glow transition hover:-translate-y-1 hover:scale-[1.03]"
              >
                Build your workspace
                <ArrowRight className="h-5 w-5 transition-transform group-hover:translate-x-2" />
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 border-t border-edge/70 px-5 py-8 text-xs text-ink-muted lg:px-8">
        <span>© {new Date().getFullYear()} ReturnLoop. Less waste. More value.</span>
        <span className="flex items-center gap-2">
          Powered by
          <span className="bg-gradient-to-r from-cyan to-rose bg-clip-text font-semibold text-transparent">
            intelligent reverse logistics
          </span>
        </span>
      </footer>
    </div>
  );
}
