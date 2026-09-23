package com.fitlog.app.data

/**
 * Migraciones del esquema, en un solo lugar.
 *
 * Las sentencias tienen que coincidir con `shared/schema/migrations/002_actividad_importada.sql`;
 * hay un test que lo verifica, para que Android y la web no se separen.
 */
object DatabaseMigrations {

    const val VERSION_1_2 = 2
    const val NAME_1_2 = "002_actividad_importada"

    const val VERSION_2_3 = 3
    const val NAME_2_3 = "003_ruta_del_recorrido"

    /** Ruta del recorrido que agrega la migracion 003. */
    val STATEMENTS_2_3: List<String> = listOf(
        "ALTER TABLE session ADD COLUMN route TEXT",
    )

    /** Columnas de actividad importada que agrega la migracion 002. */
    val STATEMENTS_1_2: List<String> = listOf(
        "ALTER TABLE session ADD COLUMN distance_m REAL",
        "ALTER TABLE session ADD COLUMN calories REAL",
        "ALTER TABLE session ADD COLUMN avg_heart_rate REAL",
        "ALTER TABLE session ADD COLUMN max_heart_rate REAL",
        "ALTER TABLE session ADD COLUMN steps INTEGER",
        "ALTER TABLE session ADD COLUMN elevation_gain_m REAL",
        "ALTER TABLE session ADD COLUMN source TEXT",
    )
}
