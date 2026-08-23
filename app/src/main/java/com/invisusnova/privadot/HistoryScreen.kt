package com.invisusnova.privadot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.invisusnova.privadot.data.HistoryDao
import com.invisusnova.privadot.data.HistoryEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(historyDao: HistoryDao, onBack: () -> Unit) {
    val historyList by historyDao.getAllHistory().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Access History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = {
                            val dbContext = context
                            coroutineScope.launch {
                                com.invisusnova.privadot.data.AppDatabase.getDatabase(dbContext).secureClearAndVacuum()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History (Secure Wipe)", tint = Color(0xFFF85149))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1117),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0D1117)
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF30363D),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No activity recorded yet",
                        color = Color(0xFF8B949E),
                        fontSize = 16.sp
                    )
                    Text(
                        "Sensor access events will appear here",
                        color = Color(0xFF484F58),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(historyList) { historyItem ->
                        HistoryItemCard(historyItem)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(item: HistoryEntity) {
    val (icon, color, label) = when (item.sensorType) {
        "CAMERA" -> Triple(Icons.Default.CameraAlt, Color(0xFF3FB950), "Camera")
        "MIC" -> Triple(Icons.Default.Mic, Color(0xFFF0883E), "Microphone")
        "LOCATION" -> Triple(Icons.Default.LocationOn, Color(0xFF58A6FF), "Location")
        else -> Triple(Icons.Default.Sensors, Color(0xFF8B949E), item.sensorType)
    }

    val context = LocalContext.current
    val appIcon = remember(item.packageName) { getAppIcon(context, item.packageName) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = item.appName,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF21262D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = item.packageName,
                        fontSize = 12.sp,
                        color = Color(0xFF8B949E)
                    )
                }
                Text(
                    text = formatTimestamp(item.timestamp),
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.align(Alignment.Top)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF30363D), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = label, fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "Duration", tint = Color(0xFF8B949E), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = formatDuration(item.durationMs), fontSize = 13.sp, color = Color(0xFF8B949E))
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs < 1000) return "1 sec"
    val seconds = durationMs / 1000
    if (seconds < 60) return "$seconds sec"
    val minutes = seconds / 60
    val remSecs = seconds % 60
    return if (remSecs > 0) "${minutes}m ${remSecs}s" else "${minutes} min"
}

private fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
            ?: Bitmap.createBitmap(drawable.intrinsicWidth.coerceAtLeast(1), drawable.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
                val canvas = Canvas(it)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
            }
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
