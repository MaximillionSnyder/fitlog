#!/usr/bin/env node
// Genera shared/seed/catalog.json con IDs ULID deterministas.
// Los IDs quedan congelados: no regenerar una vez publicado el catalogo,
// salvo para agregar entidades nuevas (las existentes deben conservar su id).

import { createHash } from 'node:crypto';
import { writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const outputPath = join(here, 'catalog.json');

const ALPHABET = '0123456789ABCDEFGHJKMNPQRSTVWXYZ';
const SEED_TIMESTAMP_MS = 1767225600000; // 2026-01-01T00:00:00Z

function encodeBase32(value, length) {
  let remaining = value;
  let out = '';
  for (let i = 0; i < length; i += 1) {
    out = ALPHABET[Number(remaining % 32n)] + out;
    remaining /= 32n;
  }
  return out;
}

function ulidFor(kind, slug) {
  const hex = createHash('sha256').update(`${kind}:${slug}`).digest('hex').slice(0, 20);
  return encodeBase32(BigInt(SEED_TIMESTAMP_MS), 10) + encodeBase32(BigInt(`0x${hex}`), 16);
}

const muscleGroups = [
  { slug: 'pecho', name: 'Pecho', bodyRegion: 'torso' },
  { slug: 'espalda', name: 'Espalda', bodyRegion: 'torso' },
  { slug: 'hombros', name: 'Hombros', bodyRegion: 'torso' },
  { slug: 'biceps', name: 'Bíceps', bodyRegion: 'brazos' },
  { slug: 'triceps', name: 'Tríceps', bodyRegion: 'brazos' },
  { slug: 'antebrazos', name: 'Antebrazos', bodyRegion: 'brazos' },
  { slug: 'core', name: 'Core', bodyRegion: 'torso' },
  { slug: 'cuadriceps', name: 'Cuádriceps', bodyRegion: 'piernas' },
  { slug: 'femorales', name: 'Isquiotibiales', bodyRegion: 'piernas' },
  { slug: 'gluteos', name: 'Glúteos', bodyRegion: 'piernas' },
  { slug: 'pantorrillas', name: 'Pantorrillas', bodyRegion: 'piernas' },
  { slug: 'cuerpo-completo', name: 'Cuerpo completo', bodyRegion: 'global' },
];

const exercises = [
  { slug: 'press-banca-barra', name: 'Press banca con barra', group: 'pecho', secondary: 'triceps', equipment: 'barra', kind: 'strength' },
  { slug: 'press-inclinado-mancuernas', name: 'Press inclinado con mancuernas', group: 'pecho', secondary: 'hombros', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'press-plano-mancuernas', name: 'Press plano con mancuernas', group: 'pecho', secondary: 'triceps', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'aperturas-mancuernas', name: 'Aperturas con mancuernas', group: 'pecho', secondary: null, equipment: 'mancuernas', kind: 'strength' },
  { slug: 'fondos-paralelas', name: 'Fondos en paralelas', group: 'pecho', secondary: 'triceps', equipment: 'peso-corporal', kind: 'strength' },
  { slug: 'dominadas', name: 'Dominadas', group: 'espalda', secondary: 'biceps', equipment: 'peso-corporal', kind: 'strength' },
  { slug: 'remo-barra', name: 'Remo con barra', group: 'espalda', secondary: 'biceps', equipment: 'barra', kind: 'strength' },
  { slug: 'remo-mancuerna', name: 'Remo con mancuerna', group: 'espalda', secondary: 'biceps', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'jalon-al-pecho', name: 'Jalón al pecho', group: 'espalda', secondary: 'biceps', equipment: 'polea', kind: 'strength' },
  { slug: 'remo-polea-baja', name: 'Remo en polea baja', group: 'espalda', secondary: 'biceps', equipment: 'polea', kind: 'strength' },
  { slug: 'peso-muerto', name: 'Peso muerto', group: 'espalda', secondary: 'femorales', equipment: 'barra', kind: 'strength' },
  { slug: 'press-militar-barra', name: 'Press militar con barra', group: 'hombros', secondary: 'triceps', equipment: 'barra', kind: 'strength' },
  { slug: 'elevaciones-laterales', name: 'Elevaciones laterales', group: 'hombros', secondary: null, equipment: 'mancuernas', kind: 'strength' },
  { slug: 'face-pull', name: 'Face pull', group: 'hombros', secondary: 'espalda', equipment: 'polea', kind: 'strength' },
  { slug: 'press-arnold', name: 'Press Arnold', group: 'hombros', secondary: 'triceps', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'curl-barra', name: 'Curl con barra', group: 'biceps', secondary: 'antebrazos', equipment: 'barra', kind: 'strength' },
  { slug: 'curl-martillo', name: 'Curl martillo', group: 'biceps', secondary: 'antebrazos', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'curl-inclinado-mancuernas', name: 'Curl inclinado con mancuernas', group: 'biceps', secondary: null, equipment: 'mancuernas', kind: 'strength' },
  { slug: 'extension-triceps-polea', name: 'Extensión de tríceps en polea', group: 'triceps', secondary: null, equipment: 'polea', kind: 'strength' },
  { slug: 'press-frances', name: 'Press francés', group: 'triceps', secondary: null, equipment: 'barra', kind: 'strength' },
  { slug: 'fondos-banco', name: 'Fondos en banco', group: 'triceps', secondary: 'pecho', equipment: 'peso-corporal', kind: 'strength' },
  { slug: 'sentadilla-barra', name: 'Sentadilla con barra', group: 'cuadriceps', secondary: 'gluteos', equipment: 'barra', kind: 'strength' },
  { slug: 'prensa-piernas', name: 'Prensa de piernas', group: 'cuadriceps', secondary: 'gluteos', equipment: 'maquina', kind: 'strength' },
  { slug: 'zancadas-mancuernas', name: 'Zancadas con mancuernas', group: 'cuadriceps', secondary: 'gluteos', equipment: 'mancuernas', kind: 'strength' },
  { slug: 'extension-cuadriceps', name: 'Extensión de cuádriceps', group: 'cuadriceps', secondary: null, equipment: 'maquina', kind: 'strength' },
  { slug: 'peso-muerto-rumano', name: 'Peso muerto rumano', group: 'femorales', secondary: 'gluteos', equipment: 'barra', kind: 'strength' },
  { slug: 'curl-femoral', name: 'Curl femoral', group: 'femorales', secondary: null, equipment: 'maquina', kind: 'strength' },
  { slug: 'hip-thrust', name: 'Hip thrust', group: 'gluteos', secondary: 'femorales', equipment: 'barra', kind: 'strength' },
  { slug: 'elevacion-talones-pie', name: 'Elevación de talones de pie', group: 'pantorrillas', secondary: null, equipment: 'maquina', kind: 'strength' },
  { slug: 'plancha', name: 'Plancha', group: 'core', secondary: null, equipment: 'peso-corporal', kind: 'strength' },
  { slug: 'crunch-polea', name: 'Crunch en polea', group: 'core', secondary: null, equipment: 'polea', kind: 'strength' },
  { slug: 'elevacion-piernas-colgado', name: 'Elevación de piernas colgado', group: 'core', secondary: null, equipment: 'peso-corporal', kind: 'strength' },
  { slug: 'cinta-correr', name: 'Cinta de correr', group: 'cuerpo-completo', secondary: null, equipment: 'cardio-maquina', kind: 'cardio' },
  { slug: 'bicicleta-fija', name: 'Bicicleta fija', group: 'cuerpo-completo', secondary: null, equipment: 'cardio-maquina', kind: 'cardio' },
  { slug: 'remo-ergometro', name: 'Remo ergómetro', group: 'cuerpo-completo', secondary: 'espalda', equipment: 'cardio-maquina', kind: 'cardio' },
  { slug: 'burpees', name: 'Burpees', group: 'cuerpo-completo', secondary: 'cuadriceps', equipment: 'peso-corporal', kind: 'cardio' },
];

const groupIdBySlug = new Map(
  muscleGroups.map((group) => [group.slug, ulidFor('muscle-group', group.slug)])
);

const catalog = {
  version: 1,
  generated_at_ms: SEED_TIMESTAMP_MS,
  muscle_groups: muscleGroups.map((group) => ({
    id: groupIdBySlug.get(group.slug),
    slug: group.slug,
    name: group.name,
    body_region: group.bodyRegion,
    created_at: SEED_TIMESTAMP_MS,
    updated_at: SEED_TIMESTAMP_MS,
    deleted_at: null,
  })),
  exercises: exercises.map((exercise) => ({
    id: ulidFor('exercise', exercise.slug),
    slug: exercise.slug,
    name: exercise.name,
    muscle_group_id: groupIdBySlug.get(exercise.group),
    secondary_muscle_group_id: exercise.secondary ? groupIdBySlug.get(exercise.secondary) : null,
    equipment: exercise.equipment,
    kind: exercise.kind,
    is_custom: 0,
    created_at: SEED_TIMESTAMP_MS,
    updated_at: SEED_TIMESTAMP_MS,
    deleted_at: null,
  })),
};

writeFileSync(outputPath, `${JSON.stringify(catalog, null, 2)}\n`, 'utf8');
console.log(
  `catalog.json generado: ${catalog.muscle_groups.length} grupos musculares, ${catalog.exercises.length} ejercicios`
);
