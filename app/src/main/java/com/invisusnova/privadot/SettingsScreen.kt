package com.invisusnova.privadot

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.invisusnova.privadot.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val ACTION_PREVIEW_SENSOR = "com.invisusnova.privadot.ACTION_PREVIEW_SENSOR"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settingsManager: SettingsManager, onBack: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Local temporary states
    var cameraColor by remember { mutableStateOf("#3FB950") }
    var micColor by remember { mutableStateOf("#F0883E") }
    var locationColor by remember { mutableStateOf("#58A6FF") }
    
    var dotSize by remember { mutableIntStateOf(8) }

    var cameraPosX by remember { mutableFloatStateOf(0.86f) }
    var cameraPosY by remember { mutableFloatStateOf(0.03f) }
    
    var micPosX by remember { mutableFloatStateOf(0.93f) }
    var micPosY by remember { mutableFloatStateOf(0.03f) }
    
    var locationPosX by remember { mutableFloatStateOf(1.0f) }
    var locationPosY by remember { mutableFloatStateOf(0.03f) }

    var isLoaded by remember { mutableStateOf(false) }
    var showColorPickerFor by remember { mutableStateOf<String?>(null) }

    // Load initial values once
    LaunchedEffect(Unit) {
        cameraColor = settingsManager.cameraColorFlow.first()
        micColor = settingsManager.micColorFlow.first()
        locationColor = settingsManager.locationColorFlow.first()
        dotSize = settingsManager.dotSizeFlow.first()
        
        cameraPosX = settingsManager.cameraPosXFlow.first()
        cameraPosY = settingsManager.cameraPosYFlow.first()
        micPosX = settingsManager.micPosXFlow.first()
        micPosY = settingsManager.micPosYFlow.first()
        locationPosX = settingsManager.locationPosXFlow.first()
        locationPosY = settingsManager.locationPosYFlow.first()
        
        isLoaded = true
    }

    val presetColors = listOf(
        "#3FB950" to "Green",
        "#F0883E" to "Orange",
        "#58A6FF" to "Blue",
        "#F85149" to "Red",
        "#BC8CFF" to "Purple"
    )

    fun sendPreviewIntent() {
        val intent = Intent(ACTION_PREVIEW_SENSOR).apply {
            putExtra("SENSOR_TYPE", "ALL")
            putExtra("EXTRA_CAM_COLOR", cameraColor)
            putExtra("EXTRA_MIC_COLOR", micColor)
            putExtra("EXTRA_LOC_COLOR", locationColor)
            putExtra("EXTRA_SIZE", dotSize)
            putExtra("EXTRA_CAM_X", cameraPosX)
            putExtra("EXTRA_CAM_Y", cameraPosY)
            putExtra("EXTRA_MIC_X", micPosX)
            putExtra("EXTRA_MIC_Y", micPosY)
            putExtra("EXTRA_LOC_X", locationPosX)
            putExtra("EXTRA_LOC_Y", locationPosY)
        }
        context.sendBroadcast(intent)
    }

    fun clearPreviewIntent() {
        val intent = Intent(ACTION_PREVIEW_SENSOR).apply {
            putExtra("SENSOR_TYPE", "CLEAR")
        }
        context.sendBroadcast(intent)
    }

    DisposableEffect(Unit) {
        onDispose { clearPreviewIntent() }
    }
    
    fun applyDefaultStates() {
        cameraColor = "#3FB950"
        micColor = "#F0883E"
        locationColor = "#58A6FF"
        dotSize = 8
        cameraPosX = 0.86f
        cameraPosY = 0.03f
        micPosX = 0.93f
        micPosY = 0.03f
        locationPosX = 1.0f
        locationPosY = 0.03f
        sendPreviewIntent()
    }

    fun saveAndExit() {
        coroutineScope.launch {
            settingsManager.saveCameraColor(cameraColor)
            settingsManager.saveMicColor(micColor)
            settingsManager.saveLocationColor(locationColor)
            settingsManager.saveDotSize(dotSize)
            
            settingsManager.saveCameraPos(cameraPosX, cameraPosY)
            settingsManager.saveMicPos(micPosX, micPosY)
            settingsManager.saveLocationPos(locationPosX, locationPosY)
            
            clearPreviewIntent()
            onBack()
        }
    }

    if (!isLoaded) return // Show blank until loaded

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { applyDefaultStates() }) {
                        Icon(
                            Icons.Default.Restore, 
                            contentDescription = "Reset Defaults",
                            tint = Color(0xFF8B949E)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = Color(0xFF8B949E))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color(0xFF8B949E)
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161B22))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B949E))
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { saveAndExit() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                ) {
                    Text("Save & Apply")
                }
            }
        },
        containerColor = Color(0xFF0D1117)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            
            SensorConfigSection(
                title = "CAMERA INDICATOR",
                currentColorHex = cameraColor,
                posX = cameraPosX,
                posY = cameraPosY,
                colors = presetColors,
                onColorSelected = { cameraColor = it; sendPreviewIntent() },
                onCustomColorClicked = { showColorPickerFor = "CAMERA" },
                onPosChange = { x, y -> cameraPosX = x; cameraPosY = y; sendPreviewIntent() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            SensorConfigSection(
                title = "MICROPHONE INDICATOR",
                currentColorHex = micColor,
                posX = micPosX,
                posY = micPosY,
                colors = presetColors,
                onColorSelected = { micColor = it; sendPreviewIntent() },
                onCustomColorClicked = { showColorPickerFor = "MIC" },
                onPosChange = { x, y -> micPosX = x; micPosY = y; sendPreviewIntent() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            SensorConfigSection(
                title = "LOCATION INDICATOR",
                currentColorHex = locationColor,
                posX = locationPosX,
                posY = locationPosY,
                colors = presetColors,
                onColorSelected = { locationColor = it; sendPreviewIntent() },
                onCustomColorClicked = { showColorPickerFor = "LOCATION" },
                onPosChange = { x, y -> locationPosX = x; locationPosY = y; sendPreviewIntent() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "INDICATOR SIZE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B949E),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161B22))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val previewSize = if (dotSize == 0) 1 else dotSize
                    Box(modifier = Modifier.size(previewSize.dp).clip(CircleShape).background(safeParseColor(cameraColor)))
                    Box(modifier = Modifier.size(previewSize.dp).clip(CircleShape).background(safeParseColor(micColor)))
                    Box(modifier = Modifier.size(previewSize.dp).clip(CircleShape).background(safeParseColor(locationColor)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = dotSize.toFloat(),
                onValueChange = { newValue ->
                    dotSize = newValue.toInt()
                    sendPreviewIntent()
                },
                valueRange = 0f..10f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF58A6FF),
                    activeTrackColor = Color(0xFF58A6FF),
                    inactiveTrackColor = Color(0xFF21262D)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0dp", color = Color(0xFF484F58), fontSize = 12.sp)
                Text(
                    "${dotSize}dp",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("10dp", color = Color(0xFF484F58), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showColorPickerFor != null) {
        val initialColor = when(showColorPickerFor) {
            "CAMERA" -> cameraColor
            "MIC" -> micColor
            "LOCATION" -> locationColor
            else -> "#FFFFFF"
        }

        CustomColorPickerDialog(
            initialColorHex = initialColor,
            onDismiss = { showColorPickerFor = null },
            onColorConfirmed = { hex ->
                when(showColorPickerFor) {
                    "CAMERA" -> cameraColor = hex
                    "MIC" -> micColor = hex
                    "LOCATION" -> locationColor = hex
                }
                sendPreviewIntent()
                showColorPickerFor = null
            }
        )
    }
}

@Composable
fun SensorConfigSection(
    title: String,
    currentColorHex: String,
    posX: Float,
    posY: Float,
    colors: List<Pair<String, String>>,
    onColorSelected: (String) -> Unit,
    onCustomColorClicked: () -> Unit,
    onPosChange: (x: Float, y: Float) -> Unit
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF8B949E),
        letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colors.forEach { (colorHex, name) ->
            val color = safeParseColor(colorHex)
            val isSelected = currentColorHex.equals(colorHex, ignoreCase = true)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (isSelected) Modifier.border(
                                width = 3.dp,
                                color = Color.White,
                                shape = CircleShape
                            ) else Modifier
                        )
                        .clickable { onColorSelected(colorHex) }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = if (isSelected) Color.White else Color(0xFF484F58)
                )
            }
        }
        
        val isCustomSelected = colors.none { it.first.equals(currentColorHex, ignoreCase = true) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isCustomSelected) safeParseColor(currentColorHex) else Color(0xFF21262D))
                    .then(
                        if (isCustomSelected) Modifier.border(
                            width = 3.dp,
                            color = Color.White,
                            shape = CircleShape
                        ) else Modifier.border(
                            width = 1.dp,
                            color = Color(0xFF484F58),
                            shape = CircleShape
                        )
                    )
                    .clickable { onCustomColorClicked() },
                contentAlignment = Alignment.Center
            ) {
                if (!isCustomSelected) {
                    Icon(Icons.Default.Add, contentDescription = "Custom", tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Custom",
                fontSize = 10.sp,
                color = if (isCustomSelected) Color.White else Color(0xFF484F58)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("X: ${((posX) * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Text("Y: ${((posY) * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
    }
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Slider(
            value = posX,
            onValueChange = { onPosChange(it, posY) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF), inactiveTrackColor = Color(0xFF21262D)
            ),
            modifier = Modifier.weight(1f)
        )
        Slider(
            value = posY,
            onValueChange = { onPosChange(posX, it) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF58A6FF), activeTrackColor = Color(0xFF58A6FF), inactiveTrackColor = Color(0xFF21262D)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}


@Composable
fun CustomColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorConfirmed: (String) -> Unit
) {
    var red by remember { mutableFloatStateOf(0f) }
    var green by remember { mutableFloatStateOf(0f) }
    var blue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(initialColorHex) {
        try {
            val colorInt = android.graphics.Color.parseColor(initialColorHex)
            red = android.graphics.Color.red(colorInt).toFloat()
            green = android.graphics.Color.green(colorInt).toFloat()
            blue = android.graphics.Color.blue(colorInt).toFloat()
        } catch (e: Exception) {
            red = 255f
            green = 255f
            blue = 255f
        }
    }

    val currentColorHex = String.format("#%02X%02X%02X", red.toInt(), green.toInt(), blue.toInt())
    val currentColor = safeParseColor(currentColorHex)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Pick a Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(2.dp, Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(currentColorHex, color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                Spacer(modifier = Modifier.height(24.dp))

                // RGB Sliders
                ColorSlider("Red", red, Color.Red) { red = it }
                ColorSlider("Green", green, Color.Green) { green = it }
                ColorSlider("Blue", blue, Color.Blue) { blue = it }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF8B949E))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onColorConfirmed(currentColorHex) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3FB950))
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label.take(1), color = color, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Color(0xFF21262D)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(value.toInt().toString(), color = Color.White, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
    }
}

fun safeParseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.White
    }
}
