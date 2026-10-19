package com.gamebooster.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GamesScreen() {
    val ctx = LocalContext.current
    var all by remember { mutableStateOf<List<AppItem>?>(null) }
    var pinned by remember { mutableStateOf(GameStore.pinned(ctx)) }
    var picker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        all = withContext(Dispatchers.Default) { GameStore.launchableApps(ctx) }
    }

    Box(Modifier.fillMaxSize()) {
        val apps = all
        if (apps == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            val games = GameStore.visibleGames(apps, pinned)
            if (games.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center).padding(32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Belum ada game", style = MaterialTheme.typography.titleMedium)
                    Hint("Ketuk tombol + untuk menambahkan game dari daftar aplikasi.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(games, key = { it.pkg }) { g ->
                        AppRow(
                            item = g,
                            trailing = {
                                IconButton(onClick = { GameStore.openAppInfo(ctx, g.pkg) }) {
                                    Icon(Icons.Filled.Info, contentDescription = "Info aplikasi / hapus cache")
                                }
                                if (g.pkg in pinned) {
                                    IconButton(onClick = {
                                        GameStore.setPinned(ctx, g.pkg, false)
                                        pinned = GameStore.pinned(ctx)
                                    }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Hapus dari daftar")
                                    }
                                }
                                IconButton(onClick = { GameStore.launch(ctx, g.pkg) }) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Buka game",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            onClick = { GameStore.launch(ctx, g.pkg) }
                        )
                    }
                }
            }
            FloatingActionButton(
                onClick = { picker = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) { Icon(Icons.Filled.Add, contentDescription = "Tambah game") }

            if (picker) {
                val candidates = apps.filter { it.pkg !in pinned && !it.autoGame }
                AlertDialog(
                    onDismissRequest = { picker = false },
                    confirmButton = { TextButton(onClick = { picker = false }) { Text("Selesai") } },
                    title = { Text("Tambah game") },
                    text = {
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(candidates, key = { it.pkg }) { a ->
                                AppRow(
                                    item = a,
                                    trailing = {},
                                    onClick = {
                                        GameStore.setPinned(ctx, a.pkg, true)
                                        pinned = GameStore.pinned(ctx)
                                    },
                                    flat = true
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppRow(
    item: AppItem,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    flat: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (flat) androidx.compose.ui.graphics.Color.Transparent
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val icon = item.icon
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(44.dp))
            } else {
                Box(Modifier.size(44.dp))
            }
            Text(
                item.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                style = MaterialTheme.typography.bodyLarge
            )
            trailing()
        }
    }
}
