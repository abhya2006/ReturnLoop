import { useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { AlertCircle, CheckCircle2, FileSpreadsheet, Upload, XCircle } from 'lucide-react';
import { importApi } from '@/api/client';
import { Card, CardBody, CardHeader } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { ApiError } from '@/lib/api';
import { cn } from '@/lib/cn';
import type { ImportReport } from '@/lib/types';

const SAMPLE = `return_number,reason,sku,quantity,condition,customer_name,source_reference
RET-2026-90001,DEFECTIVE,NWR-PH-0142,1,GOOD,Jordan Alvarez,ORD-88213
RET-2026-90001,DEFECTIVE,NWR-AU-0221,2,LIKE_NEW,Jordan Alvarez,ORD-88213
RET-2026-90002,CHANGED_MIND,NWR-CH-0663,1,GOOD,Sam Whitfield,ORD-88220`;

export function ImportPage() {
  const toast = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [mode, setMode] = useState<'returns' | 'inventory'>('returns');
  const [report, setReport] = useState<ImportReport | null>(null);

  const importMutation = useMutation({
    mutationFn: (payload: { file: File; mode: 'returns' | 'inventory' }) =>
      payload.mode === 'returns'
        ? importApi.returns(payload.file)
        : importApi.inventory(payload.file),
    onSuccess: (result) => {
      setReport(result);
      toast.success(
        'Import finished',
        `${result.rowsImported} of ${result.rowsRead} row(s) imported successfully.`,
      );
    },
    onError: (error) =>
      toast.error('The import failed', error instanceof ApiError ? error.message : undefined),
  });

  const downloadSample = () => {
    const blob = new Blob([SAMPLE], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'returnloop-returns-template.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
      <Card className="reveal">
        <CardHeader
          title="Upload a CSV"
          description="Rows are validated individually; valid rows are imported and problems are reported"
        />
        <CardBody className="space-y-4">
          <div className="flex gap-2">
            {(['returns', 'inventory'] as const).map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => {
                  setMode(option);
                  setReport(null);
                }}
                className={cn(
                  'flex-1 rounded-lg border px-3 py-2 text-[12px] font-medium capitalize transition-colors focus-visible:ring-2 focus-visible:ring-cerulean',
                  mode === option
                    ? 'border-cerulean bg-cerulean/12 text-cerulean-light'
                    : 'border-edge text-ink-muted hover:border-cerulean/50 hover:text-ink-secondary',
                )}
              >
                {option} import
              </button>
            ))}
          </div>

          <div
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => {
              event.preventDefault();
              const dropped = event.dataTransfer.files?.[0];
              if (dropped) setFile(dropped);
            }}
            className="rounded-card border border-dashed border-edge-strong bg-surface-base px-6 py-10 text-center transition-colors hover:border-cerulean/50"
          >
            <FileSpreadsheet className="mx-auto h-8 w-8 text-ink-muted" aria-hidden />
            {file ? (
              <p className="mt-3 text-[13px] font-medium text-ink-primary">{file.name}</p>
            ) : (
              <p className="mt-3 text-[13px] font-medium text-ink-primary">
                Drop a CSV file here, or choose one
              </p>
            )}
            <p className="mt-1 text-[11px] text-ink-muted">Up to 5,000 rows per file · 10 MB maximum</p>
            <input
              ref={inputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={(event) => {
                const selected = event.target.files?.[0];
                if (selected) setFile(selected);
              }}
            />
            <div className="mt-4 flex items-center justify-center gap-2">
              <Button variant="secondary" size="sm" onClick={() => inputRef.current?.click()}>
                Choose file
              </Button>
              {file ? (
                <Button variant="ghost" size="sm" onClick={() => setFile(null)} icon={<XCircle className="h-3.5 w-3.5" />}>
                  Clear
                </Button>
              ) : null}
            </div>
          </div>

          <Button
            className="w-full"
            disabled={!file}
            loading={importMutation.isPending}
            onClick={() => file && importMutation.mutate({ file, mode })}
            icon={<Upload className="h-4 w-4" />}
          >
            Run import
          </Button>

          <div className="rounded-lg border border-edge bg-surface-base px-3.5 py-3">
            <p className="text-[11px] font-semibold uppercase tracking-wide text-ink-muted">
              Expected columns
            </p>
            <p className="mt-1.5 font-mono text-[11px] leading-relaxed text-ink-secondary">
              return_number, reason, sku, quantity, condition, customer_name, source_reference
            </p>
            <p className="mt-1 font-mono text-[11px] leading-relaxed text-ink-secondary">
              quantity, product_name, category, condition, unit_value, weight_kg, location_code
            </p>
            <button
              type="button"
              onClick={downloadSample}
              className="mt-2 text-[11px] font-medium text-cerulean-light transition-colors hover:text-cerulean hover:underline"
            >
              Download a sample file
            </button>
          </div>
        </CardBody>
      </Card>

      <Card className="reveal">
        <CardHeader title="Import report" description="What was read, imported and rejected" />
        <CardBody>
          {importMutation.isError ? (
            <div
              role="alert"
              className="rounded-lg border border-danger/40 bg-danger/[0.08] px-3 py-2.5 text-xs text-danger"
            >
              {importMutation.error instanceof ApiError
                ? importMutation.error.message
                : 'The import could not be completed.'}
            </div>
          ) : !report ? (
            <div className="py-10 text-center">
              <FileSpreadsheet className="mx-auto h-8 w-8 text-ink-muted" aria-hidden />
              <p className="mt-3 text-[13px] text-ink-secondary">No import has been run yet</p>
              <p className="mt-1 text-[11px] text-ink-muted">
                Upload a file to see the row-by-row result.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-3 gap-3">
                <Stat label="Rows read" value={report.rowsRead} tone="neutral" />
                <Stat label="Imported" value={report.rowsImported} tone="success" />
                <Stat label="Rejected" value={report.errors.length} tone={report.errors.length ? 'danger' : 'neutral'} />
              </div>

              <p className="rounded-lg border border-edge bg-surface-base px-3 py-2 text-[11px] leading-relaxed text-ink-muted">
                {report.notes}
              </p>

              {report.errors.length > 0 ? (
                <div className="overflow-hidden rounded-lg border border-danger/30">
                  <table className="data-table rows-enter">
                    <thead>
                      <tr>
                        <th className="w-16">Line</th>
                        <th>Reference</th>
                        <th>Problem</th>
                      </tr>
                    </thead>
                    <tbody>
                      {report.errors.map((error, index) => (
                        <tr key={`${error.line}-${index}`}>
                          <td className="tabular-nums text-ink-muted">{error.line}</td>
                          <td className="font-mono text-[12px] text-ink-secondary">
                            {error.returnNumber ?? '—'}
                          </td>
                          <td className="text-danger">{error.message}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="flex items-start gap-2 rounded-lg border border-ok/30 bg-ok/[0.07] px-3 py-2.5">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-ok" aria-hidden />
                  <p className="text-xs text-ink-secondary">
                    Every row was imported successfully. No problems were found.
                  </p>
                </div>
              )}

              {report.errors.length > 0 ? (
                <div className="flex items-start gap-2 rounded-lg border border-warn/30 bg-warn/[0.07] px-3 py-2.5">
                  <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-warn" aria-hidden />
                  <p className="text-xs text-ink-secondary">
                    Rows that were rejected were not imported. Fix the reported problems and re-upload
                    the file; rows that already imported will be skipped by their return number.
                  </p>
                </div>
              ) : null}
            </div>
          )}
        </CardBody>
      </Card>
    </div>
  );
}

function Stat({
  label,
  value,
  tone,
}: {
  label: string;
  value: number;
  tone: 'neutral' | 'success' | 'danger';
}) {
  return (
    <div
      className={cn(
        'rounded-lg border px-3 py-2.5 text-center',
        tone === 'success' && 'border-ok/30 bg-ok/[0.07]',
        tone === 'danger' && 'border-danger/30 bg-danger/[0.07]',
        tone === 'neutral' && 'border-edge bg-surface-base',
      )}
    >
      <p className="text-lg font-bold tabular-nums text-ink-primary">{value}</p>
      <p className="mt-0.5 text-[10px] uppercase tracking-wide text-ink-muted">{label}</p>
    </div>
  );
}
