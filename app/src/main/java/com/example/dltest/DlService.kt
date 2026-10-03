package com.example.dltest

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class DlService : Service() {
    private var wl: PowerManager.WakeLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") {
            Engine.stop(); release(); stopForeground(true); stopSelf()
            return START_NOT_STICKY
        }
        val url = intent?.getStringExtra("link") ?: ""
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val b = if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel("dl", "Download test", NotificationManager.IMPORTANCE_LOW))
            Notification.Builder(this, "dl")
        } else Notification.Builder(this)
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val n = b.setContentTitle("Download test chal raha hai")
            .setContentText("Kholne ke liye tap karein")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pi).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(1, n)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dltest:wl").also { it.acquire() }
        Engine.start(cacheDir, url)
        return START_NOT_STICKY
    }

    private fun release() { try { wl?.release() } catch (_: Exception) {}; wl = null }

    override fun onDestroy() { Engine.stop(); release(); super.onDestroy() }
}
