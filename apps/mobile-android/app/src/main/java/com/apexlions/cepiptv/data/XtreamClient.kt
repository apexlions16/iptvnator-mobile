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

class XtreamClient(
    serverUrl: String,
    private val username: String,
    private val password: String,
) {
    private val baseUrl: HttpUrl = normalizeServer(serverUrl)
    private val sourceKey = "xtream|${baseUrl.host}|$username"

    suspend fun loadLibrary(): SourceResult = withContext(Dispatchers.IO) {
        val account = requestObject(null)
        val authenticated = account.optJSONObject("user_info")?.optInt("auth", 0) == 1
        if (!authenticated) error("Xtream kullanıcı adı veya parolası doğrulanamadı.")

        val liveCategories = categoryMap(requestArray("get_live_categories"))
        val vodCategories = categoryMap(requestArray("get_vod_categories"))
        val seriesCategories = categoryMap(requestArray("get_series_categories"))
        val entries = buildList {
            addAll(parseLive(requestArray("get_live_streams"), liveCategories))
            addAll(parseVod(requestArray("get_vod_streams"), vodCategories))
            addAll(parseSeries(requestArray("get_series"), seriesCategories))
        }
        SourceResult(
            title = account.optJSONObject("server_info")?.optString("url")
                ?.takeIf(String::isNotBlank) ?: baseUrl.host,
            entries = entries,
            sourceType = SourceType.XTREAM,
        )
    }

    suspend fun loadSeriesEpisodes(series: MediaEntry): List<MediaEntry> = withContext(Dispatchers.IO) {
        val seriesId = series.seriesId ?: error("Dizi kimliği bulunamadı.")
        val result = requestObject("get_series_info", mapOf("series_id" to seriesId))
        val episodesObject = result.optJSONObject("episodes") ?: return@withContext emptyList()
        val episodes = mutableListOf<MediaEntry>()
        val seasons = episodesObject.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
        seasons.forEach { season ->
            val seasonItems = episodesObject.optJSONArray(season) ?: JSONArray()
            for (index in 0 until seasonItems.length()) {
                val item = seasonItems.optJSONObject(index) ?: continue
                val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                val info = item.optJSONObject("info")
                val extension = item.optString("container_extension", "mp4").ifBlank { "mp4" }
                val episodeNumber = item.optInt("episode_num", index + 1)
                episodes += MediaEntry(
                    sourceKey = sourceKey,
                    externalId = "episode-$id",
                    name = item.optString("title").takeIf(String::isNotBlank) ?: "${episodeNumber}. bölüm",
                    url = playbackUrl("series", id, extension),
                    group = "${series.name} • ${season}. sezon",
                    logoUrl = info?.optString("movie_image")?.takeIf(String::isNotBlank) ?: series.logoUrl,
                    kind = MediaKind.EPISODE,
                    description = info?.optString("plot")?.takeIf(String::isNotBlank) ?: series.description,
                    userAgent = PlaylistLoader.DEFAULT_USER_AGENT,
                )
            }
        }
        episodes
    }

    private fun parseLive(items: JSONArray, categories: Map<String, String>): List<MediaEntry> = buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val streamId = item.optString("stream_id").takeIf(String::isNotBlank) ?: continue
            val categoryId = item.optString("category_id")
            val group = categories[categoryId] ?: "Canlı TV"
            val radio = item.optString("stream_type").equals("radio_streams", true) ||
                group.contains("radyo", true) || group.contains("radio", true)
            add(
                MediaEntry(
                    sourceKey = sourceKey,
                    externalId = "live-$streamId",
                    name = item.optString("name").ifBlank { "Adsız kanal" },
                    url = playbackUrl("live", streamId, "m3u8"),
                    group = group,
                    logoUrl = item.optString("stream_icon").takeIf(String::isNotBlank),
                    kind = if (radio) MediaKind.RADIO else MediaKind.LIVE,
                    epgId = item.optString("epg_channel_id").takeIf(String::isNotBlank),
                    userAgent = PlaylistLoader.DEFAULT_USER_AGENT,
                )
            )
        }
    }

    private fun parseVod(items: JSONArray, categories: Map<String, String>): List<MediaEntry> = buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val streamId = item.optString("stream_id").takeIf(String::isNotBlank) ?: continue
            val extension = item.optString("container_extension", "mp4").ifBlank { "mp4" }
            val categoryId = item.optString("category_id")
            add(
                MediaEntry(
                    sourceKey = sourceKey,
                    externalId = "movie-$streamId",
                    name = item.optString("name").ifBlank { "Adsız film" },
                    url = playbackUrl("movie", streamId, extension),
                    group = categories[categoryId] ?: "Filmler",
                    logoUrl = item.optString("stream_icon").takeIf(String::isNotBlank),
                    kind = MediaKind.MOVIE,
                    description = item.optString("plot").takeIf(String::isNotBlank),
                    userAgent = PlaylistLoader.DEFAULT_USER_AGENT,
                )
            )
        }
    }

    private fun parseSeries(items: JSONArray, categories: Map<String, String>): List<MediaEntry> = buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val seriesId = item.optString("series_id").takeIf(String::isNotBlank) ?: continue
            val categoryId = item.optString("category_id")
            add(
                MediaEntry(
                    sourceKey = sourceKey,
                    externalId = "series-$seriesId",
                    name = item.optString("name").ifBlank { "Adsız dizi" },
                    group = categories[categoryId] ?: "Diziler",
                    logoUrl = item.optString("cover").takeIf(String::isNotBlank),
                    kind = MediaKind.SERIES,
                    description = item.optString("plot").takeIf(String::isNotBlank),
                    seriesId = seriesId,
                )
            )
        }
    }

    private fun categoryMap(array: JSONArray): Map<String, String> = buildMap {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optString("category_id")
            val name = item.optString("category_name")
            if (id.isNotBlank() && name.isNotBlank()) put(id, name)
        }
    }

    private fun requestArray(action: String): JSONArray = requestBody(action) as? JSONArray ?: JSONArray()

    private fun requestObject(action: String?, extra: Map<String, String> = emptyMap()): JSONObject =
        requestBody(action, extra) as? JSONObject ?: JSONObject()

    private fun requestBody(action: String?, extra: Map<String, String> = emptyMap()): Any {
        val url = baseUrl.newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", username)
            .addQueryParameter("password", password)
            .apply {
                if (!action.isNullOrBlank()) addQueryParameter("action", action)
                extra.forEach { (key, value) -> addQueryParameter(key, value) }
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", PlaylistLoader.DEFAULT_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return NetworkClient.http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Xtream sunucusu ${response.code} durum kodunu döndürdü.")
            val body = response.body?.string()?.trim().orEmpty()
            if (body.startsWith("[")) JSONArray(body) else JSONObject(body.ifBlank { "{}" })
        }
    }

    private fun playbackUrl(type: String, id: String, extension: String): String = baseUrl.newBuilder()
        .addPathSegment(type)
        .addPathSegment(username)
        .addPathSegment(password)
        .addPathSegment("$id.$extension")
        .build()
        .toString()

    companion object {
        private fun normalizeServer(raw: String): HttpUrl {
            val prepared = raw.trim().removeSuffix("/")
                .removeSuffix("/player_api.php")
                .let { if (it.startsWith("http://") || it.startsWith("https://")) it else "http://$it" }
            return "$prepared/".toHttpUrl()
        }
    }
}
