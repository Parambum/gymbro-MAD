package com.gymbro.mad.data.repo

import com.gymbro.mad.data.model.FoodItem
import com.gymbro.mad.data.model.Macros
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Single constant holding the base URL for the food database API.
 */
const val FOOD_API_BASE_URL = "https://gymbro-padi.vercel.app"

/**
 * Production implementation of [FoodRepository] pointing to the web app's
 * `/api/fuel/foods/search` and `/api/fuel/barcode` endpoints.
 *
 * It throws an [IOException] on network or HTTP errors so the calling UI can
 * distinguish a connection failure from an empty search result.
 */
class NetworkFoodRepository(
    private val baseUrl: String = FOOD_API_BASE_URL,
    private val fetcher: (url: String) -> String = { defaultFetch(it) },
) : FoodRepository {

    override suspend fun search(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val endpoint = "$baseUrl/api/fuel/foods/search?q=$encoded&limit=25"
        val response = fetcher(endpoint)
        parseFoodItems(response)
    }

    override suspend fun byBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        val trimmed = barcode.trim()
        if (trimmed.isEmpty()) return@withContext null

        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val endpoint = "$baseUrl/api/fuel/barcode?code=$encoded"
        try {
            val response = fetcher(endpoint)
            parseBarcodeResponse(response)
        } catch (e: IOException) {
            if (e.message?.contains("404") == true) {
                null
            } else {
                throw e
            }
        }
    }

    companion object {
        private fun defaultFetch(urlString: String): String {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "GymBro-Android/1.0")
            }
            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }?.trim().orEmpty()
                val detail = if (errorBody.isNotEmpty()) ": $errorBody" else ""
                throw IOException("HTTP $code ${conn.responseMessage}$detail")
            }
        }
    }
}

internal fun parseBarcodeResponse(rawJson: String): FoodItem? {
    val parsed = MiniJson.parse(rawJson) ?: return null
    if (parsed is Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        val map = parsed as Map<String, Any?>
        val found = map["found"] as? Boolean ?: true
        if (!found) return null
        @Suppress("UNCHECKED_CAST")
        val foodObj = (map["food"] as? Map<*, *>)?.filterKeys { it is String } as? Map<String, Any?>
        if (foodObj != null) {
            return parseFoodItem(foodObj)
        }
        return parseFoodItem(map)
    }
    return parseFoodItems(rawJson).firstOrNull()
}

internal fun parseFoodItems(rawJson: String): List<FoodItem> {
    val parsed = MiniJson.parse(rawJson) ?: return emptyList()
    return when (parsed) {
        is List<*> -> parsed.filterIsInstance<Map<String, Any?>>().mapNotNull { parseFoodItem(it) }
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = parsed as Map<String, Any?>
            val list = (map["foods"] ?: map["items"] ?: map["results"] ?: map["data"]) as? List<*>
            if (list != null) {
                list.filterIsInstance<Map<String, Any?>>().mapNotNull { parseFoodItem(it) }
            } else {
                parseFoodItem(map)?.let { listOf(it) } ?: emptyList()
            }
        }
        else -> emptyList()
    }
}

internal fun parseFoodItem(map: Map<String, Any?>): FoodItem? {
    val id = (map["id"] ?: map["_id"] ?: map["foodId"] ?: map["code"])?.toString() ?: return null
    val name = (map["name"] ?: map["food_name"] ?: map["title"] ?: map["product_name"])?.toString() ?: "Unknown"
    val brand = (map["brand"] ?: map["brand_name"] ?: map["brands"])?.toString()?.takeIf { it.isNotBlank() }

    @Suppress("UNCHECKED_CAST")
    val macrosMap = (map["per100g"] ?: map["macros"] ?: map["nutrients"]) as? Map<String, Any?>

    val kcal = parseDouble(macrosMap?.get("kcal") ?: macrosMap?.get("calories") ?: map["kcal"] ?: map["calories"] ?: map["energyKcal"])
    val protein = parseDouble(macrosMap?.get("proteinG") ?: macrosMap?.get("protein") ?: map["proteinG"] ?: map["protein"])
    val carbs = parseDouble(macrosMap?.get("carbsG") ?: macrosMap?.get("carbs") ?: macrosMap?.get("carbohydrates") ?: map["carbsG"] ?: map["carbs"])
    val fat = parseDouble(macrosMap?.get("fatG") ?: macrosMap?.get("fat") ?: map["fatG"] ?: map["fat"])

    val barcode = (map["barcode"] ?: map["code"] ?: map["barcodeNumber"])?.toString()?.takeIf { it.isNotBlank() }

    return FoodItem(
        id = id,
        name = name,
        brand = brand,
        per100g = Macros(
            kcal = kcal,
            proteinG = protein,
            carbsG = carbs,
            fatG = fat,
        ),
        barcode = barcode,
    )
}

private fun parseDouble(value: Any?): Double {
    return when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

/**
 * Lightweight, zero-dependency pure Kotlin JSON parser.
 * Works seamlessly in both JVM unit tests and Android runtime without external dependencies.
 */
internal object MiniJson {
    fun parse(json: String): Any? {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return null
        return Parser(trimmed).parseValue()
    }

    private class Parser(private val src: String) {
        private var pos = 0

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char? = if (pos < src.length) src[pos] else null

        fun parseValue(): Any? {
            skipWhitespace()
            return when (peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBoolean()
                'n' -> parseNull()
                null -> null
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            pos++ // skip '{'
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (pos < src.length) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                if (pos < src.length && src[pos] == ':') pos++
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == '}') {
                    pos++
                    break
                }
            }
            return map
        }

        private fun parseArray(): List<Any?> {
            val list = mutableListOf<Any?>()
            pos++ // skip '['
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (pos < src.length) {
                val value = parseValue()
                list.add(value)
                skipWhitespace()
                if (pos < src.length && src[pos] == ',') {
                    pos++
                } else if (pos < src.length && src[pos] == ']') {
                    pos++
                    break
                }
            }
            return list
        }

        private fun parseString(): String {
            if (pos >= src.length || src[pos] != '"') return ""
            pos++ // skip opening quote
            val sb = StringBuilder()
            while (pos < src.length) {
                val c = src[pos++]
                if (c == '"') break
                if (c == '\\' && pos < src.length) {
                    when (val esc = src[pos++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000c')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            if (pos + 4 <= src.length) {
                                val hex = src.substring(pos, pos + 4)
                                pos += 4
                                hex.toIntOrNull(16)?.let { sb.append(it.toChar()) }
                            }
                        }
                        else -> sb.append(esc)
                    }
                } else {
                    sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNumber(): Double? {
            val start = pos
            if (pos < src.length && (src[pos] == '-' || src[pos] == '+')) pos++
            while (pos < src.length && (src[pos].isDigit() || src[pos] == '.' || src[pos] == 'e' || src[pos] == 'E' || src[pos] == '+' || src[pos] == '-')) {
                pos++
            }
            return src.substring(start, pos).toDoubleOrNull()
        }

        private fun parseBoolean(): Boolean {
            return if (src.startsWith("true", pos)) {
                pos += 4
                true
            } else if (src.startsWith("false", pos)) {
                pos += 5
                false
            } else {
                pos++
                false
            }
        }

        private fun parseNull(): Any? {
            if (src.startsWith("null", pos)) {
                pos += 4
            }
            return null
        }
    }
}
