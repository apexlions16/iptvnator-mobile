package com.apexlions.cepiptv.model

import java.security.MessageDigest

enum class MediaKind {
    LIVE,
    RADIO,
    MOVIE,
    SERIES,
    EPISODE,
}

data class MediaEntry(
    val sourceKey: String,
    val externalId: String,
    val name: String,
    val url: String? = null,
    val group: String = "Diğer",
    val logoUrl: String? = null,
    val kind: MediaKind = MediaKind.LIVE,
    val epgId: String? = null,
    val description: String? = null,
    val seriesId: String? = null,
    val command: String? = null,
    val userAgent: String? = null,
) {
    val id: String = MessageDigest.getInstance("SHA-256")
        .digest("$sourceKey|${kind.name}|$externalId".toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    val isPlayable: Boolean
        get() = !url.isNullOrBlank() || !command.isNullOrBlank()
}
