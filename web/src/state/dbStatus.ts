import { create } from 'zustand';

export type DbState = 'iniciando' | 'listo' | 'sin-opfs' | 'error';

export interface DbStatus {
  readonly state: DbState;
  readonly error?: string;
  readonly schemaVersion?: number;
  readonly sqliteVersion?: string;
  readonly persistence?: 'opfs';
}

interface DbStatusStore {
  status: DbStatus;
  setStatus: (status: DbStatus) => void;
}

export const useDbStatusStore = create<DbStatusStore>((set) => ({
  status: { state: 'iniciando' },
  setStatus: (status) => set({ status }),
}));
