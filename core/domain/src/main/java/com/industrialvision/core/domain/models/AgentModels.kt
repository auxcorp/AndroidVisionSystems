package com.industrialvision.core.domain.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Multi-Agent AI System Models
 *
 * Defines the architecture for distributed AI analysis using
 * specialized agents that collaborate to provide comprehensive
 * industrial vision insights.
 */

/**
 * Represents an AI Agent in the multi-agent system
 */
@Serializable
data class VisionAgent(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AgentType,
    val description: String,
    val capabilities: List<AgentCapability>,
    val status: AgentStatus = AgentStatus.IDLE,
    val configuration: AgentConfiguration = AgentConfiguration(),
    val statistics: AgentStatistics = AgentStatistics(),
    val modelInfo: AgentModelInfo? = null
)

@Serializable
enum class AgentType {
    // Core Vision Agents
    DEFECT_DETECTOR,
    QUALITY_ASSESSOR,
    DIMENSIONAL_ANALYZER,
    SURFACE_INSPECTOR,
    COLOR_ANALYZER,

    // Recognition Agents
    OCR_READER,
    BARCODE_SCANNER,
    OBJECT_RECOGNIZER,
    PATTERN_MATCHER,

    // Analysis Agents
    ANOMALY_DETECTOR,
    TREND_ANALYZER,
    ROOT_CAUSE_ANALYZER,
    PREDICTIVE_ANALYZER,

    // Orchestration Agents
    COORDINATOR,
    VALIDATOR,
    REPORTER,

    // LLM-Powered Agents
    INSIGHT_GENERATOR,
    RECOMMENDATION_ENGINE,
    NATURAL_LANGUAGE_INTERFACE,

    // Custom Agent
    CUSTOM
}

@Serializable
enum class AgentCapability {
    IMAGE_ANALYSIS,
    VIDEO_ANALYSIS,
    REAL_TIME_PROCESSING,
    BATCH_PROCESSING,
    DEFECT_DETECTION,
    MEASUREMENT,
    CLASSIFICATION,
    SEGMENTATION,
    TRACKING,
    OCR,
    BARCODE_READING,
    COLOR_ANALYSIS,
    PATTERN_RECOGNITION,
    ANOMALY_DETECTION,
    STATISTICAL_ANALYSIS,
    TREND_ANALYSIS,
    ROOT_CAUSE_ANALYSIS,
    PREDICTIVE_ANALYSIS,
    NATURAL_LANGUAGE,
    REPORT_GENERATION,
    DECISION_MAKING,
    LEARNING,
    CALIBRATION
}

@Serializable
enum class AgentStatus {
    IDLE,
    INITIALIZING,
    READY,
    PROCESSING,
    WAITING,
    COMPLETED,
    ERROR,
    DISABLED,
    UPDATING
}

@Serializable
data class AgentConfiguration(
    val enabled: Boolean = true,
    val priority: Int = 5,
    val maxConcurrentTasks: Int = 1,
    val timeoutMs: Long = 10000,
    val retryCount: Int = 2,
    val confidenceThreshold: Float = 0.7f,
    val useGPU: Boolean = true,
    val batchSize: Int = 1,
    val customSettings: Map<String, String> = emptyMap()
)

@Serializable
data class AgentStatistics(
    val totalTasksProcessed: Long = 0,
    val successfulTasks: Long = 0,
    val failedTasks: Long = 0,
    val averageProcessingTimeMs: Long = 0,
    val averageConfidence: Float = 0f,
    val lastActiveTimestamp: Long = 0,
    val totalProcessingTimeMs: Long = 0
)

@Serializable
data class AgentModelInfo(
    val modelName: String,
    val modelVersion: String,
    val modelType: ModelType,
    val modelPath: String,
    val inputSize: String,
    val quantization: QuantizationType = QuantizationType.NONE,
    val delegate: ModelDelegate = ModelDelegate.CPU
)

@Serializable
enum class ModelType {
    TENSORFLOW_LITE,
    ONNX,
    PYTORCH_MOBILE,
    ML_KIT,
    CUSTOM,
    LLM_API
}

@Serializable
enum class QuantizationType {
    NONE,
    FLOAT16,
    INT8,
    DYNAMIC
}

@Serializable
enum class ModelDelegate {
    CPU,
    GPU,
    NNAPI,
    HEXAGON,
    EDGE_TPU
}

/**
 * Agent Task - Work unit for an agent
 */
@Serializable
data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val type: TaskType,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.PENDING,
    val inputData: TaskInput,
    val outputData: TaskOutput? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

@Serializable
enum class TaskType {
    ANALYZE_IMAGE,
    DETECT_DEFECTS,
    READ_TEXT,
    SCAN_BARCODE,
    MEASURE_DIMENSIONS,
    ANALYZE_COLOR,
    DETECT_OBJECTS,
    MATCH_PATTERN,
    GENERATE_INSIGHT,
    GENERATE_REPORT,
    VALIDATE_RESULT,
    COORDINATE_AGENTS,
    CUSTOM
}

@Serializable
enum class TaskPriority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW,
    BACKGROUND
}

@Serializable
enum class TaskStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMEOUT
}

@Serializable
data class TaskInput(
    val imageUri: String? = null,
    val imageBytes: ByteArray? = null,
    val regionOfInterest: BoundingBox? = null,
    val parameters: Map<String, String> = emptyMap(),
    val context: TaskContext? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TaskInput
        return imageUri == other.imageUri &&
                imageBytes.contentEquals(other.imageBytes) &&
                regionOfInterest == other.regionOfInterest &&
                parameters == other.parameters &&
                context == other.context
    }

    override fun hashCode(): Int {
        var result = imageUri?.hashCode() ?: 0
        result = 31 * result + (imageBytes?.contentHashCode() ?: 0)
        result = 31 * result + (regionOfInterest?.hashCode() ?: 0)
        result = 31 * result + parameters.hashCode()
        result = 31 * result + (context?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class TaskContext(
    val inspectionId: String? = null,
    val profileId: String? = null,
    val previousResults: List<String> = emptyList(),
    val relatedAgents: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TaskOutput(
    val findings: List<Finding> = emptyList(),
    val metrics: Map<String, Float> = emptyMap(),
    val confidence: Float = 0f,
    val rawOutput: String? = null,
    val annotations: List<Annotation> = emptyList(),
    val processingTimeMs: Long = 0
)

@Serializable
data class Annotation(
    val id: String = UUID.randomUUID().toString(),
    val type: AnnotationType,
    val boundingBox: BoundingBox? = null,
    val polygon: List<Point>? = null,
    val label: String,
    val confidence: Float,
    val color: String = "#FF0000",
    val attributes: Map<String, String> = emptyMap()
)

@Serializable
enum class AnnotationType {
    BOUNDING_BOX,
    POLYGON,
    POLYLINE,
    POINT,
    MASK,
    TEXT,
    MEASUREMENT
}

/**
 * Agent Communication Message
 */
@Serializable
data class AgentMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromAgentId: String,
    val toAgentId: String,
    val type: MessageType,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val payload: MessagePayload,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String? = null,
    val replyTo: String? = null
)

@Serializable
enum class MessageType {
    REQUEST,
    RESPONSE,
    NOTIFICATION,
    BROADCAST,
    ERROR,
    HEARTBEAT,
    SYNC
}

@Serializable
enum class MessagePriority {
    URGENT,
    HIGH,
    NORMAL,
    LOW
}

@Serializable
data class MessagePayload(
    val action: String,
    val data: Map<String, String> = emptyMap(),
    val results: List<Finding>? = null,
    val error: String? = null
)

/**
 * Agent Orchestration Plan
 */
@Serializable
data class OrchestrationPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val stages: List<OrchestrationStage>,
    val createdAt: Long = System.currentTimeMillis(),
    val estimatedDurationMs: Long = 0
)

@Serializable
data class OrchestrationStage(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val order: Int,
    val agents: List<String>,
    val parallel: Boolean = false,
    val condition: StageCondition? = null,
    val timeout: Long = 30000,
    val onFailure: FailureAction = FailureAction.STOP
)

@Serializable
data class StageCondition(
    val type: ConditionType,
    val parameter: String,
    val operator: ConditionOperator,
    val value: String
)

@Serializable
enum class ConditionType {
    PREVIOUS_STAGE_SUCCESS,
    FINDING_COUNT,
    CONFIDENCE_THRESHOLD,
    CUSTOM_METRIC,
    ALWAYS
}

@Serializable
enum class ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS,
    CONTAINS,
    NOT_CONTAINS
}

@Serializable
enum class FailureAction {
    STOP,
    CONTINUE,
    RETRY,
    SKIP,
    FALLBACK
}
