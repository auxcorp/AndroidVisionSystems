package com.industrialvision.core.domain.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Core Domain Models for Industrial Vision Systems
 *
 * These models represent the fundamental entities used throughout
 * the inspection and analysis pipeline.
 */

/**
 * Represents a complete inspection session
 */
@Serializable
data class Inspection(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val profileName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: InspectionStatus = InspectionStatus.IN_PROGRESS,
    val imageUri: String? = null,
    val thumbnailUri: String? = null,
    val results: List<InspectionResult> = emptyList(),
    val metadata: InspectionMetadata = InspectionMetadata(),
    val aiAnalysis: AIAnalysis? = null,
    val overallScore: Float = 0f,
    val passFailStatus: PassFailStatus = PassFailStatus.PENDING
)

@Serializable
enum class InspectionStatus {
    PENDING,
    IN_PROGRESS,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Serializable
enum class PassFailStatus {
    PENDING,
    PASS,
    FAIL,
    WARNING,
    NEEDS_REVIEW
}

/**
 * Individual inspection result from a specific analysis module
 */
@Serializable
data class InspectionResult(
    val id: String = UUID.randomUUID().toString(),
    val moduleType: InspectionModuleType,
    val moduleName: String,
    val status: ResultStatus,
    val confidence: Float,
    val findings: List<Finding> = emptyList(),
    val metrics: Map<String, Float> = emptyMap(),
    val processingTimeMs: Long = 0,
    val rawData: String? = null
)

@Serializable
enum class InspectionModuleType {
    DEFECT_DETECTION,
    OCR_TEXT_RECOGNITION,
    BARCODE_SCANNING,
    DIMENSIONAL_MEASUREMENT,
    COLOR_ANALYSIS,
    SURFACE_INSPECTION,
    ASSEMBLY_VERIFICATION,
    OBJECT_DETECTION,
    PATTERN_MATCHING,
    EDGE_DETECTION,
    CONTOUR_ANALYSIS,
    HISTOGRAM_ANALYSIS,
    CUSTOM_MODEL
}

@Serializable
enum class ResultStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    SKIPPED,
    TIMEOUT
}

/**
 * A specific finding within an inspection result
 */
@Serializable
data class Finding(
    val id: String = UUID.randomUUID().toString(),
    val type: FindingType,
    val severity: FindingSeverity,
    val description: String,
    val confidence: Float,
    val boundingBox: BoundingBox? = null,
    val polygon: List<Point>? = null,
    val value: String? = null,
    val unit: String? = null,
    val referenceValue: String? = null,
    val deviation: Float? = null,
    val imageRegionUri: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

@Serializable
enum class FindingType {
    DEFECT,
    ANOMALY,
    MEASUREMENT,
    TEXT,
    BARCODE,
    COLOR,
    OBJECT,
    PATTERN,
    EDGE,
    CONTOUR,
    WARNING,
    INFO
}

@Serializable
enum class FindingSeverity {
    CRITICAL,
    MAJOR,
    MINOR,
    WARNING,
    INFO,
    PASS
}

/**
 * Bounding box for localized findings
 */
@Serializable
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f
)

/**
 * 2D Point representation
 */
@Serializable
data class Point(
    val x: Float,
    val y: Float
)

/**
 * Metadata about the inspection environment and capture
 */
@Serializable
data class InspectionMetadata(
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val cameraInfo: CameraInfo? = null,
    val captureSettings: CaptureSettings? = null,
    val environmentalConditions: EnvironmentalConditions? = null,
    val location: String? = null,
    val operatorId: String? = null,
    val batchId: String? = null,
    val productId: String? = null,
    val serialNumber: String? = null,
    val customFields: Map<String, String> = emptyMap()
)

@Serializable
data class CameraInfo(
    val cameraId: String,
    val sensorOrientation: Int,
    val focalLength: Float,
    val aperture: Float,
    val sensorSize: String
)

@Serializable
data class CaptureSettings(
    val resolution: String,
    val iso: Int,
    val exposureTime: Long,
    val whiteBalance: Int,
    val focusDistance: Float,
    val flashMode: String,
    val hdrEnabled: Boolean
)

@Serializable
data class EnvironmentalConditions(
    val lightLevel: Float,
    val temperature: Float? = null,
    val humidity: Float? = null
)

/**
 * AI-generated analysis and insights
 */
@Serializable
data class AIAnalysis(
    val summary: String,
    val insights: List<AIInsight>,
    val recommendations: List<String>,
    val rootCauseAnalysis: String? = null,
    val predictedOutcome: String? = null,
    val confidenceScore: Float,
    val agentContributions: List<AgentContribution> = emptyList(),
    val processingTimeMs: Long = 0
)

@Serializable
data class AIInsight(
    val category: String,
    val insight: String,
    val importance: InsightImportance,
    val relatedFindings: List<String> = emptyList(),
    val actionable: Boolean = false,
    val suggestedAction: String? = null
)

@Serializable
enum class InsightImportance {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFORMATIONAL
}

@Serializable
data class AgentContribution(
    val agentId: String,
    val agentName: String,
    val agentType: String,
    val contribution: String,
    val confidence: Float,
    val processingTimeMs: Long
)

/**
 * Inspection Profile - Configurable inspection template
 */
@Serializable
data class InspectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String = "General",
    val isActive: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val moduleConfigs: List<ModuleConfig> = emptyList(),
    val thresholds: InspectionThresholds = InspectionThresholds(),
    val captureSettings: CaptureSettingsConfig = CaptureSettingsConfig(),
    val aiSettings: AISettingsConfig = AISettingsConfig(),
    val reportSettings: ReportSettingsConfig = ReportSettingsConfig()
)

@Serializable
data class ModuleConfig(
    val moduleType: InspectionModuleType,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val settings: Map<String, String> = emptyMap(),
    val thresholds: Map<String, Float> = emptyMap(),
    val customModelPath: String? = null
)

@Serializable
data class InspectionThresholds(
    val minOverallScore: Float = 0.8f,
    val maxDefectsAllowed: Int = 0,
    val maxCriticalDefects: Int = 0,
    val maxMajorDefects: Int = 0,
    val maxMinorDefects: Int = 3,
    val dimensionalTolerance: Float = 0.05f,
    val colorDeltaE: Float = 2.0f
)

@Serializable
data class CaptureSettingsConfig(
    val preferredResolution: String = "1920x1080",
    val enableHDR: Boolean = false,
    val enableFlash: Boolean = false,
    val autoFocus: Boolean = true,
    val stabilization: Boolean = true,
    val multiCapture: Boolean = false,
    val captureCount: Int = 1,
    val captureDelay: Long = 0
)

@Serializable
data class AISettingsConfig(
    val enableLLMAnalysis: Boolean = true,
    val enableMultiAgent: Boolean = true,
    val preferredModel: String = "auto",
    val analysisDepth: AnalysisDepth = AnalysisDepth.STANDARD,
    val enableRootCauseAnalysis: Boolean = false,
    val enablePredictiveAnalysis: Boolean = false,
    val maxProcessingTimeMs: Long = 30000
)

@Serializable
enum class AnalysisDepth {
    QUICK,
    STANDARD,
    THOROUGH,
    COMPREHENSIVE
}

@Serializable
data class ReportSettingsConfig(
    val includeImages: Boolean = true,
    val includeAnnotations: Boolean = true,
    val includeMetrics: Boolean = true,
    val includeAIInsights: Boolean = true,
    val format: ReportFormat = ReportFormat.PDF,
    val autoExport: Boolean = false
)

@Serializable
enum class ReportFormat {
    PDF,
    CSV,
    JSON,
    XML,
    HTML
}
