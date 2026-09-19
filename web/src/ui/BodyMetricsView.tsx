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

const inputClass =
  'w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 py-2 text-sm text-slate-100 placeholder:text-slate-500 focus:border-sky-500 focus:outline-none';

const buttonClass =
  'rounded-lg bg-sky-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-50';

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
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold text-white">Medidas</h2>
        <p className="text-xs text-slate-400">Peso corporal y medidas, con su evolución.</p>
      </div>

      <div className="flex flex-wrap gap-2">
        {METRIC_KINDS.map((kind) => (
          <button
            key={kind}
            type="button"
            onClick={() => body.selectKind(kind as MetricKind)}
            className={`rounded-full border px-3 py-1 text-xs ${
              body.kind === kind
                ? 'border-sky-400 bg-sky-400/20 text-sky-200'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {metricKindLabel(kind as MetricKind)}
          </button>
        ))}
      </div>

      <form
        onSubmit={submit}
        className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4"
      >
        <div className="grid grid-cols-2 gap-3">
          <input
            className={inputClass}
            inputMode="decimal"
            placeholder={`Valor (${unit})`}
            value={value}
            onChange={(event) => setValue(event.target.value)}
          />
          <input
            className={inputClass}
            type="date"
            value={measuredAt}
            onChange={(event) => setMeasuredAt(event.target.value)}
          />
        </div>
        <input
          className={inputClass}
          placeholder="Notas (opcional)"
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
        />
        {(formError ?? body.error) && (
          <p className="text-sm text-rose-400">{formError ?? body.error}</p>
        )}
        <button type="submit" className={`${buttonClass} self-start`}>
          Registrar {metricKindLabel(body.kind).toLowerCase()}
        </button>
      </form>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as RangePreset[]).map((preset) => (
          <button
            key={preset}
            type="button"
            onClick={() => body.selectPreset(preset)}
            className={`rounded-full border px-3 py-1 text-xs ${
              body.preset === preset
                ? 'border-slate-400 bg-slate-700/60 text-white'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {presetLabels[preset]}
          </button>
        ))}
      </div>

      {body.loading && <p className="text-sm text-slate-400">Cargando medidas…</p>}

      <div className="grid grid-cols-4 gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4 text-center">
        <div>
          <p className="text-xs text-slate-500">Última</p>
          <p className="font-mono text-sm">
            {stats.latest === null ? '—' : formatMetricValue(stats.latest, unit)}
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Mín</p>
          <p className="font-mono text-sm">
            {stats.min === null ? '—' : formatMetricValue(stats.min, unit)}
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Máx</p>
          <p className="font-mono text-sm">
            {stats.max === null ? '—' : formatMetricValue(stats.max, unit)}
          </p>
        </div>
        <div>
          <p className="text-xs text-slate-500">Variación</p>
          <p
            className={`font-mono text-sm ${
              stats.deltaAbs === null
                ? 'text-slate-400'
                : stats.deltaAbs < 0
                  ? 'text-emerald-300'
                  : stats.deltaAbs > 0
                    ? 'text-rose-300'
                    : 'text-slate-200'
            }`}
          >
            {stats.deltaAbs === null
              ? '—'
              : `${stats.deltaAbs > 0 ? '+' : ''}${stats.deltaAbs} ${unit}`}
          </p>
        </div>
      </div>

      {!body.loading && body.series.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
          No hay medidas de {metricKindLabel(body.kind).toLowerCase()} en el periodo elegido.
        </p>
      )}

      {body.series.length > 0 && (
        <div className="rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
          <div className="flex items-baseline justify-between">
            <p className="text-xs text-slate-400">
              {metricKindLabel(body.kind)} · {body.series.length} mediciones
            </p>
            <p className="font-mono text-lg text-sky-300">
              {stats.latest === null ? '—' : formatMetricValue(stats.latest, unit)}
            </p>
          </div>
          <div className="mt-3 h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart
                data={body.series.map((point) => ({
                  label: msToDate(point.measuredAtMs).slice(0, 5),
                  value: point.value,
                }))}
                margin={{ top: 8, right: 8, bottom: 0, left: -12 }}
              >
                <CartesianGrid stroke="#1e293b" strokeDasharray="3 3" />
                <XAxis dataKey="label" tick={{ fill: '#94a3b8', fontSize: 11 }} />
                <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} width={56} domain={['auto', 'auto']} />
                <Tooltip
                  contentStyle={{
                    background: '#0f172a',
                    border: '1px solid #334155',
                    borderRadius: 8,
                    fontSize: 12,
                  }}
                  labelStyle={{ color: '#e2e8f0' }}
                  formatter={(chartValue) => [
                    formatMetricValue(Number(chartValue ?? 0), unit),
                    metricKindLabel(body.kind),
                  ]}
                />
                <Line
                  type="monotone"
                  dataKey="value"
                  stroke="#38bdf8"
                  strokeWidth={2}
                  dot={{ r: 3, fill: '#38bdf8' }}
                  activeDot={{ r: 5 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      )}

      {body.series.length > 0 && (
        <ul className="flex flex-col gap-1 text-xs text-slate-400">
          {[...body.series].reverse().map((point) => (
            <li
              key={point.id}
              className="flex items-center justify-between gap-2 rounded-lg bg-slate-900/40 px-3 py-2"
            >
              {editingId === point.id ? (
                <>
                  <input
                    className={inputClass}
                    inputMode="decimal"
                    value={editValue}
                    onChange={(event) => setEditValue(event.target.value)}
                  />
                  <input
                    className={inputClass}
                    type="date"
                    value={editDate}
                    onChange={(event) => setEditDate(event.target.value)}
                  />
                  <span className="flex gap-2">
                    <button
                      type="button"
                      className="text-xs text-emerald-300"
                      onClick={() => void saveEdit(point.id)}
                    >
                      Guardar
                    </button>
                    <button
                      type="button"
                      className="text-xs text-slate-400"
                      onClick={() => setEditingId(null)}
                    >
                      Cancelar
                    </button>
                  </span>
                </>
              ) : (
                <>
                  <span className="font-mono">{msToDate(point.measuredAtMs)}</span>
                  <span className="flex items-center gap-3">
                    <span className="font-mono text-slate-200">
                      {formatMetricValue(point.value, point.unit)}
                    </span>
                    <button
                      type="button"
                      className="text-xs text-sky-300 hover:text-sky-200"
                      onClick={() => {
                        setEditingId(point.id);
                        setEditValue(String(point.value));
                        setEditDate(msToDate(point.measuredAtMs));
                      }}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="text-xs text-rose-300 hover:text-rose-200"
                      onClick={() => void body.remove(point.id)}
                    >
                      Eliminar
                    </button>
                  </span>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
