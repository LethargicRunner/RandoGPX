package com.mathieu.randogpx

import android.content.Context
import android.util.Base64
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Sauvegarde cloud GitHub (V2.0.1).
 * Vos fichiers (GPX des randos) sont poussés dans un dépôt GitHub PRIVÉ
 * que VOUS créez (ex. RandoGPX-data), via un jeton fine-grained collé
 * dans l'app et stocké uniquement dans le téléphone.
 *
 * - envoi immédiat à la fin d'une rando (si configuré) ;
 * - file d'attente locale : sans réseau, l'envoi est retenté plus tard ;
 * - API utilisée : Contents (PUT), message "RandoGPX : sauvegarde de …".
 */
object CloudBackup {

    private const val PREFS = "cloud"
    private const val KEY_OWNER = "owner"
    private const val KEY_REPO = "repo"
    private const val KEY_TOKEN = "token"
    private const val KEY_QUEUE = "queue"

    private val pool = Executors.newSingleThreadExecutor()

    fun loadSettings(context: Context): Triple<String, String, String>? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val o = p.getString(KEY_OWNER, null)?.takeIf { it.isNotBlank() } ?: return null
        val r = p.getString(KEY_REPO, null)?.takeIf { it.isNotBlank() } ?: return null
        val t = p.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return null
        return Triple(o, r, t)
    }

    fun configured(context: Context): Boolean = loadSettings(context) != null

    fun saveSettings(context: Context, owner: String, repo: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_OWNER, owner)
            .putString(KEY_REPO, repo)
            .putString(KEY_TOKEN, token)
            .apply()
    }

    /** Ajoute un fichier à la file d'attente (dédupliquée). */
    fun enqueue(context: Context, fileName: String) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val q = (p.getStringSet(KEY_QUEUE, mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        q.add(fileName)
        p.edit().putStringSet(KEY_QUEUE, q).apply()
    }

    fun pendingCount(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_QUEUE, emptySet())?.size ?: 0

    /** Envoie tous les fichiers en attente ; rend la main via onDone(ok, fail). */
    fun flush(context: Context, onDone: (Int, Int) -> Unit) {
        val settings = loadSettings(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val queue = (p.getStringSet(KEY_QUEUE, emptySet()) ?: emptySet()).toMutableSet()
        if (settings == null || queue.isEmpty()) {
            onDone(0, queue.size)
            return
        }
        pool.execute {
            var ok = 0
            var fail = 0
            val it = queue.iterator()
            while (it.hasNext()) {
                val name = it.next()
                val bytes = readBytes(context, name)
                if (bytes != null && upload(settings, name, bytes)) {
                    it.remove()
                    ok++
                } else fail++
            }
            p.edit().putStringSet(KEY_QUEUE, queue).apply()
            onDone(ok, fail)
        }
    }

    private fun readBytes(context: Context, fileName: String): ByteArray? = try {
        val uri = findUri(context, fileName)
        if (uri == null) null
        else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) { null }

    private fun findUri(context: Context, fileName: String): android.net.Uri? {
        val collection = android.provider.MediaStore.Files.getContentUri(
            android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
        context.contentResolver.query(
            collection,
            arrayOf(android.provider.MediaStore.MediaColumns._ID),
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME + " = ? AND " +
                android.provider.MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?",
            arrayOf(fileName, "%RandoGPX%"),
            null
        )?.use { c ->
            if (c.moveToFirst())
                return android.net.Uri.withAppendedPath(collection, c.getLong(0).toString())
        }
        return null
    }

    /** PUT sur l'API Contents de GitHub (création ou mise à jour). */
    private fun upload(settings: Triple<String, String, String>, fileName: String, bytes: ByteArray): Boolean {
        val owner = settings.first
        val repo = settings.second
        val token = settings.third
        val path = "randos/" + fileName.replace(' ', '_')
        return try {
            val existingSha = getSha(owner, repo, token, path)
            val body = JSONObject().apply {
                put("message", "RandoGPX : sauvegarde de " + fileName)
                put("content", Base64.encodeToString(bytes, Base64.NO_WRAP))
                existingSha?.let { put("sha", it) }
            }
            val conn = URL("https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path)
                .openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Authorization", "Bearer " + token)
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) { false }
    }

    private fun getSha(owner: String, repo: String, token: String, path: String): String? = try {
        val conn = URL("https://api.github.com/repos/" + owner + "/" + repo + "/contents/" + path)
            .openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer " + token)
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        val sha = if (conn.responseCode == 200) {
            val txt = conn.inputStream.bufferedReader().readText()
            val s = JSONObject(txt).optString("sha")
            if (s.isEmpty()) null else s
        } else null
        conn.disconnect()
        sha
    } catch (_: Exception) { null }
}
