package com.apexlions.cepiptv.data

import com.apexlions.cepiptv.model.MediaEntry

enum class SourceType {
    M3U,
    XTREAM,
    STALKER,
}

data class SourceResult(
    val title: String,
    val entries: List<MediaEntry>,
    val sourceType: SourceType,
)
