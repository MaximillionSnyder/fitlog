import { useState, type FormEvent } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import {
  METRIC_KINDS,
  formatMetricValue,
  metricKindLabel,
  unitForKind,
  type MetricKind,
} from '@/domain/body';
import type { RangePreset } from '@/domain/progress';
import type { BodyMetricsState } from '@/state/useBodyMetrics';
import { IconPlus, IconScale } from '@/ui/icons';
import {
  Button,
  Card,
  Chip,
  EmptyState,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

const inputClass =
  'w-full rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none';

const presetLabels: Record<RangePreset, string> = {
  '30d': '30 días',
  '90d': '90 días',
  all: 'Todo',
};

function todayInputValue(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function dateToMs(value: string): number {
  const parsed = new Date(`${value}T12:00:00`).getTime();
  return Number.isFinite(parsed) ? parsed : Date.now();
}

function msToDate(value: number): string {
  const date = new Date(value);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Medidas corporales: alta, edición, borrado y evolución de la medida elegida. */
export default function BodyMetricsView({ body }: { body: BodyMetricsState }) {
  const [value, setValue] = useState('');
  const [measuredAt, setMeasuredAt] = useState(todayInputValue());
  const [notes, setNotes] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');
  const [editDate, setEditDate] = useState(todayInputValue());

  const unit = unitForKind(body.kind);
  const stats = body.stats;
  const kindLabel = metricKindLabel(body.kind);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const parsed = Number(value.replace(',', '.'));
    if (!Number.isFinite(parsed)) {
      setFormError('Ingresá un número válido');
      return;
    }
    setFormError(null);
    await body.add({
      value: parsed,
      measuredAtMs: dateToMs(measuredAt),
      notes: notes.trim() === '' ? null : notes.trim(),
    });
    setValue('');
    setNotes('');
  }

  async function saveEdit(id: string) {
    const parsed = Number(editValue.replace(',', '.'));
    if (!Number.isFinite(parsed)) {
      setFormError('Ingresá un número válido');
      return;
    }
    setFormError(null);
    await body.edit(id, {
      value: parsed,
      measuredAtMs: dateToMs(editDate),
      notes: null,
    });
    setEditingId(null);
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="fl-scroll-x flex gap-2 overflow-x-auto pb-1">
        {METRIC_KINDS.map((kind) => (
          <Chip
            key={kind}
            label={metricKindLabel(kind as MetricKind)}
            active={body.kind === kind}
            onClick={() => body.selectKind(kind as MetricKind)}
          />
        ))}
      </div>

      <Card>
        <SectionHeader title={`Registrar ${kindLabel.toLowerCase()}`} />
        <form onSubmit={submit} className="mt-3 flex flex-col gap-3">
          <div className="grid grid-cols-2 gap-3">
            <input
              className={inputClass}
              inputMode="decimal"
              aria-label={`Valor en ${unit}`}
              placeholder={`Valor (${unit})`}
              value={value}
              onChange={(event) => setValue(event.target.value)}
            />
            <input
              className={inputClass}
              type="date"
              aria-label="Fecha"
              value={measuredAt}
              onChange={(event) => setMeasuredAt(event.target.value)}
            />
          </div>
          <input
            className={inputClass}
            aria-label="Notas"
            placeholder="Notas (opcional)"
            value={notes}
            onChange={(event) => setNotes(event.target.value)}
          />
          {(formError ?? body.error) ? (
            <p className="bg-danger-soft text-danger rounded-field px-3 py-2 text-sm">
              {formError ?? body.error}
            </p>
          ) : null}
          <Button type="submit" icon={<IconPlus className="size-4" />} className="self-start">
            Registrar {kindLabel.toLowerCase()}
          </Button>
        </form>
      </Card>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as RangePreset[]).map((preset) => (
          <Chip
            key={preset}
            label={presetLabels[preset]}
            active={body.preset === preset}
            onClick={() => body.selectPreset(preset)}
          />
        ))}
      </div>

      {body.loading ? <LoadingState message="Cargando medidas…" /> : null}

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatTile
          label="Última"
          value={stats.latest === null ? '—' : formatMetricValue(stats.latest, unit)}
          icon={<IconScale className="size-4" />}
          tone="data"
        />
        <StatTile
          label="Mínima"
          value={stats.min === null ? '—' : formatMetricValue(stats.min, unit)}
        />
        <StatTile
          label="Máxima"
          value={stats.max === null ? '—' : formatMetricValue(stats.max, unit)}
        />
        <StatTile
          label="Variación"
          value={
            stats.deltaAbs === null
              ? '—'
              : `${stats.deltaAbs > 0 ? '+' : ''}${stats.deltaAbs} ${unit}`
          }
          hint={stats.count === 0 ? 'Sin mediciones' : `${stats.count} mediciones`}
        />
      </div>

      {!body.loading && body.series.length === 0 ? (
        <EmptyState
          title="Sin medidas en el periodo"
          message={`No hay medidas de ${kindLabel.toLowerCase()} en el periodo elegido. Registrá una arriba o ampliá el rango.`}
          {...(body.preset === 'all'
            ? {}
            : { actionLabel: 'Ver todo', onAction: () => body.selectPreset('all') })}
        />
      ) : null}

      {body.series.length > 0 ? (
        <Card className="!p-4">
          <div className="flex items-baseline justify-between">
            <p className="text-muted text-xs">
              {kindLabel} · {body.series.length} mediciones
            </p>
            <p className="text-data fl-num text-lg font-semibold">
              {stats.latest === null ? '—' : formatMetricValue(stats.latest, unit)}
            </p>
          </div>
          <div className="mt-3">
            <ResponsiveContainer width="100%" height={224}>
              <LineChart
                data={body.series.map((point) => ({
                  label: msToDate(point.measuredAtMs).slice(5),
                  value: point.value,
                }))}
                margin={{ top: 8, right: 8, bottom: 0, left: -12 }}
              >
                <CartesianGrid stroke="var(--fl-line)" strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fill: 'var(--fl-muted)', fontSize: 11 }} />
                <YAxis
                  tick={{ fill: 'var(--fl-muted)', fontSize: 11 }}
                  width={56}
                  domain={['auto', 'auto']}
                />
                <Tooltip
                  contentStyle={{
                    background: 'var(--fl-surface)',
                    border: '1px solid var(--fl-line-strong)',
                    borderRadius: 12,
                    fontSize: 12,
                    color: 'var(--fl-ink)',
                  }}
                  labelStyle={{ color: 'var(--fl-muted)' }}
                  formatter={(chartValue) => [
                    formatMetricValue(Number(chartValue ?? 0), unit),
                    kindLabel,
                  ]}
                />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="var(--fl-data)"
                  strokeWidth={2}
                  dot={{ r: 3, fill: 'var(--fl-data)' }}
                  activeDot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>
      ) : null}

      {body.series.length > 0 ? (
        <>
          <SectionHeader title="Mediciones" trailing={`${body.series.length}`} />
          <ul className="flex flex-col gap-2">
            {[...body.series].reverse().map((point) => (
              <li
                key={point.id}
                className="rounded-tile border-line bg-surface flex flex-wrap items-center justify-between gap-2 border px-4 py-2.5 text-xs"
              >
                {editingId === point.id ? (
                  <>
                    <input
                      className={inputClass}
                      inputMode="decimal"
                      aria-label="Valor"
                      value={editValue}
                      onChange={(event) => setEditValue(event.target.value)}
                    />
                    <input
                      className={inputClass}
                      type="date"
                      aria-label="Fecha"
                      value={editDate}
                      onChange={(event) => setEditDate(event.target.value)}
                    />
                    <span className="flex gap-2">
                      <Button onClick={() => void saveEdit(point.id)}>Guardar</Button>
                      <Button variant="secondary" onClick={() => setEditingId(null)}>
                        Cancelar
                      </Button>
                    </span>
                  </>
                ) : (
                  <>
                    <span className="text-muted fl-num">{msToDate(point.measuredAtMs)}</span>
                    <span className="flex items-center gap-3">
                      <span className="text-ink fl-num">
                        {formatMetricValue(point.value, point.unit)}
                      </span>
                      <Button
                        variant="ghost"
                        onClick={() => {
                          setEditingId(point.id);
                          setEditValue(String(point.value));
                          setEditDate(msToDate(point.measuredAtMs));
                        }}
                      >
                        Editar
                      </Button>
                      <Button variant="danger" onClick={() => void body.remove(point.id)}>
                        Eliminar
                      </Button>
                    </span>
                  </>
                )}
              </li>
            ))}
          </ul>
        </>
      ) : null}
    </div>
  );
}
