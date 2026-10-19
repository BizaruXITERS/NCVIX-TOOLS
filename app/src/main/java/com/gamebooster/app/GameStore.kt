package com.gamebooster.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppItem(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap?,
    val autoGame: Boolean
)

object GameStore {
    private const val PREFS = "games"
    private const val KEY = "pinned"

    /** Semua aplikasi yang punya ikon di launcher (selain aplikasi ini). */
    @Suppress("DEPRECATION")
    fun launchableApps(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ctx.packageName }
            .map { ai ->
                val isGame = ai.category == ApplicationInfo.CATEGORY_GAME ||
                    (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
                val icon = try {
                    ai.loadIcon(pm).toBitmap(96, 96).asImageBitmap()
                } catch (_: Exception) {
                    null
                }
                AppItem(ai.packageName, ai.loadLabel(pm).toString(), icon, isGame)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun pinned(ctx: Context): Set<String> =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet()) ?: emptySet()

    fun setPinned(ctx: Context, pkg: String, on: Boolean) {
        val set = pinned(ctx).toMutableSet()
        if (on) set.add(pkg) else set.remove(pkg)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, set).apply()
    }

    /** Game yang tampil: terdeteksi otomatis atau ditambahkan manual. */
    fun visibleGames(all: List<AppItem>, pinned: Set<String>): List<AppItem> =
        all.filter { it.autoGame || it.pkg in pinned }

    fun launch(ctx: Context, pkg: String): Boolean {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return true
    }

    /** Halaman info aplikasi, tempat tombol Hapus Cache / Penyimpanan berada. */
    fun openAppInfo(ctx: Context, pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}
