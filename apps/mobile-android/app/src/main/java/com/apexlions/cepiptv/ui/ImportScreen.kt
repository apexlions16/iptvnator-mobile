package com.apexlions.cepiptv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.apexlions.cepiptv.R
import com.apexlions.cepiptv.data.SourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    sourceType: SourceType,
    onSourceTypeChange: (SourceType) -> Unit,
    playlistUrl: String,
    onPlaylistUrlChange: (String) -> Unit,
    epgUrl: String,
    onEpgUrlChange: (String) -> Unit,
    customUserAgent: String,
    onCustomUserAgentChange: (String) -> Unit,
    autoLoadM3u: Boolean,
    onAutoLoadM3uChange: (Boolean) -> Unit,
    xtreamServer: String,
    onXtreamServerChange: (String) -> Unit,
    xtreamUsername: String,
    onXtreamUsernameChange: (String) -> Unit,
    xtreamPassword: String,
    onXtreamPasswordChange: (String) -> Unit,
    stalkerPortal: String,
    onStalkerPortalChange: (String) -> Unit,
    stalkerMac: String,
    onStalkerMacChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    infoMessage: String?,
    onLoadM3uUrl: () -> Unit,
    onLoadM3uFile: () -> Unit,
    onLoadXtream: () -> Unit,
    onLoadStalker: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.app_slogan), style = MaterialTheme.typography.labelMedium)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(stringResource(R.string.add_source), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.add_source_description), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SourceType.entries.forEach { type ->
                            FilterChip(
                                selected = sourceType == type,
                                onClick = { onSourceTypeChange(type) },
                                label = {
                                    Text(
                                        when (type) {
                                            SourceType.M3U -> stringResource(R.string.source_m3u)
                                            SourceType.XTREAM -> stringResource(R.string.source_xtream)
                                            SourceType.STALKER -> stringResource(R.string.source_stalker)
                                        }
                                    )
                                },
                            )
                        }
                    }

                    when (sourceType) {
                        SourceType.M3U -> {
                            Field(playlistUrl, onPlaylistUrlChange, R.string.playlist_url, R.string.playlist_url_hint, !isLoading)
                            OutlinedTextField(
                                value = customUserAgent,
                                onValueChange = onCustomUserAgentChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.custom_user_agent)) },
                                supportingText = { Text(stringResource(R.string.custom_user_agent_optional)) },
                                enabled = !isLoading,
                                singleLine = true,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = autoLoadM3u, onCheckedChange = onAutoLoadM3uChange, enabled = !isLoading)
                                Text(stringResource(R.string.auto_load_playlist))
                            }
                            Button(onClick = onLoadM3uUrl, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.load_from_url))
                            }
                            OutlinedButton(onClick = onLoadM3uFile, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.load_from_file))
                            }
                        }
                        SourceType.XTREAM -> {
                            Field(xtreamServer, onXtreamServerChange, R.string.server_address, R.string.server_address_hint, !isLoading)
                            Field(xtreamUsername, onXtreamUsernameChange, R.string.username, null, !isLoading)
                            OutlinedTextField(
                                value = xtreamPassword,
                                onValueChange = onXtreamPasswordChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(stringResource(R.string.password)) },
                                visualTransformation = PasswordVisualTransformation(),
                                enabled = !isLoading,
                                singleLine = true,
                            )
                            Button(onClick = onLoadXtream, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                                Icon(Icons.Default.Tv, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.connect_xtream))
                            }
                        }
                        SourceType.STALKER -> {
                            Field(stalkerPortal, onStalkerPortalChange, R.string.portal_address, R.string.portal_address_hint, !isLoading)
                            Field(stalkerMac, onStalkerMacChange, R.string.mac_address, R.string.mac_address_hint, !isLoading)
                            Button(onClick = onLoadStalker, modifier = Modifier.fillMaxWidth(), enabled = !isLoading) {
                                Icon(Icons.Default.Tv, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(stringResource(R.string.connect_portal))
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = epgUrl,
                onValueChange = onEpgUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.epg_url)) },
                placeholder = { Text(stringResource(R.string.epg_url_hint)) },
                supportingText = { Text(stringResource(R.string.epg_optional)) },
                enabled = !isLoading,
                singleLine = true,
            )

            if (isLoading) {
                Card {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(12.dp))
                        Text(stringResource(R.string.loading_source))
                    }
                }
            }
            errorMessage?.let { StatusCard(it, true) }
            infoMessage?.let { StatusCard(it, false) }
            InfoCard(stringResource(R.string.portal_security_note))
            InfoCard(stringResource(R.string.network_note))
            InfoCard(stringResource(R.string.legal_note))
            InfoCard(stringResource(R.string.source_note))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    placeholder: Int?,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        placeholder = placeholder?.let { { Text(stringResource(it)) } },
        enabled = enabled,
        singleLine = true,
    )
}

@Composable
private fun StatusCard(text: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(14.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun InfoCard(text: String) {
    Card { Text(text = text, modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall) }
}
