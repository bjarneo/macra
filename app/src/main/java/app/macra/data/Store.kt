package app.macra.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/** Keeps all app data in one JSON file in the app's private storage. */
object Store {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    private fun file(ctx: Context) = File(ctx.filesDir, "macra.json")

    @Synchronized
    fun load(ctx: Context): Data {
        val f = file(ctx)
        if (!f.exists()) return Data()
        return try {
            json.decodeFromString(Data.serializer(), f.readText())
        } catch (e: Exception) {
            // Keep the unreadable file so that no data is lost, then start empty.
            f.copyTo(File(ctx.filesDir, "macra.broken.${System.currentTimeMillis()}.json"), overwrite = true)
            Data()
        }
    }

    @Synchronized
    fun save(ctx: Context, data: Data) {
        val tmp = File(ctx.filesDir, "macra.json.tmp")
        tmp.writeText(json.encodeToString(Data.serializer(), data))
        if (!tmp.renameTo(file(ctx))) {
            file(ctx).writeText(tmp.readText())
            tmp.delete()
        }
    }
}
