package com.invisusnova.privadot

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.invisusnova.privadot.data.AppDatabase
import com.invisusnova.privadot.data.SettingsManager
import com.invisusnova.privadot.service.PrivaDoTService
import com.invisusnova.privadot.theme.PrivaDoTTheme

class MainActivity : ComponentActivity() {

    // Permission states
    private var _hasOverlayPermission = mutableStateOf(false)
    private var _hasAccessibility = mutableStateOf(false)
    private var _hasBatteryOptExempt = mutableStateOf(false)
    private var _hasNotificationPerm = mutableStateOf(false)
    private var _hasLocationPerm = mutableStateOf(false)
    private var _isServiceRunning = mutableStateOf(false)

    // Notification permission launcher
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            _hasNotificationPerm.value = granted
            if (!granted) {
                Toast.makeText(this, "Notification permission is needed for background service", Toast.LENGTH_LONG).show()
            }
        }

    // Location permission launcher
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            checkAllPermissions()
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Please tap 'Grant' again to allow 'All the time' access", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    private val bgLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            checkAllPermissions()
            if (granted) {
                Toast.makeText(this, "Background location granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Background location is needed to detect location usage in background", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAllPermissions()

        setContent {
            PrivaDoTTheme {
                val hasOverlay by _hasOverlayPermission
                val hasAccessibility by _hasAccessibility
                val hasBatteryExempt by _hasBatteryOptExempt
                val hasNotification by _hasNotificationPerm
                val hasLocation by _hasLocationPerm
                val isServiceRunning by _isServiceRunning

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0D1117)
                ) {
                    var currentScreen by remember { mutableStateOf("dashboard") }

                    when (currentScreen) {
                        "dashboard" -> {
                            DashboardScreen(
                                hasOverlayPermission = hasOverlay,
                                hasAccessibility = hasAccessibility,
                                hasBatteryExempt = hasBatteryExempt,
                                hasNotificationPerm = hasNotification,
                                hasLocationPerm = hasLocation,
                                isServiceRunning = isServiceRunning,
                                onRequestOverlay = { requestOverlayPermission() },
                                onRequestAccessibility = { requestAccessibilityPermission() },
                                onRequestBattery = { requestBatteryOptExemption() },
                                onRequestNotification = { requestNotificationPermission() },
                                onRequestLocation = { requestLocationPermission() },
                                onRequestAutostart = { openAutostartSettings() },
                                onViewHistory = { currentScreen = "history" },
                                onViewSettings = { currentScreen = "settings" }
                            )
                        }
                        "history" -> {
                            val historyDao = AppDatabase.getDatabase(this@MainActivity).historyDao()
                            HistoryScreen(
                                historyDao = historyDao,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                        "settings" -> {
                            val settingsManager = SettingsManager(this@MainActivity)
                            SettingsScreen(
                                settingsManager = settingsManager,
                                onBack = { currentScreen = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAllPermissions()
    }

    private fun checkAllPermissions() {
        // Overlay permission
        _hasOverlayPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        // Accessibility Service
        _hasAccessibility.value = isAccessibilityServiceEnabled(this, PrivaDoTService::class.java)

        // Battery optimization exemption
        _hasBatteryOptExempt.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true

        // Notification permission (Android 13+)
        _hasNotificationPerm.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        // Location permission
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasBg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
        _hasLocationPerm.value = hasFine && hasBg

        // Service state
        _isServiceRunning.value = PrivaDoTService.isRunning
    }

    private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
        val expectedComponentName = ComponentName(context, accessibilityService)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, "Find 'PrivaDoT' and enable the Accessibility Service", Toast.LENGTH_LONG).show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Enable 'Display over other apps' for PrivaDoT", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("BatteryLife")
    private fun requestBatteryOptExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    } catch (e2: Exception) {
                        Toast.makeText(this, "Please manually disable battery optimization for PrivaDoT", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestLocationPermission() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBg) {
                bgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    private fun openAutostartSettings() {
        val autostartIntents = listOf(
            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
            Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
            Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")),
            Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")),
            Intent().setComponent(ComponentName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity")),
            Intent().setComponent(ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"))
        )

        for (intent in autostartIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                    startActivity(intent)
                    Toast.makeText(this, "Enable autostart for PrivaDoT", Toast.LENGTH_LONG).show()
                    return
                }
            } catch (_: Exception) { }
        }

        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            Toast.makeText(this, "Please enable Autostart / Background activity for PrivaDoT", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Please go to Settings > Battery/Apps and allow Background execution for PrivaDoT", Toast.LENGTH_LONG).show()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Dashboard Screen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun DashboardScreen(
    hasOverlayPermission: Boolean,
    hasAccessibility: Boolean,
    hasBatteryExempt: Boolean,
    hasNotificationPerm: Boolean,
    hasLocationPerm: Boolean,
    isServiceRunning: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestAutostart: () -> Unit,
    onViewHistory: () -> Unit,
    onViewSettings: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isServiceRunning) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    var isInstructionsExpanded by remember { mutableStateOf(false) }
    var isTroubleshootingExpanded by remember { mutableStateOf(false) }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }
    var showLocationDisclosure by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D1117), Color(0xFF161B22), Color(0xFF0D1117))
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWideScreen = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .widthIn(max = 800.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(if (isWideScreen) 32.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── App Logo ──
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0D1117), Color(0xFF161B22)))
                ),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val drawable = remember { ContextCompat.getDrawable(context, R.mipmap.ic_launcher) }
            val bitmap = remember(drawable) { drawable?.toBitmap(width = 256, height = 256)?.asImageBitmap() }
            
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(72.dp)
                )
            } else {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Security,
                    contentDescription = "App Logo",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF00FF88)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text("PrivaDoT", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        Text("Privacy Indicator", fontSize = 12.sp, color = Color(0xFF8B949E), letterSpacing = 2.sp)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Status Card ──
        Card(
            modifier = Modifier.fillMaxWidth().scale(scale),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceRunning) Color(0xFF1A2E1A) else Color(0xFF21262D)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) Color(0xFF3FB950) else Color(0xFFF85149))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isServiceRunning) "PROTECTION ACTIVE" else "PROTECTION OFF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isServiceRunning) Color(0xFF3FB950) else Color(0xFFF85149),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isServiceRunning) "Accessibility service is monitoring"
                           else "Grant all permissions to activate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8B949E),
                    textAlign = TextAlign.Center
                )

                // Sensor chips when running
                AnimatedVisibility(visible = isServiceRunning, enter = fadeIn(), exit = fadeOut()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SensorChip(icon = Icons.Default.CameraAlt, label = "Camera", color = Color(0xFF3FB950))
                        SensorChip(icon = Icons.Default.Mic, label = "Mic", color = Color(0xFFF0883E))
                        SensorChip(icon = Icons.Default.LocationOn, label = "Location", color = Color(0xFF58A6FF))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ══════════════════════════════════════════════════════════════════
        // IMPORTANT INSTRUCTIONS
        // ══════════════════════════════════════════════════════════════════
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isInstructionsExpanded = !isInstructionsExpanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF261D13)),
            border = BorderStroke(1.dp, Color(0xFFE3B341))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Warning", tint = Color(0xFFE3B341), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("How to keep PrivaDoT running 24/7", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE3B341))
                    }
                    Icon(
                        imageVector = if (isInstructionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = Color(0xFFE3B341)
                    )
                }
                AnimatedVisibility(visible = isInstructionsExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Android aggressively kills background apps. Follow these steps to prevent 'Not Working' error:", fontSize = 12.sp, color = Color(0xFFD2A8FF))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("1. Allow 'Battery Unrestricted' below.\n2. Click 'Autostart' below and enable it.\n3. Open Recent Apps (Multitasking) menu and LOCK/PIN this app.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isTroubleshootingExpanded = !isTroubleshootingExpanded },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFF58A6FF), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Troubleshooting & Tips", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF58A6FF))
                    }
                    Icon(
                        imageVector = if (isTroubleshootingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse",
                        tint = Color(0xFF58A6FF)
                    )
                }
                
                AnimatedVisibility(visible = isTroubleshootingExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("• Restricted Setting (Android 13+): If Accessibility is blocked, go to Phone Settings > Apps > PrivaDoT > click 3-dots (top right) > Allow Restricted Settings.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Location Dot (Android 11): Only detects Hardware GPS usage (e.g. Navigation), not Network location.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Play Protect: If blocked during install, go to Play Store > Profile > Play Protect > Settings > Turn off 'Scan apps'.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Dots Not Showing: If the location or any other dot stops showing, simply Restart your phone to reset the sensors.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Modern vs Old Android: Supports Android 7.0+. On highly aggressive modern phones (MIUI, ColorOS) or very old phones, dots might delay. Locking the app in 'Recents' fixes this.", fontSize = 12.sp, color = Color(0xFFC9D1D9), lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ══════════════════════════════════════════════════════════════════
        // PERMISSIONS SECTION
        // ══════════════════════════════════════════════════════════════════
        Text(
            text = "REQUIRED PERMISSIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B949E),
            letterSpacing = 1.5.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (isWideScreen) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Accessibility Service", description = "Keeps the app alive & running", icon = Icons.Default.Accessibility, isGranted = hasAccessibility, onRequest = { showAccessibilityDisclosure = true })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Display Over Apps", description = "Show privacy dots on screen", icon = Icons.Default.Layers, isGranted = hasOverlayPermission, onRequest = onRequestOverlay)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Location Permission", description = "Required to detect GPS usage", icon = Icons.Default.LocationOn, isGranted = hasLocationPerm, onRequest = { showLocationDisclosure = true })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Notifications", description = "Required for background service", icon = Icons.Default.Notifications, isGranted = hasNotificationPerm, onRequest = onRequestNotification)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Battery Unrestricted", description = "Prevent system from killing PrivaDoT", icon = Icons.Default.BatteryChargingFull, isGranted = hasBatteryExempt, onRequest = onRequestBattery)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        PermissionCard(title = "Autostart (Xiaomi/Oppo)", description = "Start automatically after reboot", icon = Icons.Default.RestartAlt, isGranted = false, alwaysShowAction = true, actionLabel = "Open", onRequest = onRequestAutostart)
                    }
                }
            }
        } else {
            PermissionCard(title = "Accessibility Service", description = "Keeps the app alive & running", icon = Icons.Default.Accessibility, isGranted = hasAccessibility, onRequest = { showAccessibilityDisclosure = true })
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(title = "Display Over Apps", description = "Show privacy dots on screen", icon = Icons.Default.Layers, isGranted = hasOverlayPermission, onRequest = onRequestOverlay)
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(title = "Location Permission", description = "Required to detect GPS usage", icon = Icons.Default.LocationOn, isGranted = hasLocationPerm, onRequest = { showLocationDisclosure = true })
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(title = "Notifications", description = "Required for background service", icon = Icons.Default.Notifications, isGranted = hasNotificationPerm, onRequest = onRequestNotification)
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(title = "Battery Unrestricted", description = "Prevent system from killing PrivaDoT", icon = Icons.Default.BatteryChargingFull, isGranted = hasBatteryExempt, onRequest = onRequestBattery)
            Spacer(modifier = Modifier.height(8.dp))
            PermissionCard(title = "Autostart (Xiaomi/Oppo/Vivo/etc)", description = "Start automatically after reboot", icon = Icons.Default.RestartAlt, isGranted = false, alwaysShowAction = true, actionLabel = "Open", onRequest = onRequestAutostart)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Bottom Buttons ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onViewHistory,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E)),
                border = BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("History", fontSize = 14.sp)
            }

            OutlinedButton(
                onClick = onViewSettings,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E)),
                border = BorderStroke(1.dp, Color(0xFF30363D))
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Settings", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        if (showAccessibilityDisclosure) {
            AlertDialog(
                onDismissRequest = { showAccessibilityDisclosure = false },
                title = { Text("Prominent Disclosure", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("PrivaDoT uses the AccessibilityService API to detect and monitor when other apps access your device's camera and microphone in real-time. This allows us to display privacy indicators on your screen.\n\nWe do not collect, store, or share any of your personal data or activity.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showAccessibilityDisclosure = false
                        onRequestAccessibility()
                    }) { Text("I Agree", color = Color(0xFF3FB950)) }
                },
                dismissButton = {
                    TextButton(onClick = { showAccessibilityDisclosure = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
                },
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFC9D1D9)
            )
        }

        if (showLocationDisclosure) {
            AlertDialog(
                onDismissRequest = { showLocationDisclosure = false },
                title = { Text("Prominent Disclosure", fontWeight = FontWeight.Bold) },
                text = { 
                    Text("PrivaDoT needs access to your background location to detect when other apps are secretly using your GPS coordinates in the background. This allows us to display the location privacy indicator.\n\nWe do not track, collect, store, or share your actual location data.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLocationDisclosure = false
                        onRequestLocation()
                    }) { Text("I Agree", color = Color(0xFF3FB950)) }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationDisclosure = false }) { Text("Cancel", color = Color(0xFF8B949E)) }
                },
                containerColor = Color(0xFF161B22),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFC9D1D9)
            )
        }
    }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Permission Card Component
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    alwaysShowAction: Boolean = false,
    actionLabel: String = "Grant",
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted && !alwaysShowAction) Color(0xFF1A2E1A) else Color(0xFF161B22)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isGranted && !alwaysShowAction) Color(0xFF3FB950).copy(alpha = 0.15f)
                        else Color(0xFFF0883E).copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted && !alwaysShowAction) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted && !alwaysShowAction) Color(0xFF3FB950) else Color(0xFFF0883E),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = if (isGranted && !alwaysShowAction) "Granted ✓" else description,
                    fontSize = 11.sp,
                    color = if (isGranted && !alwaysShowAction) Color(0xFF3FB950) else Color(0xFF8B949E)
                )
            }

            if (!isGranted || alwaysShowAction) {
                Button(
                    onClick = onRequest,
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0883E)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                ) {
                    Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SensorChip(icon: ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
