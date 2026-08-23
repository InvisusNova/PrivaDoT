package com.invisusnova.privadot.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.invisusnova.privadot.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.annotation.SuppressLint

class KeepAliveService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "privadot_service"
    private val NOTIFICATION_ID = 1
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        Log.d("KeepAliveService", "onCreate called")
        
        // Acquire WakeLock to prevent CPU sleep
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PrivaDoT::KeepAliveWakeLock")
        wakeLock?.acquire()

        createNotificationChannel()
        startForegroundService()
        startSilentAudioLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("KeepAliveService", "onStartCommand called")
        // Return START_STICKY to force Android to restart this service if killed
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PrivaDoT Core Active")
            .setContentText("Keeping the app alive securely.")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PrivaDoT Background Core",
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "Keeps the app running securely"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    private fun startSilentAudioLoop() {
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.silent)
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer?.setVolume(0f, 0f)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            
            Log.d("KeepAliveService", "Continuous silent audio loop started")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("KeepAliveService", "onDestroy called")
        serviceScope.cancel()
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
