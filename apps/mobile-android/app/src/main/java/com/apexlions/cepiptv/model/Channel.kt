package com.apexlions.cepiptv.model

import java.security.MessageDigest

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Diğer",
    val logoUrl: String? = null,
) {
    val id: String = MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}
