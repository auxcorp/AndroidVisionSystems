package com.industrialvision.systems.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.industrialvision.systems.ui.theme.*

/**
 * Inspection Result Screen - Detailed inspection results view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionResultScreen(
    inspectionId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inspection Results") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = onNavigateToExport) {
                        Icon(Icons.Default.Download, contentDescription = "Export")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = StatusPass.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusPass,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "PASS",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusPass
                            )
                            Text(
                                text = "Quality Score: 94.2%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Inspection Details
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Inspection Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow("ID", "INS-2024-0247")
                        DetailRow("Profile", "PCB Inspection")
                        DetailRow("Timestamp", "2024-01-28 14:32:15")
                        DetailRow("Duration", "2.4 seconds")
                    }
                }
            }

            // Findings Summary
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Findings Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FindingSummaryRow("Critical", 0, SeverityCritical)
                        FindingSummaryRow("Major", 0, SeverityMajor)
                        FindingSummaryRow("Minor", 2, SeverityMinor)
                        FindingSummaryRow("Info", 3, SeverityInfo)
                    }
                }
            }

            // AI Analysis
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Analysis",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "The inspection results show excellent quality with minor cosmetic variations within acceptable tolerances. No functional defects detected. Recommended for production release.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confidence: 94%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FindingSummaryRow(
    severity: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = color.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = severity,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Agents Screen - AI Agents management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAgentDetail: (String) -> Unit
) {
    val agents = listOf(
        AgentInfo("1", "Defect Detection", "Active", true, "Deep learning powered defect detection"),
        AgentInfo("2", "OCR Reader", "Active", true, "Text recognition and verification"),
        AgentInfo("3", "Barcode Scanner", "Active", true, "QR and barcode scanning"),
        AgentInfo("4", "Dimensional Analyzer", "Ready", true, "Precision measurements"),
        AgentInfo("5", "Color Analyzer", "Ready", true, "Color matching and analysis"),
        AgentInfo("6", "AI Insight Generator", "Active", true, "LLM-powered insights"),
        AgentInfo("7", "Anomaly Detector", "Disabled", false, "Unsupervised anomaly detection")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agents") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(agents) { agent ->
                AgentCard(
                    agent = agent,
                    onClick = { onNavigateToAgentDetail(agent.id) }
                )
            }
        }
    }
}

data class AgentInfo(
    val id: String,
    val name: String,
    val status: String,
    val enabled: Boolean,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentCard(
    agent: AgentInfo,
    onClick: () -> Unit
) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SmartToy,
                contentDescription = null,
                tint = if (agent.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = agent.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = agent.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusChip(status = agent.status)
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status) {
        "Active" -> StatusPass
        "Ready" -> StatusPending
        "Processing" -> StatusProcessing
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * Agent Detail Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDetailScreen(
    agentId: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Defect Detection Agent") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Agent Configuration", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow("Model", "DefectNet-v2.1.0")
                        DetailRow("Input Size", "640x640")
                        DetailRow("Delegate", "GPU")
                        DetailRow("Confidence Threshold", "0.75")
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Statistics", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        DetailRow("Total Tasks", "1,247")
                        DetailRow("Success Rate", "99.2%")
                        DetailRow("Avg Processing Time", "145ms")
                    }
                }
            }
        }
    }
}

/**
 * Analytics Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInspection: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Weekly Summary", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            MetricItem("1,847", "Inspections")
                            MetricItem("94.8%", "Pass Rate")
                            MetricItem("127", "Defects")
                        }
                    }
                }
            }
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Top Defect Types", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        DefectTypeBar("Scratch", 45, MaterialTheme.colorScheme.primary)
                        DefectTypeBar("Dent", 32, MaterialTheme.colorScheme.secondary)
                        DefectTypeBar("Stain", 28, MaterialTheme.colorScheme.tertiary)
                        DefectTypeBar("Crack", 15, SeverityCritical)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DefectTypeBar(
    name: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        LinearProgressIndicator(
            progress = { count / 50f },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * Inspection History Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToInspection: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inspection History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Filter */ }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(20) { index ->
                HistoryItem(
                    id = "INS-2024-${247 - index}",
                    product = if (index % 2 == 0) "PCB Assembly A" else "Metal Housing B",
                    time = "${index * 15 + 5} min ago",
                    status = when {
                        index == 3 -> "Fail"
                        index % 5 == 0 -> "Warning"
                        else -> "Pass"
                    },
                    onClick = { onNavigateToInspection("$index") }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryItem(
    id: String,
    product: String,
    time: String,
    status: String,
    onClick: () -> Unit
) {
    val statusColor = when (status) {
        "Pass" -> StatusPass
        "Fail" -> StatusFail
        else -> StatusWarning
    }

    Card(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = id, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = product, style = MaterialTheme.typography.bodyMedium)
                Text(text = time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(status = status)
        }
    }
}

/**
 * Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfileEditor: (String) -> Unit,
    onNavigateToCalibration: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsSection("Camera") }
            item { SettingsItem("Resolution", "4032 x 3024") {} }
            item { SettingsItem("Auto Focus", "Enabled") {} }
            item { SettingsItem("Stabilization", "Enabled") {} }

            item { SettingsSection("AI Processing") }
            item { SettingsItem("GPU Acceleration", "Enabled") {} }
            item { SettingsItem("Confidence Threshold", "0.75") {} }
            item { SettingsItem("LLM Provider", "Anthropic Claude") {} }

            item { SettingsSection("Calibration") }
            item { SettingsItem("Camera Calibration", "Last: 3 days ago") { onNavigateToCalibration() } }
            item { SettingsItem("Measurement Units", "Millimeters") {} }

            item { SettingsSection("Data") }
            item { SettingsItem("Cloud Sync", "Enabled") {} }
            item { SettingsItem("Auto Export", "Disabled") {} }
            item { SettingsItem("Storage Usage", "2.4 GB / 10 GB") {} }

            item { SettingsSection("About") }
            item { SettingsItem("Version", "1.0.0") {} }
            item { SettingsItem("Build", "2024.01.28") {} }
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Profile Editor Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (profileId == "new") "New Profile" else "Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    TextButton(onClick = { /* Save */ onNavigateBack() }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = "PCB Inspection",
                onValueChange = {},
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enabled Modules", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ModuleToggle("Defect Detection", true)
            ModuleToggle("OCR", true)
            ModuleToggle("Barcode Scanning", true)
            ModuleToggle("Dimensional Measurement", false)
            ModuleToggle("Color Analysis", false)
        }
    }
}

@Composable
private fun ModuleToggle(name: String, enabled: Boolean) {
    var checked by remember { mutableStateOf(enabled) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name)
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
}

/**
 * LLM Chat Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMChatScreen(
    onNavigateBack: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val messages = remember {
        mutableStateListOf(
            ChatMsg("assistant", "Hello! I'm your AI assistant for quality control. How can I help you analyze your inspection results?")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI Assistant")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Ask about quality control...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (message.isNotBlank()) {
                                messages.add(ChatMsg("user", message))
                                messages.add(ChatMsg("assistant", "Based on your question about '$message', I can help analyze the inspection data and provide insights. Would you like me to look at specific defect patterns or overall quality trends?"))
                                message = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

data class ChatMsg(val role: String, val content: String)

@Composable
private fun ChatBubble(msg: ChatMsg) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Calibration Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Calibration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.CenterFocusWeak,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Position a calibration target in the camera view", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { /* Start calibration */ }) {
                Text("Start Calibration")
            }
        }
    }
}

/**
 * Export Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Report") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Export Format", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            ExportOption("PDF Report", "Complete inspection report with images", true)
            ExportOption("CSV Data", "Raw data for analysis", false)
            ExportOption("JSON", "Machine-readable format", false)
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { /* Export */ onNavigateBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export")
            }
        }
    }
}

@Composable
private fun ExportOption(title: String, description: String, selected: Boolean) {
    var isSelected by remember { mutableStateOf(selected) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { isSelected = true },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isSelected, onClick = { isSelected = true })
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.clickable(onClick: () -> Unit) {}
