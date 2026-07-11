package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.MediaEntry
import com.apexlions.cepiptv.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class StalkerClient(
    portalUrl: String,
    private val macAddress: String,
) {
    private val portal: HttpUrl = normalizePortal(portalUrl)
    private val sourceKey = "stalker|${portal.host}|${macAddress.uppercase()}"
    private var token: String? = null

    suspend fun loadLibrary(): SourceResult = withContext(Dispatchers.IO) {
        authenticate()
        val genres = getGenres()
        val channels = getChannels().mapNotNull { item ->
            val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val command = item.optString("cmd").takeIf(String::isNotBlank)
            val directUrl = extractHttpUrl(command)
            MediaEntry(
                sourceKey = sourceKey,
                externalId = "stalker-$id",
                name = item.optString("name").ifBlank { "Adsız kanal" },
                url = directUrl,
                group = genres[item.optString("tv_genre_id")] ?: "Canlı TV",
                logoUrl = item.optString("logo").takeIf(String::isNotBlank),
                kind = MediaKind.LIVE,
                epgId = item.optString("xmltv_id").takeIf(String::isNotBlank),
                command = if (directUrl == null) command else null,
                userAgent = STB_USER_AGENT,
            )
        }
        if (channels.isEmpty()) error("Stalker portalında oynatılabilir kanal bulunamadı.")
        SourceResult(portal.host, channels, SourceType.STALKER)
    }

    suspend fun resolvePlaybackUrl(entry: MediaEntry): String = withContext(Dispatchers.IO) {
        entry.url?.takeIf(String::isNotBlank)?.let { return@withContext it }
        val command = entry.command ?: error("Kanal komutu bulunamadı.")
        authenticate()
        val payload = request(
            type = "itv",
            action = "create_link",
            extra = mapOf(
                "cmd" to command,
                "series" to "0",
                "forced_storage" to "undefined",
                "disable_ad" to "0",
                "download" to "0",
            ),
        )
        val resolvedCommand = payload.optJSONObject("js")?.optString("cmd")
            ?: payload.optString("cmd")
        extractHttpUrl(resolvedCommand) ?: error("Portal yayın bağlantısını oluşturamadı.")
    }

    private fun authenticate() {
        if (!token.isNullOrBlank()) return
        val payload = request("stb", "handshake", mapOf("token" to ""), includeToken = false)
        token = payload.optJSONObject("js")?.optString("token")
            ?.takeIf(String::isNotBlank)
            ?: error("Stalker portalı oturum anahtarı vermedi.")
        runCatching {
            request(
                "stb",
                "get_profile",
                mapOf(
                    "hd" to "1",
                    "ver" to "ImageDescription: 0.2.18-r23-pub-250; PORTAL version: 5.5.0; API Version: JS API version: 343; STB API version: 146",
                    "num_banks" to "2",
                    "sn" to "",
                    "stb_type" to "MAG250",
                    "client_type" to "STB",
                    "image_version" to "218",
                    "video_out" to "hdmi",
                    "device_id" to "",
                    "device_id2" to "",
                    "signature" to "",
                ),
            )
        }
    }

    private fun getGenres(): Map<String, String> {
        val array = request("itv", "get_genres").optJSONArray("js") ?: JSONArray()
        return buildMap {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val title = item.optString("title")
                if (id.isNotBlank() && title.isNotBlank()) put(id, title)
            }
        }
    }

    private fun getChannels(): List<JSONObject> {
        val payload = request("itv", "get_all_channels")
        val array = payload.optJSONObject("js")?.optJSONArray("data")
            ?: payload.optJSONArray("js")
            ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) array.optJSONObject(index)?.let(::add)
        }
    }

    private fun request(
        type: String,
        action: String,
        extra: Map<String, String> = emptyMap(),
        includeToken: Boolean = true,
    ): JSONObject {
        val url = portal.newBuilder()
            .addQueryParameter("type", type)
            .addQueryParameter("action", action)
            .apply {
                extra.forEach { (key, value) -> addQueryParameter(key, value) }
                addQueryParameter("JsHttpRequest", "1-xml")
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", STB_USER_AGENT)
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .header("Cookie", "mac=${macAddress.uppercase()}; stb_lang=tr; timezone=Europe%2FIstanbul")
            .header("Accept", "application/json")
            .apply {
                if (includeToken && !token.isNullOrBlank()) header("Authorization", "Bearer $token")
            }
            .build()
        return NetworkClient.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Stalker portalı ${response.code} durum kodunu döndürdü.")
            JSONObject(response.body?.string()?.ifBlank { "{}" } ?: "{}")
        }
    }

    private fun extractHttpUrl(command: String?): String? {
        if (command.isNullOrBlank()) return null
        return Regex("https?://\\S+", RegexOption.IGNORE_CASE)
            .find(command)?.value?.trimEnd('"', '\'', ';')
    }

    companion object {
        private const val STB_USER_AGENT = "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG250 stbapp ver: 4 rev: 272 Safari/533.3"

        fun isValidMac(value: String): Boolean =
            Regex("^[0-9A-Fa-f]{2}(:[0-9A-Fa-f]{2}){5}$").matches(value.trim())

        private fun normalizePortal(raw: String): HttpUrl {
            val prepared = raw.trim().removeSuffix("/")
                .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
            val base = "$prepared/".toHttpUrl()
            return if (base.encodedPath.contains("portal.php")) base
            else base.newBuilder().addPathSegment("portal.php").build()
        }
    }
}
