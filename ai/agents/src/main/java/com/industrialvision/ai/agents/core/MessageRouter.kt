package com.industrialvision.ai.agents.core

import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Message Router - Handles inter-agent communication
 *
 * Provides:
 * - Point-to-point messaging between agents
 * - Broadcast messaging to all agents
 * - Topic-based pub/sub messaging
 * - Message queuing and delivery guarantees
 * - Message priority handling
 */
@Singleton
class MessageRouter @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Message channels for each agent
    private val agentChannels = ConcurrentHashMap<String, Channel<AgentMessage>>()

    // Topic subscriptions
    private val topicSubscriptions = ConcurrentHashMap<String, MutableSet<String>>()

    // Message history for debugging and replay
    private val messageHistory = MutableStateFlow<List<AgentMessage>>(emptyList())

    // Global message flow for monitoring
    private val _globalMessageFlow = MutableSharedFlow<AgentMessage>(replay = 100)
    val globalMessageFlow: SharedFlow<AgentMessage> = _globalMessageFlow.asSharedFlow()

    /**
     * Register an agent for messaging
     */
    fun registerAgent(agentId: String): Channel<AgentMessage> {
        val channel = Channel<AgentMessage>(Channel.BUFFERED)
        agentChannels[agentId] = channel
        return channel
    }

    /**
     * Unregister an agent
     */
    fun unregisterAgent(agentId: String) {
        agentChannels[agentId]?.close()
        agentChannels.remove(agentId)
        // Remove from all topic subscriptions
        topicSubscriptions.values.forEach { it.remove(agentId) }
    }

    /**
     * Send a message to a specific agent
     */
    suspend fun send(message: AgentMessage) {
        // Record message
        recordMessage(message)
        _globalMessageFlow.emit(message)

        // Handle message by type
        when (message.type) {
            MessageType.BROADCAST -> broadcast(message)
            else -> {
                // Point-to-point delivery
                agentChannels[message.toAgentId]?.send(message)
            }
        }
    }

    /**
     * Broadcast a message to all registered agents
     */
    private suspend fun broadcast(message: AgentMessage) {
        agentChannels.forEach { (agentId, channel) ->
            if (agentId != message.fromAgentId) {
                scope.launch {
                    channel.send(message.copy(toAgentId = agentId))
                }
            }
        }
    }

    /**
     * Subscribe an agent to a topic
     */
    fun subscribeToTopic(agentId: String, topic: String) {
        topicSubscriptions.getOrPut(topic) { mutableSetOf() }.add(agentId)
    }

    /**
     * Unsubscribe an agent from a topic
     */
    fun unsubscribeFromTopic(agentId: String, topic: String) {
        topicSubscriptions[topic]?.remove(agentId)
    }

    /**
     * Publish a message to a topic
     */
    suspend fun publishToTopic(topic: String, message: AgentMessage) {
        val subscribers = topicSubscriptions[topic] ?: return

        subscribers.forEach { agentId ->
            scope.launch {
                agentChannels[agentId]?.send(
                    message.copy(toAgentId = agentId)
                )
            }
        }
    }

    /**
     * Send a request and wait for response
     */
    suspend fun sendAndWait(
        message: AgentMessage,
        timeoutMs: Long = 10000
    ): AgentMessage? {
        val correlationId = message.id
        val responseChannel = Channel<AgentMessage>(1)

        // Register temporary response handler
        scope.launch {
            globalMessageFlow
                .filter { it.correlationId == correlationId }
                .first()
                .let { responseChannel.send(it) }
        }

        // Send the request
        send(message.copy(
            type = MessageType.REQUEST,
            correlationId = correlationId
        ))

        // Wait for response with timeout
        return kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            responseChannel.receive()
        }
    }

    /**
     * Get message flow for a specific agent
     */
    fun getAgentMessages(agentId: String): Flow<AgentMessage> {
        return globalMessageFlow.filter {
            it.toAgentId == agentId || it.type == MessageType.BROADCAST
        }
    }

    /**
     * Get all messages matching a filter
     */
    fun filterMessages(
        fromAgent: String? = null,
        toAgent: String? = null,
        type: MessageType? = null
    ): List<AgentMessage> {
        return messageHistory.value.filter { msg ->
            (fromAgent == null || msg.fromAgentId == fromAgent) &&
                    (toAgent == null || msg.toAgentId == toAgent) &&
                    (type == null || msg.type == type)
        }
    }

    private fun recordMessage(message: AgentMessage) {
        messageHistory.update { history ->
            (history + message).takeLast(1000) // Keep last 1000 messages
        }
    }

    /**
     * Clear message history
     */
    fun clearHistory() {
        messageHistory.value = emptyList()
    }
}

/**
 * Result Aggregator - Combines results from multiple agents
 */
@Singleton
class ResultAggregator @Inject constructor() {

    /**
     * Aggregate results from multiple stages into a single inspection result
     */
    fun aggregate(
        stageResults: List<StageResult>,
        profile: InspectionProfile
    ): InspectionResult {
        // Collect all findings
        val allFindings = stageResults.flatMap { it.findings }

        // Merge metrics
        val mergedMetrics = mutableMapOf<String, Float>()
        stageResults.forEach { stage ->
            stage.metrics.forEach { (key, value) ->
                mergedMetrics[key] = mergedMetrics.getOrDefault(key, 0f) + value
            }
        }

        // Calculate average confidence
        val avgConfidence = if (stageResults.isNotEmpty()) {
            stageResults.mapNotNull { it.confidence.takeIf { c -> c > 0 } }
                .average()
                .toFloat()
        } else 0f

        // Calculate total processing time
        val totalProcessingTime = stageResults.sumOf { it.processingTimeMs }

        // Determine overall status
        val status = determineResultStatus(stageResults)

        // Resolve conflicts in findings
        val resolvedFindings = resolveConflicts(allFindings)

        // Calculate defect counts
        val defectCounts = calculateDefectCounts(resolvedFindings)

        // Determine pass/fail based on thresholds
        val passFailStatus = evaluatePassFail(resolvedFindings, defectCounts, profile.thresholds)

        return InspectionResult(
            moduleType = InspectionModuleType.CUSTOM_MODEL, // Combined result
            moduleName = "Aggregated Analysis",
            status = status,
            confidence = avgConfidence,
            findings = resolvedFindings,
            metrics = mergedMetrics + defectCounts,
            processingTimeMs = totalProcessingTime
        )
    }

    private fun determineResultStatus(stageResults: List<StageResult>): ResultStatus {
        return when {
            stageResults.all { it.status == StageStatus.SUCCESS } -> ResultStatus.SUCCESS
            stageResults.any { it.status == StageStatus.FAILED } -> ResultStatus.FAILED
            stageResults.all { it.status == StageStatus.SKIPPED } -> ResultStatus.SKIPPED
            else -> ResultStatus.PARTIAL
        }
    }

    /**
     * Resolve conflicting findings from different agents
     */
    private fun resolveConflicts(findings: List<Finding>): List<Finding> {
        // Group findings by location (overlapping bounding boxes)
        val groupedByLocation = mutableListOf<MutableList<Finding>>()

        findings.forEach { finding ->
            val matchingGroup = groupedByLocation.find { group ->
                group.any { existing ->
                    hasSignificantOverlap(existing.boundingBox, finding.boundingBox)
                }
            }

            if (matchingGroup != null) {
                matchingGroup.add(finding)
            } else {
                groupedByLocation.add(mutableListOf(finding))
            }
        }

        // For each group, select the finding with highest confidence
        // or merge if they complement each other
        return groupedByLocation.map { group ->
            if (group.size == 1) {
                group.first()
            } else {
                // Merge findings from the same location
                mergeFindingGroup(group)
            }
        }
    }

    private fun hasSignificantOverlap(box1: BoundingBox?, box2: BoundingBox?): Boolean {
        if (box1 == null || box2 == null) return false

        val x1 = maxOf(box1.x, box2.x)
        val y1 = maxOf(box1.y, box2.y)
        val x2 = minOf(box1.x + box1.width, box2.x + box2.width)
        val y2 = minOf(box1.y + box1.height, box2.y + box2.height)

        if (x2 < x1 || y2 < y1) return false

        val intersectionArea = (x2 - x1) * (y2 - y1)
        val box1Area = box1.width * box1.height
        val box2Area = box2.width * box2.height
        val unionArea = box1Area + box2Area - intersectionArea

        val iou = intersectionArea / unionArea
        return iou > 0.5 // 50% IoU threshold
    }

    private fun mergeFindingGroup(group: List<Finding>): Finding {
        // Select the finding with highest confidence as base
        val baseFinding = group.maxByOrNull { it.confidence } ?: group.first()

        // Aggregate confidence (weighted average)
        val totalConfidence = group.sumOf { it.confidence.toDouble() }
        val weightedConfidence = (totalConfidence / group.size).toFloat()

        // Merge attributes
        val mergedAttributes = group.fold(mutableMapOf<String, String>()) { acc, finding ->
            acc.apply { putAll(finding.attributes) }
        }

        // Determine highest severity
        val highestSeverity = group.minByOrNull { it.severity.ordinal }?.severity
            ?: baseFinding.severity

        return baseFinding.copy(
            confidence = minOf(weightedConfidence * 1.1f, 1.0f), // Boost confidence slightly for consensus
            severity = highestSeverity,
            attributes = mergedAttributes + mapOf(
                "merged_from" to group.size.toString(),
                "consensus_confidence" to weightedConfidence.toString()
            )
        )
    }

    private fun calculateDefectCounts(findings: List<Finding>): Map<String, Float> {
        val defects = findings.filter { it.type == FindingType.DEFECT }
        return mapOf(
            "total_findings" to findings.size.toFloat(),
            "total_defects" to defects.size.toFloat(),
            "critical_defects" to defects.count { it.severity == FindingSeverity.CRITICAL }.toFloat(),
            "major_defects" to defects.count { it.severity == FindingSeverity.MAJOR }.toFloat(),
            "minor_defects" to defects.count { it.severity == FindingSeverity.MINOR }.toFloat(),
            "warnings" to defects.count { it.severity == FindingSeverity.WARNING }.toFloat()
        )
    }

    private fun evaluatePassFail(
        findings: List<Finding>,
        defectCounts: Map<String, Float>,
        thresholds: InspectionThresholds
    ): PassFailStatus {
        val criticalCount = defectCounts["critical_defects"]?.toInt() ?: 0
        val majorCount = defectCounts["major_defects"]?.toInt() ?: 0
        val minorCount = defectCounts["minor_defects"]?.toInt() ?: 0
        val totalDefects = defectCounts["total_defects"]?.toInt() ?: 0

        return when {
            criticalCount > thresholds.maxCriticalDefects -> PassFailStatus.FAIL
            majorCount > thresholds.maxMajorDefects -> PassFailStatus.FAIL
            totalDefects > thresholds.maxDefectsAllowed -> PassFailStatus.FAIL
            minorCount > thresholds.maxMinorDefects -> PassFailStatus.WARNING
            findings.any { it.severity == FindingSeverity.WARNING } -> PassFailStatus.WARNING
            else -> PassFailStatus.PASS
        }
    }
}

/**
 * Performance Monitor - Tracks agent and execution performance
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val executionMetrics = ConcurrentHashMap<String, ExecutionMetrics>()
    private val agentMetrics = ConcurrentHashMap<String, AgentPerformanceMetrics>()

    private val _metricsFlow = MutableStateFlow<PerformanceSnapshot>(PerformanceSnapshot())
    val metricsFlow: StateFlow<PerformanceSnapshot> = _metricsFlow.asStateFlow()

    fun startExecution(executionId: String) {
        executionMetrics[executionId] = ExecutionMetrics(
            startTime = System.currentTimeMillis()
        )
    }

    fun recordStageCompletion(executionId: String, stageName: String, durationMs: Long) {
        executionMetrics[executionId]?.let { metrics ->
            metrics.stageTimings[stageName] = durationMs
        }
    }

    fun recordAgentTask(agentId: String, durationMs: Long, success: Boolean) {
        val metrics = agentMetrics.getOrPut(agentId) { AgentPerformanceMetrics() }
        metrics.totalTasks++
        metrics.totalProcessingTime += durationMs
        if (success) metrics.successfulTasks++ else metrics.failedTasks++

        updateSnapshot()
    }

    fun endExecution(executionId: String) {
        executionMetrics[executionId]?.let { metrics ->
            metrics.endTime = System.currentTimeMillis()
            metrics.totalDuration = metrics.endTime - metrics.startTime
        }
        updateSnapshot()
    }

    fun recordError(executionId: String, error: Exception) {
        executionMetrics[executionId]?.let { metrics ->
            metrics.errors.add(error.message ?: "Unknown error")
        }
    }

    fun getExecutionMetrics(executionId: String): ExecutionMetrics? {
        return executionMetrics[executionId]
    }

    fun getAgentMetrics(agentId: String): AgentPerformanceMetrics? {
        return agentMetrics[agentId]
    }

    private fun updateSnapshot() {
        val totalExecutions = executionMetrics.size
        val avgExecutionTime = executionMetrics.values
            .filter { it.totalDuration > 0 }
            .map { it.totalDuration }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        val totalAgentTasks = agentMetrics.values.sumOf { it.totalTasks }
        val successRate = if (totalAgentTasks > 0) {
            agentMetrics.values.sumOf { it.successfulTasks }.toFloat() / totalAgentTasks
        } else 0f

        _metricsFlow.value = PerformanceSnapshot(
            totalExecutions = totalExecutions,
            averageExecutionTimeMs = avgExecutionTime.toLong(),
            totalAgentTasks = totalAgentTasks,
            agentSuccessRate = successRate
        )
    }
}

data class ExecutionMetrics(
    val startTime: Long,
    var endTime: Long = 0,
    var totalDuration: Long = 0,
    val stageTimings: MutableMap<String, Long> = mutableMapOf(),
    val errors: MutableList<String> = mutableListOf()
)

data class AgentPerformanceMetrics(
    var totalTasks: Long = 0,
    var successfulTasks: Long = 0,
    var failedTasks: Long = 0,
    var totalProcessingTime: Long = 0
) {
    val averageProcessingTime: Long
        get() = if (totalTasks > 0) totalProcessingTime / totalTasks else 0

    val successRate: Float
        get() = if (totalTasks > 0) successfulTasks.toFloat() / totalTasks else 0f
}

data class PerformanceSnapshot(
    val totalExecutions: Int = 0,
    val averageExecutionTimeMs: Long = 0,
    val totalAgentTasks: Long = 0,
    val agentSuccessRate: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
