import { useRef, type ChangeEvent } from 'react';

import type { ImportState } from '@/state/useImport';
import { IconArrowDown, IconCheck, IconDumbbell, IconPlus } from '@/ui/icons';
import {
  Button,
  Card,
  ErrorState,
  LabeledValue,
  LoadingState,
  SectionHeader,
  StatTile,
} from '@/ui/primitives';

/**
 * Importar entrenamientos de Huawei Health.
 *
 * Se elige la carpeta de la exportación, la app la lee y muestra qué encontró antes de escribir
 * nada: cantidad, rango de fechas, tipos y cuántos ya están en FitLog.
 */
export function ImportView({ importer }: { importer: ImportState }) {
  const inputRef = useRef<HTMLInputElement>(null);

  async function onFolderSelected(event: ChangeEvent<HTMLInputElement>) {
    const files = [...(event.target.files ?? [])].filter((file) => {
      const name = file.name.toLowerCase();
      return name.endsWith('.json') || name.endsWith('.gpx');
    });
    const contents = await Promise.all(
      files.map(async (file) => ({ name: file.name, content: await file.text() }))
    );
    event.target.value = '';
    await importer.readFiles(contents);
  }

  return (
    <div className="flex flex-col gap-4">
      <Card tone="data">
        <p className="text-ink text-sm font-semibold">Traé tus entrenamientos de Huawei Health o GPX</p>
        <p className="text-muted mt-1 text-sm">
          Elegí la carpeta de la exportación de Huawei Health (o el ZIP ya descomprimido) o los
          archivos GPX de tu reloj. Se leen los dos formatos, se pueden mezclar y nada se escribe
          hasta que confirmes.
        </p>
      </Card>

      <input
        ref={inputRef}
        type="file"
        multiple
        // El selector de carpeta del navegador: la exportación trae muchos JSON.
        {...({ webkitdirectory: 'true' } as Record<string, string>)}
        className="hidden"
        onChange={(event) => void onFolderSelected(event)}
      />

      <Button
        icon={<IconPlus className="size-4" />}
        onClick={() => inputRef.current?.click()}
        className="self-start"
      >
        {importer.step === 'empty' ? 'Elegir carpeta (Huawei Health o GPX)' : 'Elegir otra carpeta'}
      </Button>

      {importer.error ? <ErrorState message={importer.error} /> : null}
      {importer.reading ? <LoadingState message="Leyendo la exportación…" /> : null}

      {importer.step === 'preview' ? (
        <>
          <SectionHeader title="Qué se encontró" />

          {importer.workouts.length === 0 ? (
            <Card>
              <p className="text-ink text-sm font-semibold">No se encontraron entrenamientos</p>
              <p className="text-muted mt-1 text-sm">
                Revisá que la carpeta sea la de la exportación de Huawei Health (la que trae los
                archivos de entrenamientos).
              </p>
            </Card>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-3">
                <StatTile
                  label="Entrenamientos"
                  value={String(importer.workouts.length)}
                  icon={<IconDumbbell className="size-4" />}
                />
                <StatTile
                  label="Ya en FitLog"
                  value={String(importer.alreadyImported)}
                  icon={<IconCheck className="size-4" />}
                />
              </div>

              <Card className="!p-4">
                <div className="flex flex-col gap-2">
                  <LabeledValue
                    label="Desde"
                    value={importer.firstAtMs === null ? '—' : formatImportDate(importer.firstAtMs)}
                  />
                  <LabeledValue
                    label="Hasta"
                    value={importer.lastAtMs === null ? '—' : formatImportDate(importer.lastAtMs)}
                  />
                  <LabeledValue label="Archivos leídos" value={String(importer.filesRead)} />
                </div>
              </Card>

              <SectionHeader title="Tipos de deporte" />
              <ul className="flex flex-col gap-3">
                {importer.sports.map((sport) => (
                  <li key={sport.name}>
                    <Card className="!p-4">
                      <div className="flex items-center justify-between gap-3">
                        <span className="text-ink text-sm font-semibold">{sport.name}</span>
                        <span className="text-muted fl-num text-sm">{sport.count}</span>
                      </div>
                    </Card>
                  </li>
                ))}
              </ul>

              <Button
                icon={<IconArrowDown className="size-4" />}
                disabled={!importer.canImport}
                onClick={() => void importer.runImport()}
                className="self-start"
              >
                {importer.pending > 0
                  ? `Importar ${importer.pending} entrenamientos`
                  : 'No hay nada nuevo para importar'}
              </Button>

              {importer.importing ? <LoadingState message="Importando entrenamientos…" /> : null}
            </>
          )}
        </>
      ) : null}

      {importer.step === 'done' ? (
        <>
          <SectionHeader title="Resultado" />
          <Card tone="accent">
            <p className="text-accent-text text-sm font-semibold">Importación terminada</p>
            <div className="mt-3 flex flex-col gap-2">
              <LabeledValue
                label="Sesiones agregadas"
                value={String(importer.result?.imported ?? 0)}
              />
              <LabeledValue label="Ya estaban" value={String(importer.result?.skipped ?? 0)} />
            </div>
          </Card>
          <p className="text-muted text-xs">
            Las sesiones importadas no tienen series: Huawei Health no exporta peso ni reps. Podés
            abrirlas desde Entrenar y completarlas.
          </p>
          <Button variant="secondary" onClick={importer.reset} className="self-start">
            Importar otra exportación
          </Button>
        </>
      ) : null}
    </div>
  );
}

function formatImportDate(timestampMs: number): string {
  return new Intl.DateTimeFormat('es', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(timestampMs);
}
