package com.industrialvision.ai.agents.core

import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Agent Orchestrator - Central coordinator for the multi-agent AI system
 *
 * This orchestrator manages:
 * - Agent lifecycle (registration, activation, deactivation)
 * - Task distribution and load balancing
 * - Inter-agent communication
 * - Pipeline execution with parallel and sequential stages
 * - Result aggregation and conflict resolution
 * - Performance monitoring and optimization
 */
@Singleton
class AgentOrchestrator @Inject constructor(
    private val agentRegistry: AgentRegistry,
    private val messageRouter: MessageRouter,
    private val resultAggregator: ResultAggregator,
    private val performanceMonitor: PerformanceMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _orchestrationState = MutableStateFlow(OrchestrationState())
    val orchestrationState: StateFlow<OrchestrationState> = _orchestrationState.asStateFlow()

    private val taskQueue = Channel<AgentTask>(Channel.BUFFERED)
    private val activeExecutions = ConcurrentHashMap<String, Job>()

    init {
        startTaskProcessor()
        startHealthMonitor()
    }

    /**
     * Execute an inspection using the configured orchestration plan
     */
    suspend fun executeInspection(
        inspection: Inspection,
        profile: InspectionProfile,
        imageData: ByteArray
    ): InspectionResult {
        val executionId = inspection.id

        _orchestrationState.update {
            it.copy(
                isExecuting = true,
                currentExecutionId = executionId,
                currentStage = "Initializing"
            )
        }

        return try {
            // Build orchestration plan based on profile
            val plan = buildOrchestrationPlan(profile)

            // Initialize performance tracking
            performanceMonitor.startExecution(executionId)

            // Execute each stage
            val stageResults = mutableListOf<StageResult>()

            for (stage in plan.stages.sortedBy { it.order }) {
                _orchestrationState.update {
                    it.copy(
                        currentStage = stage.name,
                        stageProgress = 0f
                    )
                }

                // Check stage condition
                if (!evaluateCondition(stage.condition, stageResults)) {
                    continue
                }

                val stageResult = executeStage(
                    stage = stage,
                    imageData = imageData,
                    context = TaskContext(
                        inspectionId = inspection.id,
                        profileId = profile.id,
                        previousResults = stageResults.flatMap { it.findings.map { f -> f.id } }
                    )
                )

                stageResults.add(stageResult)

                // Handle stage failure
                if (stageResult.status == StageStatus.FAILED) {
                    when (stage.onFailure) {
                        FailureAction.STOP -> break
                        FailureAction.RETRY -> {
                            // Retry logic with exponential backoff
                            val retryResult = retryStage(stage, imageData, TaskContext(inspectionId = inspection.id))
                            if (retryResult.status == StageStatus.FAILED) break
                            stageResults[stageResults.lastIndex] = retryResult
                        }
                        FailureAction.CONTINUE -> continue
                        FailureAction.SKIP -> continue
                        FailureAction.FALLBACK -> {
                            // Use fallback agents if available
                            executeFallback(stage, imageData)
                        }
                    }
                }
            }

            // Aggregate all results
            val aggregatedResult = resultAggregator.aggregate(
                stageResults = stageResults,
                profile = profile
            )

            performanceMonitor.endExecution(executionId)

            _orchestrationState.update {
                it.copy(
                    isExecuting = false,
                    lastExecutionId = executionId,
                    currentStage = "Completed"
                )
            }

            aggregatedResult
        } catch (e: Exception) {
            performanceMonitor.recordError(executionId, e)
            _orchestrationState.update {
                it.copy(
                    isExecuting = false,
                    lastError = e.message
                )
            }
            throw e
        }
    }

    /**
     * Execute a single stage of the orchestration plan
     */
    private suspend fun executeStage(
        stage: OrchestrationStage,
        imageData: ByteArray,
        context: TaskContext
    ): StageResult = withContext(Dispatchers.Default) {
        val agents = stage.agents.mapNotNull { agentRegistry.getAgent(it) }

        if (agents.isEmpty()) {
            return@withContext StageResult(
                stageId = stage.id,
                stageName = stage.name,
                status = StageStatus.SKIPPED,
                findings = emptyList(),
                metrics = emptyMap()
            )
        }

        val startTime = System.currentTimeMillis()

        val taskResults = if (stage.parallel) {
            // Execute agents in parallel
            agents.map { agent ->
                async {
                    executeAgentTask(agent, imageData, context)
                }
            }.awaitAll()
        } else {
            // Execute agents sequentially
            agents.map { agent ->
                executeAgentTask(agent, imageData, context)
            }
        }

        // Merge results from all agents in the stage
        val allFindings = taskResults.flatMap { it.findings }
        val allMetrics = taskResults.fold(mutableMapOf<String, Float>()) { acc, result ->
            acc.apply { putAll(result.metrics) }
        }
        val avgConfidence = taskResults.map { it.confidence }.average().toFloat()

        val processingTime = System.currentTimeMillis() - startTime

        StageResult(
            stageId = stage.id,
            stageName = stage.name,
            status = if (taskResults.all { it.findings.isNotEmpty() || it.confidence > 0 })
                StageStatus.SUCCESS else StageStatus.PARTIAL,
            findings = allFindings,
            metrics = allMetrics,
            confidence = avgConfidence,
            processingTimeMs = processingTime,
            agentResults = taskResults
        )
    }

    /**
     * Execute a task on a specific agent
     */
    private suspend fun executeAgentTask(
        agent: VisionAgent,
        imageData: ByteArray,
        context: TaskContext
    ): TaskOutput {
        val task = AgentTask(
            agentId = agent.id,
            type = mapAgentTypeToTaskType(agent.type),
            inputData = TaskInput(
                imageBytes = imageData,
                context = context
            )
        )

        // Get the executor for this agent type
        val executor = agentRegistry.getExecutor(agent.type)
            ?: return TaskOutput(confidence = 0f)

        return try {
            withTimeout(agent.configuration.timeoutMs) {
                executor.execute(task)
            }
        } catch (e: TimeoutCancellationException) {
            TaskOutput(
                confidence = 0f,
                rawOutput = "Task timed out after ${agent.configuration.timeoutMs}ms"
            )
        } catch (e: Exception) {
            TaskOutput(
                confidence = 0f,
                rawOutput = "Error: ${e.message}"
            )
        }
    }

    /**
     * Build an orchestration plan based on the inspection profile
     */
    private fun buildOrchestrationPlan(profile: InspectionProfile): OrchestrationPlan {
        val stages = mutableListOf<OrchestrationStage>()
        var order = 0

        // Stage 1: Pre-processing and calibration
        stages.add(
            OrchestrationStage(
                name = "Pre-processing",
                order = order++,
                agents = listOf("preprocessor", "calibrator"),
                parallel = true
            )
        )

        // Stage 2: Primary detection based on enabled modules
        val primaryAgents = profile.moduleConfigs
            .filter { it.enabled }
            .sortedByDescending { it.priority }
            .take(4) // Limit parallel agents for performance
            .map { mapModuleToAgent(it.moduleType) }

        stages.add(
            OrchestrationStage(
                name = "Primary Analysis",
                order = order++,
                agents = primaryAgents,
                parallel = true,
                timeout = profile.aiSettings.maxProcessingTimeMs / 2
            )
        )

        // Stage 3: Secondary analysis (depends on primary results)
        if (profile.aiSettings.analysisDepth >= AnalysisDepth.STANDARD) {
            stages.add(
                OrchestrationStage(
                    name = "Secondary Analysis",
                    order = order++,
                    agents = listOf("anomaly_detector", "pattern_matcher"),
                    parallel = true,
                    condition = StageCondition(
                        type = ConditionType.PREVIOUS_STAGE_SUCCESS,
                        parameter = "Primary Analysis",
                        operator = ConditionOperator.EQUALS,
                        value = "true"
                    )
                )
            )
        }

        // Stage 4: AI Insight Generation
        if (profile.aiSettings.enableLLMAnalysis) {
            stages.add(
                OrchestrationStage(
                    name = "AI Insight Generation",
                    order = order++,
                    agents = listOf("insight_generator", "recommendation_engine"),
                    parallel = false
                )
            )
        }

        // Stage 5: Root Cause Analysis (if enabled and defects found)
        if (profile.aiSettings.enableRootCauseAnalysis) {
            stages.add(
                OrchestrationStage(
                    name = "Root Cause Analysis",
                    order = order++,
                    agents = listOf("root_cause_analyzer"),
                    parallel = false,
                    condition = StageCondition(
                        type = ConditionType.FINDING_COUNT,
                        parameter = "defects",
                        operator = ConditionOperator.GREATER_THAN,
                        value = "0"
                    )
                )
            )
        }

        // Stage 6: Validation and Report
        stages.add(
            OrchestrationStage(
                name = "Validation & Report",
                order = order++,
                agents = listOf("validator", "reporter"),
                parallel = false
            )
        )

        return OrchestrationPlan(
            name = "Inspection Plan for ${profile.name}",
            description = "Auto-generated plan based on profile configuration",
            stages = stages,
            estimatedDurationMs = profile.aiSettings.maxProcessingTimeMs
        )
    }

    private fun mapAgentTypeToTaskType(agentType: AgentType): TaskType {
        return when (agentType) {
            AgentType.DEFECT_DETECTOR -> TaskType.DETECT_DEFECTS
            AgentType.OCR_READER -> TaskType.READ_TEXT
            AgentType.BARCODE_SCANNER -> TaskType.SCAN_BARCODE
            AgentType.DIMENSIONAL_ANALYZER -> TaskType.MEASURE_DIMENSIONS
            AgentType.COLOR_ANALYZER -> TaskType.ANALYZE_COLOR
            AgentType.OBJECT_RECOGNIZER -> TaskType.DETECT_OBJECTS
            AgentType.PATTERN_MATCHER -> TaskType.MATCH_PATTERN
            AgentType.INSIGHT_GENERATOR -> TaskType.GENERATE_INSIGHT
            AgentType.REPORTER -> TaskType.GENERATE_REPORT
            AgentType.VALIDATOR -> TaskType.VALIDATE_RESULT
            AgentType.COORDINATOR -> TaskType.COORDINATE_AGENTS
            else -> TaskType.ANALYZE_IMAGE
        }
    }

    private fun mapModuleToAgent(moduleType: InspectionModuleType): String {
        return when (moduleType) {
            InspectionModuleType.DEFECT_DETECTION -> "defect_detector"
            InspectionModuleType.OCR_TEXT_RECOGNITION -> "ocr_reader"
            InspectionModuleType.BARCODE_SCANNING -> "barcode_scanner"
            InspectionModuleType.DIMENSIONAL_MEASUREMENT -> "dimensional_analyzer"
            InspectionModuleType.COLOR_ANALYSIS -> "color_analyzer"
            InspectionModuleType.SURFACE_INSPECTION -> "surface_inspector"
            InspectionModuleType.ASSEMBLY_VERIFICATION -> "assembly_verifier"
            InspectionModuleType.OBJECT_DETECTION -> "object_recognizer"
            InspectionModuleType.PATTERN_MATCHING -> "pattern_matcher"
            InspectionModuleType.EDGE_DETECTION -> "edge_detector"
            InspectionModuleType.CONTOUR_ANALYSIS -> "contour_analyzer"
            InspectionModuleType.HISTOGRAM_ANALYSIS -> "histogram_analyzer"
            InspectionModuleType.CUSTOM_MODEL -> "custom_model"
        }
    }

    private fun evaluateCondition(condition: StageCondition?, stageResults: List<StageResult>): Boolean {
        if (condition == null) return true

        return when (condition.type) {
            ConditionType.ALWAYS -> true
            ConditionType.PREVIOUS_STAGE_SUCCESS -> {
                stageResults.lastOrNull()?.status == StageStatus.SUCCESS
            }
            ConditionType.FINDING_COUNT -> {
                val count = stageResults.flatMap { it.findings }.size
                evaluateOperator(count.toFloat(), condition.operator, condition.value.toFloatOrNull() ?: 0f)
            }
            ConditionType.CONFIDENCE_THRESHOLD -> {
                val avgConfidence = stageResults.mapNotNull { it.confidence }.average().toFloat()
                evaluateOperator(avgConfidence, condition.operator, condition.value.toFloatOrNull() ?: 0f)
            }
            ConditionType.CUSTOM_METRIC -> {
                val metricValue = stageResults
                    .flatMap { it.metrics.entries }
                    .find { it.key == condition.parameter }
                    ?.value ?: 0f
                evaluateOperator(metricValue, condition.operator, condition.value.toFloatOrNull() ?: 0f)
            }
        }
    }

    private fun evaluateOperator(actual: Float, operator: ConditionOperator, expected: Float): Boolean {
        return when (operator) {
            ConditionOperator.EQUALS -> actual == expected
            ConditionOperator.NOT_EQUALS -> actual != expected
            ConditionOperator.GREATER_THAN -> actual > expected
            ConditionOperator.LESS_THAN -> actual < expected
            ConditionOperator.GREATER_THAN_OR_EQUALS -> actual >= expected
            ConditionOperator.LESS_THAN_OR_EQUALS -> actual <= expected
            else -> true
        }
    }

    private suspend fun retryStage(
        stage: OrchestrationStage,
        imageData: ByteArray,
        context: TaskContext
    ): StageResult {
        var attempt = 0
        var lastResult: StageResult? = null

        while (attempt < 3) {
            delay(1000L * (attempt + 1)) // Exponential backoff
            lastResult = executeStage(stage, imageData, context)
            if (lastResult.status == StageStatus.SUCCESS) {
                return lastResult
            }
            attempt++
        }

        return lastResult ?: StageResult(
            stageId = stage.id,
            stageName = stage.name,
            status = StageStatus.FAILED,
            findings = emptyList(),
            metrics = emptyMap()
        )
    }

    private suspend fun executeFallback(stage: OrchestrationStage, imageData: ByteArray) {
        // Fallback implementation - use simpler/faster agents
    }

    private fun startTaskProcessor() {
        scope.launch {
            for (task in taskQueue) {
                launch {
                    processTask(task)
                }
            }
        }
    }

    private suspend fun processTask(task: AgentTask) {
        val agent = agentRegistry.getAgent(task.agentId) ?: return
        val executor = agentRegistry.getExecutor(agent.type) ?: return

        try {
            executor.execute(task)
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun startHealthMonitor() {
        scope.launch {
            while (isActive) {
                delay(30000) // Check every 30 seconds
                checkAgentHealth()
            }
        }
    }

    private suspend fun checkAgentHealth() {
        agentRegistry.getAllAgents().forEach { agent ->
            // Send heartbeat and check response
            val message = AgentMessage(
                fromAgentId = "orchestrator",
                toAgentId = agent.id,
                type = MessageType.HEARTBEAT,
                payload = MessagePayload(action = "ping")
            )
            messageRouter.send(message)
        }
    }

    fun shutdown() {
        scope.cancel()
        taskQueue.close()
    }
}

/**
 * Orchestration state for UI observation
 */
data class OrchestrationState(
    val isExecuting: Boolean = false,
    val currentExecutionId: String? = null,
    val currentStage: String = "",
    val stageProgress: Float = 0f,
    val lastExecutionId: String? = null,
    val lastError: String? = null,
    val activeAgentCount: Int = 0,
    val queuedTaskCount: Int = 0
)

/**
 * Result from a single orchestration stage
 */
data class StageResult(
    val stageId: String,
    val stageName: String,
    val status: StageStatus,
    val findings: List<Finding>,
    val metrics: Map<String, Float>,
    val confidence: Float = 0f,
    val processingTimeMs: Long = 0,
    val agentResults: List<TaskOutput> = emptyList()
)

enum class StageStatus {
    SUCCESS,
    PARTIAL,
    FAILED,
    SKIPPED,
    TIMEOUT
}
