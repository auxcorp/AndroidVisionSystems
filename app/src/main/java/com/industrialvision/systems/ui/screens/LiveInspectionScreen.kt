package com.industrialvision.systems.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.industrialvision.systems.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Live Inspection Screen - Real-time inspection interface
 *
 * Features:
 * - Real-time camera preview with overlays
 * - Live defect detection with bounding boxes
 * - Continuous analysis feedback
 * - Inspection statistics
 * - Recording capability
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveInspectionScreen(
    onNavigateBack: () -> Unit,
    onInspectionComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(true) }
    var isRecording by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(0) }
    var fps by remember { mutableStateOf(0f) }
    var defectsDetected by remember { mutableStateOf(0) }
    var passCount by remember { mutableStateOf(0) }
    var failCount by remember { mutableStateOf(0) }

    // Simulated live detection
    var currentDetections by remember { mutableStateOf<List<LiveDetection>>(emptyList()) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(100) // 10 FPS simulation
            frameCount++
            fps = 10f

            // Simulate random detections
            if (frameCount % 30 == 0) {
                val hasDefect = (Math.random() < 0.1)
                if (hasDefect) {
                    defectsDetected++
                    failCount++
                    currentDetections = listOf(
                        LiveDetection(
                            id = System.currentTimeMillis().toString(),
                            type = listOf("Scratch", "Dent", "Crack").random(),
                            confidence = (0.7f + Math.random().toFloat() * 0.25f),
                            x = (Math.random() * 0.6f + 0.2f).toFloat(),
                            y = (Math.random() * 0.6f + 0.2f).toFloat(),
                            width = (Math.random() * 0.15f + 0.05f).toFloat(),
                            height = (Math.random() * 0.15f + 0.05f).toFloat()
                        )
                    )
                } else {
                    passCount++
                    currentDetections = emptyList()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            LiveInspectionTopBar(
                isRunning = isRunning,
                isRecording = isRecording,
                onBackClick = onNavigateBack,
                onPauseResumeClick = { isRunning = !isRunning },
                onRecordClick = { isRecording = !isRecording }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera Preview with Overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
            ) {
                // Simulated camera preview
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Live Camera Feed",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                // Detection Overlays
                currentDetections.forEach { detection ->
                    DetectionOverlay(detection = detection)
                }

                // Status Overlay
                LiveStatusOverlay(
                    isRunning = isRunning,
                    isRecording = isRecording,
                    fps = fps,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Current Status Indicator
                CurrentStatusIndicator(
                    hasDefect = currentDetections.isNotEmpty(),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            // Statistics Panel
            LiveStatisticsPanel(
                defectsDetected = defectsDetected,
                passCount = passCount,
                failCount = failCount,
                fps = fps
            )

            // Detection Log
            DetectionLogPanel(
                detections = currentDetections,
                modifier = Modifier.weight(0.5f)
            )

            // Control Panel
            LiveControlPanel(
                isRunning = isRunning,
                onStopClick = {
                    isRunning = false
                    onInspectionComplete("live_${System.currentTimeMillis()}")
                },
                onCaptureClick = {
                    // Capture current frame
                }
            )
        }
    }
}

data class LiveDetection(
    val id: String,
    val type: String,
    val confidence: Float,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

@Composable
private fun DetectionOverlay(detection: LiveDetection) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = (detection.x * 300).dp,
                top = (detection.y * 400).dp
            )
    ) {
        Box(
            modifier = Modifier
                .size(
                    width = (detection.width * 300).dp,
                    height = (detection.height * 300).dp
                )
                .border(
                    width = 2.dp,
                    color = when {
                        detection.confidence > 0.9f -> SeverityCritical
                        detection.confidence > 0.8f -> SeverityMajor
                        else -> SeverityMinor
                    },
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = "${detection.type} ${(detection.confidence * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveInspectionTopBar(
    isRunning: Boolean,
    isRecording: Boolean,
    onBackClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onRecordClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRunning) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StatusPass)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Live Inspection")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        },
        actions = {
            IconButton(onClick = onPauseResumeClick) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Resume"
                )
            }
            IconButton(onClick = onRecordClick) {
                Icon(
                    if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun LiveStatusOverlay(
    isRunning: Boolean,
    isRecording: Boolean,
    fps: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "REC",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Text(
                text = "${fps.toInt()} FPS",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CurrentStatusIndicator(
    hasDefect: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(8.dp),
        color = if (hasDefect) SeverityCritical else StatusPass,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (hasDefect) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hasDefect) "DEFECT" else "PASS",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveStatisticsPanel(
    defectsDetected: Int,
    passCount: Int,
    failCount: Int,
    fps: Float
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(
                label = "Inspected",
                value = "${passCount + failCount}",
                color = MaterialTheme.colorScheme.primary
            )
            StatColumn(
                label = "Pass",
                value = "$passCount",
                color = StatusPass
            )
            StatColumn(
                label = "Fail",
                value = "$failCount",
                color = StatusFail
            )
            StatColumn(
                label = "Defects",
                value = "$defectsDetected",
                color = StatusWarning
            )
            StatColumn(
                label = "Rate",
                value = if (passCount + failCount > 0)
                    "${(passCount * 100 / (passCount + failCount))}%"
                else "0%",
                color = if (passCount > failCount) StatusPass else StatusFail
            )
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetectionLogPanel(
    detections: List<LiveDetection>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Detection Log",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (detections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusPass,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No defects detected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(detections) { detection ->
                        DetectionLogItem(detection = detection)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionLogItem(detection: LiveDetection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    when {
                        detection.confidence > 0.9f -> SeverityCritical
                        detection.confidence > 0.8f -> SeverityMajor
                        else -> SeverityMinor
                    }
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = detection.type,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${(detection.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LiveControlPanel(
    isRunning: Boolean,
    onStopClick: () -> Unit,
    onCaptureClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCaptureClick,
                enabled = isRunning
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Capture")
            }

            Button(
                onClick = onStopClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusFail
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop & Save")
            }
        }
    }
}
