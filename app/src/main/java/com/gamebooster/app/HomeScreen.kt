package com.gamebooster.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    var ram by remember { mutableStateOf(SystemInfo.ram(ctx)) }
    var bat by remember { mutableStateOf(SystemInfo.battery(ctx)) }

    // Izin bisa berubah saat pengguna kembali dari layar pengaturan
    var dndOk by remember { mutableStateOf(FocusMode.hasDndAccess(ctx)) }
    var writeOk by remember { mutableStateOf(FocusMode.canWriteSettings(ctx)) }
    var overlayOk by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var focusOn by remember { mutableStateOf(FocusMode.isActive(ctx)) }
    var brightness by remember { mutableFloatStateOf(FocusMode.savedBrightness(ctx).toFloat()) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                dndOk = FocusMode.hasDndAccess(ctx)
                writeOk = FocusMode.canWriteSettings(ctx)
                overlayOk = Settings.canDrawOverlays(ctx)
                focusOn = FocusMode.isActive(ctx)
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            ram = SystemInfo.ram(ctx)
            bat = SystemInfo.battery(ctx)
            delay(2000)
        }
    }

    ScreenColumn {
        Text(
            "Game Booster",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Hint(SystemInfo.deviceLine())

        Section("Info perangkat") {
            Text("RAM ${ram.usedMb} / ${ram.totalMb} MB  (${ram.percent}%)")
            LinearProgressIndicator(
                progress = { ram.percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Baterai ${bat.level}%" + if (bat.charging) "  (mengisi)" else "")
            LinearProgressIndicator(
                progress = { (bat.level.coerceAtLeast(0)) / 100f },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Suhu baterai ${"%.1f".format(bat.tempC)}°C")
            Hint("Android tidak mengizinkan aplikasi biasa membaca suhu CPU, jadi yang tampil adalah suhu baterai.")
        }

        Section("Mode fokus") {
            Hint("Mengaktifkan Jangan Ganggu (hanya prioritas) dan mengunci kecerahan manual. Kondisi semula dikembalikan saat dimatikan.")
            if (!dndOk) {
                OutlinedButton(onClick = { FocusMode.openDndAccess(ctx) }) {
                    Text("Izinkan akses Jangan Ganggu")
                }
            }
            if (!writeOk) {
                OutlinedButton(onClick = { FocusMode.openWriteSettings(ctx) }) {
                    Text("Izinkan ubah kecerahan")
                }
            }
            Text("Kecerahan terkunci: ${(brightness / 255f * 100).toInt()}%")
            Slider(
                value = brightness,
                onValueChange = { brightness = it },
                valueRange = 10f..255f,
                enabled = !focusOn
            )
            Button(
                onClick = {
                    if (focusOn) FocusMode.disable(ctx) else FocusMode.enable(ctx, brightness.toInt())
                    focusOn = FocusMode.isActive(ctx)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = dndOk || writeOk || focusOn
            ) {
                Text(if (focusOn) "Matikan mode fokus" else "Aktifkan mode fokus")
            }
        }

        Section("Overlay monitor") {
            Hint("Kotak kecil di atas game berisi RAM, suhu, dan refresh rate layar. Bisa digeser dengan jari. Angka Hz adalah frame layar per detik, bukan FPS asli game.")
            if (!overlayOk) {
                OutlinedButton(onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Izinkan tampil di atas aplikasi lain") }
            }
            Button(
                onClick = {
                    val i = Intent(ctx, OverlayService::class.java)
                    if (OverlayService.running) {
                        i.action = OverlayService.ACTION_STOP
                        ctx.startService(i)
                    } else {
                        ContextCompat.startForegroundService(ctx, i)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = overlayOk
            ) {
                Text(if (OverlayService.running) "Matikan overlay" else "Nyalakan overlay")
            }
        }

        Section("Pintasan") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    openFirst(
                        ctx,
                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS),
                        Intent("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS")
                    )
                }) { Text("Penghemat baterai") }
                OutlinedButton(onClick = {
                    openFirst(
                        ctx,
                        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
                        Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS)
                    )
                }) { Text("Penyimpanan") }
            }
            Hint("Untuk membersihkan cache satu game, buka tab Game lalu ketuk ikon info pada game tersebut.")
        }
    }
}

/** Mencoba beberapa intent berurutan sampai ada yang bisa dibuka. */
fun openFirst(ctx: android.content.Context, vararg intents: Intent) {
    for (i in intents) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
        }
    }
    try {
        ctx.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}
