package com.industrialvision.ai.agents.executors

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.industrialvision.ai.agents.core.AgentExecutor
import com.industrialvision.core.domain.models.*
import com.industrialvision.vision.ml.DefectDetectionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defect Detection Agent Executor
 *
 * Uses deep learning models to detect various types of defects:
 * - Surface defects (scratches, dents, corrosion)
 * - Structural defects (cracks, holes, deformations)
 * - Manufacturing defects (misalignment, missing components)
 * - Contamination (stains, foreign particles)
 */
@Singleton
class DefectDetectionExecutor @Inject constructor(
    private val defectModel: DefectDetectionModel
) : AgentExecutor {

    companion object {
        // Defect categories
        const val SCRATCH = "scratch"
        const val DENT = "dent"
        const val CRACK = "crack"
        const val CORROSION = "corrosion"
        const val STAIN = "stain"
        const val HOLE = "hole"
        const val MISALIGNMENT = "misalignment"
        const val MISSING_COMPONENT = "missing_component"
        const val FOREIGN_PARTICLE = "foreign_particle"
        const val DEFORMATION = "deformation"
        const val BURR = "burr"
        const val WELD_DEFECT = "weld_defect"
    }

    override suspend fun execute(task: AgentTask): TaskOutput = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        try {
            // Get image data
            val imageBytes = task.inputData.imageBytes
                ?: return@withContext TaskOutput(
                    confidence = 0f,
                    rawOutput = "No image data provided"
                )

            // Decode image
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return@withContext TaskOutput(
                    confidence = 0f,
                    rawOutput = "Failed to decode image"
                )

            // Apply region of interest if specified
            val processedBitmap = task.inputData.regionOfInterest?.let { roi ->
                cropToRegion(bitmap, roi)
            } ?: bitmap

            // Run defect detection model
            val detections = defectModel.detect(processedBitmap)

            // Convert to findings
            val findings = detections.map { detection ->
                Finding(
                    type = FindingType.DEFECT,
                    severity = mapDefectSeverity(detection.category, detection.confidence),
                    description = generateDescription(detection),
                    confidence = detection.confidence,
                    boundingBox = detection.boundingBox,
                    value = detection.category,
                    attributes = mapOf(
                        "defect_type" to detection.category,
                        "area_pixels" to calculateArea(detection.boundingBox).toString(),
                        "aspect_ratio" to calculateAspectRatio(detection.boundingBox).toString(),
                        "location" to describeLocation(detection.boundingBox, processedBitmap.width, processedBitmap.height)
                    )
                )
            }

            // Create annotations for visualization
            val annotations = detections.map { detection ->
                Annotation(
                    type = AnnotationType.BOUNDING_BOX,
                    boundingBox = detection.boundingBox,
                    label = "${detection.category} (${(detection.confidence * 100).toInt()}%)",
                    confidence = detection.confidence,
                    color = getColorForSeverity(
                        mapDefectSeverity(detection.category, detection.confidence)
                    )
                )
            }

            // Calculate metrics
            val metrics = calculateMetrics(detections, processedBitmap)

            // Overall confidence
            val overallConfidence = if (detections.isNotEmpty()) {
                detections.map { it.confidence }.average().toFloat()
            } else 1.0f // High confidence in "no defects found"

            val processingTime = System.currentTimeMillis() - startTime

            TaskOutput(
                findings = findings,
                metrics = metrics,
                confidence = overallConfidence,
                annotations = annotations,
                processingTimeMs = processingTime,
                rawOutput = buildRawOutput(detections, processingTime)
            )
        } catch (e: Exception) {
            TaskOutput(
                confidence = 0f,
                rawOutput = "Error: ${e.message}",
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    override fun getCapabilities(): List<AgentCapability> = listOf(
        AgentCapability.IMAGE_ANALYSIS,
        AgentCapability.DEFECT_DETECTION,
        AgentCapability.CLASSIFICATION,
        AgentCapability.SEGMENTATION,
        AgentCapability.REAL_TIME_PROCESSING
    )

    override fun isAvailable(): Boolean = defectModel.isLoaded()

    private fun cropToRegion(bitmap: Bitmap, roi: BoundingBox): Bitmap {
        val x = (roi.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (roi.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = (roi.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val height = (roi.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    private fun mapDefectSeverity(category: String, confidence: Float): FindingSeverity {
        // Severity based on defect type and confidence
        val baseSeverity = when (category.lowercase()) {
            CRACK, HOLE, MISSING_COMPONENT -> FindingSeverity.CRITICAL
            DEFORMATION, WELD_DEFECT, CORROSION -> FindingSeverity.MAJOR
            DENT, SCRATCH, BURR -> FindingSeverity.MINOR
            STAIN, FOREIGN_PARTICLE -> FindingSeverity.WARNING
            else -> FindingSeverity.INFO
        }

        // Adjust based on confidence
        return if (confidence < 0.6f && baseSeverity.ordinal > FindingSeverity.WARNING.ordinal) {
            FindingSeverity.entries[baseSeverity.ordinal + 1] // Lower severity for low confidence
        } else {
            baseSeverity
        }
    }

    private fun generateDescription(detection: DetectionResult): String {
        val sizeDescription = when {
            detection.boundingBox.width * detection.boundingBox.height < 0.01 -> "small"
            detection.boundingBox.width * detection.boundingBox.height < 0.05 -> "medium"
            else -> "large"
        }

        val locationDescription = describeLocation(
            detection.boundingBox,
            1000, // Normalized to 1000x1000
            1000
        )

        return "Detected $sizeDescription ${detection.category.replace("_", " ")} defect " +
                "at $locationDescription with ${(detection.confidence * 100).toInt()}% confidence"
    }

    private fun describeLocation(box: BoundingBox, imageWidth: Int, imageHeight: Int): String {
        val centerX = box.x + box.width / 2
        val centerY = box.y + box.height / 2

        val horizontal = when {
            centerX < 0.33 -> "left"
            centerX > 0.66 -> "right"
            else -> "center"
        }

        val vertical = when {
            centerY < 0.33 -> "top"
            centerY > 0.66 -> "bottom"
            else -> "middle"
        }

        return "$vertical-$horizontal"
    }

    private fun calculateArea(box: BoundingBox): Float {
        return box.width * box.height
    }

    private fun calculateAspectRatio(box: BoundingBox): Float {
        return if (box.height > 0) box.width / box.height else 0f
    }

    private fun calculateMetrics(detections: List<DetectionResult>, bitmap: Bitmap): Map<String, Float> {
        val totalArea = detections.sumOf {
            (it.boundingBox.width * it.boundingBox.height).toDouble()
        }.toFloat()

        val defectsByType = detections.groupBy { it.category }

        return mapOf(
            "defect_count" to detections.size.toFloat(),
            "total_defect_area" to totalArea,
            "defect_density" to (detections.size.toFloat() / (bitmap.width * bitmap.height / 1000000f)),
            "max_defect_area" to (detections.maxOfOrNull {
                it.boundingBox.width * it.boundingBox.height
            } ?: 0f),
            "avg_confidence" to (detections.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0f)
        ) + defectsByType.mapKeys { "count_${it.key}" }.mapValues { it.value.size.toFloat() }
    }

    private fun getColorForSeverity(severity: FindingSeverity): String {
        return when (severity) {
            FindingSeverity.CRITICAL -> "#FF0000" // Red
            FindingSeverity.MAJOR -> "#FF6600" // Orange
            FindingSeverity.MINOR -> "#FFCC00" // Yellow
            FindingSeverity.WARNING -> "#00CCFF" // Cyan
            FindingSeverity.INFO -> "#00FF00" // Green
            FindingSeverity.PASS -> "#00FF00" // Green
        }
    }

    private fun buildRawOutput(detections: List<DetectionResult>, processingTimeMs: Long): String {
        return buildString {
            appendLine("=== Defect Detection Results ===")
            appendLine("Processing time: ${processingTimeMs}ms")
            appendLine("Total detections: ${detections.size}")
            appendLine()

            if (detections.isEmpty()) {
                appendLine("No defects detected.")
            } else {
                detections.forEachIndexed { index, detection ->
                    appendLine("Detection ${index + 1}:")
                    appendLine("  Type: ${detection.category}")
                    appendLine("  Confidence: ${(detection.confidence * 100).toInt()}%")
                    appendLine("  Location: (${detection.boundingBox.x}, ${detection.boundingBox.y})")
                    appendLine("  Size: ${detection.boundingBox.width} x ${detection.boundingBox.height}")
                    appendLine()
                }
            }
        }
    }
}

/**
 * Detection result from the ML model
 */
data class DetectionResult(
    val category: String,
    val confidence: Float,
    val boundingBox: BoundingBox,
    val segmentationMask: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DetectionResult
        return category == other.category &&
                confidence == other.confidence &&
                boundingBox == other.boundingBox
    }

    override fun hashCode(): Int {
        var result = category.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + boundingBox.hashCode()
        return result
    }
}
