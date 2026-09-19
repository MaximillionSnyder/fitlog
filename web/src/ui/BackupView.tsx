import { useState, type ChangeEvent } from 'react';

import { UI_GROUPS, type MergeSummary, type TableName } from '@/domain/backup';
import type { BackupState } from '@/state/useBackup';
import { IconArrowDown, IconShield } from '@/ui/icons';
import { Button, Card, ErrorState, LabeledValue, SectionHeader } from '@/ui/primitives';

/**
 * Respaldo: exportar por secciones e importar fusionando con los datos locales.
 *
 * El encabezado lo pone el shell; acá solo van las dos acciones, la confirmacion previa a fusionar
 * (nada se toca hasta que el usuario confirma) y el resumen de lo que hizo la fusion.
 */

const fieldClass =
  'rounded-field border border-line bg-surface-low px-3 py-2 text-sm text-ink placeholder:text-faint focus:border-accent focus:outline-none';

const sectionLabels: Record<TableName, string> = {
  exercise: 'Ejercicios',
  routine: 'Rutinas',
  routine_exercise: 'Ejercicios de rutina',
  session: 'Sesiones',
  set_entry: 'Series',
  body_metric: 'Medidas',
  app_setting: 'Ajustes',
};

interface SummaryRow {
  table: TableName;
  inserted: number;
  updated: number;
  ignored: number;
  remapped: number;
}

function summaryRows(summary: MergeSummary): SummaryRow[] {
  return (Object.keys(summary) as TableName[])
    .map((table) => ({ table, ...summary[table] }))
    .filter((row) => row.inserted + row.updated + row.ignored + row.remapped > 0);
}

/**
 * Una linea del resumen por tabla: lo aplicado en verde y lo rechazado (gana lo local) en rojo, con
 * el mismo layout de LabeledValue pero con el color semantico en el numero.
 */
function MergeLine({
  label,
  value,
  mark,
  tone,
}: {
  label: string;
  value: number;
  mark: string;
  tone: 'success' | 'danger' | 'data';
}) {
  const toneClass =
    tone === 'success'
      ? 'bg-success-soft text-success'
      : tone === 'danger'
        ? 'bg-danger-soft text-danger'
        : 'bg-data-soft text-data';

  return (
    <div className="flex items-center justify-between gap-3 text-sm">
      <span className="text-muted">{label}</span>
      <span className={`fl-num rounded-full px-2 py-0.5 text-xs font-semibold ${toneClass}`}>
        {mark}
        {value}
      </span>
    </div>
  );
}

export default function BackupView({ backup }: { backup: BackupState }) {
  const [fileName, setFileName] = useState<string | null>(null);
  const [pendingJson, setPendingJson] = useState<string | null>(null);

  async function onFileSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    setPendingJson(await file.text());
    event.target.value = '';
  }

  function discardSelection() {
    setPendingJson(null);
    setFileName(null);
  }

  async function mergePending() {
    if (!pendingJson) return;
    await backup.importJson(pendingJson);
    discardSelection();
  }

  const rows = backup.summary ? summaryRows(backup.summary) : [];
  const applied = rows.reduce((total, row) => total + row.inserted + row.updated, 0);
  const rejected = rows.reduce((total, row) => total + row.ignored, 0);
  const remapped = rows.reduce((total, row) => total + row.remapped, 0);

  return (
    <div className="flex flex-col gap-5">
      <Card>
        <div className="flex items-start gap-3">
          <span className="rounded-field bg-accent-soft text-accent-text grid size-10 shrink-0 place-items-center">
            <IconShield className="size-5" />
          </span>
          <div className="min-w-0">
            <p className="text-ink text-sm font-semibold">Tus datos, en un archivo tuyo</p>
            <p className="text-muted mt-1 text-sm">
              Exportá tus datos a un archivo o fusioná un respaldo de otro dispositivo. Todo queda en
              este dispositivo: el archivo lo guardás y lo compartís vos.
            </p>
          </div>
        </div>
      </Card>

      {backup.error ? <ErrorState message={backup.error} /> : null}

      <SectionHeader title="Exportar" />

      <Card>
        <p className="text-ink text-sm font-semibold">Secciones a exportar</p>
        <p className="text-muted mt-1 text-xs">Elegí qué querés incluir en el archivo.</p>

        <div className="mt-3 flex flex-wrap gap-2">
          {UI_GROUPS.map((group) => {
            const active = backup.sections.includes(group.id);
            return (
              <label
                key={group.id}
                className={`rounded-full flex items-center gap-2 border px-3 py-1.5 text-xs font-medium transition ${
                  active
                    ? 'border-accent bg-accent-soft text-accent-text'
                    : 'border-line text-muted hover:text-ink'
                }`}
              >
                <input
                  type="checkbox"
                  checked={active}
                  onChange={() => backup.toggleSection(group.id)}
                  className="accent-accent size-3.5 shrink-0"
                />
                {group.label}
              </label>
            );
          })}
        </div>

        <Button
          className="mt-4"
          disabled={backup.busy}
          onClick={() => void backup.exportNow()}
          icon={<IconArrowDown className="size-4" />}
        >
          Descargar respaldo
        </Button>

        <p className="text-faint mt-3 text-xs">
          Los ejercicios propios que usan tus rutinas y entrenamientos se incluyen automáticamente.
        </p>
      </Card>

      <SectionHeader title="Importar y fusionar" />

      <Card>
        <p className="text-ink text-sm font-semibold">Archivo de respaldo</p>
        <p className="text-muted mt-1 text-xs">
          Se aceptan archivos .json exportados desde FitLog.
        </p>

        <input
          type="file"
          accept="application/json"
          onChange={(event) => void onFileSelected(event)}
          className={`${fieldClass} mt-3 w-full file:mr-3 file:rounded-field file:border-0 file:bg-accent-soft file:px-3 file:py-1.5 file:text-xs file:font-semibold file:text-accent-text`}
        />

        {fileName ? (
          <div className="mt-2">
            <LabeledValue label="Archivo" value={fileName} />
          </div>
        ) : null}

        {pendingJson ? (
          <div className="rounded-tile mt-3 flex flex-col gap-3 border border-warning/40 bg-warning-soft p-4">
            <p className="text-warning flex items-start gap-2 text-sm">
              <IconShield className="mt-0.5 size-4 shrink-0" />
              <span>
                Se van a fusionar los datos del archivo con los de este dispositivo. Las filas locales
                que no estén en el archivo no se tocan; en empates gana lo local.
              </span>
            </p>
            <div className="flex flex-wrap gap-2">
              <Button disabled={backup.busy} onClick={() => void mergePending()}>
                Fusionar
              </Button>
              <Button variant="ghost" disabled={backup.busy} onClick={discardSelection}>
                Cancelar
              </Button>
            </div>
          </div>
        ) : null}
      </Card>

      {backup.summary ? (
        <>
          <SectionHeader title="Resumen de la fusión" />

          <Card>
            {rows.length === 0 ? (
              <p className="text-muted text-sm">
                No había filas nuevas ni más recientes: no se cambió nada.
              </p>
            ) : (
              <>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="bg-success-soft text-success rounded-full px-3 py-1 text-xs font-semibold">
                    <span className="fl-num">{applied}</span> aplicadas
                  </span>
                  <span className="bg-danger-soft text-danger rounded-full px-3 py-1 text-xs font-semibold">
                    <span className="fl-num">{rejected}</span> rechazadas
                  </span>
                  {remapped > 0 ? (
                    <span className="bg-data-soft text-data rounded-full px-3 py-1 text-xs font-semibold">
                      <span className="fl-num">{remapped}</span> remapeadas
                    </span>
                  ) : null}
                </div>

                <ul className="mt-4 flex flex-col gap-3">
                  {rows.map((row) => (
                    <li key={row.table} className="rounded-tile border-line bg-surface-low border p-4">
                      <p className="text-ink text-sm font-semibold">{sectionLabels[row.table]}</p>
                      <div className="mt-3 flex flex-col gap-1.5">
                        <MergeLine label="Nuevas" value={row.inserted} mark="+" tone="success" />
                        <MergeLine label="Actualizadas" value={row.updated} mark="~" tone="success" />
                        <MergeLine
                          label="Ignoradas (gana lo local)"
                          value={row.ignored}
                          mark="="
                          tone="danger"
                        />
                        {row.remapped > 0 ? (
                          <MergeLine
                            label="Remapeadas"
                            value={row.remapped}
                            mark="↻"
                            tone="data"
                          />
                        ) : null}
                      </div>
                    </li>
                  ))}
                </ul>
              </>
            )}

            <Button variant="ghost" className="mt-4" onClick={backup.clearSummary}>
              Cerrar resumen
            </Button>
          </Card>
        </>
      ) : null}
    </div>
  );
}
