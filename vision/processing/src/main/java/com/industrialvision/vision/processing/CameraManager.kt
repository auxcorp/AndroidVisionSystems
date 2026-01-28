package com.industrialvision.vision.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Camera Manager - Professional camera control for industrial vision
 *
 * Features:
 * - High-resolution image capture
 * - Real-time frame analysis
 * - Focus and exposure control
 * - HDR support
 * - Zoom and stabilization
 * - Multiple camera support
 */
@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    private val _frameFlow = MutableSharedFlow<FrameData>(replay = 0, extraBufferCapacity = 1)
    val frameFlow: SharedFlow<FrameData> = _frameFlow.asSharedFlow()

    private var frameProcessor: FrameProcessor? = null

    /**
     * Initialize camera system
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        try {
            cameraProvider = suspendCancellableCoroutine { continuation ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener({
                    try {
                        continuation.resume(future.get())
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            _cameraState.update { it.copy(isInitialized = true) }
            true
        } catch (e: Exception) {
            _cameraState.update { it.copy(error = e.message) }
            false
        }
    }

    /**
     * Start camera preview
     */
    suspend fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        settings: CameraSettings = CameraSettings()
    ) = withContext(Dispatchers.Main) {
        val provider = cameraProvider ?: throw IllegalStateException("Camera not initialized")

        try {
            // Unbind previous use cases
            provider.unbindAll()

            // Build camera selector
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(
                    if (settings.useFrontCamera) CameraSelector.LENS_FACING_FRONT
                    else CameraSelector.LENS_FACING_BACK
                )
                .build()

            // Build preview
            preview = Preview.Builder()
                .setTargetResolution(settings.previewResolution)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            // Build image capture
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(settings.captureResolution)
                .setCaptureMode(
                    if (settings.highQualityCapture) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                    else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .setFlashMode(
                    when (settings.flashMode) {
                        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    }
                )
                .build()

            // Build image analysis
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(settings.analysisResolution)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            // Bind use cases
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            // Configure camera controls
            camera?.let { cam ->
                cam.cameraControl.enableTorch(settings.torchEnabled)

                if (settings.autoFocus) {
                    val factory = previewView.meteringPointFactory
                    val point = factory.createPoint(
                        previewView.width / 2f,
                        previewView.height / 2f
                    )
                    cam.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(point).build()
                    )
                }
            }

            _cameraState.update {
                it.copy(
                    isRunning = true,
                    currentSettings = settings
                )
            }
        } catch (e: Exception) {
            _cameraState.update { it.copy(error = e.message) }
            throw e
        }
    }

    /**
     * Capture high-resolution image
     */
    suspend fun captureImage(): CapturedImage = withContext(Dispatchers.IO) {
        val capture = imageCapture ?: throw IllegalStateException("Image capture not initialized")

        suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        try {
                            val bitmap = image.toBitmap()
                            val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                            val bytes = bitmapToBytes(rotatedBitmap)

                            image.close()

                            continuation.resume(
                                CapturedImage(
                                    bitmap = rotatedBitmap,
                                    bytes = bytes,
                                    width = rotatedBitmap.width,
                                    height = rotatedBitmap.height,
                                    timestamp = System.currentTimeMillis(),
                                    metadata = extractMetadata(image)
                                )
                            )
                        } catch (e: Exception) {
                            image.close()
                            continuation.resumeWithException(e)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                }
            )
        }
    }

    /**
     * Set frame processor for real-time analysis
     */
    fun setFrameProcessor(processor: FrameProcessor) {
        frameProcessor = processor
    }

    /**
     * Focus on specific point
     */
    fun focusOnPoint(x: Float, y: Float, viewWidth: Int, viewHeight: Int) {
        camera?.let { cam ->
            val factory = SurfaceOrientedMeteringPointFactory(
                viewWidth.toFloat(),
                viewHeight.toFloat()
            )
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        }
    }

    /**
     * Set zoom level
     */
    fun setZoom(zoomRatio: Float) {
        camera?.cameraControl?.setZoomRatio(zoomRatio.coerceIn(1f, getMaxZoom()))
    }

    /**
     * Get max zoom level
     */
    fun getMaxZoom(): Float {
        return camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
    }

    /**
     * Toggle torch
     */
    fun setTorch(enabled: Boolean) {
        camera?.cameraControl?.enableTorch(enabled)
        _cameraState.update {
            it.copy(currentSettings = it.currentSettings?.copy(torchEnabled = enabled))
        }
    }

    /**
     * Stop camera
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        _cameraState.update { it.copy(isRunning = false) }
    }

    /**
     * Release resources
     */
    fun release() {
        stopCamera()
        analysisExecutor.shutdown()
        frameProcessor = null
    }

    private fun processFrame(imageProxy: ImageProxy) {
        try {
            val bitmap = imageProxy.toBitmap()
            val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

            val frameData = FrameData(
                bitmap = rotatedBitmap,
                width = rotatedBitmap.width,
                height = rotatedBitmap.height,
                timestamp = System.currentTimeMillis(),
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )

            // Process with frame processor if set
            frameProcessor?.process(frameData)

            // Emit to flow
            _frameFlow.tryEmit(frameData)
        } finally {
            imageProxy.close()
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = android.graphics.YuvImage(
            nv21,
            ImageFormat.NV21,
            width,
            height,
            null
        )

        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
        val imageBytes = out.toByteArray()

        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun bitmapToBytes(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun extractMetadata(image: ImageProxy): CaptureMetadata {
        return CaptureMetadata(
            rotationDegrees = image.imageInfo.rotationDegrees,
            timestamp = image.imageInfo.timestamp
        )
    }
}

// Data Classes

data class CameraState(
    val isInitialized: Boolean = false,
    val isRunning: Boolean = false,
    val currentSettings: CameraSettings? = null,
    val error: String? = null
)

data class CameraSettings(
    val useFrontCamera: Boolean = false,
    val previewResolution: Size = Size(1920, 1080),
    val captureResolution: Size = Size(4032, 3024),
    val analysisResolution: Size = Size(1280, 720),
    val flashMode: FlashMode = FlashMode.OFF,
    val torchEnabled: Boolean = false,
    val autoFocus: Boolean = true,
    val highQualityCapture: Boolean = true,
    val enableHDR: Boolean = false,
    val stabilization: Boolean = true
)

enum class FlashMode {
    ON, OFF, AUTO
}

data class CapturedImage(
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val metadata: CaptureMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CapturedImage
        return timestamp == other.timestamp && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

data class CaptureMetadata(
    val rotationDegrees: Int,
    val timestamp: Long
)

data class FrameData(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val timestamp: Long,
    val rotationDegrees: Int
)

/**
 * Interface for real-time frame processing
 */
interface FrameProcessor {
    fun process(frame: FrameData)
}
