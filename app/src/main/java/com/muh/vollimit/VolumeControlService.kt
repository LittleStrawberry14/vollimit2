package com.muh.vollimit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat

class VolumeControlService : Service() {

    private lateinit var audioManager: AudioManager
    private var volumeLimitPercentage = 70
    private var volumeObserver: VolumeObserver? = null

    private inner class VolumeObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            limitVolume()
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        
        volumeObserver = VolumeObserver(Handler(Looper.getMainLooper()))
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver!!
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val newLimit = it.getIntExtra("volumePercentage", volumeLimitPercentage)
            if (newLimit != volumeLimitPercentage) {
                volumeLimitPercentage = newLimit
                // Update notification if limit changed
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(1, createNotification())
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, createNotification())
        }
        
        // Immediate check when service starts
        limitVolume()
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        volumeObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
    }

    private fun limitVolume() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (maxVolume * (volumeLimitPercentage / 100.0)).toInt()
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (currentVolume > targetVolume) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Volume Control"
            val descriptionText = "Channel for Volume Control Service"
            val importance = NotificationManager.IMPORTANCE_LOW // Reduced importance to avoid noise
            val channel = NotificationChannel("volume_control_channel", name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, "volume_control_channel")
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text, volumeLimitPercentage))
        .setSmallIcon(R.drawable.ic_launcher_new)
        .setPriority(NotificationCompat.PRIORITY_LOW) // Use low priority
        .setOngoing(true)
        .build()
}
