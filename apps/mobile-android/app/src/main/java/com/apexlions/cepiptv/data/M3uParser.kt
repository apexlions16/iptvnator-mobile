package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.MediaEntry
import com.apexlions.cepiptv.model.MediaKind

object M3uParser {
    private val attributePattern = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(
        content: String,
        sourceKey: String = "m3u",
        userAgent: String? = null,
    ): List<MediaEntry> {
        val lines = content.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        val entries = mutableListOf<MediaEntry>()
        var pendingInfo: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> pendingInfo = line
                line.startsWith("#") -> Unit
                line.startsWith("http://", ignoreCase = true) ||
                    line.startsWith("https://", ignoreCase = true) -> {
                    val info = pendingInfo
                    val attributes = info
                        ?.let { attributePattern.findAll(it) }
                        ?.associate { match -> match.groupValues[1].lowercase() to match.groupValues[2] }
                        .orEmpty()

                    val listedName = info
                        ?.substringAfter(',', missingDelimiterValue = "")
                        ?.trim()
                        .orEmpty()
                    val name = attributes["tvg-name"]
                        ?.takeIf(String::isNotBlank)
                        ?: listedName.takeIf(String::isNotBlank)
                        ?: "Adsız kanal"
                    val group = attributes["group-title"]
                        ?.takeIf(String::isNotBlank)
                        ?: "Diğer"
                    val radio = attributes["radio"].equals("true", ignoreCase = true) ||
                        group.contains("radyo", ignoreCase = true) ||
                        group.contains("radio", ignoreCase = true)

                    entries += MediaEntry(
                        sourceKey = sourceKey,
                        externalId = line,
                        name = name,
                        url = line,
                        group = group,
                        logoUrl = attributes["tvg-logo"]?.takeIf(String::isNotBlank),
                        kind = if (radio) MediaKind.RADIO else MediaKind.LIVE,
                        epgId = attributes["tvg-id"]?.takeIf(String::isNotBlank)
                            ?: attributes["tvg-name"]?.takeIf(String::isNotBlank),
                        userAgent = userAgent?.takeIf(String::isNotBlank),
                    )
                    pendingInfo = null
                }
            }
        }

        return entries.distinctBy { it.url }
    }
}
