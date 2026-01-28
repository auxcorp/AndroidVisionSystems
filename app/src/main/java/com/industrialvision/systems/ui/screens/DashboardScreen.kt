package com.industrialvision.systems.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.industrialvision.systems.ui.theme.*

/**
 * Dashboard Screen - Main hub of the Industrial Vision System
 *
 * Features:
 * - Quick action buttons for common tasks
 * - Real-time statistics overview
 * - Recent inspection history
 * - Agent status monitoring
 * - Quick access to all features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToLiveInspection: () -> Unit,
    onNavigateToAgents: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLLMChat: () -> Unit,
    onNavigateToInspectionResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            DashboardTopBar(
                onSettingsClick = onNavigateToSettings
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToCamera,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Inspection")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Statistics Overview
            item {
                StatisticsCard()
            }

            // Quick Actions
            item {
                QuickActionsSection(
                    onCameraClick = onNavigateToCamera,
                    onLiveInspectionClick = onNavigateToLiveInspection,
                    onAgentsClick = onNavigateToAgents,
                    onAnalyticsClick = onNavigateToAnalytics,
                    onLLMChatClick = onNavigateToLLMChat
                )
            }

            // Agent Status
            item {
                AgentStatusSection(
                    onViewAllClick = onNavigateToAgents
                )
            }

            // Recent Inspections
            item {
                RecentInspectionsSection(
                    onViewAllClick = onNavigateToHistory,
                    onInspectionClick = onNavigateToInspectionResult
                )
            }

            // System Health
            item {
                SystemHealthCard()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Industrial Vision",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI-Powered Quality Control",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = { /* Notifications */ }) {
                BadgedBox(badge = { Badge { Text("3") } }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun StatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Inspections",
                    value = "247",
                    icon = Icons.Default.DocumentScanner,
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    label = "Pass Rate",
                    value = "94.2%",
                    icon = Icons.Default.CheckCircle,
                    color = StatusPass
                )
                StatItem(
                    label = "Defects Found",
                    value = "18",
                    icon = Icons.Default.Warning,
                    color = StatusWarning
                )
                StatItem(
                    label = "Critical",
                    value = "2",
                    icon = Icons.Default.Error,
                    color = StatusFail
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsSection(
    onCameraClick: () -> Unit,
    onLiveInspectionClick: () -> Unit,
    onAgentsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onLLMChatClick: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                QuickActionCard(
                    title = "Capture",
                    subtitle = "Take photo",
                    icon = Icons.Default.PhotoCamera,
                    gradient = listOf(Color(0xFF0055D4), Color(0xFF003087)),
                    onClick = onCameraClick
                )
            }
            item {
                QuickActionCard(
                    title = "Live",
                    subtitle = "Real-time inspection",
                    icon = Icons.Default.Videocam,
                    gradient = listOf(Color(0xFF00875A), Color(0xFF00522E)),
                    onClick = onLiveInspectionClick
                )
            }
            item {
                QuickActionCard(
                    title = "AI Agents",
                    subtitle = "View status",
                    icon = Icons.Default.SmartToy,
                    gradient = listOf(Color(0xFF7C4DFF), Color(0xFF5E35B1)),
                    onClick = onAgentsClick
                )
            }
            item {
                QuickActionCard(
                    title = "Analytics",
                    subtitle = "View reports",
                    icon = Icons.Default.Analytics,
                    gradient = listOf(Color(0xFFE65100), Color(0xFF8B3000)),
                    onClick = onAnalyticsClick
                )
            }
            item {
                QuickActionCard(
                    title = "AI Chat",
                    subtitle = "Ask questions",
                    icon = Icons.Default.Chat,
                    gradient = listOf(Color(0xFF00BCD4), Color(0xFF0097A7)),
                    onClick = onLLMChatClick
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun AgentStatusSection(
    onViewAllClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Agents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAllClick) {
                Text("View All")
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AgentStatusRow(
                    name = "Defect Detection",
                    status = "Active",
                    statusColor = StatusPass
                )
                AgentStatusRow(
                    name = "OCR Reader",
                    status = "Active",
                    statusColor = StatusPass
                )
                AgentStatusRow(
                    name = "Dimensional Analyzer",
                    status = "Processing",
                    statusColor = StatusProcessing
                )
                AgentStatusRow(
                    name = "AI Insight Generator",
                    status = "Ready",
                    statusColor = StatusPending
                )
            }
        }
    }
}

@Composable
private fun AgentStatusRow(
    name: String,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentInspectionsSection(
    onViewAllClick: () -> Unit,
    onInspectionClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Inspections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onViewAllClick) {
                Text("View All")
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RecentInspectionItem(
                id = "INS-2024-0247",
                product = "PCB Assembly A",
                time = "2 min ago",
                status = "Pass",
                statusColor = StatusPass,
                onClick = { onInspectionClick("1") }
            )
            RecentInspectionItem(
                id = "INS-2024-0246",
                product = "Metal Housing B",
                time = "15 min ago",
                status = "Warning",
                statusColor = StatusWarning,
                onClick = { onInspectionClick("2") }
            )
            RecentInspectionItem(
                id = "INS-2024-0245",
                product = "PCB Assembly A",
                time = "28 min ago",
                status = "Fail",
                statusColor = StatusFail,
                onClick = { onInspectionClick("3") }
            )
        }
    }
}

@Composable
private fun RecentInspectionItem(
    id: String,
    product: String,
    time: String,
    status: String,
    statusColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = id,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = product,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SystemHealthCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "System Health",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthIndicator(
                    label = "Camera",
                    status = "OK",
                    color = StatusPass
                )
                HealthIndicator(
                    label = "ML Models",
                    status = "OK",
                    color = StatusPass
                )
                HealthIndicator(
                    label = "Storage",
                    status = "78%",
                    color = StatusWarning
                )
                HealthIndicator(
                    label = "Network",
                    status = "OK",
                    color = StatusPass
                )
            }
        }
    }
}

@Composable
private fun HealthIndicator(
    label: String,
    status: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = status,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
