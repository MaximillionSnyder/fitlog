import type { ButtonHTMLAttributes, ReactNode } from 'react';

import { deltaText } from '@/domain/format';
import {
  IconArrowDown,
  IconArrowUp,
  IconChevronRight,
} from '@/ui/icons';

/**
 * Primitivas del sistema de diseno de la web.
 *
 * Las pantallas se construyen con estas piezas (tarjeta, tile, encabezado, accion) para que dos
 * vistas distintas no inventen su propio estilo. Los colores salen siempre de los tokens.
 */

export function Card({
  children,
  className = '',
  onClick,
  tone,
}: {
  children: ReactNode;
  className?: string;
  onClick?: () => void;
  tone?: 'surface' | 'accent' | 'data' | 'danger';
}) {
  const toneClass =
    tone === 'accent'
      ? 'bg-accent-soft'
      : tone === 'data'
        ? 'bg-data-soft'
        : tone === 'danger'
          ? 'bg-danger-soft'
          : 'bg-surface';
  const base = `rounded-card border border-line ${toneClass} p-5`;

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        className={`${base} w-full text-left transition hover:border-line-strong focus-visible:ring-2 focus-visible:ring-accent focus-visible:outline-none ${className}`}
      >
        {children}
      </button>
    );
  }

  return <section className={`${base} ${className}`}>{children}</section>;
}

export function SectionHeader({
  title,
  trailing,
  className = '',
}: {
  title: string;
  trailing?: string | undefined;
  className?: string;
}) {
  return (
    <div className={`flex items-center justify-between gap-3 ${className}`}>
      <h2 className="text-muted text-[0.6875rem] font-semibold tracking-[0.14em] uppercase">
        {title}
      </h2>
      {trailing ? <span className="text-faint text-xs">{trailing}</span> : null}
    </div>
  );
}

export function DeltaBadge({ percent, suffix }: { percent: number; suffix?: string }) {
  const positive = percent > 0.5;
  const negative = percent < -0.5;
  const tone = positive
    ? 'bg-success-soft text-success'
    : negative
      ? 'bg-danger-soft text-danger'
      : 'bg-surface-high text-muted';
  const Icon = positive ? IconArrowUp : negative ? IconArrowDown : null;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-lg px-2 py-0.5 text-[0.6875rem] font-semibold ${tone}`}
    >
      {Icon ? <Icon className="size-3" /> : null}
      <span className="fl-num">
        {deltaText(percent)}
        {suffix ? ` ${suffix}` : ''}
      </span>
    </span>
  );
}

export function StatTile({
  label,
  value,
  unit,
  icon,
  delta,
  hint,
  tone = 'ink',
  onClick,
}: {
  label: string;
  value: string;
  unit?: string | undefined;
  icon?: ReactNode | undefined;
  delta?: number | null | undefined;
  hint?: string | undefined;
  tone?: 'ink' | 'accent' | 'data';
  onClick?: () => void;
}) {
  const valueTone =
    tone === 'accent' ? 'text-accent-text' : tone === 'data' ? 'text-data' : 'text-ink';
  const content = (
    <>
      <div className="text-muted flex items-center gap-2 text-xs font-medium">
        {icon}
        <span className="truncate">{label}</span>
      </div>
      <div className="flex items-end gap-1">
        <span className={`fl-num text-2xl font-semibold ${valueTone}`}>{value}</span>
        {unit ? <span className="text-muted pb-1 text-xs">{unit}</span> : null}
      </div>
      {delta !== undefined && delta !== null ? (
        <DeltaBadge percent={delta} />
      ) : hint ? (
        <span className="text-faint text-[0.6875rem]">{hint}</span>
      ) : null}
    </>
  );

  const className = `flex flex-col gap-1 rounded-tile border border-line bg-surface p-4 text-left ${
    onClick ? 'transition hover:border-line-strong' : ''
  }`;

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={`${className} w-full`}>
        {content}
      </button>
    );
  }

  return <div className={className}>{content}</div>;
}

export function NavigationRow({
  title,
  description,
  icon,
  onClick,
  trailing,
}: {
  title: string;
  description?: string;
  icon: ReactNode;
  onClick: () => void;
  trailing?: ReactNode | undefined;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-card border-line bg-surface hover:border-line-strong flex w-full items-center gap-3 border p-4 text-left transition focus-visible:ring-2 focus-visible:ring-accent focus-visible:outline-none"
    >
      <span className="rounded-field bg-surface-high text-accent grid size-10 shrink-0 place-items-center">
        {icon}
      </span>
      <span className="min-w-0 flex-1">
        <span className="text-ink block text-sm font-semibold">{title}</span>
        {description ? (
          <span className="text-muted block truncate text-xs">{description}</span>
        ) : null}
      </span>
      {trailing ?? <IconChevronRight className="text-muted size-4 shrink-0" />}
    </button>
  );
}

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger';

export function Button({
  children,
  variant = 'primary',
  className = '',
  icon,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant | undefined;
  icon?: ReactNode | undefined;
}) {
  const variants: Record<ButtonVariant, string> = {
    primary: 'bg-accent text-accent-ink hover:brightness-105',
    secondary: 'border border-line-strong text-ink hover:bg-surface-high',
    ghost: 'text-muted hover:text-ink hover:bg-surface-high',
    danger: 'bg-danger-soft text-danger hover:brightness-105',
  };
  return (
    <button
      type="button"
      className={`rounded-field inline-flex items-center justify-center gap-2 px-4 py-2.5 text-sm font-semibold transition focus-visible:ring-2 focus-visible:ring-accent focus-visible:outline-none disabled:opacity-50 ${variants[variant]} ${className}`}
      {...props}
    >
      {icon}
      {children}
    </button>
  );
}

export function Chip({
  label,
  active = false,
  onClick,
}: {
  label: string;
  active?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-full border px-3 py-1 text-xs font-medium whitespace-nowrap transition ${
        active
          ? 'border-accent bg-accent-soft text-accent-text'
          : 'border-line text-muted hover:text-ink'
      }`}
    >
      {label}
    </button>
  );
}

export function EmptyState({
  title,
  message,
  actionLabel,
  onAction,
}: {
  title?: string | undefined;
  message: string;
  actionLabel?: string | undefined;
  onAction?: (() => void) | undefined;
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-6 py-10 text-center">
      <span className="bg-accent-soft text-accent-text grid size-12 place-items-center rounded-full">
        <svg viewBox="0 0 24 24" className="size-6" fill="none" stroke="currentColor" strokeWidth={1.6}>
          <path d="M12 3.6c.9 4.6 2.4 6 7 6.9-4.6.9-6.1 2.4-7 7-.9-4.6-2.4-6.1-7-7 4.6-.9 6.1-2.3 7-6.9Z" />
        </svg>
      </span>
      {title ? <p className="text-ink text-sm font-semibold">{title}</p> : null}
      <p className="text-muted max-w-xs text-sm">{message}</p>
      {actionLabel && onAction ? (
        <Button variant="secondary" onClick={onAction}>
          {actionLabel}
        </Button>
      ) : null}
    </div>
  );
}

export function LoadingState({ message = 'Cargando…' }: { message?: string }) {
  return (
    <div className="text-muted flex items-center justify-center gap-3 py-10 text-sm">
      <span className="border-line border-t-accent size-4 animate-spin rounded-full border-2" />
      {message}
    </div>
  );
}

export function ErrorState({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="rounded-card bg-danger-soft text-danger flex flex-col gap-2 p-5 text-sm">
      <p className="font-semibold">Algo salió mal</p>
      <p>{message}</p>
      {onRetry ? (
        <Button variant="secondary" onClick={onRetry} className="self-start">
          Reintentar
        </Button>
      ) : null}
    </div>
  );
}

export function LabeledValue({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between gap-3 text-sm">
      <span className="text-muted">{label}</span>
      <span className="text-ink fl-num">{value}</span>
    </div>
  );
}

export function ScreenIntro({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <header className="flex flex-col gap-1">
      <h1 className="text-ink text-xl font-semibold tracking-tight">{title}</h1>
      {subtitle ? <p className="text-muted text-sm">{subtitle}</p> : null}
    </header>
  );
}
