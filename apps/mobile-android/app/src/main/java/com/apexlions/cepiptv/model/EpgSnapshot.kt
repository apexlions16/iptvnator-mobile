package com.apexlions.cepiptv.model

data class EpgSnapshot(
    val currentTitle: String? = null,
    val currentStartMillis: Long? = null,
    val currentEndMillis: Long? = null,
    val nextTitle: String? = null,
)
