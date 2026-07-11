package com.apexlions.cepiptv.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.apexlions.cepiptv.R
import com.apexlions.cepiptv.data.FavoritesStore
import com.apexlions.cepiptv.data.M3uParser
import com.apexlions.cepiptv.data.PlaylistLoader
import com.apexlions.cepiptv.data.SourceType
import com.apexlions.cepiptv.data.StalkerClient
import com.apexlions.cepiptv.data.XmlTvParser
import com.apexlions.cepiptv.data.XtreamClient
import com.apexlions.cepiptv.model.EpgSnapshot
import com.apexlions.cepiptv.model.MediaEntry
import com.apexlions.cepiptv.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CepIptvApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { FavoritesStore(context.applicationContext) }

    var sourceType by rememberSaveable { mutableStateOf(SourceType.M3U) }
    var playlistUrl by rememberSaveable { mutableStateOf(store.getLastPlaylistUrl()) }
    var epgUrl by rememberSaveable { mutableStateOf(store.getLastEpgUrl()) }
    var customUserAgent by rememberSaveable { mutableStateOf(store.getLastUserAgent()) }
    var autoLoadM3u by rememberSaveable { mutableStateOf(store.shouldAutoLoadM3u()) }
    var xtreamServer by rememberSaveable { mutableStateOf("") }
    var xtreamUsername by rememberSaveable { mutableStateOf("") }
    var xtreamPassword by rememberSaveable { mutableStateOf("") }
    var stalkerPortal by rememberSaveable { mutableStateOf("") }
    var stalkerMac by rememberSaveable { mutableStateOf("") }

    var library by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var sourceTitle by remember { mutableStateOf("") }
    var epg by remember { mutableStateOf<Map<String, EpgSnapshot>>(emptyMap()) }
    var favorites by remember { mutableStateOf(store.getFavorites()) }
    var recentIds by remember { mutableStateOf(store.getRecentIds()) }
    var selectedEntry by remember { mutableStateOf<MediaEntry?>(null) }
    var seriesParent by remember { mutableStateOf<MediaEntry?>(null) }
    var seriesEpisodes by remember { mutableStateOf<List<MediaEntry>?>(null) }
    var activeXtreamClient by remember { mutableStateOf<XtreamClient?>(null) }
    var activeStalkerClient by remember { mutableStateOf<StalkerClient?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    suspend fun loadEpgIfConfigured() {
        if (!PlaylistLoader.isValidHttpUrl(epgUrl)) {
            epg = emptyMap()
            return
        }
        runCatching {
            XmlTvParser.parse(PlaylistLoader.download(epgUrl, customUserAgent))
        }.onSuccess {
            epg = it
            infoMessage = context.getString(R.string.epg_loaded)
        }.onFailure {
            epg = emptyMap()
            infoMessage = context.getString(R.string.epg_failed_nonfatal)
        }
    }

    suspend fun applyM3u(content: String, sourceKey: String) {
        val parsed = M3uParser.parse(content, sourceKey, customUserAgent)
        if (parsed.isEmpty()) error(context.getString(R.string.parse_failed))
        library = parsed
        sourceTitle = playlistUrl.takeIf(String::isNotBlank) ?: context.getString(R.string.local_playlist)
        activeXtreamClient = null
        activeStalkerClient = null
        seriesEpisodes = null
        seriesParent = null
        loadEpgIfConfigured()
    }

    fun runLoading(block: suspend () -> Unit) {
        scope.launch {
            isLoading = true
            errorMessage = null
            infoMessage = null
            runCatching { block() }.onFailure { throwable ->
                errorMessage = throwable.message?.takeIf(String::isNotBlank)
                    ?: context.getString(R.string.generic_load_failed)
            }
            isLoading = false
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runLoading {
            val content = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error(context.getString(R.string.file_read_failed))
            }
            applyM3u(content, "m3u-file|$uri")
        }
    }

    LaunchedEffect(Unit) {
        if (store.shouldAutoLoadM3u() && PlaylistLoader.isValidHttpUrl(store.getLastPlaylistUrl())) {
            isLoading = true
            runCatching {
                playlistUrl = store.getLastPlaylistUrl()
                epgUrl = store.getLastEpgUrl()
                customUserAgent = store.getLastUserAgent()
                val content = PlaylistLoader.download(playlistUrl, customUserAgent)
                applyM3u(content, "m3u-url|$playlistUrl")
            }.onFailure { errorMessage = context.getString(R.string.auto_load_failed) }
            isLoading = false
        }
    }

    selectedEntry?.let { entry ->
        PlayerScreen(
            entry = entry,
            isFavorite = entry.id in favorites,
            resumePositionMillis = if (entry.kind == MediaKind.MOVIE || entry.kind == MediaKind.EPISODE) {
                store.getResumePosition(entry.id)
            } else 0L,
            onBack = { selectedEntry = null },
            onToggleFavorite = { favorites = store.toggleFavorite(entry.id) },
            onSavePosition = { store.saveResumePosition(entry.id, it) },
        )
        return
    }

    val shownEntries = seriesEpisodes ?: library
    if (shownEntries.isNotEmpty()) {
        LibraryScreen(
            title = seriesParent?.name ?: sourceTitle,
            entries = shownEntries,
            epg = epg,
            favorites = favorites,
            recentIds = recentIds,
            isSeriesLevel = seriesEpisodes != null,
            isLoading = isLoading,
            errorMessage = errorMessage,
            infoMessage = infoMessage,
            onBackFromSeries = { seriesEpisodes = null; seriesParent = null; errorMessage = null },
            onChangeSource = {
                library = emptyList(); seriesEpisodes = null; seriesParent = null
                epg = emptyMap(); errorMessage = null; infoMessage = null
            },
            onToggleFavorite = { favorites = store.toggleFavorite(it.id) },
            onOpen = { entry ->
                when {
                    entry.kind == MediaKind.SERIES -> runLoading {
                        val client = activeXtreamClient ?: error(context.getString(R.string.series_source_missing))
                        val episodes = client.loadSeriesEpisodes(entry)
                        if (episodes.isEmpty()) error(context.getString(R.string.no_episodes))
                        seriesParent = entry
                        seriesEpisodes = episodes
                    }
                    entry.command != null -> runLoading {
                        val client = activeStalkerClient ?: error(context.getString(R.string.portal_session_missing))
                        recentIds = store.markRecent(entry.id)
                        selectedEntry = entry.copy(url = client.resolvePlaybackUrl(entry))
                    }
                    entry.isPlayable -> {
                        recentIds = store.markRecent(entry.id)
                        selectedEntry = entry
                    }
                }
            },
        )
        return
    }

    ImportScreen(
        sourceType = sourceType,
        onSourceTypeChange = { sourceType = it },
        playlistUrl = playlistUrl,
        onPlaylistUrlChange = { playlistUrl = it },
        epgUrl = epgUrl,
        onEpgUrlChange = { epgUrl = it },
        customUserAgent = customUserAgent,
        onCustomUserAgentChange = { customUserAgent = it },
        autoLoadM3u = autoLoadM3u,
        onAutoLoadM3uChange = { autoLoadM3u = it },
        xtreamServer = xtreamServer,
        onXtreamServerChange = { xtreamServer = it },
        xtreamUsername = xtreamUsername,
        onXtreamUsernameChange = { xtreamUsername = it },
        xtreamPassword = xtreamPassword,
        onXtreamPasswordChange = { xtreamPassword = it },
        stalkerPortal = stalkerPortal,
        onStalkerPortalChange = { stalkerPortal = it },
        stalkerMac = stalkerMac,
        onStalkerMacChange = { stalkerMac = it },
        isLoading = isLoading,
        errorMessage = errorMessage,
        infoMessage = infoMessage,
        onLoadM3uUrl = {
            if (!PlaylistLoader.isValidHttpUrl(playlistUrl)) errorMessage = context.getString(R.string.invalid_url)
            else runLoading {
                applyM3u(PlaylistLoader.download(playlistUrl, customUserAgent), "m3u-url|$playlistUrl")
                store.saveM3uConfig(playlistUrl, epgUrl, customUserAgent, autoLoadM3u)
            }
        },
        onLoadM3uFile = {
            fileLauncher.launch(arrayOf("application/x-mpegURL", "application/vnd.apple.mpegurl", "audio/x-mpegurl", "text/plain", "*/*"))
        },
        onLoadXtream = {
            if (!PlaylistLoader.isValidHttpUrl(xtreamServer) || xtreamUsername.isBlank() || xtreamPassword.isBlank()) {
                errorMessage = context.getString(R.string.xtream_fields_required)
            } else runLoading {
                val client = XtreamClient(xtreamServer, xtreamUsername, xtreamPassword)
                val result = client.loadLibrary()
                if (result.entries.isEmpty()) error(context.getString(R.string.empty_portal))
                activeXtreamClient = client; activeStalkerClient = null
                sourceTitle = result.title; library = result.entries; loadEpgIfConfigured()
            }
        },
        onLoadStalker = {
            if (!PlaylistLoader.isValidHttpUrl(stalkerPortal) || !StalkerClient.isValidMac(stalkerMac)) {
                errorMessage = context.getString(R.string.stalker_fields_required)
            } else runLoading {
                val client = StalkerClient(stalkerPortal, stalkerMac)
                val result = client.loadLibrary()
                activeStalkerClient = client; activeXtreamClient = null
                sourceTitle = result.title; library = result.entries; loadEpgIfConfigured()
            }
        },
    )
}
