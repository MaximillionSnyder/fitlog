package com.fitlog.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

object TestVectors {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(name: String): JsonElement = json.parseToJsonElement(loadText(name))

    fun loadText(name: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(name)
            ?: error("Recurso no encontrado: $name")
        return stream.bufferedReader().use { it.readText() }
    }
}
