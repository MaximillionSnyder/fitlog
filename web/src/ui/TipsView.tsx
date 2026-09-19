import { useState } from 'react';

import { TIP_LIMIT, type Tip } from '@/domain/insights';
import type { CatalogState } from '@/state/useCatalog';
import type { TipsPreset, TipsState } from '@/state/useTips';

const presetLabels: Record<TipsPreset, string> = {
  '30d': 'Últimos 30 días',
  '90d': 'Últimos 90 días',
};

const severityStyles: Record<Tip['severity'], string> = {
  warning: 'border-amber-400/40 bg-amber-400/10 text-amber-200',
  info: 'border-sky-400/40 bg-sky-400/10 text-sky-200',
  success: 'border-emerald-400/40 bg-emerald-400/10 text-emerald-200',
};

const severityLabels: Record<Tip['severity'], string> = {
  warning: 'Atención',
  info: 'Info',
  success: 'Logro',
};

export default function TipsView({
  tips,
  catalog,
}: {
  tips: TipsState;
  catalog: CatalogState;
}) {
  const [showAll, setShowAll] = useState(false);

  const exerciseNameById = new Map(
    catalog.snapshot.exercises.map((exercise) => [exercise.id, exercise.name])
  );
  const groupNameBySlug = new Map(
    catalog.snapshot.groups.map((group) => [group.slug, group.name])
  );

  function subjectLabel(tip: Tip): string | null {
    if (tip.subject === null) return null;
    if (tip.kind === 'imbalance') {
      return groupNameBySlug.get(tip.subject) ?? tip.subject;
    }
    return exerciseNameById.get(tip.subject) ?? tip.subject;
  }

  const visible = showAll ? tips.tips : tips.tips.slice(0, TIP_LIMIT);

  return (
    <section className="flex flex-col gap-4">
      <div>
        <h2 className="text-xl font-semibold text-white">Tips</h2>
        <p className="text-xs text-slate-400">
          Observaciones sobre tus últimos entrenamientos, generadas con reglas fijas.
        </p>
      </div>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as TipsPreset[]).map((option) => (
          <button
            key={option}
            type="button"
            onClick={() => tips.selectPreset(option)}
            className={`rounded-full border px-3 py-1 text-xs ${
              tips.preset === option
                ? 'border-slate-400 bg-slate-700/60 text-white'
                : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-slate-500'
            }`}
          >
            {presetLabels[option]}
          </button>
        ))}
      </div>

      {tips.error && <p className="text-sm text-rose-400">{tips.error}</p>}
      {tips.loading && <p className="text-sm text-slate-400">Analizando entrenamientos…</p>}

      {!tips.loading && tips.tips.length === 0 && (
        <p className="rounded-2xl border border-dashed border-slate-700 p-6 text-center text-sm text-slate-400">
          No hay observaciones para este periodo. Seguí registrando entrenamientos.
        </p>
      )}

      <ul className="flex flex-col gap-2">
        {visible.map((tip, index) => (
          <li
            key={`${tip.kind}-${tip.subject ?? 'global'}-${index}`}
            className={`rounded-xl border px-4 py-3 text-sm ${severityStyles[tip.severity]}`}
          >
            <p className="flex items-center gap-2 text-xs uppercase tracking-wide">
              <span className="rounded-full bg-slate-950/40 px-2 py-0.5">
                {severityLabels[tip.severity]}
              </span>
              {subjectLabel(tip) && <span className="font-semibold">{subjectLabel(tip)}</span>}
            </p>
            <p className="mt-1 text-slate-100">{tip.message}</p>
          </li>
        ))}
      </ul>

      {tips.tips.length > TIP_LIMIT && (
        <button
          type="button"
          className="self-start text-xs text-sky-300 hover:text-sky-200"
          onClick={() => setShowAll((current) => !current)}
        >
          {showAll ? 'Mostrar menos' : `Ver los ${tips.tips.length} consejos`}
        </button>
      )}
    </section>
  );
}
