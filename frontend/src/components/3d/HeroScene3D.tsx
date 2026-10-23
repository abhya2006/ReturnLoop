import { useEffect, useRef } from 'react';
import { BarChart3, Package, Recycle, Truck, Warehouse } from 'lucide-react';
import './scene.css';

const cards = [
  {
    key: 'routes',
    position: 'float-card-top-left',
    icon: Truck,
    accent: 'cerulean',
    label: 'Active routes',
    value: '8 in progress',
    foot: '2h 18m to next stop',
    meter: 74,
    bob: { duration: '6.4s', delay: '0s' },
  },
  {
    key: 'value',
    position: 'float-card-top-right',
    icon: BarChart3,
    accent: 'rose',
    label: 'Value recovered',
    value: '$48,290',
    foot: '↑ 12.8% this month',
    meter: 88,
    bob: { duration: '7.2s', delay: '0.9s' },
  },
  {
    key: 'inventory',
    position: 'float-card-bottom-left',
    icon: Warehouse,
    accent: 'violet',
    label: 'Items in motion',
    value: '1,284 items',
    foot: 'Across 6 lifecycle stages',
    meter: 62,
    bob: { duration: '8s', delay: '1.6s' },
  },
  {
    key: 'waste',
    position: 'float-card-bottom-right',
    icon: Package,
    accent: 'ok',
    label: 'Waste diverted',
    value: '78% recovery',
    foot: 'Estimated from weight',
    meter: 78,
    bob: { duration: '6.8s', delay: '2.2s' },
  },
] as const;

const accentStyles: Record<string, { icon: string; edge: string; glow: string; bar: string }> = {
  cerulean: {
    icon: 'bg-cerulean/15 text-cerulean-light',
    edge: 'rgba(0, 200, 255, 0.34)',
    glow: 'rgba(0, 200, 255, 0.55)',
    bar: 'linear-gradient(90deg, #00e6ff, #0077ff)',
  },
  rose: {
    icon: 'bg-rose/15 text-rose-light',
    edge: 'rgba(255, 45, 149, 0.36)',
    glow: 'rgba(255, 45, 149, 0.55)',
    bar: 'linear-gradient(90deg, #ff6bb4, #ff2d95)',
  },
  violet: {
    icon: 'bg-violet/15 text-violet-light',
    edge: 'rgba(140, 90, 255, 0.36)',
    glow: 'rgba(140, 90, 255, 0.55)',
    bar: 'linear-gradient(90deg, #b79bff, #8c5aff)',
  },
  ok: {
    icon: 'bg-ok/15 text-ok',
    edge: 'rgba(0, 255, 196, 0.32)',
    glow: 'rgba(0, 255, 196, 0.5)',
    bar: 'linear-gradient(90deg, #00ffc4, #00b4a0)',
  },
};

/** Fixed, hand-placed dust so the field looks intentional rather than random. */
const dust = [
  { left: '12%', top: '22%', size: 3, z: -60, opacity: 0.55, drift: '11s', delay: '0s' },
  { left: '26%', top: '68%', size: 2, z: 40, opacity: 0.4, drift: '14s', delay: '1.4s' },
  { left: '44%', top: '14%', size: 2, z: 90, opacity: 0.6, drift: '9s', delay: '2.1s' },
  { left: '58%', top: '78%', size: 3, z: -30, opacity: 0.45, drift: '12s', delay: '0.7s' },
  { left: '72%', top: '30%', size: 2, z: 60, opacity: 0.5, drift: '16s', delay: '2.8s' },
  { left: '84%', top: '62%', size: 3, z: -80, opacity: 0.38, drift: '13s', delay: '1.9s' },
  { left: '8%', top: '52%', size: 2, z: 20, opacity: 0.42, drift: '15s', delay: '3.3s' },
  { left: '36%', top: '44%', size: 2, z: 120, opacity: 0.55, drift: '10s', delay: '0.4s' },
  { left: '66%', top: '12%', size: 2, z: -50, opacity: 0.4, drift: '17s', delay: '2.5s' },
  { left: '92%', top: '40%', size: 2, z: 70, opacity: 0.45, drift: '12.5s', delay: '1.1s' },
];

export function HeroScene3D() {
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const root = rootRef.current;
    if (!root) return;
    if (!window.matchMedia('(pointer: fine)').matches) return;
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    let frame = 0;
    const target = { x: 0, y: 0 };
    const current = { x: 0, y: 0 };

    const tick = () => {
      current.x += (target.x - current.x) * 0.09;
      current.y += (target.y - current.y) * 0.09;
      root.style.setProperty('--rx', `${current.x.toFixed(2)}deg`);
      root.style.setProperty('--ry', `${current.y.toFixed(2)}deg`);
      if (Math.abs(target.x - current.x) > 0.05 || Math.abs(target.y - current.y) > 0.05) {
        frame = requestAnimationFrame(tick);
      } else {
        frame = 0;
      }
    };

    const handleMove = (event: PointerEvent) => {
      const bounds = root.getBoundingClientRect();
      target.x = ((event.clientX - bounds.left) / bounds.width - 0.5) * 16;
      target.y = ((event.clientY - bounds.top) / bounds.height - 0.5) * 13;
      if (!frame) frame = requestAnimationFrame(tick);
    };

    const handleLeave = () => {
      target.x = 0;
      target.y = 0;
      if (!frame) frame = requestAnimationFrame(tick);
    };

    root.addEventListener('pointermove', handleMove);
    root.addEventListener('pointerleave', handleLeave);
    return () => {
      if (frame) cancelAnimationFrame(frame);
      root.removeEventListener('pointermove', handleMove);
      root.removeEventListener('pointerleave', handleLeave);
    };
  }, []);

  return (
    <div ref={rootRef} className="scene-root" aria-hidden="true">
      <div className="scene-glow scene-glow-cyan" />
      <div className="scene-glow scene-glow-rose" />
      <div className="scene-glow scene-glow-violet" />

      <div className="scene-stage">
        <div className="scene-floor" />
        <div className="cube-shadow" />

        {/* Orbit tracks, each with its own ring and riding satellites */}
        <div className="orbit-track track-1">
          <div className="orbit-ring" />
          <div className="orbit-plane">
            <div className="orbit-satellite" />
            <div className="orbit-satellite orbit-satellite-b" />
          </div>
        </div>
        <div className="orbit-track track-2">
          <div className="orbit-ring" />
          <div className="orbit-plane">
            <div className="orbit-satellite" />
          </div>
        </div>
        <div className="orbit-track track-3">
          <div className="orbit-ring" />
          <div className="orbit-plane">
            <div className="orbit-satellite" />
          </div>
        </div>

        {/* The cube */}
        <div className="cube-anchor">
          <div className="cube">
            <div className="cube-face cube-face-front" />
            <div className="cube-face cube-face-back" />
            <div className="cube-face cube-face-right" />
            <div className="cube-face cube-face-left" />
            <div className="cube-face cube-face-top" />
            <div className="cube-face cube-face-bottom" />
          </div>
          <div className="cube-core">
            <Recycle />
          </div>
        </div>

        {/* Ambient particles */}
        <div className="scene-dust">
          {dust.map(({ left, top, size, z, opacity, drift, delay }, i) => (
            <span
              key={i}
              style={
                {
                  left,
                  top,
                  '--size': `${size}px`,
                  '--z': `${z}px`,
                  '--opacity': opacity,
                  '--drift': drift,
                  '--delay': delay,
                } as React.CSSProperties
              }
            />
          ))}
        </div>

        {/* Floating data cards */}
        {cards.map(({ key, position, icon: Icon, accent, label, value, foot, meter, bob }) => {
          const tone = accentStyles[accent];
          return (
            <div key={key} className={`float-card ${position}`}>
              <div
                className="float-card-inner"
                style={
                  {
                    '--bob-duration': bob.duration,
                    '--bob-delay': bob.delay,
                    '--card-edge': tone.edge,
                    '--card-glow': tone.glow,
                  } as React.CSSProperties
                }
              >
                <div className="flex items-center gap-2.5">
                  <span className={`float-card-icon ${tone.icon}`}>
                    <Icon className="h-4 w-4" />
                  </span>
                  <div className="min-w-0">
                    <p className="float-card-label">{label}</p>
                    <p className="float-card-value">{value}</p>
                  </div>
                </div>
                <div className="float-card-meter">
                  <span className="block h-full rounded-full" style={{ width: `${meter}%`, background: tone.bar }} />
                </div>
                <p className="mt-2 text-[10px] text-ink-muted">{foot}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
