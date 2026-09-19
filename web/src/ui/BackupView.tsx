import { useState, type ChangeEvent } from 'react';

import { UI_GROUPS, type MergeSummary, type TableName } from '@/domain/backup';
import type { BackupState } from '@/state/useBackup';

const buttonClass =
  'rounded-lg bg-sky-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-sky-400 disabled:opacity-50';

const sectionLabels: Record<TableName, string> = {
  exercise: 'Ejercicios',
  routine: 'Rutinas',
  routine_exercise: 'Ejercicios de rutina',
  session: 'Sesiones',
  set_entry: 'Series',
  body_metric: 'Medidas',
  app_setting: 'Ajustes',
};

function summaryRows(summary: MergeSummary): { table: TableName; inserted: number; updated: number; ignored: number; remapped: number }[] {
  return (Object.keys(summary) as TableName[])
    .map((table) => ({ table, ...summary[table] }))
    .filter((row) => row.inserted + row.updated + row.ignored + row.remapped > 0);
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

  return (
    <section className="flex flex-col gap-5">
      <div>
        <h2 className="text-xl font-semibold text-white">Respaldo</h2>
        <p className="text-xs text-slate-400">
          Exportá tus datos a un archivo o fusioná un respaldo de otro dispositivo.
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-slate-500">Exportar</h3>
        <div className="flex flex-wrap gap-3">
          {UI_GROUPS.map((group) => (
            <label key={group.id} className="flex items-center gap-2 text-sm text-slate-200">
              <input
                type="checkbox"
                checked={backup.sections.includes(group.id)}
                onChange={() => backup.toggleSection(group.id)}
              />
              {group.label}
            </label>
          ))}
        </div>
        <button type="button" className={`${buttonClass} self-start`} disabled={backup.busy} onClick={() => void backup.exportNow()}>
          Descargar respaldo
        </button>
        <p className="text-xs text-slate-500">
          Los ejercicios propios que usan tus rutinas y entrenamientos se incluyen automáticamente.
        </p>
      </div>

      <div className="flex flex-col gap-3 rounded-2xl border border-slate-700/60 bg-slate-900/60 p-4">
        <h3 className="text-sm font-semibold uppercase tracking-widest text-slate-500">Importar y fusionar</h3>
        <input
          type="file"
          accept="application/json"
          onChange={(event) => void onFileSelected(event)}
          className="text-xs text-slate-300"
        />
        {fileName && <p className="text-xs text-slate-400">Archivo: {fileName}</p>}

        {pendingJson && (
          <div className="flex flex-col gap-2 rounded-xl border border-amber-400/40 bg-amber-400/10 p-3">
            <p className="text-sm text-amber-200">
              Se van a fusionar los datos del archivo con los de este dispositivo. Las filas locales que
              no estén en el archivo no se tocan; en empates gana lo local.
            </p>
            <div className="flex gap-2">
              <button
                type="button"
                className={buttonClass}
                disabled={backup.busy}
                onClick={() => {
                  void backup.importJson(pendingJson).then(() => {
                    setPendingJson(null);
                    setFileName(null);
                  });
                }}
              >
                Fusionar
              </button>
              <button
                type="button"
                className="text-xs text-slate-400 hover:text-slate-200"
                onClick={() => {
                  setPendingJson(null);
                  setFileName(null);
                }}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}

        {backup.error && <p className="text-sm text-rose-400">{backup.error}</p>}
      </div>

      {backup.summary && (
        <div className="flex flex-col gap-2 rounded-2xl border border-emerald-400/40 bg-emerald-400/10 p-4">
          <h3 className="text-sm font-semibold uppercase tracking-widest text-emerald-200">Resumen</h3>
          {summaryRows(backup.summary).length === 0 ? (
            <p className="text-sm text-slate-200">No había filas nuevas ni más recientes: no se cambió nada.</p>
          ) : (
            <ul className="flex flex-col gap-1 text-sm text-slate-100">
              {summaryRows(backup.summary).map((row) => (
                <li key={row.table} className="flex flex-wrap gap-3">
                  <span className="font-medium">{sectionLabels[row.table]}</span>
                  <span className="font-mono text-xs text-slate-300">
                    +{row.inserted} nuevas · ~{row.updated} actualizadas · ={row.ignored} ignoradas
                    {row.remapped > 0 ? ` · ↻${row.remapped} remapeadas` : ''}
                  </span>
                </li>
              ))}
            </ul>
          )}
          <button type="button" className="self-start text-xs text-slate-400 hover:text-slate-200" onClick={backup.clearSummary}>
            Cerrar resumen
          </button>
        </div>
      )}
    </section>
  );
}
