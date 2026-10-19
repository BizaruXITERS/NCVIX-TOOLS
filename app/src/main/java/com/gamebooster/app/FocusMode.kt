package com.gamebooster.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Mode fokus: Jangan Ganggu + kecerahan terkunci manual. Nilai lama disimpan lalu dikembalikan. */
object FocusMode {
    private const val PREFS = "focus"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun nm(ctx: Context) = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun hasDndAccess(ctx: Context) = nm(ctx).isNotificationPolicyAccessGranted
    fun canWriteSettings(ctx: Context) = Settings.System.canWrite(ctx)
    fun isActive(ctx: Context) = prefs(ctx).getBoolean("active", false)
    fun savedBrightness(ctx: Context) = prefs(ctx).getInt("level", 160)

    fun openDndAccess(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openWriteSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** [brightness] 0..255. Bagian yang izinnya belum ada dilewati. */
    fun enable(ctx: Context, brightness: Int) {
        val p = prefs(ctx)
        val e = p.edit()
        e.putInt("level", brightness)
        if (!p.getBoolean("active", false)) {
            if (hasDndAccess(ctx)) {
                e.putInt("oldFilter", nm(ctx).currentInterruptionFilter)
            }
            if (canWriteSettings(ctx)) {
                val cr = ctx.contentResolver
                e.putInt("oldMode", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, 1))
                e.putInt("oldBright", Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128))
            }
        }
        e.putBoolean("active", true).apply()

        if (hasDndAccess(ctx)) {
            nm(ctx).setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
        if (canWriteSettings(ctx)) {
            val cr = ctx.contentResolver
            Settings.System.putInt(
                cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness.coerceIn(1, 255))
        }
    }

    fun disable(ctx: Context) {
        val p = prefs(ctx)
        if (hasDndAccess(ctx) && p.contains("oldFilter")) {
            nm(ctx).setInterruptionFilter(p.getInt("oldFilter", NotificationManager.INTERRUPTION_FILTER_ALL))
        }
        if (canWriteSettings(ctx) && p.contains("oldMode")) {
            val cr = ctx.contentResolver
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, p.getInt("oldBright", 128))
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE, p.getInt("oldMode", 1))
        }
        p.edit().putBoolean("active", false).apply()
    }
}
