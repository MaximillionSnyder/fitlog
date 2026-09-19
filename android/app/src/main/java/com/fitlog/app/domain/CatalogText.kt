package com.fitlog.app.domain

import java.text.Normalizer
import java.util.Locale

object CatalogText {

    fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase(Locale.ROOT)
            .replace(WHITESPACE, " ")
            .trim()

    fun slugify(value: String): String =
        normalize(value)
            .replace(NON_ALPHANUMERIC, "-")
            .trim('-')

    private val DIACRITICS = Regex("\\p{Mn}+")
    private val WHITESPACE = Regex("\\s+")
    private val NON_ALPHANUMERIC = Regex("[^a-z0-9]+")
}
