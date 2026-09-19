package com.fitlog.app.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

object Backup {

    const val FORMAT = "fitlog-backup"
    const val FORMAT_VERSION = 1

    val TABLES = listOf(
        "exercise",
        "routine",
        "routine_exercise",
        "session",
        "set_entry",
        "body_metric",
        "app_setting",
    )

    enum class ErrorCode { INVALID_JSON, UNKNOWN_FORMAT, UNSUPPORTED_VERSION, INVALID_ROW, MISSING_REFERENCE }

    class BackupException(
        val code: ErrorCode,
        val detail: String,
    ) : Exception("$code: $detail")

    data class Row(val values: Map<String, Any?>)

    data class File(
        val format: String,
        val formatVersion: Int,
        val exportedAtMs: Long,
        val appVersion: String,
        val sections: Map<String, List<Row>>,
    )

    data class Snapshot(val tables: MutableMap<String, MutableList<Row>>) {
        fun rows(table: String): MutableList<Row> = tables.getOrPut(table) { mutableListOf() }
    }

    data class SectionSummary(
        val inserted: Int = 0,
        val updated: Int = 0,
        val ignored: Int = 0,
        val remapped: Int = 0,
    )

    data class MergeResult(
        val snapshot: Snapshot,
        val summary: Map<String, SectionSummary>,
    )

    private data class TableSpec(val pk: String, val required: List<String>)

    private val TABLE_SPECS = mapOf(
        "exercise" to TableSpec(
            pk = "id",
            required = listOf("id", "slug", "name", "muscle_group_id", "is_custom", "updated_at"),
        ),
        "routine" to TableSpec(pk = "id", required = listOf("id", "name", "updated_at")),
        "routine_exercise" to TableSpec(
            pk = "id",
            required = listOf("id", "routine_id", "exercise_id", "position", "updated_at"),
        ),
        "session" to TableSpec(pk = "id", required = listOf("id", "started_at", "updated_at")),
        "set_entry" to TableSpec(
            pk = "id",
            required = listOf("id", "session_id", "exercise_id", "set_index", "updated_at"),
        ),
        "body_metric" to TableSpec(
            pk = "id",
            required = listOf("id", "measured_at", "kind", "value", "unit", "updated_at"),
        ),
        "app_setting" to TableSpec(pk = "key", required = listOf("key", "value", "updated_at")),
    )

    private data class Reference(val table: String, val column: String, val target: String)

    private val REFERENCES = listOf(
        Reference("exercise", "muscle_group_id", "exercise"),
        Reference("routine_exercise", "routine_id", "routine"),
        Reference("routine_exercise", "exercise_id", "exercise"),
        Reference("set_entry", "session_id", "session"),
        Reference("set_entry", "exercise_id", "exercise"),
    )

    fun emptySnapshot(): Snapshot = Snapshot(TABLES.associateWith { mutableListOf() }.toMutableMap())

    private fun JsonElement.toValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> if (isString) content else doubleOrNull ?: content.toBooleanStrictOrNull() ?: content
        is JsonArray -> throw BackupException(ErrorCode.INVALID_JSON, "No se admiten listas dentro de una fila")
        is JsonObject -> throw BackupException(ErrorCode.INVALID_JSON, "No se admiten objetos dentro de una fila")
    }

    fun parse(json: String): File {
        val root = try {
            Json.parseToJsonElement(json)
        } catch (error: Exception) {
            throw BackupException(ErrorCode.INVALID_JSON, "El archivo no es JSON válido")
        }
        if (root !is JsonObject) {
            throw BackupException(ErrorCode.INVALID_JSON, "El archivo no contiene un objeto JSON")
        }

        val format = root["format"]?.jsonPrimitive?.content
        if (format != FORMAT) {
            throw BackupException(
                ErrorCode.UNKNOWN_FORMAT,
                "Se esperaba el formato $FORMAT y llegó ${format ?: "sin formato"}",
            )
        }

        val version = root["format_version"]?.jsonPrimitive?.doubleOrNull?.toInt()
            ?: throw BackupException(ErrorCode.INVALID_JSON, "Falta format_version")
        if (version > FORMAT_VERSION) {
            throw BackupException(
                ErrorCode.UNSUPPORTED_VERSION,
                "El archivo es de la versión $version y esta app soporta hasta la $FORMAT_VERSION",
            )
        }

        val sections = mutableMapOf<String, List<Row>>()
        val sectionsObject = root["sections"] as? JsonObject
        if (sectionsObject != null) {
            for (table in TABLES) {
                val array = sectionsObject[table] as? JsonArray ?: continue
                sections[table] = array.map { element ->
                    val rowObject = element as? JsonObject
                        ?: throw BackupException(ErrorCode.INVALID_JSON, "La sección $table no contiene objetos")
                    Row(rowObject.mapValues { (_, value) -> value.toValue() })
                }
            }
        }

        return File(
            format = format,
            formatVersion = version,
            exportedAtMs = root["exported_at_ms"]?.jsonPrimitive?.doubleOrNull?.toLong() ?: 0L,
            appVersion = root["app_version"]?.jsonPrimitive?.content ?: "",
            sections = sections,
        )
    }

    private fun required(table: String): List<String> = TABLE_SPECS.getValue(table).required

    private fun primaryKey(table: String): String = TABLE_SPECS.getValue(table).pk

    fun validate(file: File, local: Snapshot) {
        for (table in TABLES) {
            val rows = file.sections[table] ?: continue
            for (row in rows) {
                for (field in required(table)) {
                    if (!row.values.containsKey(field)) {
                        val identifier = row.values[primaryKey(table)]?.toString() ?: "?"
                        throw BackupException(
                            ErrorCode.INVALID_ROW,
                            "La fila $identifier de $table no tiene el campo $field",
                        )
                    }
                }
            }
        }

        val known = mutableMapOf<String, MutableSet<String>>()
        for (table in TABLES) {
            val ids = mutableSetOf<String>()
            local.rows(table).forEach { row -> ids.add(row.values[primaryKey(table)]?.toString() ?: "") }
            (file.sections[table] ?: emptyList()).forEach { row ->
                ids.add(row.values[primaryKey(table)]?.toString() ?: "")
            }
            known[table] = ids
        }

        val localGroups = local.rows("exercise").mapNotNull { it.values["muscle_group_id"]?.toString() }.toSet()
        val fileGroups = (file.sections["exercise"] ?: emptyList())
            .mapNotNull { it.values["muscle_group_id"]?.toString() }
            .toSet()

        for (reference in REFERENCES) {
            val rows = file.sections[reference.table] ?: continue
            for (row in rows) {
                val value = row.values[reference.column]?.toString() ?: continue
                if (reference.table == "exercise" && reference.column == "muscle_group_id") {
                    if (value !in localGroups && value !in fileGroups) {
                        throw BackupException(
                            ErrorCode.MISSING_REFERENCE,
                            "El ejercicio ${row.values["id"]} referencia el grupo muscular $value, que no existe en la base",
                        )
                    }
                    continue
                }
                val candidates = known[reference.target]
                if (candidates == null || value !in candidates) {
                    throw BackupException(
                        ErrorCode.MISSING_REFERENCE,
                        "La fila ${row.values[primaryKey(reference.table)]} de ${reference.table} " +
                            "referencia ${reference.target} $value, que no está en el archivo ni en la base",
                    )
                }
            }
        }
    }

    private fun updatedAt(row: Row): Double = (row.values["updated_at"] as? Double) ?: 0.0

    private data class MergeCounts(val rows: MutableList<Row>, val inserted: Int, val updated: Int, val ignored: Int)

    private fun mergeByPrimaryKey(
        localRows: List<Row>,
        importedRows: List<Row>,
        pk: String,
    ): MergeCounts {
        val byId = linkedMapOf<String, Row>()
        localRows.forEach { row -> byId[row.values[pk]?.toString() ?: ""] = row }

        var inserted = 0
        var updated = 0
        var ignored = 0

        for (imported in importedRows) {
            val key = imported.values[pk]?.toString() ?: ""
            val existing = byId[key]
            when {
                existing == null -> {
                    byId[key] = imported
                    inserted += 1
                }
                updatedAt(imported) > updatedAt(existing) -> {
                    byId[key] = imported
                    updated += 1
                }
                else -> ignored += 1
            }
        }

        val rows = byId.entries
            .sortedBy { it.key }
            .map { it.value }
            .toMutableList()
        return MergeCounts(rows, inserted, updated, ignored)
    }

    private fun remap(row: Row, column: String, idMap: Map<String, String>): Row {
        val value = row.values[column]?.toString() ?: return row
        val mapped = idMap[value] ?: return row
        return Row(row.values + (column to mapped))
    }

    fun merge(local: Snapshot, file: File): MergeResult {
        validate(file, local)

        val snapshot = Snapshot(
            TABLES.associateWith { table -> local.rows(table).toMutableList() }.toMutableMap()
        )
        val summary = TABLES.associateWith { SectionSummary() }.toMutableMap()

        val idMap = mutableMapOf<String, String>()
        val localById = snapshot.rows("exercise").associateBy { it.values["id"]?.toString() ?: "" }
        val localBySlug = snapshot.rows("exercise").associateBy { it.values["slug"]?.toString() ?: "" }

        val importedExercises = mutableListOf<Row>()
        for (imported in file.sections["exercise"] ?: emptyList()) {
            val id = imported.values["id"]?.toString() ?: ""
            if (localById.containsKey(id)) {
                importedExercises.add(imported)
                continue
            }
            val existingBySlug = localBySlug[imported.values["slug"]?.toString() ?: ""]
            if (existingBySlug != null) {
                idMap[id] = existingBySlug.values["id"]?.toString() ?: ""
                summary["exercise"] = summary.getValue("exercise").copy(
                    remapped = summary.getValue("exercise").remapped + 1,
                )
                continue
            }
            importedExercises.add(imported)
        }

        val exerciseMerge = mergeByPrimaryKey(snapshot.rows("exercise"), importedExercises, primaryKey("exercise"))
        snapshot.tables["exercise"] = exerciseMerge.rows
        summary["exercise"] = summary.getValue("exercise").copy(
            inserted = exerciseMerge.inserted,
            updated = exerciseMerge.updated,
            ignored = exerciseMerge.ignored,
        )

        for (table in TABLES) {
            if (table == "exercise") continue
            val importedRows = (file.sections[table] ?: emptyList()).map { row ->
                if (table == "routine_exercise" || table == "set_entry") {
                    remap(row, "exercise_id", idMap)
                } else {
                    row
                }
            }
            val merged = mergeByPrimaryKey(snapshot.rows(table), importedRows, primaryKey(table))
            snapshot.tables[table] = merged.rows
            summary[table] = SectionSummary(
                inserted = merged.inserted,
                updated = merged.updated,
                ignored = merged.ignored,
            )
        }

        return MergeResult(snapshot = snapshot, summary = summary)
    }

    fun serialize(
        snapshot: Snapshot,
        sections: List<String>,
        appVersion: String,
        nowMs: Long,
    ): String {
        val requested = sections.toSet()

        val sets = if ("set_entry" in requested) snapshot.rows("set_entry") else emptyList()
        val routineItems = if ("routine_exercise" in requested) snapshot.rows("routine_exercise") else emptyList()

        val referencedExerciseIds = mutableSetOf<String>()
        sets.forEach { row -> row.values["exercise_id"]?.toString()?.let { referencedExerciseIds.add(it) } }
        routineItems.forEach { row ->
            row.values["exercise_id"]?.toString()?.let { referencedExerciseIds.add(it) }
        }

        val exercises = snapshot.rows("exercise").filter { row ->
            val isCustom = (row.values["is_custom"] as? Double)?.toInt() == 1
            val id = row.values["id"]?.toString() ?: ""
            isCustom && ("exercise" in requested || id in referencedExerciseIds)
        }

        val selected = linkedMapOf<String, List<Row>>()
        if (exercises.isNotEmpty()) selected["exercise"] = exercises
        if ("routine" in requested) selected["routine"] = snapshot.rows("routine")
        if (routineItems.isNotEmpty()) selected["routine_exercise"] = routineItems
        if ("session" in requested) selected["session"] = snapshot.rows("session")
        if (sets.isNotEmpty()) selected["set_entry"] = sets
        if ("body_metric" in requested) selected["body_metric"] = snapshot.rows("body_metric")
        if ("app_setting" in requested) selected["app_setting"] = snapshot.rows("app_setting")

        val root = buildJsonObject {
            put("format", JsonPrimitive(FORMAT))
            put("format_version", JsonPrimitive(FORMAT_VERSION))
            put("exported_at_ms", JsonPrimitive(nowMs))
            put("app_version", JsonPrimitive(appVersion))
            put(
                "sections",
                buildJsonObject {
                    for ((table, rows) in selected) {
                        put(
                            table,
                            buildJsonArray {
                                rows.forEach { row ->
                                    add(
                                        buildJsonObject {
                                            for ((column, value) in row.values) {
                                                when (value) {
                                                    null -> put(column, JsonNull)
                                                    is String -> put(column, JsonPrimitive(value))
                                                    is Double -> put(
                                                        column,
                                                        JsonPrimitive(
                                                            if (value == value.toLong().toDouble()) value.toLong() else value
                                                        ),
                                                    )
                                                    else -> put(column, JsonPrimitive(value.toString()))
                                                }
                                            }
                                        }
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }

        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), root)
    }
}
