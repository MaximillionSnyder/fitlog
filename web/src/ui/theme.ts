import { useCallback, useEffect, useState } from 'react';

export type ThemeChoice = 'system' | 'light' | 'dark';

const STORAGE_KEY = 'fitlog-theme';

function readStored(): ThemeChoice {
  if (typeof localStorage === 'undefined') return 'system';
  const value = localStorage.getItem(STORAGE_KEY);
  return value === 'light' || value === 'dark' ? value : 'system';
}

function systemPrefersDark(): boolean {
  if (typeof window === 'undefined' || !window.matchMedia) return true;
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Aplica la clase de tema en <html> y expone el modo elegido.
 *
 * `system` sigue al sistema operativo; `light` y `dark` lo fuerzan y se guardan en el dispositivo.
 * El script inline de index.html aplica la misma logica antes de montar React, para que no haya
 * destello de tema equivocado al cargar.
 */
export function applyTheme(choice: ThemeChoice, prefersDark: boolean): void {
  if (typeof document === 'undefined') return;
  const dark = choice === 'system' ? prefersDark : choice === 'dark';
  const root = document.documentElement;
  root.classList.toggle('dark', dark);
  root.classList.toggle('light', !dark);
}

export function useTheme(): {
  readonly choice: ThemeChoice;
  readonly isDark: boolean;
  setChoice(choice: ThemeChoice): void;
  toggle(): void;
} {
  const [choice, setChoiceState] = useState<ThemeChoice>(() => readStored());
  const [prefersDark, setPrefersDark] = useState<boolean>(() => systemPrefersDark());

  // El tema es estado derivado: no hace falta sincronizarlo con otro setState.
  const isDark = choice === 'system' ? prefersDark : choice === 'dark';

  // El efecto solo habla con el sistema externo (la clase en <html>).
  useEffect(() => {
    applyTheme(choice, prefersDark);
  }, [choice, prefersDark]);

  useEffect(() => {
    if (!window.matchMedia) return;
    const query = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (event: MediaQueryListEvent) => setPrefersDark(event.matches);
    query.addEventListener('change', onChange);
    return () => query.removeEventListener('change', onChange);
  }, []);

  const setChoice = useCallback((next: ThemeChoice) => {
    if (typeof localStorage !== 'undefined') localStorage.setItem(STORAGE_KEY, next);
    setChoiceState(next);
  }, []);

  const toggle = useCallback(() => {
    setChoice(isDark ? 'light' : 'dark');
  }, [isDark, setChoice]);

  return { choice, isDark, setChoice, toggle };
}
