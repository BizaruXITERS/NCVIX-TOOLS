package com.gamebooster.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Overlay kecil di atas game: RAM terpakai, suhu baterai, dan refresh rate layar (Hz).
 * Angka "Hz" dihitung dari jumlah frame layar per detik, bukan FPS asli game.
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_STOP = "com.gamebooster.app.STOP_OVERLAY"
        private const val CHANNEL = "overlay"
        private const val NOTIF_ID = 1002
        var running by mutableStateOf(false)
    }

    private val main = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var view: TextView? = null
    private var active = false
    private var frames = 0

    private val frameCb = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frames++
            if (active) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            val hz = frames
            frames = 0
            val ram = SystemInfo.ram(this@OverlayService)
            val bat = SystemInfo.battery(this@OverlayService)
            view?.text = "RAM ${ram.usedMb}/${ram.totalMb} MB (${ram.percent}%)\n" +
                "Suhu ${"%.1f".format(bat.tempC)}°C  ·  Layar $hz Hz"
            main.postDelayed(this, 1000)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (active) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        createChannel()
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        addOverlay()
        active = true
        running = true
        Choreographer.getInstance().postFrameCallback(frameCb)
        main.post(tick)
        return START_NOT_STICKY
    }

    private fun addOverlay() {
        val manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm = manager
        val density = resources.displayMetrics.density
        val tv = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            val padH = (10 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            background = GradientDrawable().apply {
                setColor(0xB3000000.toInt())
                cornerRadius = 10 * density
            }
            text = "Memuat..."
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (12 * density).toInt()
            y = (80 * density).toInt()
        }
        tv.setOnTouchListener(object : View.OnTouchListener {
            var startX = 0
            var startY = 0
            var touchX = 0f
            var touchY = 0f
            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = lp.x; startY = lp.y
                        touchX = e.rawX; touchY = e.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        lp.x = startX + (e.rawX - touchX).toInt()
                        lp.y = startY + (e.rawY - touchY).toInt()
                        manager.updateViewLayout(v, lp)
                    }
                }
                return true
            }
        })
        manager.addView(tv, lp)
        view = tv
    }

    override fun onDestroy() {
        active = false
        running = false
        main.removeCallbacks(tick)
        Choreographer.getInstance().removeFrameCallback(frameCb)
        try { view?.let { wm?.removeView(it) } } catch (_: Exception) {}
        view = null
        super.onDestroy()
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Overlay monitor", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val stop = PendingIntent.getService(
            this, 3,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Game Booster")
            .setContentText("Overlay monitor aktif")
            .addAction(0, "Matikan", stop)
            .setOngoing(true)
            .build()
    }
}
