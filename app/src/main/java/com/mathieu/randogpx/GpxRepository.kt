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
                put(MediaStore.Files.DISPLAY_NAME, name)
                put(MediaStore.Files.MIME_TYPE, "application/gpx+xml")
                put(MediaStore.Files.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/" + FOLDER)
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
                arrayOf(MediaStore.Files.DISPLAY_NAME, MediaStore.Files._ID),
                MediaStore.Files.DISPLAY_NAME + " LIKE ? AND " +
                    MediaStore.Files.RELATIVE_PATH + " LIKE ?",
                arrayOf("%.gpx", "%" + FOLDER + "%"),
                MediaStore.Files.DISPLAY_NAME + " ASC"
            )
            query?.use { c ->
                val nameIdx = c.getColumnIndex(MediaStore.Files.DISPLAY_NAME)
                val idIdx = c.getColumnIndex(MediaStore.Files._ID)
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

    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
        }
        return uri.lastPathSegment ?: "rando.gpx"
    }
}
