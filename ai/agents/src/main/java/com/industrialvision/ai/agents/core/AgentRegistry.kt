package com.industrialvision.ai.agents.core

import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent Registry - Central registry for all AI agents in the system
 *
 * Manages agent registration, lifecycle, and provides lookup services
 * for the orchestrator and other system components.
 */
@Singleton
class AgentRegistry @Inject constructor() {

    private val agents = ConcurrentHashMap<String, VisionAgent>()
    private val executors = ConcurrentHashMap<AgentType, AgentExecutor>()
    private val agentsByType = ConcurrentHashMap<AgentType, MutableList<String>>()

    private val _registeredAgents = MutableStateFlow<List<VisionAgent>>(emptyList())
    val registeredAgents: StateFlow<List<VisionAgent>> = _registeredAgents.asStateFlow()

    init {
        // Register default agents
        registerDefaultAgents()
    }

    /**
     * Register a new agent
     */
    fun registerAgent(agent: VisionAgent) {
        agents[agent.id] = agent
        agentsByType.getOrPut(agent.type) { mutableListOf() }.add(agent.id)
        updateAgentList()
    }

    /**
     * Register an executor for a specific agent type
     */
    fun registerExecutor(type: AgentType, executor: AgentExecutor) {
        executors[type] = executor
    }

    /**
     * Get an agent by ID
     */
    fun getAgent(id: String): VisionAgent? = agents[id]

    /**
     * Get an executor for a specific agent type
     */
    fun getExecutor(type: AgentType): AgentExecutor? = executors[type]

    /**
     * Get all agents of a specific type
     */
    fun getAgentsByType(type: AgentType): List<VisionAgent> {
        return agentsByType[type]?.mapNotNull { agents[it] } ?: emptyList()
    }

    /**
     * Get all registered agents
     */
    fun getAllAgents(): List<VisionAgent> = agents.values.toList()

    /**
     * Update agent status
     */
    fun updateAgentStatus(agentId: String, status: AgentStatus) {
        agents[agentId]?.let { agent ->
            agents[agentId] = agent.copy(status = status)
            updateAgentList()
        }
    }

    /**
     * Update agent statistics
     */
    fun updateAgentStatistics(agentId: String, update: (AgentStatistics) -> AgentStatistics) {
        agents[agentId]?.let { agent ->
            agents[agentId] = agent.copy(statistics = update(agent.statistics))
            updateAgentList()
        }
    }

    /**
     * Enable/disable an agent
     */
    fun setAgentEnabled(agentId: String, enabled: Boolean) {
        agents[agentId]?.let { agent ->
            agents[agentId] = agent.copy(
                configuration = agent.configuration.copy(enabled = enabled),
                status = if (enabled) AgentStatus.READY else AgentStatus.DISABLED
            )
            updateAgentList()
        }
    }

    /**
     * Unregister an agent
     */
    fun unregisterAgent(agentId: String) {
        agents[agentId]?.let { agent ->
            agentsByType[agent.type]?.remove(agentId)
            agents.remove(agentId)
            updateAgentList()
        }
    }

    private fun updateAgentList() {
        _registeredAgents.value = agents.values.toList()
    }

    /**
     * Register the default set of agents
     */
    private fun registerDefaultAgents() {
        // Defect Detection Agent
        registerAgent(
            VisionAgent(
                id = "defect_detector",
                name = "Defect Detection Agent",
                type = AgentType.DEFECT_DETECTOR,
                description = "Deep learning powered defect detection using custom CNN models",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.DEFECT_DETECTION,
                    AgentCapability.CLASSIFICATION,
                    AgentCapability.SEGMENTATION,
                    AgentCapability.REAL_TIME_PROCESSING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 10,
                    useGPU = true,
                    confidenceThreshold = 0.75f
                ),
                modelInfo = AgentModelInfo(
                    modelName = "DefectNet-v2",
                    modelVersion = "2.1.0",
                    modelType = ModelType.TENSORFLOW_LITE,
                    modelPath = "models/defect_detector.tflite",
                    inputSize = "640x640",
                    quantization = QuantizationType.FLOAT16,
                    delegate = ModelDelegate.GPU
                )
            )
        )

        // Quality Assessment Agent
        registerAgent(
            VisionAgent(
                id = "quality_assessor",
                name = "Quality Assessment Agent",
                type = AgentType.QUALITY_ASSESSOR,
                description = "Comprehensive quality scoring using multi-factor analysis",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.CLASSIFICATION,
                    AgentCapability.STATISTICAL_ANALYSIS
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 9,
                    confidenceThreshold = 0.8f
                )
            )
        )

        // OCR Reader Agent
        registerAgent(
            VisionAgent(
                id = "ocr_reader",
                name = "OCR Text Recognition Agent",
                type = AgentType.OCR_READER,
                description = "Advanced OCR with support for multiple languages and fonts",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.OCR,
                    AgentCapability.REAL_TIME_PROCESSING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 8,
                    confidenceThreshold = 0.9f
                ),
                modelInfo = AgentModelInfo(
                    modelName = "ML Kit Text Recognition",
                    modelVersion = "16.0.0",
                    modelType = ModelType.ML_KIT,
                    modelPath = "mlkit://text-recognition",
                    inputSize = "variable"
                )
            )
        )

        // Barcode Scanner Agent
        registerAgent(
            VisionAgent(
                id = "barcode_scanner",
                name = "Barcode & QR Scanner Agent",
                type = AgentType.BARCODE_SCANNER,
                description = "High-speed barcode and QR code detection and decoding",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.BARCODE_READING,
                    AgentCapability.REAL_TIME_PROCESSING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 8,
                    confidenceThreshold = 0.95f
                ),
                modelInfo = AgentModelInfo(
                    modelName = "ML Kit Barcode Scanning",
                    modelVersion = "17.0.1",
                    modelType = ModelType.ML_KIT,
                    modelPath = "mlkit://barcode-scanning",
                    inputSize = "variable"
                )
            )
        )

        // Dimensional Analyzer Agent
        registerAgent(
            VisionAgent(
                id = "dimensional_analyzer",
                name = "Dimensional Measurement Agent",
                type = AgentType.DIMENSIONAL_ANALYZER,
                description = "Precise dimensional measurements using computer vision",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.MEASUREMENT,
                    AgentCapability.CALIBRATION
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 7,
                    confidenceThreshold = 0.85f
                )
            )
        )

        // Color Analyzer Agent
        registerAgent(
            VisionAgent(
                id = "color_analyzer",
                name = "Color Analysis Agent",
                type = AgentType.COLOR_ANALYZER,
                description = "Color matching and analysis with Delta-E calculations",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.COLOR_ANALYSIS
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 6,
                    confidenceThreshold = 0.9f
                )
            )
        )

        // Surface Inspector Agent
        registerAgent(
            VisionAgent(
                id = "surface_inspector",
                name = "Surface Inspection Agent",
                type = AgentType.SURFACE_INSPECTOR,
                description = "Surface quality inspection for scratches, dents, and blemishes",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.DEFECT_DETECTION,
                    AgentCapability.SEGMENTATION
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 8,
                    useGPU = true,
                    confidenceThreshold = 0.7f
                )
            )
        )

        // Object Recognizer Agent
        registerAgent(
            VisionAgent(
                id = "object_recognizer",
                name = "Object Recognition Agent",
                type = AgentType.OBJECT_RECOGNIZER,
                description = "General object detection and recognition",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.CLASSIFICATION,
                    AgentCapability.REAL_TIME_PROCESSING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 6,
                    useGPU = true
                ),
                modelInfo = AgentModelInfo(
                    modelName = "ML Kit Object Detection",
                    modelVersion = "17.0.0",
                    modelType = ModelType.ML_KIT,
                    modelPath = "mlkit://object-detection",
                    inputSize = "variable"
                )
            )
        )

        // Pattern Matcher Agent
        registerAgent(
            VisionAgent(
                id = "pattern_matcher",
                name = "Pattern Matching Agent",
                type = AgentType.PATTERN_MATCHER,
                description = "Template and pattern matching for assembly verification",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.PATTERN_RECOGNITION
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 7,
                    confidenceThreshold = 0.85f
                )
            )
        )

        // Anomaly Detector Agent
        registerAgent(
            VisionAgent(
                id = "anomaly_detector",
                name = "Anomaly Detection Agent",
                type = AgentType.ANOMALY_DETECTOR,
                description = "Unsupervised anomaly detection using autoencoders",
                capabilities = listOf(
                    AgentCapability.IMAGE_ANALYSIS,
                    AgentCapability.ANOMALY_DETECTION
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 5,
                    useGPU = true
                ),
                modelInfo = AgentModelInfo(
                    modelName = "AnomalyNet",
                    modelVersion = "1.0.0",
                    modelType = ModelType.TENSORFLOW_LITE,
                    modelPath = "models/anomaly_detector.tflite",
                    inputSize = "224x224",
                    delegate = ModelDelegate.GPU
                )
            )
        )

        // Trend Analyzer Agent
        registerAgent(
            VisionAgent(
                id = "trend_analyzer",
                name = "Trend Analysis Agent",
                type = AgentType.TREND_ANALYZER,
                description = "Statistical analysis of inspection trends over time",
                capabilities = listOf(
                    AgentCapability.STATISTICAL_ANALYSIS,
                    AgentCapability.TREND_ANALYSIS
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 4
                )
            )
        )

        // Root Cause Analyzer Agent
        registerAgent(
            VisionAgent(
                id = "root_cause_analyzer",
                name = "Root Cause Analysis Agent",
                type = AgentType.ROOT_CAUSE_ANALYZER,
                description = "AI-powered root cause analysis for quality issues",
                capabilities = listOf(
                    AgentCapability.STATISTICAL_ANALYSIS,
                    AgentCapability.ROOT_CAUSE_ANALYSIS,
                    AgentCapability.NATURAL_LANGUAGE
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 3,
                    timeoutMs = 20000
                )
            )
        )

        // Insight Generator Agent (LLM-powered)
        registerAgent(
            VisionAgent(
                id = "insight_generator",
                name = "AI Insight Generator",
                type = AgentType.INSIGHT_GENERATOR,
                description = "LLM-powered intelligent insight generation",
                capabilities = listOf(
                    AgentCapability.NATURAL_LANGUAGE,
                    AgentCapability.DECISION_MAKING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 2,
                    timeoutMs = 30000
                ),
                modelInfo = AgentModelInfo(
                    modelName = "Claude/GPT-4",
                    modelVersion = "latest",
                    modelType = ModelType.LLM_API,
                    modelPath = "api://llm-provider",
                    inputSize = "text"
                )
            )
        )

        // Recommendation Engine Agent (LLM-powered)
        registerAgent(
            VisionAgent(
                id = "recommendation_engine",
                name = "Recommendation Engine",
                type = AgentType.RECOMMENDATION_ENGINE,
                description = "Generates actionable recommendations based on findings",
                capabilities = listOf(
                    AgentCapability.NATURAL_LANGUAGE,
                    AgentCapability.DECISION_MAKING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 2,
                    timeoutMs = 20000
                )
            )
        )

        // Coordinator Agent
        registerAgent(
            VisionAgent(
                id = "coordinator",
                name = "Agent Coordinator",
                type = AgentType.COORDINATOR,
                description = "Coordinates multi-agent workflows and manages dependencies",
                capabilities = listOf(
                    AgentCapability.DECISION_MAKING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 10
                )
            )
        )

        // Validator Agent
        registerAgent(
            VisionAgent(
                id = "validator",
                name = "Result Validator",
                type = AgentType.VALIDATOR,
                description = "Validates and cross-checks results from other agents",
                capabilities = listOf(
                    AgentCapability.DECISION_MAKING
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 1
                )
            )
        )

        // Reporter Agent
        registerAgent(
            VisionAgent(
                id = "reporter",
                name = "Report Generator",
                type = AgentType.REPORTER,
                description = "Generates comprehensive inspection reports",
                capabilities = listOf(
                    AgentCapability.REPORT_GENERATION,
                    AgentCapability.NATURAL_LANGUAGE
                ),
                status = AgentStatus.READY,
                configuration = AgentConfiguration(
                    priority = 1
                )
            )
        )
    }
}

/**
 * Interface for agent task execution
 */
interface AgentExecutor {
    suspend fun execute(task: AgentTask): TaskOutput
    fun getCapabilities(): List<AgentCapability>
    fun isAvailable(): Boolean
}
