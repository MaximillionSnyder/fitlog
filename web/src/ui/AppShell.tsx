import type { ReactNode } from 'react';

import {
  isTopLevel,
  titleForView,
  topLevelDestinations,
  type View,
} from '@/ui/destinations';
import { IconBack, IconMoon, IconSun } from '@/ui/icons';
import { FitLogWordmark } from '@/ui/Logo';

/**
 * Shell de la web: rail lateral en escritorio y barra inferior en movil.
 *
 * El mismo layout que la app Android: cinco destinos de primer nivel, encabezado provisto por el
 * shell y una accion de volver en las pantallas secundarias.
 */
export function AppShell({
  view,
  onNavigate,
  onBack,
  canGoBack,
  isDark,
  onToggleTheme,
  children,
}: {
  view: View;
  onNavigate: (view: View) => void;
  onBack: () => void;
  canGoBack: boolean;
  isDark: boolean;
  onToggleTheme: () => void;
  children: ReactNode;
}) {
  const title = titleForView(view);
  const topLevel = isTopLevel(view);

  return (
    <div className="fl-app-bg min-h-dvh">
      <div className="mx-auto flex w-full max-w-5xl gap-6 px-4 pb-28 md:px-6 md:pb-10">
        <DesktopRail view={view} onNavigate={onNavigate} />

        <main className="flex min-w-0 flex-1 flex-col gap-5 py-6">
          <header className="flex items-center justify-between gap-3">
            <div className="flex min-w-0 items-center gap-3">
              {canGoBack && !topLevel ? (
                <button
                  type="button"
                  onClick={onBack}
                  aria-label="Volver"
                  className="rounded-field border-line text-muted hover:text-ink grid size-9 place-items-center border transition"
                >
                  <IconBack className="size-4" />
                </button>
              ) : null}
              <div className="min-w-0">
                {title ? (
                  <h1 className="text-ink truncate text-xl font-semibold tracking-tight">
                    {title}
                  </h1>
                ) : (
                  <>
                    <FitLogWordmark />
                    <p className="text-muted mt-0.5 text-xs">
                      Registro de entrenamiento local-first
                    </p>
                  </>
                )}
              </div>
            </div>

            <ThemeToggle isDark={isDark} onToggle={onToggleTheme} />
          </header>

          {children}
        </main>
      </div>

      <MobileTabBar view={view} onNavigate={onNavigate} />
    </div>
  );
}

function DesktopRail({ view, onNavigate }: { view: View; onNavigate: (view: View) => void }) {
  return (
    <aside className="sticky top-0 hidden h-dvh w-56 shrink-0 flex-col gap-2 py-6 md:flex">
      <div className="px-1 pb-3">
        <FitLogWordmark />
      </div>
      {topLevelDestinations.map((destination) => {
        const active = destination.id === view;
        return (
          <button
            key={destination.id}
            type="button"
            onClick={() => onNavigate(destination.id)}
            className={`flex items-center gap-3 rounded-field px-3 py-2.5 text-sm font-medium transition ${
              active
                ? 'bg-accent-soft text-accent-text'
                : 'text-muted hover:bg-surface-high hover:text-ink'
            }`}
          >
            {destination.icon}
            {destination.label}
          </button>
        );
      })}
    </aside>
  );
}

function MobileTabBar({ view, onNavigate }: { view: View; onNavigate: (view: View) => void }) {
  return (
    <nav
      className="border-line bg-surface/95 fixed inset-x-0 bottom-0 z-20 border-t backdrop-blur md:hidden"
      style={{ paddingBottom: 'env(safe-area-inset-bottom)' }}
    >
      <div className="mx-auto flex max-w-lg items-stretch justify-between px-2">
        {topLevelDestinations.map((destination) => {
          const active = destination.id === view;
          return (
            <button
              key={destination.id}
              type="button"
              onClick={() => onNavigate(destination.id)}
              className={`flex flex-1 flex-col items-center gap-1 rounded-field px-2 py-2 text-[0.6875rem] font-medium transition ${
                active ? 'text-accent-text' : 'text-muted'
              }`}
            >
              <span
                className={`grid h-8 w-12 place-items-center rounded-full transition ${
                  active ? 'bg-accent-soft' : ''
                }`}
              >
                {destination.icon}
              </span>
              {destination.label}
            </button>
          );
        })}
      </div>
    </nav>
  );
}

function ThemeToggle({ isDark, onToggle }: { isDark: boolean; onToggle: () => void }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
      className="rounded-field border-line bg-surface text-muted hover:text-ink grid size-9 shrink-0 place-items-center border transition"
    >
      {isDark ? <IconSun className="size-4" /> : <IconMoon className="size-4" />}
    </button>
  );
}
