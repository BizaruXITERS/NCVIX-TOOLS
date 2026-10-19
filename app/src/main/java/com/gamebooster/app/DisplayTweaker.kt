package com.gamebooster.app

import android.content.Context
import android.content.pm.PackageManager

/**
 * Ubah resolusi dan DPI lewat perintah `wm`.
 * Butuh izin WRITE_SECURE_SETTINGS yang diberikan sekali lewat ADB:
 *   adb shell pm grant com.gamebooster.app android.permission.WRITE_SECURE_SETTINGS
 */
object DisplayTweaker {
    const val GRANT_COMMAND =
        "adb shell pm grant com.gamebooster.app android.permission.WRITE_SECURE_SETTINGS"

    data class State(
        val physicalW: Int, val physicalH: Int,
        val currentW: Int, val currentH: Int,
        val physicalDpi: Int, val currentDpi: Int
    )

    fun hasPermission(ctx: Context) =
        ctx.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            PackageManager.PERMISSION_GRANTED

    private fun run(cmd: String): Pair<Int, String> = try {
        val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText()
        val err = p.errorStream.bufferedReader().readText()
        p.waitFor() to (out + err).trim()
    } catch (e: Exception) {
        -1 to (e.message ?: "gagal menjalankan perintah")
    }

    private fun parseSize(line: String?): Pair<Int, Int>? {
        val m = Regex("(\\d+)x(\\d+)").find(line ?: "") ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private fun parseDpi(line: String?): Int? =
        Regex("(\\d+)\\s*$").find(line?.trim() ?: "")?.groupValues?.get(1)?.toIntOrNull()

    /** Membaca ukuran dan DPI fisik + yang sedang aktif. Null kalau gagal dibaca. */
    fun read(ctx: Context): State? {
        val size = run("wm size").second.lines()
        val dens = run("wm density").second.lines()
        val phys = parseSize(size.firstOrNull { it.contains("Physical", true) }) ?: return null
        val over = parseSize(size.firstOrNull { it.contains("Override", true) }) ?: phys
        val physDpi = parseDpi(dens.firstOrNull { it.contains("Physical", true) }) ?: return null
        val overDpi = parseDpi(dens.firstOrNull { it.contains("Override", true) }) ?: physDpi
        return State(phys.first, phys.second, over.first, over.second, physDpi, overDpi)
    }

    /** Mengembalikan pesan error, atau null kalau berhasil. */
    fun setSize(w: Int, h: Int): String? {
        val (code, out) = run("wm size ${w}x$h")
        return if (code == 0 && !out.contains("Exception", true)) null else out.ifBlank { "Gagal" }
    }

    fun setDpi(dpi: Int): String? {
        val (code, out) = run("wm density $dpi")
        return if (code == 0 && !out.contains("Exception", true)) null else out.ifBlank { "Gagal" }
    }

    fun resetAll() {
        run("wm size reset")
        run("wm density reset")
    }

    /** DPI yang menghasilkan smallest width [swDp] untuk sisi pendek [shortPx]. */
    fun dpiForSmallestWidth(shortPx: Int, swDp: Int): Int =
        (shortPx * 160f / swDp.coerceAtLeast(1)).toInt()

    fun smallestWidthDp(shortPx: Int, dpi: Int): Int =
        (shortPx * 160f / dpi.coerceAtLeast(1)).toInt()
}
