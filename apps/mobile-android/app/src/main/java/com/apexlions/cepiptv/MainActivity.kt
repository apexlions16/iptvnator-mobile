package com.apexlions.cepiptv

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.apexlions.cepiptv.data.FavoritesStore
import com.apexlions.cepiptv.data.M3uParser
import com.apexlions.cepiptv.data.PlaylistLoader
import com.apexlions.cepiptv.model.Channel
import com.apexlions.cepiptv.ui.theme.CepIptvTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CepIptvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CepIptvApp()
                }
            }
        }
    }
}

@Composable
private fun CepIptvApp() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val store = remember { FavoritesStore(context.applicationContext) }

    var playlistUrl by rememberSaveable { mutableStateOf(store.getLastPlaylistUrl()) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var favorites by remember { mutableStateOf(store.getFavorites()) }
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }

    fun applyPlaylist(content: String) {
        val parsed = M3uParser.parse(content)
        if (parsed.isEmpty()) {
            errorMessage = context.getString(R.string.parse_failed)
            return
        }
        channels = parsed
        selectedGroup = null
        favoritesOnly = false
        searchQuery = ""
        errorMessage = null
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: error("Dosya açılamadı.")
                }
            }
            result.onSuccess(::applyPlaylist)
                .onFailure { errorMessage = context.getString(R.string.file_read_failed) }
            isLoading = false
        }
    }

    selectedChannel?.let { channel ->
        PlayerScreen(
            channel = channel,
            isFavorite = channel.id in favorites,
            onBack = { selectedChannel = null },
            onToggleFavorite = { favorites = store.toggleFavorite(channel.id) },
        )
        return
    }

    if (channels.isEmpty()) {
        ImportScreen(
            playlistUrl = playlistUrl,
            onPlaylistUrlChange = { playlistUrl = it },
            isLoading = isLoading,
            errorMessage = errorMessage,
            onLoadFromUrl = {
                if (!PlaylistLoader.isValidHttpUrl(playlistUrl)) {
                    errorMessage = context.getString(R.string.invalid_url)
                } else {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        runCatching { PlaylistLoader.download(playlistUrl) }
                            .onSuccess { content ->
                                applyPlaylist(content)
                                if (channels.isNotEmpty()) store.saveLastPlaylistUrl(playlistUrl)
                            }
                            .onFailure {
                                errorMessage = context.getString(R.string.download_failed)
                            }
                        isLoading = false
                    }
                }
            },
            onLoadFromFile = {
                fileLauncher.launch(
                    arrayOf(
                        "application/x-mpegURL",
                        "application/vnd.apple.mpegurl",
                        "audio/x-mpegurl",
                        "text/plain",
                        "*/*",
                    )
                )
            },
        )
    } else {
        ChannelListScreen(
            channels = channels,
            favorites = favorites,
            searchQuery = searchQuery,
            selectedGroup = selectedGroup,
            favoritesOnly = favoritesOnly,
            onSearchChange = { searchQuery = it },
            onGroupChange = { selectedGroup = it },
            onFavoritesOnlyChange = { favoritesOnly = it },
            onToggleFavorite = { channel -> favorites = store.toggleFavorite(channel.id) },
            onPlay = { selectedChannel = it },
            onClear = {
                channels = emptyList()
                searchQuery = ""
                selectedGroup = null
                favoritesOnly = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportScreen(
    playlistUrl: String,
    onPlaylistUrlChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onLoadFromUrl: () -> Unit,
    onLoadFromFile: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.app_slogan),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Oynatma listenizi ekleyin", fontWeight = FontWeight.SemiBold)
                            Text(
                                "M3U veya M3U8 adresi kullanabilir ya da cihazınızdan bir dosya seçebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = playlistUrl,
                        onValueChange = onPlaylistUrlChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.playlist_url)) },
                        placeholder = { Text(stringResource(R.string.playlist_url_hint)) },
                        singleLine = true,
                    )

                    Button(
                        onClick = onLoadFromUrl,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.loading))
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.load_from_url))
                        }
                    }

                    OutlinedButton(
                        onClick = onLoadFromFile,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.load_from_file))
                    }
                }
            }

            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    )
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            InfoCard(stringResource(R.string.network_note))
            InfoCard(stringResource(R.string.legal_note))
            InfoCard(stringResource(R.string.source_note))
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelListScreen(
    channels: List<Channel>,
    favorites: Set<String>,
    searchQuery: String,
    selectedGroup: String?,
    favoritesOnly: Boolean,
    onSearchChange: (String) -> Unit,
    onGroupChange: (String?) -> Unit,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onPlay: (Channel) -> Unit,
    onClear: () -> Unit,
) {
    val groups = remember(channels) { channels.map(Channel::group).distinct().sorted() }
    val filteredChannels = channels.filter { channel ->
        val matchesSearch = searchQuery.isBlank() ||
            channel.name.contains(searchQuery, ignoreCase = true) ||
            channel.group.contains(searchQuery, ignoreCase = true)
        val matchesGroup = selectedGroup == null || channel.group == selectedGroup
        val matchesFavorite = !favoritesOnly || channel.id in favorites
        matchesSearch && matchesGroup && matchesFavorite
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.channels), fontWeight = FontWeight.Bold)
                        Text(
                            stringResource(R.string.channel_count, channels.size),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.clear_list),
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.search_channel)) },
                singleLine = true,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedGroup == null && !favoritesOnly,
                    onClick = {
                        onGroupChange(null)
                        onFavoritesOnlyChange(false)
                    },
                    label = { Text(stringResource(R.string.all_channels)) },
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { onFavoritesOnlyChange(!favoritesOnly) },
                    label = { Text(stringResource(R.string.favorites)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
                groups.forEach { group ->
                    FilterChip(
                        selected = selectedGroup == group,
                        onClick = {
                            onGroupChange(if (selectedGroup == group) null else group)
                        },
                        label = { Text(group, maxLines = 1) },
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (filteredChannels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (favoritesOnly) stringResource(R.string.no_favorites)
                        else stringResource(R.string.no_channels),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredChannels, key = Channel::id) { channel ->
                        ChannelRow(
                            channel = channel,
                            isFavorite = channel.id in favorites,
                            onToggleFavorite = { onToggleFavorite(channel) },
                            onPlay = { onPlay(channel) },
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onPlay),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Icon(Icons.Default.Tv, contentDescription = null)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.group,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(
                        if (isFavorite) R.string.remove_favorite else R.string.add_favorite
                    ),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    channel: Channel,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playbackError by remember(channel.url) { mutableStateOf<String?>(null) }

    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri(channel.url)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(channel.name)
                        .setArtist(channel.group)
                        .build()
                )
                .build()
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = context.getString(R.string.playback_error)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.play()
                Lifecycle.Event.ON_PAUSE -> player.pause()
                else -> Unit
            }
        }

        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        (context as? ComponentActivity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            player.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
            (context as? ComponentActivity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        channel.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.remove_favorite else R.string.add_favorite
                            ),
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { it.player = player },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(androidx.compose.ui.graphics.Color.Black),
            )

            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(channel.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(channel.group, color = MaterialTheme.colorScheme.onSurfaceVariant)
                playbackError?.let { message ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        )
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
        }
    }
}
