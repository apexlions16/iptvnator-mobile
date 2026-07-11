package com.apexlions.cepiptv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit

object PlaylistLoader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun isValidHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    suspend fun download(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url.trim())
            .header("User-Agent", "VLC/3.0.18 LibVLC/3.0.18 CepIPTV/1.0")
            .header("Accept", "application/x-mpegURL, application/vnd.apple.mpegurl, text/plain, */*")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Sunucu ${response.code} durum kodunu döndürdü.")
            }
            response.body?.string()?.takeIf(String::isNotBlank)
                ?: error("Sunucu boş bir oynatma listesi döndürdü.")
        }
    }
}
