package com.apexlions.cepiptv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.apexlions.cepiptv.R
import com.apexlions.cepiptv.model.EpgSnapshot
import com.apexlions.cepiptv.model.MediaEntry
import com.apexlions.cepiptv.model.MediaKind

private enum class LibraryMode { ALL, FAVORITES, RECENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    title: String,
    entries: List<MediaEntry>,
    epg: Map<String, EpgSnapshot>,
    favorites: Set<String>,
    recentIds: List<String>,
    isSeriesLevel: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onBackFromSeries: () -> Unit,
    onChangeSource: () -> Unit,
    onToggleFavorite: (MediaEntry) -> Unit,
    onOpen: (MediaEntry) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<MediaKind?>(null) }
    var group by rememberSaveable { mutableStateOf<String?>(null) }
    var mode by rememberSaveable { mutableStateOf(LibraryMode.ALL) }
    val kinds = remember(entries) { entries.map(MediaEntry::kind).distinct() }
    val groups = remember(entries, kind) {
        entries.filter { kind == null || it.kind == kind }.map(MediaEntry::group).distinct().sorted()
    }
    val recentOrder = remember(recentIds) { recentIds.withIndex().associate { it.value to it.index } }
    val filtered = entries.filter { entry ->
        (kind == null || entry.kind == kind) &&
            (group == null || entry.group == group) &&
            (query.isBlank() || entry.name.contains(query, true) || entry.group.contains(query, true) || entry.description.orEmpty().contains(query, true)) &&
            when (mode) {
                LibraryMode.ALL -> true
                LibraryMode.FAVORITES -> entry.id in favorites
                LibraryMode.RECENT -> entry.id in recentOrder
            }
    }.let { list -> if (mode == LibraryMode.RECENT) list.sortedBy { recentOrder[it.id] ?: Int.MAX_VALUE } else list }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.item_count, entries.size), style = MaterialTheme.typography.labelMedium)
                    }
                },
                navigationIcon = {
                    if (isSeriesLevel) IconButton(onClick = onBackFromSeries) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onChangeSource) {
                        Icon(Icons.Default.Source, contentDescription = stringResource(R.string.change_source))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.search_content)) },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = mode == LibraryMode.ALL, onClick = { mode = LibraryMode.ALL }, label = { Text(stringResource(R.string.all_content)) })
                FilterChip(
                    selected = mode == LibraryMode.FAVORITES,
                    onClick = { mode = if (mode == LibraryMode.FAVORITES) LibraryMode.ALL else LibraryMode.FAVORITES },
                    label = { Text(stringResource(R.string.favorites)) },
                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                FilterChip(
                    selected = mode == LibraryMode.RECENT,
                    onClick = { mode = if (mode == LibraryMode.RECENT) LibraryMode.ALL else LibraryMode.RECENT },
                    label = { Text(stringResource(R.string.recently_viewed)) },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            if (kinds.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = kind == null, onClick = { kind = null; group = null }, label = { Text(stringResource(R.string.all_types)) })
                    kinds.forEach { itemKind ->
                        FilterChip(
                            selected = kind == itemKind,
                            onClick = { kind = if (kind == itemKind) null else itemKind; group = null },
                            label = { Text(kindLabel(itemKind)) },
                            leadingIcon = { Icon(kindIcon(itemKind), contentDescription = null, modifier = Modifier.size(18.dp)) },
                        )
                    }
                }
            }
            if (groups.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = group == null, onClick = { group = null }, label = { Text(stringResource(R.string.all_groups)) })
                    groups.forEach { itemGroup ->
                        FilterChip(
                            selected = group == itemGroup,
                            onClick = { group = if (group == itemGroup) null else itemGroup },
                            label = { Text(itemGroup, maxLines = 1) },
                        )
                    }
                }
            }
            infoMessage?.let { InlineStatus(it, false) }
            errorMessage?.let { InlineStatus(it, true) }
            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.loading))
                }
            }
            if (filtered.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        when (mode) {
                            LibraryMode.FAVORITES -> stringResource(R.string.no_favorites)
                            LibraryMode.RECENT -> stringResource(R.string.no_recent)
                            LibraryMode.ALL -> stringResource(R.string.no_content)
                        },
                        modifier = Modifier.padding(24.dp),
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = MediaEntry::id) { entry ->
                        EntryRow(
                            entry = entry,
                            epgSnapshot = entry.epgId?.let(epg::get) ?: epg[entry.name],
                            isFavorite = entry.id in favorites,
                            onToggleFavorite = { onToggleFavorite(entry) },
                            onOpen = { onOpen(entry) },
                        )
                    }
                    item { Spacer(Modifier.size(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: MediaEntry,
    epgSnapshot: EpgSnapshot?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).clickable(onClick = onOpen),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(58.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!entry.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.logoUrl,
                        contentDescription = entry.name,
                        modifier = Modifier.fillMaxSize().padding(5.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else Icon(kindIcon(entry.kind), contentDescription = null)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(entry.group, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                epgSnapshot?.currentTitle?.let {
                    Text(stringResource(R.string.now_playing, it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                }
                epgSnapshot?.nextTitle?.let {
                    Text(stringResource(R.string.next_program, it), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(if (isFavorite) R.string.remove_favorite else R.string.add_favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InlineStatus(message: String, isError: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Text(message, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun kindLabel(kind: MediaKind): String = when (kind) {
    MediaKind.LIVE -> stringResource(R.string.live_tv)
    MediaKind.RADIO -> stringResource(R.string.radio)
    MediaKind.MOVIE -> stringResource(R.string.movies)
    MediaKind.SERIES -> stringResource(R.string.series)
    MediaKind.EPISODE -> stringResource(R.string.episodes)
}

private fun kindIcon(kind: MediaKind): ImageVector = when (kind) {
    MediaKind.LIVE -> Icons.Default.LiveTv
    MediaKind.RADIO -> Icons.Default.Podcasts
    MediaKind.MOVIE -> Icons.Default.Movie
    MediaKind.SERIES -> Icons.Default.VideoLibrary
    MediaKind.EPISODE -> Icons.Default.Tv
}
