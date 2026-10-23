import { useEffect } from 'react';

/**
 * Reveals every `.reveal` element as it scrolls into view.
 *
 * Content often mounts *after* the route changes — query results replace
 * skeletons a beat later — so a one-shot scan would miss it and leave those
 * sections stuck at opacity 0. A MutationObserver keeps watching and picks up
 * any `.reveal` node that appears later.
 */
export function useReveal(key?: string) {
  useEffect(() => {
    const revealed = new WeakSet<Element>();

    if (!('IntersectionObserver' in window)) {
      document.querySelectorAll<HTMLElement>('.reveal').forEach((el) => el.classList.add('is-revealed'));
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('is-revealed');
            observer.unobserve(entry.target);
          }
        });
      },
      // A tiny positive threshold (rather than a negative bottom margin) means
      // an element peeking in at the bottom edge still counts as visible.
      { threshold: 0.01, rootMargin: '0px 0px 60px 0px' },
    );

    const watch = (el: HTMLElement) => {
      if (revealed.has(el) || el.classList.contains('is-revealed')) return;
      revealed.add(el);
      observer.observe(el);
    };

    const scan = () => {
      document.querySelectorAll<HTMLElement>('.reveal').forEach(watch);
    };

    scan();

    // Pick up sections that mount after their data resolves. Batched into a
    // frame so a burst of DOM churn costs one scan, not one per node.
    let scheduled = 0;
    const mutationObserver = new MutationObserver(() => {
      if (scheduled) return;
      scheduled = requestAnimationFrame(() => {
        scheduled = 0;
        scan();
      });
    });
    mutationObserver.observe(document.body, { childList: true, subtree: true });

    return () => {
      if (scheduled) cancelAnimationFrame(scheduled);
      mutationObserver.disconnect();
      observer.disconnect();
    };
  }, [key]);
}
