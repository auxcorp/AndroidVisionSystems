package com.industrialvision.systems

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Industrial Vision Systems Application
 *
 * A professional-grade industrial vision platform featuring:
 * - Multi-Agent AI architecture for distributed analysis
 * - Deep Learning powered defect detection
 * - LLM integration for intelligent insights
 * - Real-time camera processing pipeline
 * - Comprehensive industrial inspection modules
 */
@HiltAndroidApp
class IndustrialVisionApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        initializeVisionSystem()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Vision Processing Channel
            val processingChannel = NotificationChannel(
                CHANNEL_VISION_PROCESSING,
                "Vision Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active vision processing tasks"
                setShowBadge(false)
            }

            // Inspection Results Channel
            val resultsChannel = NotificationChannel(
                CHANNEL_INSPECTION_RESULTS,
                "Inspection Results",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for completed inspection results"
                enableVibration(true)
            }

            // Agent Activity Channel
            val agentChannel = NotificationChannel(
                CHANNEL_AGENT_ACTIVITY,
                "AI Agent Activity",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for AI agent status and insights"
            }

            // Alerts Channel
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Quality Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical quality control alerts"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannels(
                listOf(processingChannel, resultsChannel, agentChannel, alertsChannel)
            )
        }
    }

    private fun initializeVisionSystem() {
        // Initialize core vision components lazily
        // TensorFlow Lite, ML Kit models, and agent systems
        // are initialized on first use for faster startup
    }

    companion object {
        lateinit var instance: IndustrialVisionApp
            private set

        const val CHANNEL_VISION_PROCESSING = "vision_processing"
        const val CHANNEL_INSPECTION_RESULTS = "inspection_results"
        const val CHANNEL_AGENT_ACTIVITY = "agent_activity"
        const val CHANNEL_ALERTS = "quality_alerts"
    }
}
