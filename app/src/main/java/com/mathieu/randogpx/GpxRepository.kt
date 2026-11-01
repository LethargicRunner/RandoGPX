package com.mathieu.randogpx

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object GpxRepository {

    const val FOLDER = "RandoGPX"

    fun save(context: Context, displayName: String, bytes: ByteArray): String {
        val name = displayName.substringBeforeLast('.')
            .replace(Regex("[^A-Za-z0-9 ._\\-]"), "_")
            .ifBlank { "rando_" + System.currentTimeMillis() } + ".gpx"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/gpx+xml")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + FOLDER)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            ) ?: throw IllegalStateException("Création du fichier impossible")
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER
            ).apply { mkdirs() }
            File(dir, name).writeBytes(bytes)
        }
        return name
    }

    fun list(context: Context): List<Pair<String, Uri>> {
        val result = mutableListOf<Pair<String, Uri>>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val query = context.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns._ID
                ),
                MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND " +
                    MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?",
                arrayOf("%.gpx", "%" + FOLDER + "%"),
                MediaStore.MediaColumns.DISPLAY_NAME + " ASC"
            )
            query?.use { c ->
                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val idIdx = c.getColumnIndex(MediaStore.MediaColumns._ID)
                while (c.moveToNext()) {
                    result += c.getString(nameIdx) to
                            Uri.withAppendedPath(collection, c.getLong(idIdx).toString())
                }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER
            )
            dir?.listFiles()?.filter { it.extension == "gpx" }?.sortedBy { it.name }?.forEach {
                result += it.name to Uri.fromFile(it)
            }
        }
        return result
    }

    data class Entry(val name: String, val uri: Uri, val sizeBytes: Long, val dateMs: Long)

    /** V2.2 : liste détaillée (taille + date) pour un affichage plus clair. */
    fun listDetailed(context: Context): List<Entry> {
        val result = mutableListOf<Entry>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val query = context.contentResolver.query(
                collection,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED
                ),
                MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ? AND " +
                    MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?",
                arrayOf("%.gpx", "%" + FOLDER + "%"),
                MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
            )
            query?.use { c ->
                val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val idIdx = c.getColumnIndex(MediaStore.MediaColumns._ID)
                val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val dateIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                while (c.moveToNext()) {
                    result += Entry(
                        c.getString(nameIdx),
                        Uri.withAppendedPath(collection, c.getLong(idIdx).toString()),
                        c.getLong(sizeIdx),
                        c.getLong(dateIdx) * 1000L
                    )
                }
            }
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                FOLDER
            )
            dir?.listFiles()?.filter { it.extension == "gpx" }?.sortedByDescending { it.lastModified() }?.forEach {
                result += Entry(it.name, Uri.fromFile(it), it.length(), it.lastModified())
            }
        }
        return result
    }

    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return uri.lastPathSegment ?: "rando.gpx"
    }
}
