package com.industrialvision.systems.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.industrialvision.ai.agents.core.AgentOrchestrator
import com.industrialvision.ai.llm.LLMService
import com.industrialvision.core.data.repository.InspectionRepository
import com.industrialvision.core.domain.models.*
import com.industrialvision.vision.processing.VisionPipeline
import com.industrialvision.vision.processing.PipelineConfig
import com.industrialvision.vision.processing.PipelineProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Inspection ViewModel - Manages the complete inspection workflow
 *
 * Handles:
 * - Image capture and processing
 * - Multi-agent analysis coordination
 * - Result aggregation
 * - AI insights generation
 * - Inspection persistence
 */
@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val inspectionRepository: InspectionRepository,
    private val visionPipeline: VisionPipeline,
    private val agentOrchestrator: AgentOrchestrator,
    private val llmService: LLMService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InspectionUiState())
    val uiState: StateFlow<InspectionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<InspectionEvent>()
    val events: SharedFlow<InspectionEvent> = _events.asSharedFlow()

    val pipelineProgress: SharedFlow<PipelineProgress> = visionPipeline.progressFlow

    private var currentInspectionJob: Job? = null
    private var currentInspection: Inspection? = null

    init {
        // Check if we have an inspection ID from navigation
        savedStateHandle.get<String>("inspectionId")?.let { id ->
            loadExistingInspection(id)
        }

        // Load default profile
        loadDefaultProfile()

        // Observe pipeline state
        observePipelineState()
    }

    private fun loadExistingInspection(id: String) {
        viewModelScope.launch {
            try {
                val inspection = inspectionRepository.getInspectionById(id)
                if (inspection != null) {
                    currentInspection = inspection
                    _uiState.update {
                        it.copy(
                            currentInspection = inspection,
                            phase = InspectionPhase.COMPLETED
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load inspection $id")
            }
        }
    }

    private fun loadDefaultProfile() {
        viewModelScope.launch {
            try {
                val profile = inspectionRepository.getDefaultProfile()
                    ?: InspectionProfile(
                        id = "default",
                        name = "Default Inspection",
                        description = "Standard quality inspection profile",
                        moduleConfigs = listOf(
                            ModuleConfig(InspectionModuleType.DEFECT_DETECTION, enabled = true, priority = 1),
                            ModuleConfig(InspectionModuleType.OCR_TEXT_RECOGNITION, enabled = true, priority = 2),
                            ModuleConfig(InspectionModuleType.BARCODE_SCANNING, enabled = true, priority = 3),
                            ModuleConfig(InspectionModuleType.DIMENSIONAL_MEASUREMENT, enabled = false, priority = 4),
                            ModuleConfig(InspectionModuleType.COLOR_ANALYSIS, enabled = false, priority = 5)
                        )
                    )
                _uiState.update { it.copy(selectedProfile = profile) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load default profile")
            }
        }
    }

    private fun observePipelineState() {
        viewModelScope.launch {
            visionPipeline.pipelineState.collect { state ->
                _uiState.update {
                    it.copy(
                        isProcessing = state.isExecuting,
                        pipelineError = state.lastError
                    )
                }
            }
        }
    }

    fun selectProfile(profile: InspectionProfile) {
        _uiState.update { it.copy(selectedProfile = profile) }
    }

    /**
     * Start a new inspection with the captured image
     */
    fun startInspection(bitmap: Bitmap, imageUri: String?) {
        currentInspectionJob?.cancel()

        currentInspectionJob = viewModelScope.launch {
            val profile = _uiState.value.selectedProfile ?: return@launch
            val inspectionId = UUID.randomUUID().toString()

            _uiState.update {
                it.copy(
                    phase = InspectionPhase.PROCESSING,
                    isProcessing = true,
                    capturedImage = bitmap,
                    progress = 0f,
                    currentStage = "Initializing..."
                )
            }

            try {
                // Create inspection object
                val inspection = Inspection(
                    id = inspectionId,
                    profileId = profile.id,
                    profileName = profile.name,
                    imageUri = imageUri,
                    status = InspectionStatus.IN_PROGRESS
                )
                currentInspection = inspection

                // Stage 1: Vision Pipeline Processing
                _uiState.update { it.copy(currentStage = "Analyzing image...", progress = 0.1f) }

                val enabledModules = profile.moduleConfigs
                    .filter { it.enabled }
                    .map { it.moduleType }

                val pipelineConfig = PipelineConfig(
                    enabledModules = enabledModules,
                    parallelExecution = true
                )

                val pipelineResult = visionPipeline.execute(bitmap, pipelineConfig)

                if (!pipelineResult.success) {
                    throw Exception(pipelineResult.error ?: "Pipeline processing failed")
                }

                // Stage 2: Multi-Agent Analysis
                _uiState.update { it.copy(currentStage = "Running AI analysis...", progress = 0.5f) }

                val taskInput = TaskInput(
                    imageBytes = bitmapToBytes(bitmap),
                    inspectionId = inspectionId,
                    profileId = profile.id
                )

                val agentResults = agentOrchestrator.executeInspectionPipeline(
                    taskInput,
                    enabledModules.map { it.name }
                )

                // Stage 3: Aggregate Results
                _uiState.update { it.copy(currentStage = "Aggregating results...", progress = 0.7f) }

                val inspectionResults = agentResults.map { (agentId, output) ->
                    InspectionResult(
                        moduleType = getModuleTypeFromAgentId(agentId),
                        moduleName = agentId,
                        status = if (output.confidence > 0.5f) ResultStatus.SUCCESS else ResultStatus.PARTIAL,
                        confidence = output.confidence,
                        findings = output.findings,
                        metrics = output.metrics,
                        processingTimeMs = output.processingTimeMs
                    )
                }

                // Calculate overall score
                val allFindings = inspectionResults.flatMap { it.findings }
                val criticalCount = allFindings.count { it.severity == FindingSeverity.CRITICAL }
                val majorCount = allFindings.count { it.severity == FindingSeverity.MAJOR }
                val minorCount = allFindings.count { it.severity == FindingSeverity.MINOR }

                val overallScore = calculateOverallScore(criticalCount, majorCount, minorCount)
                val passFailStatus = determinePassFail(overallScore, profile.thresholds, criticalCount, majorCount)

                // Stage 4: Generate AI Insights (if enabled)
                var aiAnalysis: AIAnalysis? = null
                if (profile.aiSettings.enableLLMAnalysis && allFindings.isNotEmpty()) {
                    _uiState.update { it.copy(currentStage = "Generating insights...", progress = 0.85f) }

                    aiAnalysis = generateAIAnalysis(allFindings, inspectionResults)
                }

                // Stage 5: Finalize and Save
                _uiState.update { it.copy(currentStage = "Saving results...", progress = 0.95f) }

                val completedInspection = inspection.copy(
                    status = InspectionStatus.COMPLETED,
                    results = inspectionResults,
                    overallScore = overallScore,
                    passFailStatus = passFailStatus,
                    aiAnalysis = aiAnalysis
                )

                // Save to database
                inspectionRepository.saveInspection(completedInspection)
                currentInspection = completedInspection

                // Update UI
                _uiState.update {
                    it.copy(
                        phase = InspectionPhase.COMPLETED,
                        isProcessing = false,
                        currentInspection = completedInspection,
                        progress = 1f,
                        currentStage = "Complete"
                    )
                }

                _events.emit(InspectionEvent.InspectionComplete(completedInspection))

            } catch (e: Exception) {
                Timber.e(e, "Inspection failed")
                _uiState.update {
                    it.copy(
                        phase = InspectionPhase.ERROR,
                        isProcessing = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                _events.emit(InspectionEvent.Error(e.message ?: "Inspection failed"))
            }
        }
    }

    /**
     * Cancel the current inspection
     */
    fun cancelInspection() {
        currentInspectionJob?.cancel()
        _uiState.update {
            it.copy(
                phase = InspectionPhase.CAPTURE,
                isProcessing = false,
                capturedImage = null
            )
        }
    }

    /**
     * Retry failed inspection
     */
    fun retryInspection() {
        _uiState.value.capturedImage?.let { bitmap ->
            startInspection(bitmap, currentInspection?.imageUri)
        }
    }

    private fun calculateOverallScore(critical: Int, major: Int, minor: Int): Float {
        // Weighted penalty system
        val criticalPenalty = critical * 30f
        val majorPenalty = major * 15f
        val minorPenalty = minor * 5f

        val totalPenalty = criticalPenalty + majorPenalty + minorPenalty
        return (100f - totalPenalty).coerceIn(0f, 100f) / 100f
    }

    private fun determinePassFail(
        score: Float,
        thresholds: InspectionThresholds,
        critical: Int,
        major: Int
    ): PassFailStatus {
        return when {
            critical > thresholds.maxCriticalDefects -> PassFailStatus.FAIL
            major > thresholds.maxMajorDefects -> PassFailStatus.FAIL
            score < thresholds.minOverallScore -> PassFailStatus.FAIL
            score < thresholds.minOverallScore + 0.1f -> PassFailStatus.WARNING
            else -> PassFailStatus.PASS
        }
    }

    private suspend fun generateAIAnalysis(
        findings: List<Finding>,
        results: List<InspectionResult>
    ): AIAnalysis {
        return try {
            val findingsSummary = findings.groupBy { it.severity }.map { (severity, items) ->
                "${items.size} ${severity.name.lowercase()} findings"
            }.joinToString(", ")

            val prompt = """
                Analyze the following quality inspection results and provide insights:

                Findings Summary: $findingsSummary

                Detailed Findings:
                ${findings.take(10).joinToString("\n") { "- ${it.severity}: ${it.description}" }}

                Provide:
                1. A brief summary of the inspection
                2. Key insights about the quality issues
                3. Recommended actions
                4. If applicable, potential root causes
            """.trimIndent()

            val response = llmService.generateInsights(prompt)

            AIAnalysis(
                summary = response.summary,
                insights = response.insights.map { insight ->
                    AIInsight(
                        category = insight.category,
                        insight = insight.text,
                        importance = mapImportance(insight.importance),
                        actionable = insight.actionable,
                        suggestedAction = insight.suggestedAction
                    )
                },
                recommendations = response.recommendations,
                rootCauseAnalysis = response.rootCause,
                confidenceScore = response.confidence,
                processingTimeMs = response.processingTimeMs
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate AI analysis")
            AIAnalysis(
                summary = "AI analysis unavailable",
                insights = emptyList(),
                recommendations = emptyList(),
                confidenceScore = 0f
            )
        }
    }

    private fun mapImportance(importance: String): InsightImportance {
        return when (importance.lowercase()) {
            "critical" -> InsightImportance.CRITICAL
            "high" -> InsightImportance.HIGH
            "medium" -> InsightImportance.MEDIUM
            "low" -> InsightImportance.LOW
            else -> InsightImportance.INFORMATIONAL
        }
    }

    private fun getModuleTypeFromAgentId(agentId: String): InspectionModuleType {
        return when {
            agentId.contains("defect") -> InspectionModuleType.DEFECT_DETECTION
            agentId.contains("ocr") -> InspectionModuleType.OCR_TEXT_RECOGNITION
            agentId.contains("barcode") -> InspectionModuleType.BARCODE_SCANNING
            agentId.contains("dimension") -> InspectionModuleType.DIMENSIONAL_MEASUREMENT
            agentId.contains("color") -> InspectionModuleType.COLOR_ANALYSIS
            agentId.contains("surface") -> InspectionModuleType.SURFACE_INSPECTION
            else -> InspectionModuleType.CUSTOM_MODEL
        }
    }

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        return stream.toByteArray()
    }

    override fun onCleared() {
        super.onCleared()
        currentInspectionJob?.cancel()
    }
}

data class InspectionUiState(
    val phase: InspectionPhase = InspectionPhase.CAPTURE,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val currentStage: String = "",
    val capturedImage: Bitmap? = null,
    val currentInspection: Inspection? = null,
    val selectedProfile: InspectionProfile? = null,
    val error: String? = null,
    val pipelineError: String? = null
)

enum class InspectionPhase {
    CAPTURE,
    PREVIEW,
    PROCESSING,
    COMPLETED,
    ERROR
}

sealed class InspectionEvent {
    data class InspectionComplete(val inspection: Inspection) : InspectionEvent()
    data class Error(val message: String) : InspectionEvent()
    object CaptureRequested : InspectionEvent()
}
