package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.Channel

object M3uParser {
    private val attributePattern = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    fun parse(content: String): List<Channel> {
        val lines = content.lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        val channels = mutableListOf<Channel>()
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
                    val logo = attributes["tvg-logo"]
                        ?.takeIf(String::isNotBlank)

                    channels += Channel(
                        name = name,
                        url = line,
                        group = group,
                        logoUrl = logo,
                    )
                    pendingInfo = null
                }
            }
        }

        return channels.distinctBy(Channel::url)
    }
}
