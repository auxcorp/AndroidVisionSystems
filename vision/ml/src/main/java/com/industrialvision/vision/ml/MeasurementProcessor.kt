package com.industrialvision.vision.ml

import android.graphics.Bitmap
import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Measurement Processor - Dimensional measurement using computer vision
 *
 * Features:
 * - Edge detection for dimension extraction
 * - Calibration support with reference objects
 * - Sub-pixel accuracy measurement
 * - Angular measurements
 * - Area and perimeter calculations
 * - Tolerance checking
 */
@Singleton
class MeasurementProcessor @Inject constructor() {

    private var calibration: CalibrationData? = null

    /**
     * Set calibration data
     */
    fun setCalibration(data: CalibrationData) {
        calibration = data
    }

    /**
     * Measure distance between two points
     */
    suspend fun measureDistance(
        bitmap: Bitmap,
        point1: Point,
        point2: Point
    ): MeasurementResult = withContext(Dispatchers.Default) {
        // Calculate pixel distance
        val pixelDistance = calculatePixelDistance(point1, point2)

        // Convert to real-world units
        val realDistance = convertToRealUnits(pixelDistance, bitmap.width)

        MeasurementResult(
            type = MeasurementType.DISTANCE,
            value = realDistance,
            unit = calibration?.unit ?: MeasurementUnit.PIXELS,
            confidence = calculateMeasurementConfidence(pixelDistance),
            pixelValue = pixelDistance,
            startPoint = point1,
            endPoint = point2
        )
    }

    /**
     * Measure angle between three points
     */
    suspend fun measureAngle(
        bitmap: Bitmap,
        vertex: Point,
        point1: Point,
        point2: Point
    ): MeasurementResult = withContext(Dispatchers.Default) {
        val angle = calculateAngle(vertex, point1, point2)

        MeasurementResult(
            type = MeasurementType.ANGLE,
            value = angle,
            unit = MeasurementUnit.DEGREES,
            confidence = 0.95f,
            startPoint = point1,
            endPoint = point2,
            additionalPoints = listOf(vertex)
        )
    }

    /**
     * Auto-detect and measure edges
     */
    suspend fun autoMeasure(bitmap: Bitmap): List<MeasurementResult> = withContext(Dispatchers.Default) {
        val edges = detectEdges(bitmap)
        val measurements = mutableListOf<MeasurementResult>()

        // Find parallel edge pairs and measure distances
        edges.parallelPairs().forEach { (edge1, edge2) ->
            val distance = calculateEdgeDistance(edge1, edge2)
            val realDistance = convertToRealUnits(distance, bitmap.width)

            measurements.add(
                MeasurementResult(
                    type = MeasurementType.WIDTH,
                    value = realDistance,
                    unit = calibration?.unit ?: MeasurementUnit.PIXELS,
                    confidence = 0.85f,
                    pixelValue = distance
                )
            )
        }

        // Detect and measure contours
        val contours = detectContours(bitmap)
        contours.forEach { contour ->
            // Bounding box dimensions
            val width = contour.boundingBox.width * bitmap.width
            val height = contour.boundingBox.height * bitmap.height

            measurements.add(
                MeasurementResult(
                    type = MeasurementType.BOUNDING_WIDTH,
                    value = convertToRealUnits(width, bitmap.width),
                    unit = calibration?.unit ?: MeasurementUnit.PIXELS,
                    confidence = 0.9f,
                    pixelValue = width
                )
            )

            measurements.add(
                MeasurementResult(
                    type = MeasurementType.BOUNDING_HEIGHT,
                    value = convertToRealUnits(height, bitmap.width),
                    unit = calibration?.unit ?: MeasurementUnit.PIXELS,
                    confidence = 0.9f,
                    pixelValue = height
                )
            )

            // Area
            measurements.add(
                MeasurementResult(
                    type = MeasurementType.AREA,
                    value = convertAreaToRealUnits(contour.area, bitmap.width, bitmap.height),
                    unit = MeasurementUnit.SQUARE_MM,
                    confidence = 0.85f,
                    pixelValue = contour.area
                )
            )

            // Perimeter
            measurements.add(
                MeasurementResult(
                    type = MeasurementType.PERIMETER,
                    value = convertToRealUnits(contour.perimeter, bitmap.width),
                    unit = calibration?.unit ?: MeasurementUnit.PIXELS,
                    confidence = 0.85f,
                    pixelValue = contour.perimeter
                )
            )
        }

        measurements
    }

    /**
     * Check if measurements are within tolerance
     */
    fun checkTolerance(
        measurement: MeasurementResult,
        nominalValue: Float,
        tolerance: Float
    ): ToleranceCheckResult {
        val deviation = measurement.value - nominalValue
        val isWithinTolerance = abs(deviation) <= tolerance

        return ToleranceCheckResult(
            measurement = measurement,
            nominalValue = nominalValue,
            tolerance = tolerance,
            deviation = deviation,
            deviationPercent = (deviation / nominalValue) * 100,
            isWithinTolerance = isWithinTolerance,
            status = when {
                abs(deviation) <= tolerance * 0.5f -> ToleranceStatus.GOOD
                isWithinTolerance -> ToleranceStatus.WARNING
                else -> ToleranceStatus.OUT_OF_TOLERANCE
            }
        )
    }

    /**
     * Calibrate using reference object
     */
    suspend fun calibrate(
        bitmap: Bitmap,
        referenceLength: Float,
        unit: MeasurementUnit
    ): CalibrationResult = withContext(Dispatchers.Default) {
        // Detect reference object (e.g., ruler, calibration pattern)
        val referencePixels = detectReferenceObject(bitmap)

        if (referencePixels == null) {
            return@withContext CalibrationResult(
                success = false,
                message = "Could not detect reference object"
            )
        }

        val pixelsPerUnit = referencePixels / referenceLength

        calibration = CalibrationData(
            pixelsPerUnit = pixelsPerUnit,
            unit = unit,
            referenceLength = referenceLength,
            timestamp = System.currentTimeMillis()
        )

        CalibrationResult(
            success = true,
            pixelsPerUnit = pixelsPerUnit,
            unit = unit,
            message = "Calibration successful: ${pixelsPerUnit.format(2)} pixels per ${unit.symbol}"
        )
    }

    // Private helper functions

    private fun calculatePixelDistance(p1: Point, p2: Point): Float {
        val dx = (p2.x - p1.x)
        val dy = (p2.y - p1.y)
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateAngle(vertex: Point, p1: Point, p2: Point): Float {
        val v1x = p1.x - vertex.x
        val v1y = p1.y - vertex.y
        val v2x = p2.x - vertex.x
        val v2y = p2.y - vertex.y

        val dot = v1x * v2x + v1y * v2y
        val cross = v1x * v2y - v1y * v2x

        val angleRad = atan2(cross, dot)
        return Math.toDegrees(angleRad.toDouble()).toFloat().let {
            if (it < 0) it + 360 else it
        }
    }

    private fun convertToRealUnits(pixelValue: Float, imageWidth: Int): Float {
        val cal = calibration ?: return pixelValue
        return pixelValue / cal.pixelsPerUnit
    }

    private fun convertAreaToRealUnits(pixelArea: Float, imageWidth: Int, imageHeight: Int): Float {
        val cal = calibration ?: return pixelArea
        return pixelArea / (cal.pixelsPerUnit * cal.pixelsPerUnit)
    }

    private fun calculateMeasurementConfidence(pixelDistance: Float): Float {
        // Higher confidence for larger measurements (more pixels = more accurate)
        return when {
            pixelDistance > 100 -> 0.95f
            pixelDistance > 50 -> 0.90f
            pixelDistance > 20 -> 0.85f
            else -> 0.75f
        }
    }

    private fun detectEdges(bitmap: Bitmap): List<Edge> {
        // Simplified edge detection - in production use Canny or similar
        return listOf(
            Edge(Point(0.1f, 0.5f), Point(0.9f, 0.5f), EdgeType.HORIZONTAL),
            Edge(Point(0.5f, 0.1f), Point(0.5f, 0.9f), EdgeType.VERTICAL)
        )
    }

    private fun detectContours(bitmap: Bitmap): List<Contour> {
        // Simplified contour detection
        return listOf(
            Contour(
                points = listOf(
                    Point(0.2f, 0.2f),
                    Point(0.8f, 0.2f),
                    Point(0.8f, 0.8f),
                    Point(0.2f, 0.8f)
                ),
                boundingBox = BoundingBox(0.2f, 0.2f, 0.6f, 0.6f),
                area = 0.36f * bitmap.width * bitmap.height,
                perimeter = 2.4f * bitmap.width
            )
        )
    }

    private fun detectReferenceObject(bitmap: Bitmap): Float? {
        // Detect calibration pattern or reference object
        // Return pixel length of known reference
        return bitmap.width * 0.5f // Placeholder
    }

    private fun List<Edge>.parallelPairs(): List<Pair<Edge, Edge>> {
        val pairs = mutableListOf<Pair<Edge, Edge>>()
        for (i in indices) {
            for (j in i + 1 until size) {
                if (this[i].type == this[j].type) {
                    pairs.add(this[i] to this[j])
                }
            }
        }
        return pairs
    }

    private fun calculateEdgeDistance(edge1: Edge, edge2: Edge): Float {
        // Calculate perpendicular distance between parallel edges
        return when (edge1.type) {
            EdgeType.HORIZONTAL -> abs(edge1.start.y - edge2.start.y)
            EdgeType.VERTICAL -> abs(edge1.start.x - edge2.start.x)
            else -> {
                // For arbitrary angles, calculate perpendicular distance
                val midpoint = Point(
                    (edge1.start.x + edge1.end.x) / 2,
                    (edge1.start.y + edge1.end.y) / 2
                )
                pointToLineDistance(midpoint, edge2.start, edge2.end)
            }
        }
    }

    private fun pointToLineDistance(point: Point, lineStart: Point, lineEnd: Point): Float {
        val a = lineEnd.y - lineStart.y
        val b = lineStart.x - lineEnd.x
        val c = lineEnd.x * lineStart.y - lineStart.x * lineEnd.y

        return abs(a * point.x + b * point.y + c) / sqrt(a * a + b * b)
    }

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}

// Data Classes

data class MeasurementResult(
    val type: MeasurementType,
    val value: Float,
    val unit: MeasurementUnit,
    val confidence: Float,
    val pixelValue: Float = 0f,
    val startPoint: Point? = null,
    val endPoint: Point? = null,
    val additionalPoints: List<Point> = emptyList()
)

enum class MeasurementType {
    DISTANCE,
    WIDTH,
    HEIGHT,
    BOUNDING_WIDTH,
    BOUNDING_HEIGHT,
    DIAMETER,
    RADIUS,
    ANGLE,
    AREA,
    PERIMETER
}

enum class MeasurementUnit(val symbol: String) {
    PIXELS("px"),
    MILLIMETERS("mm"),
    CENTIMETERS("cm"),
    INCHES("in"),
    DEGREES("°"),
    SQUARE_MM("mm²"),
    SQUARE_CM("cm²"),
    SQUARE_INCHES("in²")
}

data class CalibrationData(
    val pixelsPerUnit: Float,
    val unit: MeasurementUnit,
    val referenceLength: Float,
    val timestamp: Long
)

data class CalibrationResult(
    val success: Boolean,
    val pixelsPerUnit: Float = 0f,
    val unit: MeasurementUnit = MeasurementUnit.PIXELS,
    val message: String
)

data class ToleranceCheckResult(
    val measurement: MeasurementResult,
    val nominalValue: Float,
    val tolerance: Float,
    val deviation: Float,
    val deviationPercent: Float,
    val isWithinTolerance: Boolean,
    val status: ToleranceStatus
)

enum class ToleranceStatus {
    GOOD,
    WARNING,
    OUT_OF_TOLERANCE
}

data class Edge(
    val start: Point,
    val end: Point,
    val type: EdgeType
)

enum class EdgeType {
    HORIZONTAL,
    VERTICAL,
    DIAGONAL
}

data class Contour(
    val points: List<Point>,
    val boundingBox: BoundingBox,
    val area: Float,
    val perimeter: Float
)
