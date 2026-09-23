package com.fitlog.app.ui

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream

/**
 * Lee los archivos JSON de la carpeta elegida en la exportacion de Huawei Health.
 *
 * Usa solo APIs del sistema (DocumentsContract): la exportacion anida los entrenamientos en
 * subcarpetas, asi que se recorre el arbol completo. El limite de archivos es defensivo, para que
 * una carpeta enorme no bloquee la lectura.
 */
object ImportFiles {

    /** Archivos leidos y cuantos quedaron afuera por tamano. */
    data class ReadResult(
        val files: List<Pair<String, String>>,
        val skipped: Int = 0,
    )

    /** Lee los `.json` y `.gpx` de un ZIP sin descomprimir (la exportacion llega como ZIP). */
    fun readZipFile(context: Context, uri: Uri): ReadResult {
        val contents = mutableListOf<Pair<String, String>>()
        var skipped = 0
        var totalBytes = 0L

        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(BufferedInputStream(stream)).use { zip ->
                    while (contents.size < MAX_FILES) {
                        val entry = zip.nextEntry ?: break
                        val isJson = entry.name.endsWith(".json", ignoreCase = true)
                        val isGpx = entry.name.endsWith(".gpx", ignoreCase = true)
                        if (entry.isDirectory || (!isJson && !isGpx)) {
                            zip.closeEntry()
                            continue
                        }
                        // Una exportacion real puede traer archivos enormes de rutas: se cortan
                        // por archivo y por total para no quedarse sin memoria.
                        val bytes = readBounded(zip, MAX_FILE_BYTES)
                        if (bytes == null || totalBytes + bytes.size > MAX_TOTAL_BYTES) {
                            skipped += 1
                            zip.closeEntry()
                            continue
                        }
                        totalBytes += bytes.size
                        contents += entry.name to bytes.toString(Charsets.UTF_8)
                        zip.closeEntry()
                    }
                }
            }
        }
        return ReadResult(files = contents, skipped = skipped)
    }

    /** Lee todos los `.json` del arbol elegido, en cualquier subcarpeta. */
    fun readJsonFiles(context: Context, treeUri: Uri): ReadResult {
        val contents = mutableListOf<Pair<String, String>>()
        val counter = intArrayOf(0, 0)
        walk(context, treeUri, DocumentsContract.getTreeDocumentId(treeUri), contents, counter)
        return ReadResult(files = contents, skipped = counter[1])
    }

    /** Nombre de la carpeta elegida, para mostrarlo en pantalla. */
    fun displayName(treeUri: Uri): String? =
        runCatching {
            DocumentsContract.getTreeDocumentId(treeUri).substringAfterLast(':').ifEmpty { null }
        }.getOrNull()

    private fun walk(
        context: Context,
        treeUri: Uri,
        documentId: String,
        out: MutableList<Pair<String, String>>,
        counter: IntArray,
    ) {
        if (out.size >= MAX_FILES) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val cursor = runCatching {
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )
        }.getOrNull() ?: return

        cursor.use { rows ->
            while (rows.moveToNext() && out.size < MAX_FILES) {
                val childId = rows.stringAt(DocumentsContract.Document.COLUMN_DOCUMENT_ID) ?: continue
                val name = rows.stringAt(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty()
                val mime = rows.stringAt(DocumentsContract.Document.COLUMN_MIME_TYPE).orEmpty()

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(context, treeUri, childId, out, counter)
                    continue
                }
                val isJson = name.endsWith(".json", ignoreCase = true)
                val isGpx = name.endsWith(".gpx", ignoreCase = true)
                if (!isJson && !isGpx) continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                val bytes = runCatching {
                    context.contentResolver.openInputStream(documentUri)?.use { stream ->
                        readBounded(stream, MAX_FILE_BYTES)
                    }
                }.getOrNull()
                if (bytes == null || counter[0] + bytes.size > MAX_TOTAL_BYTES) {
                    counter[1] += 1
                    continue
                }
                counter[0] += bytes.size
                out += name to bytes.toString(Charsets.UTF_8)
            }
        }
    }

    private fun Cursor.stringAt(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    /**
     * Lee hasta [limit] bytes; devuelve `null` si el contenido es mas grande.
     *
     * Una exportacion real puede incluir archivos de rutas de varios megabytes; sin este tope, un
     * archivo enorme se lleva la memoria de la app.
     */
    private fun readBounded(stream: java.io.InputStream, limit: Int): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        while (true) {
            val read = stream.read(chunk)
            if (read < 0) break
            if (buffer.size() + read > limit) return null
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    /** Limite de archivos por importacion: una exportacion real ronda los cientos. */
    private const val MAX_FILES = 600

    /** Tope por archivo y total, para no quedarse sin memoria con una exportacion grande. */
    private const val MAX_FILE_BYTES = 12 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 48L * 1024 * 1024
}
