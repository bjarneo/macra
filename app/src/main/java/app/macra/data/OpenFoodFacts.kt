package app.macra.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Product lookup and text search on Open Food Facts. */
object OpenFoodFacts {
    private const val USER_AGENT = "Macra/1.0 (Android)"
    private const val FIELDS = "code,product_name,product_name_en,generic_name,brands,nutriments,serving_quantity,serving_size"

    sealed interface Lookup {
        data class Found(val food: Food) : Lookup
        data object NotFound : Lookup
        data object NoNutrition : Lookup
        data class Failed(val message: String) : Lookup
    }

    suspend fun product(code: String): Lookup = withContext(Dispatchers.IO) {
        val codes = if (code.length == 12) listOf(code, "0$code") else listOf(code)
        var last: Lookup = Lookup.NotFound
        for (c in codes) {
            last = try {
                val body = get("https://world.openfoodfacts.org/api/v2/product/$c.json?fields=$FIELDS")
                if (body == null) {
                    Lookup.NotFound
                } else {
                    val root = Store.json.parseToJsonElement(body).jsonObject
                    val status = root["status"].num()
                    val product = root["product"] as? JsonObject
                    if (status != 1.0 || product == null) {
                        Lookup.NotFound
                    } else {
                        parse(product, code, scanned = true)?.let { Lookup.Found(it) } ?: Lookup.NoNutrition
                    }
                }
            } catch (e: Exception) {
                Lookup.Failed(e.message ?: "Network error")
            }
            if (last !is Lookup.NotFound) break
        }
        last
    }

    suspend fun search(query: String): List<Food> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val body = get("https://search.openfoodfacts.org/search?q=$q&page_size=24&fields=$FIELDS") ?: return@withContext emptyList()
        val hits = Store.json.parseToJsonElement(body).jsonObject["hits"] as? JsonArray ?: return@withContext emptyList()
        hits.mapNotNull { h -> (h as? JsonObject)?.let { parse(it, it["code"].str() ?: "", scanned = false) } }
            .distinctBy { it.id }
    }

    private fun get(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/json")
        return try {
            when (conn.responseCode) {
                404 -> null
                in 200..299 -> conn.inputStream.bufferedReader().use { it.readText() }
                else -> throw java.io.IOException("HTTP ${conn.responseCode}")
            }
        } catch (e: FileNotFoundException) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(p: JsonObject, code: String, scanned: Boolean): Food? {
        if (code.isBlank()) return null
        val name = listOf("product_name", "product_name_en", "generic_name")
            .firstNotNullOfOrNull { k -> p[k].str()?.trim()?.takeIf { it.isNotEmpty() } } ?: return null
        val n = p["nutriments"] as? JsonObject ?: return null
        val kcal = n["energy-kcal_100g"].num()
            ?: n["energy-kj_100g"].num()?.div(4.184)
            ?: n["energy_100g"].num()?.div(4.184)
            ?: return null
        val brand = when (val b = p["brands"]) {
            is JsonArray -> b.firstOrNull().str()
            else -> b.str()?.split(',')?.firstOrNull()
        }?.trim()?.takeIf { it.isNotEmpty() }
        val servG = p["serving_quantity"].num()?.toInt()?.takeIf { it in 1..5000 }
        val servLabel = p["serving_size"].str()?.replace(Regex("\\(.*?\\)"), "")?.trim()?.takeIf { it.isNotEmpty() && it.length <= 24 }
            ?: "1 serving"
        val sodium = n["sodium_100g"].num()?.times(1000) ?: n["salt_100g"].num()?.times(400) ?: 0.0
        return Food(
            id = "off:$code",
            name = name,
            src = if (scanned) "Scanned · EAN $code" else listOfNotNull("Open Food Facts", brand).joinToString(" · "),
            kcal = kcal.round1(),
            p = (n["proteins_100g"].num() ?: 0.0).round1(),
            c = (n["carbohydrates_100g"].num() ?: 0.0).round1(),
            fat = (n["fat_100g"].num() ?: 0.0).round1(),
            fib = (n["fiber_100g"].num() ?: 0.0).round1(),
            sug = (n["sugars_100g"].num() ?: 0.0).round1(),
            sat = (n["saturated-fat_100g"].num() ?: 0.0).round1(),
            na = sodium.round1(),
            serv = servG?.let { listOf(Serving(servLabel, it)) }.orEmpty(),
            def = servG ?: 100,
            barcode = code,
        )
    }

    private fun Double.round1() = Math.round(this * 10) / 10.0
    private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.contentOrNull
    private fun JsonElement?.num(): Double? = (this as? JsonPrimitive)?.contentOrNull?.toDoubleOrNull()
}
