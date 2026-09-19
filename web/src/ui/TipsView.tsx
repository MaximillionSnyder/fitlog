import { useState } from 'react';

import { TIP_LIMIT, type Tip } from '@/domain/insights';
import type { CatalogState } from '@/state/useCatalog';
import type { TipsPreset, TipsState } from '@/state/useTips';
import { IconSpark } from '@/ui/icons';
import {
  Button,
  Card,
  Chip,
  EmptyState,
  ErrorState,
  LoadingState,
  SectionHeader,
} from '@/ui/primitives';

const presetLabels: Record<TipsPreset, string> = {
  '30d': 'Últimos 30 días',
  '90d': 'Últimos 90 días',
};

const severityStyles: Record<Tip['severity'], string> = {
  warning: 'bg-warning-soft text-warning',
  info: 'bg-data-soft text-data',
  success: 'bg-success-soft text-success',
};

const severityLabels: Record<Tip['severity'], string> = {
  warning: 'Atención',
  info: 'Info',
  success: 'Logro',
};

/** Observaciones del periodo: cada tip es una tarjeta con su severidad. */
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
    <div className="flex flex-col gap-4">
      <p className="text-muted flex items-center gap-2 text-sm">
        <IconSpark className="text-accent-text size-4 shrink-0" />
        Observaciones generadas con reglas fijas sobre tus entrenamientos.
      </p>

      <div className="flex flex-wrap gap-2">
        {(Object.keys(presetLabels) as TipsPreset[]).map((option) => (
          <Chip
            key={option}
            label={presetLabels[option]}
            active={tips.preset === option}
            onClick={() => tips.selectPreset(option)}
          />
        ))}
      </div>

      {tips.error ? <ErrorState message={tips.error} /> : null}
      {tips.loading ? <LoadingState message="Analizando entrenamientos…" /> : null}

      {!tips.loading && tips.tips.length === 0 ? (
        <EmptyState
          title="Sin observaciones"
          message="No hay observaciones para este periodo. Seguí registrando entrenamientos."
        />
      ) : null}

      <ul className="flex flex-col gap-3">
        {visible.map((tip, index) => (
          <li
            key={`${tip.kind}-${tip.subject ?? 'global'}-${index}`}
            className="rounded-card border-line bg-surface border p-4"
          >
            <p className="flex flex-wrap items-center gap-2 text-[0.6875rem] font-semibold tracking-[0.12em] uppercase">
              <span className={`rounded-full px-2 py-0.5 ${severityStyles[tip.severity]}`}>
                {severityLabels[tip.severity]}
              </span>
              {subjectLabel(tip) ? (
                <span className="text-muted normal-case">{subjectLabel(tip)}</span>
              ) : null}
            </p>
            <p className="text-ink mt-2 text-sm">{tip.message}</p>
          </li>
        ))}
      </ul>

      {tips.tips.length > TIP_LIMIT ? (
        <Button
          variant="ghost"
          className="self-start"
          onClick={() => setShowAll((current) => !current)}
        >
          {showAll ? 'Mostrar menos' : `Ver los ${tips.tips.length} consejos`}
        </Button>
      ) : null}

      {tips.tips.length > 0 ? (
        <SectionHeader title="Periodo" trailing={`${tips.tips.length} observaciones`} />
      ) : null}

      {tips.tips.length > 0 ? (
        <Card className="!p-4">
          <p className="text-muted text-xs">
            Los tips se calculan sobre las series efectivas (sin calentamiento) del periodo elegido y
            no modifican tus datos.
          </p>
        </Card>
      ) : null}
    </div>
  );
}
