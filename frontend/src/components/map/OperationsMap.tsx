import { useMemo, useState } from 'react';
import { MapPin, Minus, Plus, Truck, Warehouse, Building2, Info } from 'lucide-react';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/format';
import type { MapMarker } from '@/lib/types';

interface Projected {
  x: number;
  y: number;
}

/**
 * Projects a coordinate into the panel's normalised space using an
 * equirectangular projection over the bounding box of the visible markers.
 */
function project(
  latitude: number,
  longitude: number,
  bounds: { minLat: number; maxLat: number; minLon: number; maxLon: number },
): Projected {
  const latSpan = Math.max(bounds.maxLat - bounds.minLat, 0.0001);
  const lonSpan = Math.max(bounds.maxLon - bounds.minLon, 0.0001);
  return {
    x: ((longitude - bounds.minLon) / lonSpan) * 100,
    y: (1 - (latitude - bounds.minLat) / latSpan) * 100,
  };
}

const statusStyles: Record<string, { dot: string; label: string }> = {
  SCHEDULED: { dot: 'bg-cerulean-light', label: 'Scheduled' },
  IN_TRANSIT: { dot: 'bg-warn', label: 'In transit' },
  COLLECTED: { dot: 'bg-rose', label: 'Collected' },
  DELIVERED: { dot: 'bg-ok', label: 'Delivered' },
  FAILED: { dot: 'bg-danger', label: 'Delayed / failed' },
};

/**
 * A clearly labelled simulated operations map.
 *
 * <p>ReturnLoop renders its own map surface with projected coordinates rather
 * than depending on a third-party tile provider, so the product works with no
 * API credentials. The banner states plainly that the routes are estimates.
 */
export function OperationsMap({ markers }: { markers: MapMarker[] }) {
  const [zoom, setZoom] = useState(1);
  const [selected, setSelected] = useState<MapMarker | null>(null);

  const bounds = useMemo(() => {
    if (markers.length === 0) {
      return { minLat: 25, maxLat: 50, minLon: -125, maxLon: -70 };
    }
    const lats = markers.map((marker) => marker.latitude);
    const lons = markers.map((marker) => marker.longitude);
    const padLat = Math.max((Math.max(...lats) - Math.min(...lats)) * 0.15, 0.5);
    const padLon = Math.max((Math.max(...lons) - Math.min(...lons)) * 0.15, 0.5);
    return {
      minLat: Math.min(...lats) - padLat,
      maxLat: Math.max(...lats) + padLat,
      minLon: Math.min(...lons) - padLon,
      maxLon: Math.max(...lons) + padLon,
    };
  }, [markers]);

  const positioned = useMemo(
    () =>
      markers.map((marker) => ({
        marker,
        position: project(marker.latitude, marker.longitude, bounds),
      })),
    [markers, bounds],
  );

  const legend = [
    { label: 'Scheduled', dot: 'bg-cerulean-light' },
    { label: 'In transit', dot: 'bg-warn' },
    { label: 'Collected', dot: 'bg-rose' },
    { label: 'Delivered', dot: 'bg-ok' },
    { label: 'Delayed / failed', dot: 'bg-danger' },
  ];

  return (
    <div className="relative">
      <div className="mb-3 flex items-start gap-2 rounded-lg border border-cerulean/25 bg-cerulean/[0.06] px-3 py-2">
        <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-cerulean-light" aria-hidden />
        <p className="text-[11px] leading-relaxed text-ink-secondary">
          <span className="font-medium text-ink-primary">Simulated map.</span> Positions are plotted from
          the coordinates stored on each pickup. Route distances and durations elsewhere in the product
          are great-circle estimates, not live navigation data.
        </p>
      </div>

      <div className="relative overflow-hidden rounded-card border border-edge bg-[#070C10]">
        <div className="grid-noise absolute inset-0 opacity-70" aria-hidden />

        <div
          className="relative h-[360px] w-full origin-center transition-transform duration-300"
          style={{ transform: `scale(${zoom})` }}
        >
          {positioned.map(({ marker, position }) => {
            const style = statusStyles[marker.status] ?? { dot: 'bg-ink-muted', label: marker.status };
            const Icon = marker.type === 'WAREHOUSE' ? Warehouse : marker.type === 'PARTNER' ? Building2 : Truck;
            const isSelected = selected?.id === marker.id;
            return (
              <button
                key={marker.id}
                type="button"
                onClick={() => setSelected(isSelected ? null : marker)}
                title={`${marker.name} — ${style.label}`}
                className="group absolute -translate-x-1/2 -translate-y-1/2 focus-visible:outline-none"
                style={{ left: `${position.x}%`, top: `${position.y}%` }}
              >
                <span
                  className={cn(
                    'flex h-7 w-7 items-center justify-center rounded-full border-2 border-jet shadow-panel transition-transform group-hover:scale-110',
                    style.dot,
                    isSelected && 'scale-125 ring-2 ring-rose',
                  )}
                >
                  <Icon className="h-3.5 w-3.5 text-jet" aria-hidden />
                </span>
                <span className="pointer-events-none absolute left-1/2 top-full mt-1 -translate-x-1/2 whitespace-nowrap rounded border border-edge bg-surface-elevated px-1.5 py-0.5 text-[10px] font-medium text-ink-primary opacity-0 transition-opacity group-hover:opacity-100">
                  {marker.reference}
                </span>
              </button>
            );
          })}
        </div>

        <div className="absolute right-3 top-3 flex flex-col gap-1">
          <button
            type="button"
            onClick={() => setZoom((value) => Math.min(value + 0.25, 2.5))}
            aria-label="Zoom in"
            className="flex h-7 w-7 items-center justify-center rounded-lg border border-edge bg-surface-elevated/90 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <Plus className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            onClick={() => setZoom((value) => Math.max(value - 0.25, 0.75))}
            aria-label="Zoom out"
            className="flex h-7 w-7 items-center justify-center rounded-lg border border-edge bg-surface-elevated/90 text-ink-secondary transition-colors hover:border-cerulean/60 hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
          >
            <Minus className="h-3.5 w-3.5" />
          </button>
        </div>

        {positioned.length === 0 ? (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-center">
            <MapPin className="h-6 w-6 text-ink-muted" aria-hidden />
            <p className="text-xs text-ink-muted">No pickup locations to display yet.</p>
          </div>
        ) : null}

        {selected ? (
          <div className="absolute bottom-3 left-3 right-3 animate-fade-up rounded-lg border border-edge bg-surface-elevated/95 p-3 backdrop-blur sm:left-3 sm:right-auto sm:w-72">
            <div className="flex items-start justify-between gap-2">
              <div className="min-w-0">
                <p className="truncate text-[12px] font-semibold text-ink-primary">
                  {selected.name}
                </p>
                <p className="mt-0.5 text-[11px] text-ink-muted">
                  {selected.reference} · {statusStyles[selected.status]?.label ?? selected.statusLabel}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setSelected(null)}
                className="shrink-0 rounded px-1 text-[11px] text-ink-muted hover:text-ink-primary focus-visible:ring-2 focus-visible:ring-cerulean"
              >
                Close
              </button>
            </div>
            <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-[11px]">
              <dt className="text-ink-muted">Latitude</dt>
              <dd className="text-right tabular-nums text-ink-secondary">
                {selected.latitude.toFixed(4)}
              </dd>
              <dt className="text-ink-muted">Longitude</dt>
              <dd className="text-right tabular-nums text-ink-secondary">
                {selected.longitude.toFixed(4)}
              </dd>
              <dt className="text-ink-muted">Scheduled</dt>
              <dd className="text-right text-ink-secondary">
                {selected.scheduledDate ? formatDate(selected.scheduledDate) : '—'}
              </dd>
            </dl>
          </div>
        ) : null}
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1.5">
        {legend.map((entry) => (
          <span key={entry.label} className="inline-flex items-center gap-1.5 text-[11px] text-ink-muted">
            <span className={cn('h-2 w-2 rounded-full', entry.dot)} aria-hidden />
            {entry.label}
          </span>
        ))}
      </div>
    </div>
  );
}
