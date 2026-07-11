package com.apexlions.cepiptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URI

object PlaylistLoader {
    const val DEFAULT_USER_AGENT = "VLC/3.0.18 LibVLC/3.0.18 CepIPTV/1.1"

    fun isValidHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    suspend fun download(url: String, userAgent: String? = null): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", userAgent?.takeIf(String::isNotBlank) ?: DEFAULT_USER_AGENT)
            .header("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, application/xml, text/xml, text/plain, */*")
            .build()

        NetworkClient.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Sunucu ${response.code} durum kodunu döndürdü.")
            }
            response.body?.string()?.takeIf(String::isNotBlank)
                ?: error("Sunucu boş içerik döndürdü.")
        }
    }
}
