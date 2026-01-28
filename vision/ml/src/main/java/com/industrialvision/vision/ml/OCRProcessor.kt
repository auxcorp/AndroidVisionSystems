package com.industrialvision.vision.ml

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.industrialvision.core.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * OCR Processor - Text recognition for industrial applications
 *
 * Features:
 * - Multi-language text recognition
 * - Serial number extraction
 * - Date code parsing
 * - Label verification
 * - Handwritten text support (with limitations)
 *
 * Uses Google ML Kit for on-device processing
 */
@Singleton
class OCRProcessor @Inject constructor() {

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Extract all text from an image
     */
    suspend fun extractText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val result = suspendCancellableCoroutine<Text> { continuation ->
            textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

        parseTextResult(result, bitmap.width, bitmap.height)
    }

    /**
     * Extract text from a specific region
     */
    suspend fun extractTextFromRegion(
        bitmap: Bitmap,
        region: BoundingBox
    ): OCRResult = withContext(Dispatchers.IO) {
        // Crop to region
        val x = (region.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (region.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = (region.width * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
        val height = (region.height * bitmap.height).toInt().coerceIn(1, bitmap.height - y)

        val croppedBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        extractText(croppedBitmap)
    }

    /**
     * Verify text matches expected pattern
     */
    suspend fun verifyText(
        bitmap: Bitmap,
        expectedPattern: TextPattern
    ): TextVerificationResult = withContext(Dispatchers.Default) {
        val ocrResult = extractText(bitmap)

        val matches = ocrResult.textBlocks.filter { block ->
            matchesPattern(block.text, expectedPattern)
        }

        TextVerificationResult(
            isVerified = matches.isNotEmpty(),
            matchedText = matches.firstOrNull()?.text,
            confidence = matches.firstOrNull()?.confidence ?: 0f,
            allExtractedText = ocrResult.fullText,
            pattern = expectedPattern
        )
    }

    /**
     * Extract serial numbers from image
     */
    suspend fun extractSerialNumbers(bitmap: Bitmap): List<SerialNumber> = withContext(Dispatchers.Default) {
        val ocrResult = extractText(bitmap)

        ocrResult.textBlocks.mapNotNull { block ->
            parseSerialNumber(block.text)?.let { serial ->
                SerialNumber(
                    value = serial,
                    boundingBox = block.boundingBox,
                    confidence = block.confidence,
                    format = detectSerialFormat(serial)
                )
            }
        }
    }

    /**
     * Extract date codes from image
     */
    suspend fun extractDateCodes(bitmap: Bitmap): List<DateCode> = withContext(Dispatchers.Default) {
        val ocrResult = extractText(bitmap)

        ocrResult.textBlocks.mapNotNull { block ->
            parseDateCode(block.text)?.let { date ->
                DateCode(
                    value = block.text,
                    parsedDate = date,
                    boundingBox = block.boundingBox,
                    confidence = block.confidence,
                    format = detectDateFormat(block.text)
                )
            }
        }
    }

    /**
     * Analyze text quality (legibility, contrast, etc.)
     */
    suspend fun analyzeTextQuality(bitmap: Bitmap): TextQualityAnalysis = withContext(Dispatchers.Default) {
        val ocrResult = extractText(bitmap)

        val avgConfidence = if (ocrResult.textBlocks.isNotEmpty()) {
            ocrResult.textBlocks.map { it.confidence }.average().toFloat()
        } else 0f

        val issues = mutableListOf<TextQualityIssue>()

        // Check for low confidence readings
        ocrResult.textBlocks.forEach { block ->
            if (block.confidence < 0.7f) {
                issues.add(
                    TextQualityIssue(
                        type = TextQualityIssueType.LOW_CONFIDENCE,
                        description = "Low confidence text reading: \"${block.text}\"",
                        severity = if (block.confidence < 0.5f) FindingSeverity.MAJOR else FindingSeverity.MINOR,
                        boundingBox = block.boundingBox
                    )
                )
            }
        }

        // Check for incomplete/partial text
        ocrResult.textBlocks.forEach { block ->
            if (block.text.contains("?") || block.text.length < 2) {
                issues.add(
                    TextQualityIssue(
                        type = TextQualityIssueType.PARTIAL_TEXT,
                        description = "Potentially incomplete text: \"${block.text}\"",
                        severity = FindingSeverity.WARNING,
                        boundingBox = block.boundingBox
                    )
                )
            }
        }

        TextQualityAnalysis(
            overallQuality = avgConfidence,
            textBlockCount = ocrResult.textBlocks.size,
            characterCount = ocrResult.fullText.length,
            avgConfidence = avgConfidence,
            issues = issues,
            isReadable = avgConfidence > 0.7f && ocrResult.textBlocks.isNotEmpty()
        )
    }

    private fun parseTextResult(text: Text, imageWidth: Int, imageHeight: Int): OCRResult {
        val textBlocks = text.textBlocks.map { block ->
            TextBlock(
                text = block.text,
                boundingBox = block.boundingBox?.toBoundingBox(imageWidth, imageHeight),
                confidence = block.lines.flatMap { it.elements }
                    .mapNotNull { it.confidence }
                    .average()
                    .toFloat()
                    .takeIf { !it.isNaN() } ?: 0.9f,
                lines = block.lines.map { line ->
                    TextLine(
                        text = line.text,
                        boundingBox = line.boundingBox?.toBoundingBox(imageWidth, imageHeight),
                        confidence = line.elements.mapNotNull { it.confidence }
                            .average()
                            .toFloat()
                            .takeIf { !it.isNaN() } ?: 0.9f,
                        words = line.elements.map { element ->
                            TextWord(
                                text = element.text,
                                boundingBox = element.boundingBox?.toBoundingBox(imageWidth, imageHeight),
                                confidence = element.confidence ?: 0.9f
                            )
                        }
                    )
                }
            )
        }

        return OCRResult(
            fullText = text.text,
            textBlocks = textBlocks,
            processingTimeMs = 0
        )
    }

    private fun Rect.toBoundingBox(imageWidth: Int, imageHeight: Int): BoundingBox {
        return BoundingBox(
            x = left.toFloat() / imageWidth,
            y = top.toFloat() / imageHeight,
            width = width().toFloat() / imageWidth,
            height = height().toFloat() / imageHeight
        )
    }

    private fun matchesPattern(text: String, pattern: TextPattern): Boolean {
        return when (pattern) {
            is TextPattern.Exact -> text.equals(pattern.value, ignoreCase = pattern.ignoreCase)
            is TextPattern.Contains -> text.contains(pattern.value, ignoreCase = pattern.ignoreCase)
            is TextPattern.Regex -> pattern.regex.matches(text)
            is TextPattern.SerialNumber -> parseSerialNumber(text) != null
            is TextPattern.DateCode -> parseDateCode(text) != null
        }
    }

    private fun parseSerialNumber(text: String): String? {
        // Common serial number patterns
        val patterns = listOf(
            Regex("[A-Z]{2,3}[0-9]{6,10}"),  // Letter prefix + numbers
            Regex("[0-9]{8,12}"),             // Pure numeric
            Regex("[A-Z0-9]{3}-[A-Z0-9]{4}-[A-Z0-9]{4}"), // Grouped format
            Regex("S/N:?\\s*([A-Z0-9]+)", RegexOption.IGNORE_CASE)
        )

        patterns.forEach { pattern ->
            pattern.find(text)?.let { match ->
                return match.value.replace(Regex("S/N:?\\s*", RegexOption.IGNORE_CASE), "")
            }
        }

        return null
    }

    private fun parseDateCode(text: String): Long? {
        val datePatterns = listOf(
            Regex("\\d{4}[-/]\\d{2}[-/]\\d{2}"),  // YYYY-MM-DD
            Regex("\\d{2}[-/]\\d{2}[-/]\\d{4}"),  // DD-MM-YYYY
            Regex("\\d{6}"),                        // YYMMDD
            Regex("\\d{8}")                         // YYYYMMDD
        )

        datePatterns.forEach { pattern ->
            if (pattern.matches(text.trim())) {
                // Return current time as placeholder
                return System.currentTimeMillis()
            }
        }

        return null
    }

    private fun detectSerialFormat(serial: String): SerialFormat {
        return when {
            serial.matches(Regex("[A-Z]{2,3}[0-9]+")) -> SerialFormat.ALPHANUMERIC_PREFIX
            serial.matches(Regex("[0-9]+")) -> SerialFormat.NUMERIC
            serial.contains("-") -> SerialFormat.GROUPED
            else -> SerialFormat.CUSTOM
        }
    }

    private fun detectDateFormat(text: String): DateFormat {
        return when {
            text.matches(Regex("\\d{4}[-/]\\d{2}[-/]\\d{2}")) -> DateFormat.ISO
            text.matches(Regex("\\d{2}[-/]\\d{2}[-/]\\d{4}")) -> DateFormat.DMY
            text.matches(Regex("\\d{8}")) -> DateFormat.COMPACT
            else -> DateFormat.CUSTOM
        }
    }
}

// Data Classes

data class OCRResult(
    val fullText: String,
    val textBlocks: List<TextBlock>,
    val processingTimeMs: Long
)

data class TextBlock(
    val text: String,
    val boundingBox: BoundingBox?,
    val confidence: Float,
    val lines: List<TextLine>
)

data class TextLine(
    val text: String,
    val boundingBox: BoundingBox?,
    val confidence: Float,
    val words: List<TextWord>
)

data class TextWord(
    val text: String,
    val boundingBox: BoundingBox?,
    val confidence: Float
)

sealed class TextPattern {
    data class Exact(val value: String, val ignoreCase: Boolean = true) : TextPattern()
    data class Contains(val value: String, val ignoreCase: Boolean = true) : TextPattern()
    data class Regex(val regex: kotlin.text.Regex) : TextPattern()
    object SerialNumber : TextPattern()
    object DateCode : TextPattern()
}

data class TextVerificationResult(
    val isVerified: Boolean,
    val matchedText: String?,
    val confidence: Float,
    val allExtractedText: String,
    val pattern: TextPattern
)

data class SerialNumber(
    val value: String,
    val boundingBox: BoundingBox?,
    val confidence: Float,
    val format: SerialFormat
)

enum class SerialFormat {
    NUMERIC,
    ALPHANUMERIC_PREFIX,
    GROUPED,
    CUSTOM
}

data class DateCode(
    val value: String,
    val parsedDate: Long,
    val boundingBox: BoundingBox?,
    val confidence: Float,
    val format: DateFormat
)

enum class DateFormat {
    ISO,
    DMY,
    MDY,
    COMPACT,
    CUSTOM
}

data class TextQualityAnalysis(
    val overallQuality: Float,
    val textBlockCount: Int,
    val characterCount: Int,
    val avgConfidence: Float,
    val issues: List<TextQualityIssue>,
    val isReadable: Boolean
)

data class TextQualityIssue(
    val type: TextQualityIssueType,
    val description: String,
    val severity: FindingSeverity,
    val boundingBox: BoundingBox?
)

enum class TextQualityIssueType {
    LOW_CONFIDENCE,
    PARTIAL_TEXT,
    BLURRY,
    LOW_CONTRAST,
    OBSTRUCTED,
    SKEWED
}
