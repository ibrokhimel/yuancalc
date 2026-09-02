package uz.yuancalc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class LatestRelease(
    /** The release tag with any leading "v" stripped, e.g. "1.2". */
    val versionName: String,
    /** The release page — where the APK is downloaded from. */
    val pageUrl: String,
)

interface UpdatesApi {
    /** Null when the check fails for any reason; never throws. */
    suspend fun fetchLatest(): LatestRelease?
}

private val json = Json { ignoreUnknownKeys = true }

/**
 * Reads the newest release from GitHub. Unauthenticated, which is fine for a
 * user-triggered check (60 requests/hour per IP); the caller treats any
 * failure — offline, rate-limited, no releases yet — as "couldn't check",
 * never as "update available".
 */
class GitHubUpdatesApi(
    private val client: OkHttpClient,
    private val repo: String = "ibrokhimel/yuancalc",
) : UpdatesApi {
    override suspend fun fetchLatest(): LatestRelease? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val root = json.parseToJsonElement(body).jsonObject
                val tag = root["tag_name"]?.jsonPrimitive?.content ?: return@use null
                val url = root["html_url"]?.jsonPrimitive?.content ?: return@use null
                LatestRelease(versionName = tag.removePrefix("v").removePrefix("V"), pageUrl = url)
            }
        }.getOrNull()
    }
}
