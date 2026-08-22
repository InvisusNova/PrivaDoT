package com.invisusnova.privadot.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.invisusnova.privadot.data.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class PreviewConfig(
    val cameraColor: String,
    val micColor: String,
    val locationColor: String,
    val dotSize: Int,
    val cameraPosX: Float,
    val cameraPosY: Float,
    val micPosX: Float,
    val micPosY: Float,
    val locationPosX: Float,
    val locationPosY: Float
)

class OverlayManager(
    private val context: Context,
    private val settingsManager: SettingsManager,
    private val coroutineScope: CoroutineScope
) {
    private var windowManager: WindowManager? = null
    
    private var cameraContainer: FrameLayout? = null
    private var micContainer: FrameLayout? = null
    private var locationContainer: FrameLayout? = null
    
    private var cameraParams: WindowManager.LayoutParams? = null
    private var micParams: WindowManager.LayoutParams? = null
    private var locationParams: WindowManager.LayoutParams? = null
    
    private var isCameraActive = false
    private var isMicActive = false
    private var isLocationActive = false
    
    private var previewConfig: PreviewConfig? = null

    // Saved flows
    private var savedCameraColor: Int = Color.parseColor("#3FB950")
    private var savedMicColor: Int = Color.parseColor("#F0883E")
    private var savedLocationColor: Int = Color.parseColor("#58A6FF")
    private var savedDotSize: Int = 8
    
    private var savedCameraPosX: Float = 0.86f
    private var savedCameraPosY: Float = 0.03f
    private var savedMicPosX: Float = 0.93f
    private var savedMicPosY: Float = 0.03f
    private var savedLocationPosX: Float = 1.0f
    private var savedLocationPosY: Float = 0.03f

    init {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayViews()
        observeSettings()
    }

    private fun safeParseColor(hex: String, default: String): Int {
        return try { Color.parseColor(hex) } catch (e: Exception) { Color.parseColor(default) }
    }

    private fun observeSettings() {
        coroutineScope.launch { settingsManager.cameraColorFlow.collect { savedCameraColor = safeParseColor(it, "#3FB950"); drawDots() } }
        coroutineScope.launch { settingsManager.micColorFlow.collect { savedMicColor = safeParseColor(it, "#F0883E"); drawDots() } }
        coroutineScope.launch { settingsManager.locationColorFlow.collect { savedLocationColor = safeParseColor(it, "#58A6FF"); drawDots() } }
        coroutineScope.launch { settingsManager.dotSizeFlow.collect { savedDotSize = it; drawDots() } }
        
        coroutineScope.launch { settingsManager.cameraPosXFlow.collect { savedCameraPosX = it; updatePosition(cameraContainer, cameraParams, it, savedCameraPosY) } }
        coroutineScope.launch { settingsManager.cameraPosYFlow.collect { savedCameraPosY = it; updatePosition(cameraContainer, cameraParams, savedCameraPosX, it) } }
        
        coroutineScope.launch { settingsManager.micPosXFlow.collect { savedMicPosX = it; updatePosition(micContainer, micParams, it, savedMicPosY) } }
        coroutineScope.launch { settingsManager.micPosYFlow.collect { savedMicPosY = it; updatePosition(micContainer, micParams, savedMicPosX, it) } }
        
        coroutineScope.launch { settingsManager.locationPosXFlow.collect { savedLocationPosX = it; updatePosition(locationContainer, locationParams, it, savedLocationPosY) } }
        coroutineScope.launch { settingsManager.locationPosYFlow.collect { savedLocationPosY = it; updatePosition(locationContainer, locationParams, savedLocationPosX, it) } }
    }

    private fun createBaseParams(): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        return params
    }

    private fun addDotToContainer(container: FrameLayout?) {
        val dot = View(context)
        dot.visibility = View.GONE
        container?.addView(dot)
    }

    private fun createOverlayViews() {
        cameraContainer = FrameLayout(context)
        cameraParams = createBaseParams()
        
        micContainer = FrameLayout(context)
        micParams = createBaseParams()
        
        locationContainer = FrameLayout(context)
        locationParams = createBaseParams()
        
        addDotToContainer(cameraContainer)
        addDotToContainer(micContainer)
        addDotToContainer(locationContainer)
        
        try {
            windowManager?.addView(cameraContainer, cameraParams)
            windowManager?.addView(micContainer, micParams)
            windowManager?.addView(locationContainer, locationParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePosition(container: FrameLayout?, params: WindowManager.LayoutParams?, percentX: Float, percentY: Float) {
        if (container == null || params == null) return
        
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val scale = metrics.density
        
        // Use preview size if active, else saved size
        val currentSize = previewConfig?.dotSize ?: savedDotSize
        val sizePx = (currentSize * scale).toInt()
        val estimatedSizePx = sizePx + (8 * scale).toInt() // margins
        
        val maxX = screenWidth - estimatedSizePx
        val maxY = screenHeight - estimatedSizePx

        params.x = (maxX * percentX).toInt()
        params.y = (maxY * percentY).toInt()
        
        container.post {
            if (container.isAttachedToWindow) {
                try {
                    windowManager?.updateViewLayout(container, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateIndicators(camera: Boolean, mic: Boolean, location: Boolean) {
        this.isCameraActive = camera
        this.isMicActive = mic
        this.isLocationActive = location
        drawDots()
    }
    
    fun setPreviewConfig(config: PreviewConfig) {
        this.previewConfig = config
        drawDots()
    }

    fun clearPreviewConfig() {
        this.previewConfig = null
        drawDots()
    }

    private fun drawDots() {
        val config = previewConfig
        if (config != null) {
            // Preview mode: Draw everything using config
            drawDotInContainer(cameraContainer, safeParseColor(config.cameraColor, "#3FB950"), true, config.dotSize)
            drawDotInContainer(micContainer, safeParseColor(config.micColor, "#F0883E"), true, config.dotSize)
            drawDotInContainer(locationContainer, safeParseColor(config.locationColor, "#58A6FF"), true, config.dotSize)
            
            updatePosition(cameraContainer, cameraParams, config.cameraPosX, config.cameraPosY)
            updatePosition(micContainer, micParams, config.micPosX, config.micPosY)
            updatePosition(locationContainer, locationParams, config.locationPosX, config.locationPosY)
        } else {
            // Normal mode
            drawDotInContainer(cameraContainer, savedCameraColor, isCameraActive, savedDotSize)
            drawDotInContainer(micContainer, savedMicColor, isMicActive, savedDotSize)
            drawDotInContainer(locationContainer, savedLocationColor, isLocationActive, savedDotSize)
            
            updatePosition(cameraContainer, cameraParams, savedCameraPosX, savedCameraPosY)
            updatePosition(micContainer, micParams, savedMicPosX, savedMicPosY)
            updatePosition(locationContainer, locationParams, savedLocationPosX, savedLocationPosY)
        }
    }

    private fun drawDotInContainer(container: FrameLayout?, color: Int, isActive: Boolean, sizeDp: Int) {
        container?.post {
            if (container.childCount > 0) {
                val dot = container.getChildAt(0)
                if (isActive && sizeDp > 0) {
                    val scale = context.resources.displayMetrics.density
                    val sizePx = (sizeDp * scale + 0.5f).toInt()
                    
                    val params = FrameLayout.LayoutParams(sizePx, sizePx)
                    params.setMargins(4, 4, 4, 4)
                    dot.layoutParams = params
                    
                    val shape = android.graphics.drawable.GradientDrawable()
                    shape.shape = android.graphics.drawable.GradientDrawable.OVAL
                    shape.setColor(color)
                    dot.background = shape
                    
                    dot.visibility = View.VISIBLE
                } else {
                    dot.visibility = View.GONE
                }
            }
        }
    }

    fun removeOverlay() {
        try {
            cameraContainer?.let { windowManager?.removeView(it) }
            micContainer?.let { windowManager?.removeView(it) }
            locationContainer?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cameraContainer = null; micContainer = null; locationContainer = null
        cameraParams = null; micParams = null; locationParams = null
    }
}
