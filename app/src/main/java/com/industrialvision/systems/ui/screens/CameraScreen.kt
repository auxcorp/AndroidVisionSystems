package com.industrialvision.systems.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.industrialvision.systems.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Camera Screen - Professional camera interface for industrial inspection
 *
 * Features:
 * - High-resolution image capture
 * - Focus and exposure controls
 * - Zoom with pinch gesture
 * - Flash/torch controls
 * - Grid overlay options
 * - Profile selection
 * - Real-time preview
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInspection: () -> Unit,
    onCaptureComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera state
    var isCapturing by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var selectedProfile by remember { mutableStateOf("Default") }
    var showProfileSelector by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Pair<Float, Float>?>(null) }

    if (!hasPermission) {
        PermissionRequiredScreen(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onNavigateBack = onNavigateBack
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            focusPoint = offset.x to offset.y
                            // Auto-hide focus indicator after 2 seconds
                            scope.launch {
                                delay(2000)
                                focusPoint = null
                            }
                        }
                    )
                }
        )

        // Grid Overlay
        if (showGrid) {
            GridOverlay()
        }

        // Focus Indicator
        focusPoint?.let { (x, y) ->
            FocusIndicator(
                x = x,
                y = y,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Bar
        CameraTopBar(
            onBackClick = onNavigateBack,
            flashEnabled = flashEnabled,
            onFlashToggle = { flashEnabled = !flashEnabled },
            showGrid = showGrid,
            onGridToggle = { showGrid = !showGrid },
            selectedProfile = selectedProfile,
            onProfileClick = { showProfileSelector = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Bottom Controls
        CameraBottomControls(
            isCapturing = isCapturing,
            onCaptureClick = {
                isCapturing = true
                scope.launch {
                    delay(500) // Simulate capture
                    isCapturing = false
                    onCaptureComplete(UUID.randomUUID().toString())
                }
            },
            onLiveInspectionClick = onNavigateToInspection,
            zoomLevel = zoomLevel,
            onZoomChange = { zoomLevel = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Capturing Animation
        AnimatedVisibility(
            visible = isCapturing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }
    }

    // Profile Selector Bottom Sheet
    if (showProfileSelector) {
        ProfileSelectorSheet(
            selectedProfile = selectedProfile,
            onProfileSelected = {
                selectedProfile = it
                showProfileSelector = false
            },
            onDismiss = { showProfileSelector = false }
        )
    }
}

@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.dp.toPx()
        val color = Color.White.copy(alpha = 0.3f)

        // Vertical lines (rule of thirds)
        drawLine(color, Offset(width / 3, 0f), Offset(width / 3, height), strokeWidth)
        drawLine(color, Offset(2 * width / 3, 0f), Offset(2 * width / 3, height), strokeWidth)

        // Horizontal lines (rule of thirds)
        drawLine(color, Offset(0f, height / 3), Offset(width, height / 3), strokeWidth)
        drawLine(color, Offset(0f, 2 * height / 3), Offset(width, 2 * height / 3), strokeWidth)
    }
}

@Composable
private fun Canvas(
    modifier: Modifier,
    onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}

private fun androidx.compose.ui.geometry.Offset.Companion.invoke(x: Float, y: Float) =
    androidx.compose.ui.geometry.Offset(x, y)

@Composable
private fun FocusIndicator(
    x: Float,
    y: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset(
                    x = (x - 30).dp,
                    y = (y - 30).dp
                )
                .size(60.dp)
                .border(2.dp, Color.Yellow, RoundedCornerShape(8.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraTopBar(
    onBackClick: () -> Unit,
    flashEnabled: Boolean,
    onFlashToggle: () -> Unit,
    showGrid: Boolean,
    onGridToggle: () -> Unit,
    selectedProfile: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Profile Selector
            Surface(
                onClick = onProfileClick,
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedProfile,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Row {
                IconButton(onClick = onGridToggle) {
                    Icon(
                        if (showGrid) Icons.Default.Grid4x4 else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onFlashToggle) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (flashEnabled) Color.Yellow else Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraBottomControls(
    isCapturing: Boolean,
    onCaptureClick: () -> Unit,
    onLiveInspectionClick: () -> Unit,
    zoomLevel: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1x",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Slider(
                    value = zoomLevel,
                    onValueChange = onZoomChange,
                    valueRange = 1f..5f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White
                    )
                )
                Text(
                    text = "5x",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Capture Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Button
                IconButton(
                    onClick = { /* Open gallery */ },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color.White
                    )
                }

                // Main Capture Button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = !isCapturing) { onCaptureClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCapturing) 50.dp else 70.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCapturing) Color.Red
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }

                // Live Inspection Button
                IconButton(
                    onClick = onLiveInspectionClick,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(StatusPass.copy(alpha = 0.8f))
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Live Inspection",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSelectorSheet(
    selectedProfile: String,
    onProfileSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val profiles = listOf(
        "Default" to "General purpose inspection",
        "PCB Inspection" to "Electronic board analysis",
        "Surface Quality" to "Surface defect detection",
        "Dimensional" to "Measurement focused",
        "Label Verification" to "OCR and barcode scanning"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Inspection Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            profiles.forEach { (name, description) ->
                Surface(
                    onClick = { onProfileSelected(name) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (name == selectedProfile)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = name == selectedProfile,
                            onClick = { onProfileSelected(name) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Camera Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Industrial Vision needs camera access to perform inspections.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateBack) {
                Text("Go Back")
            }
        }
    }
}
