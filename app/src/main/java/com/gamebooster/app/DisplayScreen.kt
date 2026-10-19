package com.gamebooster.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun evenPx(v: Float) = (v.roundToInt() / 2) * 2

private fun copyText(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("adb", text))
    Toast.makeText(ctx, "Perintah disalin", Toast.LENGTH_SHORT).show()
}

@Composable
fun DisplayScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var permOk by remember { mutableStateOf(DisplayTweaker.hasPermission(ctx)) }
    var st by remember { mutableStateOf<DisplayTweaker.State?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var choice by remember { mutableStateOf("keep") }
    var autoDpi by remember { mutableStateOf(true) }
    var dpiText by remember { mutableStateOf("") }
    var swText by remember { mutableStateOf("") }
    var customW by remember { mutableStateOf("") }
    var customH by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var msg by remember { mutableStateOf("") }

    fun refresh() {
        scope.launch {
            st = withContext(Dispatchers.IO) { DisplayTweaker.read(ctx) }
            permOk = DisplayTweaker.hasPermission(ctx)
            loaded = true
        }
    }
    LaunchedEffect(Unit) { refresh() }

    ScreenColumn {
        Text(
            "Resolusi & DPI",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        if (!permOk) {
            Section("Izin dibutuhkan (sekali saja)") {
                Hint(
                    "Android melarang aplikasi mengubah resolusi/DPI tanpa izin khusus. " +
                        "Sambungkan HP ke PC dengan USB debugging (atau ADB wireless), lalu jalankan perintah ini satu kali:"
                )
                Text(
                    DisplayTweaker.GRANT_COMMAND,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { copyText(ctx, DisplayTweaker.GRANT_COMMAND) }) { Text("Salin perintah") }
                    OutlinedButton(onClick = { refresh() }) { Text("Cek ulang izin") }
                }
            }
        }

        val s = st
        if (s == null) {
            Hint(if (loaded) "Gagal membaca info layar dari sistem." else "Membaca info layar...")
            if (loaded) OutlinedButton(onClick = { refresh() }) { Text("Coba lagi") }
        } else {
            val physShort = minOf(s.physicalW, s.physicalH)
            val curShort = minOf(s.currentW, s.currentH)

            val target: Pair<Int, Int>? = when (choice) {
                "720", "1080" -> {
                    val f = choice.toInt().toFloat() / physShort
                    evenPx(s.physicalW * f) to evenPx(s.physicalH * f)
                }
                "asli" -> s.physicalW to s.physicalH
                "kustom" -> {
                    val w = customW.toIntOrNull()
                    val h = customH.toIntOrNull()
                    if (w != null && h != null && w in 320..8000 && h in 320..8000) w to h else null
                }
                else -> null
            }
            val newShort = target?.let { minOf(it.first, it.second) } ?: curShort
            val dpiToApply: Int? = dpiText.toIntOrNull()
                ?: if (autoDpi && target != null) s.physicalDpi * newShort / physShort else null
            val effDpi = dpiToApply ?: s.currentDpi
            val dpiBad = dpiToApply != null && dpiToApply !in 120..800

            Section("Kondisi layar") {
                Text("Asli: ${s.physicalW}×${s.physicalH} · ${s.physicalDpi} dpi")
                Text(
                    "Aktif: ${s.currentW}×${s.currentH} · ${s.currentDpi} dpi · " +
                        "lebar kecil ${DisplayTweaker.smallestWidthDp(curShort, s.currentDpi)} dp"
                )
            }

            Section("Resolusi") {
                val opts = buildList {
                    add("keep")
                    if (physShort >= 1080) add("1080")
                    if (physShort >= 720) add("720")
                    add("kustom")
                    add("asli")
                }
                ChipRow(
                    options = opts,
                    selected = choice,
                    label = {
                        when (it) {
                            "keep" -> "Tetap"
                            "asli" -> "Asli"
                            "kustom" -> "Kustom"
                            else -> "${it}p"
                        }
                    },
                    onSelect = {
                        choice = it
                        dpiText = ""
                        swText = ""
                    }
                )
                if (choice == "kustom") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customW,
                            onValueChange = { v ->
                                customW = v.filter { c -> c.isDigit() }.take(4)
                                dpiText = ""
                                swText = ""
                            },
                            label = { Text("Lebar (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customH,
                            onValueChange = { v ->
                                customH = v.filter { c -> c.isDigit() }.take(4)
                                dpiText = ""
                                swText = ""
                            },
                            label = { Text("Tinggi (px)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Hint(
                        "Isi sesuai orientasi asli HP (potret: lebar lebih kecil dari tinggi), contoh 900 × 2000. " +
                            "Batas 320–8000 px. Rasio yang berbeda dari layar asli bisa membuat tampilan melar atau ada bar hitam."
                    )
                    if (target == null && (customW.isNotEmpty() || customH.isNotEmpty())) {
                        Text("Lebar dan tinggi harus diisi, antara 320 dan 8000.", color = MaterialTheme.colorScheme.error)
                    }
                }
                if (target != null) Text("Hasil: ${target.first}×${target.second}")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(checked = autoDpi, onCheckedChange = { autoDpi = it })
                    Text("Sesuaikan DPI otomatis (ukuran tampilan tetap)", modifier = Modifier.weight(1f))
                }
            }

            Section("DPI / lebar kecil") {
                OutlinedTextField(
                    value = dpiText,
                    onValueChange = { v ->
                        dpiText = v.filter { c -> c.isDigit() }.take(4)
                        swText = ""
                    },
                    label = { Text("DPI (kosong = otomatis / tidak diubah)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = swText,
                    onValueChange = { v ->
                        swText = v.filter { c -> c.isDigit() }.take(4)
                        swText.toIntOrNull()?.takeIf { it >= 100 }?.let {
                            dpiText = DisplayTweaker.dpiForSmallestWidth(newShort, it).toString()
                        }
                    },
                    label = { Text("Lebar kecil dp (mis. 360, 411)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Hint(
                    "Hasil: $effDpi dpi → lebar kecil ${DisplayTweaker.smallestWidthDp(newShort, effDpi)} dp. " +
                        "Angka lebar kecil makin kecil = tampilan makin besar."
                )
                if (dpiBad) {
                    Text("DPI harus antara 120 dan 800.", color = MaterialTheme.colorScheme.error)
                }
            }

            Section("Terapkan") {
                Hint("Setelah diterapkan ada waktu 15 detik untuk memilih Pertahankan. Kalau tidak, otomatis kembali normal.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                val err = withContext(Dispatchers.IO) {
                                    var e: String? = null
                                    if (target != null) e = DisplayTweaker.setSize(target.first, target.second)
                                    if (e == null && dpiToApply != null) e = DisplayTweaker.setDpi(dpiToApply)
                                    e
                                }
                                if (err != null) {
                                    msg = "Gagal: $err"
                                } else {
                                    msg = ""
                                    countdown = 15
                                }
                                refresh()
                            }
                        },
                        enabled = permOk && !dpiBad && (choice != "kustom" || target != null) &&
                            (target != null || dpiToApply != null)
                    ) { Text("Terapkan") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { DisplayTweaker.resetAll() }
                                choice = "keep"
                                customW = ""
                                customH = ""
                                dpiText = ""
                                swText = ""
                                msg = ""
                                refresh()
                            }
                        },
                        enabled = permOk
                    ) { Text("Reset ke bawaan") }
                }
                if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.error)
                Hint("Darurat kalau layar jadi aneh, dari PC: adb shell wm size reset && adb shell wm density reset")
            }
        }
    }

    countdown?.let { left ->
        LaunchedEffect(left) {
            delay(1000)
            if (left > 1) {
                countdown = left - 1
            } else {
                withContext(Dispatchers.IO) { DisplayTweaker.resetAll() }
                countdown = null
                refresh()
            }
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Pertahankan pengaturan layar?") },
            text = { Text("Otomatis kembali normal dalam $left detik.") },
            confirmButton = { TextButton(onClick = { countdown = null }) { Text("Pertahankan") } },
            dismissButton = {
                TextButton(onClick = {
                    countdown = null
                    scope.launch {
                        withContext(Dispatchers.IO) { DisplayTweaker.resetAll() }
                        refresh()
                    }
                }) { Text("Kembalikan") }
            }
        )
    }
}
