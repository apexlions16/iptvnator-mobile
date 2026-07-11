package com.apexlions.cepiptv.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("cep_iptv_ayarlar", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> =
        preferences.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()

    fun toggleFavorite(entryId: String): Set<String> {
        val updated = getFavorites().toMutableSet().apply {
            if (!add(entryId)) remove(entryId)
        }
        preferences.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return updated
    }

    fun getRecentIds(): List<String> = preferences.getString(KEY_RECENT, "")
        .orEmpty().lineSequence().filter(String::isNotBlank).toList()

    fun markRecent(entryId: String): List<String> {
        val updated = buildList {
            add(entryId)
            addAll(getRecentIds().filterNot { it == entryId })
        }.take(30)
        preferences.edit().putString(KEY_RECENT, updated.joinToString("\n")).apply()
        return updated
    }

    fun getResumePosition(entryId: String): Long =
        preferences.getLong("$KEY_POSITION_PREFIX$entryId", 0L)

    fun saveResumePosition(entryId: String, positionMillis: Long) {
        preferences.edit().putLong("$KEY_POSITION_PREFIX$entryId", positionMillis.coerceAtLeast(0L)).apply()
    }

    fun getLastPlaylistUrl(): String = preferences.getString(KEY_LAST_URL, "").orEmpty()
    fun getLastEpgUrl(): String = preferences.getString(KEY_LAST_EPG_URL, "").orEmpty()
    fun getLastUserAgent(): String = preferences.getString(KEY_LAST_USER_AGENT, "").orEmpty()
    fun shouldAutoLoadM3u(): Boolean = preferences.getBoolean(KEY_AUTO_LOAD, false)

    fun saveM3uConfig(url: String, epgUrl: String, userAgent: String, autoLoad: Boolean) {
        preferences.edit()
            .putString(KEY_LAST_URL, url.trim())
            .putString(KEY_LAST_EPG_URL, epgUrl.trim())
            .putString(KEY_LAST_USER_AGENT, userAgent.trim())
            .putBoolean(KEY_AUTO_LOAD, autoLoad)
            .apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favori_icerikler"
        private const val KEY_RECENT = "son_izlenenler"
        private const val KEY_POSITION_PREFIX = "izleme_konumu_"
        private const val KEY_LAST_URL = "son_liste_adresi"
        private const val KEY_LAST_EPG_URL = "son_epg_adresi"
        private const val KEY_LAST_USER_AGENT = "son_user_agent"
        private const val KEY_AUTO_LOAD = "m3u_otomatik_yukle"
    }
}
