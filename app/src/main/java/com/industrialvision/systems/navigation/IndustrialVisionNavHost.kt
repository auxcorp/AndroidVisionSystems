package com.industrialvision.systems.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.industrialvision.systems.ui.screens.*

/**
 * Navigation Routes for Industrial Vision Systems
 */
object Routes {
    const val DASHBOARD = "dashboard"
    const val CAMERA = "camera"
    const val LIVE_INSPECTION = "live_inspection"
    const val INSPECTION_RESULT = "inspection_result/{inspectionId}"
    const val AGENTS = "agents"
    const val AGENT_DETAIL = "agent_detail/{agentId}"
    const val ANALYTICS = "analytics"
    const val INSPECTION_HISTORY = "inspection_history"
    const val SETTINGS = "settings"
    const val PROFILE_EDITOR = "profile_editor/{profileId}"
    const val LLM_CHAT = "llm_chat"
    const val CALIBRATION = "calibration"
    const val EXPORT = "export"

    fun inspectionResult(inspectionId: String) = "inspection_result/$inspectionId"
    fun agentDetail(agentId: String) = "agent_detail/$agentId"
    fun profileEditor(profileId: String = "new") = "profile_editor/$profileId"
}

/**
 * Main Navigation Host for Industrial Vision Systems
 */
@Composable
fun IndustrialVisionNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(300)
            )
        }
    ) {
        // Dashboard - Main hub
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Routes.CAMERA) },
                onNavigateToLiveInspection = { navController.navigate(Routes.LIVE_INSPECTION) },
                onNavigateToAgents = { navController.navigate(Routes.AGENTS) },
                onNavigateToAnalytics = { navController.navigate(Routes.ANALYTICS) },
                onNavigateToHistory = { navController.navigate(Routes.INSPECTION_HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToLLMChat = { navController.navigate(Routes.LLM_CHAT) },
                onNavigateToInspectionResult = { id ->
                    navController.navigate(Routes.inspectionResult(id))
                }
            )
        }

        // Camera Preview
        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInspection = { navController.navigate(Routes.LIVE_INSPECTION) },
                onCaptureComplete = { inspectionId ->
                    navController.navigate(Routes.inspectionResult(inspectionId))
                }
            )
        }

        // Live Inspection
        composable(Routes.LIVE_INSPECTION) {
            LiveInspectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onInspectionComplete = { inspectionId ->
                    navController.navigate(Routes.inspectionResult(inspectionId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                }
            )
        }

        // Inspection Result Detail
        composable(
            route = Routes.INSPECTION_RESULT,
            arguments = listOf(
                navArgument("inspectionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getString("inspectionId") ?: return@composable
            InspectionResultScreen(
                inspectionId = inspectionId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExport = { navController.navigate(Routes.EXPORT) }
            )
        }

        // AI Agents Hub
        composable(Routes.AGENTS) {
            AgentsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAgentDetail = { agentId ->
                    navController.navigate(Routes.agentDetail(agentId))
                }
            )
        }

        // Agent Detail
        composable(
            route = Routes.AGENT_DETAIL,
            arguments = listOf(
                navArgument("agentId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val agentId = backStackEntry.arguments?.getString("agentId") ?: return@composable
            AgentDetailScreen(
                agentId = agentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Analytics Dashboard
        composable(Routes.ANALYTICS) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInspection = { inspectionId ->
                    navController.navigate(Routes.inspectionResult(inspectionId))
                }
            )
        }

        // Inspection History
        composable(Routes.INSPECTION_HISTORY) {
            InspectionHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInspection = { inspectionId ->
                    navController.navigate(Routes.inspectionResult(inspectionId))
                }
            )
        }

        // Settings
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfileEditor = { profileId ->
                    navController.navigate(Routes.profileEditor(profileId))
                },
                onNavigateToCalibration = { navController.navigate(Routes.CALIBRATION) }
            )
        }

        // Profile Editor
        composable(
            route = Routes.PROFILE_EDITOR,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "new"
            ProfileEditorScreen(
                profileId = profileId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // LLM Chat Interface
        composable(Routes.LLM_CHAT) {
            LLMChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Calibration
        composable(Routes.CALIBRATION) {
            CalibrationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Export
        composable(Routes.EXPORT) {
            ExportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
