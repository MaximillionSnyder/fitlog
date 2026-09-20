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

    /** Lee los `.json` de un ZIP sin descomprimir (la exportacion llega como ZIP). */
    fun readZipFile(context: Context, uri: Uri): List<String> {
        val contents = mutableListOf<String>()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ZipInputStream(BufferedInputStream(stream)).use { zip ->
                    while (contents.size < MAX_FILES) {
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory || !entry.name.endsWith(".json", ignoreCase = true)) {
                            zip.closeEntry()
                            continue
                        }
                        contents += zip.readBytes().toString(Charsets.UTF_8)
                        zip.closeEntry()
                    }
                }
            }
        }
        return contents
    }

    /** Lee todos los `.json` del arbol elegido, en cualquier subcarpeta. */
    fun readJsonFiles(context: Context, treeUri: Uri): List<String> {
        val contents = mutableListOf<String>()
        walk(context, treeUri, DocumentsContract.getTreeDocumentId(treeUri), contents)
        return contents
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
        out: MutableList<String>,
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
                    walk(context, treeUri, childId, out)
                    continue
                }
                if (!name.endsWith(".json", ignoreCase = true)) continue

                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                val text = runCatching {
                    context.contentResolver.openInputStream(documentUri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }
                }.getOrNull()
                if (text != null) out += text
            }
        }
    }

    private fun Cursor.stringAt(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    /** Limite de archivos por importacion: una exportacion real ronda los cientos. */
    private const val MAX_FILES = 600
}
