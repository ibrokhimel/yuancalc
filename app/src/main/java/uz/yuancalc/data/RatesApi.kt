package uz.yuancalc.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class FetchedRates(
    val cnyToUsd: Double,
    val usdToUzs: Double,
    val fetchedAtEpochSeconds: Long,
)

interface RatesApi {
    /** Returns null when the source responds but the response is unusable. */
    suspend fun fetch(): FetchedRates?
}

private val json = Json { ignoreUnknownKeys = true }

fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

private fun OkHttpClient.getBody(url: String): String? {
    val request = Request.Builder().url(url).build()
    newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        return response.body?.string()
    }
}

/** open.er-api.com: { "rates": { "UZS": n, "CNY": n }, "time_last_update_unix": n } */
class OpenErRatesApi(private val client: OkHttpClient) : RatesApi {
    override suspend fun fetch(): FetchedRates? {
        val body = client.getBody("https://open.er-api.com/v6/latest/USD") ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        val rates = root["rates"]?.jsonObject ?: return null
        val usdToUzs = rates["UZS"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        val usdToCny = rates["CNY"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        if (usdToCny == 0.0) return null
        val at = root["time_last_update_unix"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: (System.currentTimeMillis() / 1000)
        return FetchedRates(1.0 / usdToCny, usdToUzs, at)
    }
}

/** fawazahmed0 currency-api: { "date": "...", "usd": { "uzs": n, "cny": n } } */
class CurrencyApiRatesApi(private val client: OkHttpClient) : RatesApi {
    override suspend fun fetch(): FetchedRates? {
        val body = client.getBody(
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/usd.json"
        ) ?: return null
        val root = json.parseToJsonElement(body).jsonObject
        val usd = root["usd"]?.jsonObject ?: return null
        val usdToUzs = usd["uzs"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        val usdToCny = usd["cny"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return null
        if (usdToCny == 0.0) return null
        return FetchedRates(1.0 / usdToCny, usdToUzs, System.currentTimeMillis() / 1000)
    }
}
