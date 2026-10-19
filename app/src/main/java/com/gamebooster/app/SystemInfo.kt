package com.gamebooster.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

data class RamInfo(val usedMb: Long, val totalMb: Long, val availMb: Long) {
    val percent: Int get() = if (totalMb == 0L) 0 else ((usedMb * 100) / totalMb).toInt()
}

data class BatteryInfo(val level: Int, val tempC: Float, val charging: Boolean)

object SystemInfo {
    private const val MB = 1024L * 1024L

    fun ram(ctx: Context): RamInfo {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / MB
        val avail = mi.availMem / MB
        return RamInfo(total - avail, total, avail)
    }

    fun battery(ctx: Context): BatteryInfo {
        val i: Intent? = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        return BatteryInfo(pct, temp, charging)
    }

    fun deviceLine(): String =
        "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}"

    /** Ukuran layar saat ini dalam piksel (lebar, tinggi) dalam orientasi sekarang. */
    @Suppress("DEPRECATION")
    fun screenSize(ctx: Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }

    @Suppress("DEPRECATION")
    fun refreshRate(ctx: Context): Float {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return wm.defaultDisplay.refreshRate
    }
}
