import { useEffect, useRef, useState } from 'react';

/**
 * Animates a number from 0 to `value` once, when it first scrolls into view.
 *
 * The rendered string is produced by `format`, so currency and unit suffixes
 * stay identical to the static value — only the digits move. Reduced motion
 * skips straight to the final value.
 */
export function useCountUp(value: number, duration = 900) {
  const [display, setDisplay] = useState(0);
  const ref = useRef<HTMLElement>(null);
  const started = useRef(false);

  useEffect(() => {
    if (!Number.isFinite(value)) {
      setDisplay(value);
      return;
    }

    // Counting is a number settling into place, not spatial movement, so it
    // stays for motion-averse users — just faster.
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const total = reduced ? Math.min(duration, 280) : duration;

    const node = ref.current;
    let frame = 0;
    let cancelled = false;

    const run = () => {
      const start = performance.now();
      const tick = (now: number) => {
        if (cancelled) return;
        const progress = Math.min(Math.max((now - start) / total, 0), 1);
        // Cubic ease-out: quick start, gentle settle.
        const eased = 1 - Math.pow(1 - progress, 3);
        // Rounding is the caller's job, but a negative interim value would
        // read as "-0" for small targets — clamp the travel to the real range.
        const next = value * eased;
        setDisplay(value >= 0 ? Math.max(next, 0) : Math.min(next, 0));
        if (progress < 1) frame = requestAnimationFrame(tick);
        else setDisplay(value);
      };
      frame = requestAnimationFrame(tick);
    };

    const begin = () => {
      if (started.current) return;
      started.current = true;
      run();
    };

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          begin();
          observer.disconnect();
        }
      },
      // threshold 0: the span sits inside an element that is still sliding in,
      // so requiring a visible fraction can mean it never reports as visible.
      { threshold: 0, rootMargin: '0px 0px -10px 0px' },
    );

    // If it is already on screen, start now rather than waiting for the
    // observer's first async callback — otherwise the number can sit at 0.
    const rect = node?.getBoundingClientRect();
    if (rect && rect.top < window.innerHeight && rect.bottom > 0) {
      begin();
    } else if (node) {
      observer.observe(node);
    } else {
      begin();
    }

    return () => {
      cancelled = true;
      cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, [value, duration]);

  return { display, ref };
}

/**
 * A number that counts up. `format` receives the in-flight value so the
 * caller controls decimals, currency symbol and suffix.
 */
export function CountUp({
  value,
  format,
  duration = 900,
  className,
}: {
  value: number;
  format: (n: number) => string;
  duration?: number;
  className?: string;
}) {
  const { display, ref } = useCountUp(value, duration);
  return (
    <span ref={ref as React.RefObject<HTMLSpanElement>} className={className}>
      {format(display)}
    </span>
  );
}
