package com.apexlions.cepiptv.data

import android.content.Context

class FavoritesStore(context: Context) {
    private val preferences = context.getSharedPreferences("cep_iptv_ayarlar", Context.MODE_PRIVATE)

    fun getFavorites(): Set<String> =
        preferences.getStringSet(KEY_FAVORITES, emptySet())?.toSet().orEmpty()

    fun toggleFavorite(channelId: String): Set<String> {
        val updated = getFavorites().toMutableSet().apply {
            if (!add(channelId)) remove(channelId)
        }
        preferences.edit().putStringSet(KEY_FAVORITES, updated).apply()
        return updated
    }

    fun getLastPlaylistUrl(): String = preferences.getString(KEY_LAST_URL, "").orEmpty()

    fun saveLastPlaylistUrl(url: String) {
        preferences.edit().putString(KEY_LAST_URL, url.trim()).apply()
    }

    companion object {
        private const val KEY_FAVORITES = "favori_kanallar"
        private const val KEY_LAST_URL = "son_liste_adresi"
    }
}
