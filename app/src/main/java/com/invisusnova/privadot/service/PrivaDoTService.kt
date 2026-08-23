package com.invisusnova.privadot.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.invisusnova.privadot.ACTION_PREVIEW_SENSOR
import com.invisusnova.privadot.MainActivity
import com.invisusnova.privadot.R
import com.invisusnova.privadot.data.AppDatabase
import com.invisusnova.privadot.data.HistoryDao
import com.invisusnova.privadot.data.HistoryEntity
import com.invisusnova.privadot.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PrivaDoTService : AccessibilityService() {

    companion object {
        @Volatile
        var isRunning = false
            private set
    }

    private var overlayManager: OverlayManager? = null
    private var sensorDetector: SensorUsageDetector? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var historyDao: HistoryDao

    private var currentForegroundPackage: String = "Unknown"
    
    // Tracking sensor starts
    private var cameraStartTime: Long = 0
    private var micStartTime: Long = 0
    private var locationStartTime: Long = 0
    
    // Tracking active states to detect changes
    private var isCameraCurrentlyActive = false
    private var isMicCurrentlyActive = false
    private var isLocationCurrentlyActive = false

    private val previewReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PREVIEW_SENSOR) {
                val type = intent.getStringExtra("SENSOR_TYPE")
                if (type == "CLEAR" || type == null) {
                    overlayManager?.clearPreviewConfig()
                } else {
                    val config = PreviewConfig(
                        cameraColor = intent.getStringExtra("EXTRA_CAM_COLOR") ?: "#3FB950",
                        micColor = intent.getStringExtra("EXTRA_MIC_COLOR") ?: "#F0883E",
                        locationColor = intent.getStringExtra("EXTRA_LOC_COLOR") ?: "#58A6FF",
                        dotSize = intent.getIntExtra("EXTRA_SIZE", 8),
                        cameraPosX = intent.getFloatExtra("EXTRA_CAM_X", 0.86f),
                        cameraPosY = intent.getFloatExtra("EXTRA_CAM_Y", 0.03f),
                        micPosX = intent.getFloatExtra("EXTRA_MIC_X", 0.93f),
                        micPosY = intent.getFloatExtra("EXTRA_MIC_Y", 0.03f),
                        locationPosX = intent.getFloatExtra("EXTRA_LOC_X", 1.0f),
                        locationPosY = intent.getFloatExtra("EXTRA_LOC_Y", 0.03f)
                    )
                    overlayManager?.setPreviewConfig(config)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
        Log.d("PrivaDoTService", "Accessibility Service Connected")

        startKeepAliveMechanisms()

        historyDao = AppDatabase.getDatabase(this).historyDao()

        try {
            overlayManager = OverlayManager(this, SettingsManager(this), serviceScope)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        sensorDetector = SensorUsageDetector(this) { camera, mic, location ->
            overlayManager?.updateIndicators(camera, mic, location)

            val time = System.currentTimeMillis()
            
            // Handle Camera
            if (camera && !isCameraCurrentlyActive) {
                cameraStartTime = time
                isCameraCurrentlyActive = true
            } else if (!camera && isCameraCurrentlyActive) {
                isCameraCurrentlyActive = false
                logHistory("CAMERA", cameraStartTime, time)
            }
            
            // Handle Mic
            if (mic && !isMicCurrentlyActive) {
                micStartTime = time
                isMicCurrentlyActive = true
            } else if (!mic && isMicCurrentlyActive) {
                isMicCurrentlyActive = false
                logHistory("MIC", micStartTime, time)
            }
            
            // Handle Location
            if (location && !isLocationCurrentlyActive) {
                locationStartTime = time
                isLocationCurrentlyActive = true
            } else if (!location && isLocationCurrentlyActive) {
                isLocationCurrentlyActive = false
                logHistory("LOCATION", locationStartTime, time)
            }
        }
        sensorDetector?.startDetecting()
        
        ContextCompat.registerReceiver(
            this,
            previewReceiver,
            IntentFilter(ACTION_PREVIEW_SENSOR),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun logHistory(sensorType: String, startTime: Long, endTime: Long) {
        val duration = endTime - startTime
        val pkg = currentForegroundPackage
        var appName = pkg
        try {
            val pm = packageManager
            val info = pm.getApplicationInfo(pkg, 0)
            appName = pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            // App not found or system UI
        }
        
        serviceScope.launch {
            historyDao.insert(
                HistoryEntity(
                    sensorType = sensorType,
                    packageName = pkg,
                    appName = appName,
                    timestamp = startTime,
                    durationMs = duration
                )
            )
        }
    }

    private var lastAudioCheckTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let {
                currentForegroundPackage = it
            }
        }
        
        val now = System.currentTimeMillis()
        if (now - lastAudioCheckTime > 1000) {
            lastAudioCheckTime = now
            sensorDetector?.checkAudioMode()
        }
    }

    override fun onInterrupt() {
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isRunning = false
        Log.d("PrivaDoTService", "Accessibility Service Unbound")
        sensorDetector?.stopDetecting()
        overlayManager?.removeOverlay()
        serviceScope.cancel()
        stopKeepAliveMechanisms()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            unregisterReceiver(previewReceiver)
        } catch (e: Exception) {}
        
        sensorDetector?.stopDetecting()
        overlayManager?.removeOverlay()
        serviceScope.cancel()
    }

    private fun startKeepAliveMechanisms() {
        try {
            val serviceIntent = Intent(this, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopKeepAliveMechanisms() {
        try {
            stopService(Intent(this, KeepAliveService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
