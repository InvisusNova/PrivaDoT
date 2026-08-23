package com.invisusnova.privadot.service

import android.app.AppOpsManager
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.Executors

class SensorUsageDetector(
    private val context: Context,
    private val onUsageChanged: (camera: Boolean, mic: Boolean, location: Boolean) -> Unit
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    
    private var isCameraActive = false
    private var isAudioRecordingMicActive = false
    private var isAppOpsMicActive = false
    private var isAudioModeMicActive = false
    private val isMicActive: Boolean
        get() = isAudioRecordingMicActive || isAppOpsMicActive || isAudioModeMicActive
    private var isGnssLocationActive = false
    private var isAppOpsLocationActive = false
    private val isLocationActive: Boolean
        get() = isGnssLocationActive || isAppOpsLocationActive
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    // Keep track of unavailable cameras
    private val unavailableCameras = mutableSetOf<String>()

    // 1. Camera Callback (API 21+)
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            unavailableCameras.remove(cameraId)
            isCameraActive = unavailableCameras.isNotEmpty()
            notifyChanges()
        }

        override fun onCameraUnavailable(cameraId: String) {
            unavailableCameras.add(cameraId)
            isCameraActive = true
            notifyChanges()
        }
    }

    // 2. Mic Callback (API 24+)
    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>?) {
            isAudioRecordingMicActive = configs?.isNotEmpty() == true
            notifyChanges()
        }
    }

    // 3. Location Callback (API 24+ GnssStatus)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
    private val gnssCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        object : android.location.GnssStatus.Callback() {
            override fun onStarted() {
                isGnssLocationActive = true
                notifyChanges()
            }
            override fun onStopped() {
                isGnssLocationActive = false
                notifyChanges()
            }
        }
    } else null

    // 4. AppOps Location & Mic Callback (API 29+)
    private val appOpsListener = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        object : AppOpsManager.OnOpActiveChangedListener {
            override fun onOpActiveChanged(op: String, uid: Int, packageName: String, active: Boolean) {
                if (op == AppOpsManager.OPSTR_COARSE_LOCATION || op == AppOpsManager.OPSTR_FINE_LOCATION) {
                    isAppOpsLocationActive = active
                    notifyChanges()
                } else if (op == AppOpsManager.OPSTR_RECORD_AUDIO || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && op == "android:phone_call_microphone")) {
                    isAppOpsMicActive = active
                    notifyChanges()
                }
            }
        }
    } else null


    fun startDetecting() {
        // Register Camera Callback
        cameraManager.registerAvailabilityCallback(cameraCallback, mainHandler)

        // Register Mic Callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            audioManager.registerAudioRecordingCallback(audioCallback, mainHandler)
            // Check initial state
            isAudioRecordingMicActive = audioManager.activeRecordingConfigurations.isNotEmpty()
        }

        // Register Location Callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
            try {
                if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    locationManager.registerGnssStatusCallback(gnssCallback, mainHandler)
                }
            } catch (e: Exception) {
                Log.e("SensorUsageDetector", "Location tracking failed", e)
            }
        }
        
        // Register AppOps Location & Mic Callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && appOpsListener != null) {
            try {
                val ops = mutableListOf(AppOpsManager.OPSTR_COARSE_LOCATION, AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ops.add("android:phone_call_microphone")
                }
                appOpsManager.startWatchingActive(
                    ops.toTypedArray(),
                    executor,
                    appOpsListener
                )
            } catch (e: Exception) {
                Log.e("SensorUsageDetector", "AppOps tracking failed", e)
            }
        }

        checkAudioMode()
        notifyChanges()
    }

    private fun notifyChanges() {
        mainHandler.post {
            onUsageChanged(isCameraActive, isMicActive, isLocationActive)
        }
    }

    fun stopDetecting() {
        try {
            cameraManager.unregisterAvailabilityCallback(cameraCallback)
        } catch (e: Exception) {}
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                audioManager.unregisterAudioRecordingCallback(audioCallback)
            } catch (e: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && gnssCallback != null) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback)
            } catch (e: Exception) {
                Log.e("SensorUsageDetector", "Location stop failed", e)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && appOpsListener != null) {
            try {
                appOpsManager.stopWatchingActive(appOpsListener)
            } catch (e: Exception) {}
        }
        
        try {
            executor.shutdown()
        } catch (e: Exception) {}
    }

    fun checkAudioMode() {
        val mode = audioManager.mode
        val isActive = mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION
        if (isAudioModeMicActive != isActive) {
            isAudioModeMicActive = isActive
            notifyChanges()
        }
    }
}
