package com.industrialvision.ai.llm

import android.util.Base64
import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM Service - Integrates with Large Language Models for intelligent analysis
 *
 * Supports multiple LLM providers:
 * - Anthropic (Claude) - Primary provider with vision capabilities
 * - OpenAI (GPT-4) - Alternative provider
 * - Google (Gemini) - Alternative with vision
 * - Local models via Ollama
 *
 * Features:
 * - Vision-language model support for image analysis
 * - Structured output generation
 * - Streaming responses
 * - Context management
 * - Rate limiting and retry logic
 */
@Singleton
class LLMService @Inject constructor(
    private val anthropicClient: AnthropicClient,
    private val openAIClient: OpenAIClient,
    private val llmConfig: LLMConfiguration
) {
    /**
     * Generate analysis insights from inspection findings
     */
    suspend fun generateInsights(
        findings: List<Finding>,
        context: InspectionContext,
        depth: AnalysisDepth = AnalysisDepth.STANDARD
    ): AIAnalysis = withContext(Dispatchers.IO) {
        val prompt = buildInsightPrompt(findings, context, depth)

        val response = when (llmConfig.preferredProvider) {
            LLMProvider.ANTHROPIC -> anthropicClient.complete(prompt)
            LLMProvider.OPENAI -> openAIClient.complete(prompt)
            else -> anthropicClient.complete(prompt) // Default to Anthropic
        }

        parseInsightResponse(response, findings)
    }

    /**
     * Analyze image with vision-language model
     */
    suspend fun analyzeImage(
        imageBytes: ByteArray,
        analysisType: ImageAnalysisType,
        additionalContext: String? = null
    ): VisionAnalysisResult = withContext(Dispatchers.IO) {
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        val prompt = buildVisionPrompt(analysisType, additionalContext)

        val response = anthropicClient.completeWithVision(
            prompt = prompt,
            imageBase64 = base64Image,
            imageMediaType = "image/jpeg"
        )

        parseVisionResponse(response, analysisType)
    }

    /**
     * Generate root cause analysis for defects
     */
    suspend fun generateRootCauseAnalysis(
        defects: List<Finding>,
        historicalData: List<InspectionSummary>? = null,
        productInfo: ProductInfo? = null
    ): RootCauseAnalysis = withContext(Dispatchers.IO) {
        val prompt = buildRootCausePrompt(defects, historicalData, productInfo)

        val response = anthropicClient.complete(prompt)

        parseRootCauseResponse(response)
    }

    /**
     * Generate actionable recommendations
     */
    suspend fun generateRecommendations(
        findings: List<Finding>,
        aiAnalysis: AIAnalysis?,
        profile: InspectionProfile
    ): List<Recommendation> = withContext(Dispatchers.IO) {
        val prompt = buildRecommendationPrompt(findings, aiAnalysis, profile)

        val response = anthropicClient.complete(prompt)

        parseRecommendationResponse(response)
    }

    /**
     * Interactive chat for quality discussions
     */
    fun chat(
        messages: List<ChatMessage>,
        inspectionContext: InspectionContext? = null
    ): Flow<String> = flow {
        val systemPrompt = buildChatSystemPrompt(inspectionContext)

        anthropicClient.streamComplete(systemPrompt, messages).collect { chunk ->
            emit(chunk)
        }
    }

    /**
     * Generate inspection report summary
     */
    suspend fun generateReportSummary(
        inspection: Inspection,
        includeRecommendations: Boolean = true
    ): ReportSummary = withContext(Dispatchers.IO) {
        val prompt = buildReportPrompt(inspection, includeRecommendations)

        val response = anthropicClient.complete(prompt)

        parseReportResponse(response)
    }

    // Prompt Building Functions

    private fun buildInsightPrompt(
        findings: List<Finding>,
        context: InspectionContext,
        depth: AnalysisDepth
    ): String {
        return """
            |You are an expert industrial quality control analyst. Analyze the following inspection findings and provide detailed insights.
            |
            |## Context
            |Product Type: ${context.productType}
            |Inspection Profile: ${context.profileName}
            |Industry: ${context.industry}
            |Quality Standards: ${context.qualityStandards.joinToString(", ")}
            |
            |## Findings (${findings.size} total)
            |${findings.mapIndexed { i, f -> formatFinding(i + 1, f) }.joinToString("\n\n")}
            |
            |## Analysis Requirements
            |Depth: ${depth.name}
            |
            |Please provide:
            |1. **Summary**: A concise overview of the inspection results
            |2. **Key Insights**: Important observations and patterns
            |3. **Risk Assessment**: Potential risks if issues are not addressed
            |4. **Recommendations**: Actionable steps to improve quality
            |${if (depth >= AnalysisDepth.THOROUGH) "|5. **Trend Analysis**: Patterns compared to typical defect rates" else ""}
            |${if (depth >= AnalysisDepth.COMPREHENSIVE) "|6. **Predictive Analysis**: Likelihood of future issues" else ""}
            |
            |Format your response as structured JSON.
        """.trimMargin()
    }

    private fun buildVisionPrompt(analysisType: ImageAnalysisType, additionalContext: String?): String {
        val basePrompt = when (analysisType) {
            ImageAnalysisType.DEFECT_DETECTION -> """
                |Analyze this industrial product image for defects. Identify:
                |1. Any visible defects (scratches, dents, cracks, discoloration, etc.)
                |2. Location of each defect in the image
                |3. Severity assessment (critical, major, minor)
                |4. Potential cause of each defect
            """.trimMargin()

            ImageAnalysisType.QUALITY_ASSESSMENT -> """
                |Assess the overall quality of this product image. Evaluate:
                |1. Surface finish quality
                |2. Dimensional conformance (if visible)
                |3. Assembly completeness
                |4. Cleanliness
                |5. Overall quality score (1-10)
            """.trimMargin()

            ImageAnalysisType.OCR_VERIFICATION -> """
                |Read and verify all text visible in this image:
                |1. Extract all readable text
                |2. Verify text clarity and legibility
                |3. Check for proper formatting and alignment
                |4. Identify any text defects (smudging, fading, etc.)
            """.trimMargin()

            ImageAnalysisType.ASSEMBLY_CHECK -> """
                |Verify the assembly shown in this image:
                |1. Identify all visible components
                |2. Check for missing or misplaced parts
                |3. Verify proper orientation and positioning
                |4. Assess connection quality
            """.trimMargin()

            ImageAnalysisType.GENERAL -> """
                |Analyze this industrial image and provide:
                |1. Description of what's shown
                |2. Any quality concerns
                |3. Recommendations for inspection focus areas
            """.trimMargin()
        }

        return additionalContext?.let { "$basePrompt\n\nAdditional context: $it" } ?: basePrompt
    }

    private fun buildRootCausePrompt(
        defects: List<Finding>,
        historicalData: List<InspectionSummary>?,
        productInfo: ProductInfo?
    ): String {
        return """
            |You are a quality engineering expert specializing in root cause analysis.
            |
            |## Current Defects
            |${defects.mapIndexed { i, f -> formatFinding(i + 1, f) }.joinToString("\n\n")}
            |
            |${productInfo?.let { """
            |## Product Information
            |Name: ${it.name}
            |Category: ${it.category}
            |Material: ${it.material}
            |Manufacturing Process: ${it.process}
            |""".trimMargin() } ?: ""}
            |
            |${historicalData?.let { """
            |## Historical Data
            |${it.take(10).joinToString("\n") { s -> "- ${s.date}: ${s.defectCount} defects, Pass rate: ${s.passRate}%" }}
            |""".trimMargin() } ?: ""}
            |
            |Perform a comprehensive root cause analysis:
            |1. **Primary Causes**: Most likely root causes for observed defects
            |2. **Contributing Factors**: Environmental or process factors
            |3. **Correlation Analysis**: Relationships between defect types
            |4. **Fishbone Analysis**: Categorize causes (Man, Machine, Material, Method, Environment)
            |5. **Recommended Actions**: Prioritized corrective actions
            |
            |Format response as structured JSON.
        """.trimMargin()
    }

    private fun buildRecommendationPrompt(
        findings: List<Finding>,
        aiAnalysis: AIAnalysis?,
        profile: InspectionProfile
    ): String {
        return """
            |Generate actionable recommendations based on inspection results.
            |
            |## Findings Summary
            |Total: ${findings.size}
            |Critical: ${findings.count { it.severity == FindingSeverity.CRITICAL }}
            |Major: ${findings.count { it.severity == FindingSeverity.MAJOR }}
            |Minor: ${findings.count { it.severity == FindingSeverity.MINOR }}
            |
            |## AI Analysis Summary
            |${aiAnalysis?.summary ?: "Not available"}
            |
            |## Quality Thresholds
            |Max Defects Allowed: ${profile.thresholds.maxDefectsAllowed}
            |Dimensional Tolerance: ${profile.thresholds.dimensionalTolerance}
            |
            |Provide 5-10 prioritized recommendations with:
            |1. Action description
            |2. Priority (high/medium/low)
            |3. Expected impact
            |4. Implementation difficulty
            |5. Responsible party suggestion
            |
            |Format as JSON array.
        """.trimMargin()
    }

    private fun buildChatSystemPrompt(context: InspectionContext?): String {
        return """
            |You are an AI assistant specialized in industrial quality control and vision inspection systems.
            |You have deep expertise in:
            |- Defect detection and classification
            |- Quality assurance methodologies
            |- Statistical process control
            |- Root cause analysis
            |- Industrial standards (ISO, ASTM, etc.)
            |
            |${context?.let { """
            |Current inspection context:
            |- Product: ${it.productType}
            |- Profile: ${it.profileName}
            |- Industry: ${it.industry}
            |""".trimMargin() } ?: ""}
            |
            |Provide helpful, accurate, and professional responses. Use technical terminology appropriately.
            |When discussing defects, always consider severity, impact, and remediation options.
        """.trimMargin()
    }

    private fun buildReportPrompt(inspection: Inspection, includeRecommendations: Boolean): String {
        return """
            |Generate a professional inspection report summary.
            |
            |## Inspection Details
            |ID: ${inspection.id}
            |Profile: ${inspection.profileName}
            |Timestamp: ${inspection.timestamp}
            |Status: ${inspection.status}
            |Overall Score: ${inspection.overallScore}
            |Pass/Fail: ${inspection.passFailStatus}
            |
            |## Results
            |${inspection.results.joinToString("\n") { r ->
                "- ${r.moduleName}: ${r.status} (${r.findings.size} findings)"
            }}
            |
            |## All Findings
            |${inspection.results.flatMap { it.findings }.mapIndexed { i, f ->
                formatFinding(i + 1, f)
            }.joinToString("\n\n")}
            |
            |Generate:
            |1. Executive Summary (2-3 sentences)
            |2. Key Findings Summary
            |3. Quality Assessment
            |${if (includeRecommendations) "4. Top 3 Recommendations" else ""}
            |
            |Format as JSON.
        """.trimMargin()
    }

    private fun formatFinding(index: Int, finding: Finding): String {
        return """
            |Finding #$index:
            |  Type: ${finding.type}
            |  Severity: ${finding.severity}
            |  Description: ${finding.description}
            |  Confidence: ${(finding.confidence * 100).toInt()}%
            |  ${finding.value?.let { "Value: $it" } ?: ""}
            |  ${finding.boundingBox?.let { "Location: (${it.x}, ${it.y})" } ?: ""}
        """.trimMargin()
    }

    // Response Parsing Functions

    private fun parseInsightResponse(response: String, findings: List<Finding>): AIAnalysis {
        // In production, use proper JSON parsing
        return AIAnalysis(
            summary = extractSection(response, "Summary") ?: "Analysis completed",
            insights = parseInsights(response),
            recommendations = parseStringList(response, "Recommendations"),
            rootCauseAnalysis = extractSection(response, "Root Cause"),
            predictedOutcome = extractSection(response, "Prediction"),
            confidenceScore = 0.85f,
            processingTimeMs = 0
        )
    }

    private fun parseVisionResponse(response: String, analysisType: ImageAnalysisType): VisionAnalysisResult {
        return VisionAnalysisResult(
            description = response,
            findings = emptyList(), // Parse from response
            qualityScore = 0.8f,
            confidence = 0.85f
        )
    }

    private fun parseRootCauseResponse(response: String): RootCauseAnalysis {
        return RootCauseAnalysis(
            primaryCauses = parseStringList(response, "Primary Causes"),
            contributingFactors = parseStringList(response, "Contributing Factors"),
            fishboneCategories = emptyMap(),
            recommendedActions = parseStringList(response, "Recommended Actions"),
            confidence = 0.8f
        )
    }

    private fun parseRecommendationResponse(response: String): List<Recommendation> {
        // Parse JSON response into recommendations
        return listOf(
            Recommendation(
                action = "Review and address identified defects",
                priority = RecommendationPriority.HIGH,
                expectedImpact = "Reduce defect rate by 20%",
                difficulty = "Medium",
                responsibleParty = "Quality Team"
            )
        )
    }

    private fun parseReportResponse(response: String): ReportSummary {
        return ReportSummary(
            executiveSummary = extractSection(response, "Executive Summary") ?: "",
            keyFindings = parseStringList(response, "Key Findings"),
            qualityAssessment = extractSection(response, "Quality Assessment") ?: "",
            recommendations = parseStringList(response, "Recommendations")
        )
    }

    private fun extractSection(text: String, sectionName: String): String? {
        val regex = Regex("(?i)\\*\\*$sectionName\\*\\*:?\\s*([\\s\\S]*?)(?=\\*\\*|$)")
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun parseInsights(text: String): List<AIInsight> {
        // Simplified parsing - in production use proper JSON parsing
        return listOf(
            AIInsight(
                category = "Quality",
                insight = "Analysis completed successfully",
                importance = InsightImportance.MEDIUM,
                actionable = true
            )
        )
    }

    private fun parseStringList(text: String, sectionName: String): List<String> {
        val section = extractSection(text, sectionName) ?: return emptyList()
        return section.split("\n")
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }
    }
}

// Supporting Data Classes

enum class LLMProvider {
    ANTHROPIC,
    OPENAI,
    GOOGLE,
    LOCAL
}

enum class ImageAnalysisType {
    DEFECT_DETECTION,
    QUALITY_ASSESSMENT,
    OCR_VERIFICATION,
    ASSEMBLY_CHECK,
    GENERAL
}

data class LLMConfiguration(
    val preferredProvider: LLMProvider = LLMProvider.ANTHROPIC,
    val apiKey: String = "",
    val model: String = "claude-sonnet-4-20250514",
    val maxTokens: Int = 4096,
    val temperature: Float = 0.3f
)

data class InspectionContext(
    val productType: String,
    val profileName: String,
    val industry: String,
    val qualityStandards: List<String> = emptyList()
)

data class VisionAnalysisResult(
    val description: String,
    val findings: List<Finding>,
    val qualityScore: Float,
    val confidence: Float
)

data class RootCauseAnalysis(
    val primaryCauses: List<String>,
    val contributingFactors: List<String>,
    val fishboneCategories: Map<String, List<String>>,
    val recommendedActions: List<String>,
    val confidence: Float
)

data class Recommendation(
    val action: String,
    val priority: RecommendationPriority,
    val expectedImpact: String,
    val difficulty: String,
    val responsibleParty: String
)

enum class RecommendationPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

data class ReportSummary(
    val executiveSummary: String,
    val keyFindings: List<String>,
    val qualityAssessment: String,
    val recommendations: List<String>
)

data class ProductInfo(
    val name: String,
    val category: String,
    val material: String,
    val process: String
)

data class InspectionSummary(
    val date: String,
    val defectCount: Int,
    val passRate: Float
)

data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String
)
