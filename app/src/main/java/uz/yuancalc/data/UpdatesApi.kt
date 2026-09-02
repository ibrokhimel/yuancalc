package uz.yuancalc.data

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class LatestRelease(
    /** The release tag with any leading "v" stripped, e.g. "1.2". */
    val versionName: String,
    /** The release page — the fallback when no APK asset is attached. */
    val pageUrl: String,
    /** Direct download URL of the attached APK, when the release has one. */
    val apkUrl: String?,
)

interface UpdatesApi {
    /** Null when the check fails for any reason; never throws. */
    suspend fun fetchLatest(): LatestRelease?

    /**
     * Streams [url] into [into], reporting whole-percent progress. False on
     * any failure; never throws.
     */
    suspend fun download(url: String, into: File, onProgress: (Int) -> Unit): Boolean
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
                val apk = root["assets"]?.jsonArray?.firstNotNullOfOrNull { asset ->
                    val obj = asset.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content
                    if (name != null && name.endsWith(".apk")) {
                        obj["browser_download_url"]?.jsonPrimitive?.content
                    } else {
                        null
                    }
                }
                LatestRelease(
                    versionName = tag.removePrefix("v").removePrefix("V"),
                    pageUrl = url,
                    apkUrl = apk,
                )
            }
        }.getOrNull()
    }

    override suspend fun download(
        url: String,
        into: File,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                into.parentFile?.mkdirs()
                val total = body.contentLength()
                into.outputStream().use { out ->
                    val input = body.byteStream()
                    val buffer = ByteArray(64 * 1024)
                    var received = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        received += n
                        if (total > 0) onProgress(((received * 100) / total).toInt())
                    }
                }
                true
            }
        }.getOrDefault(false)
    }
}
