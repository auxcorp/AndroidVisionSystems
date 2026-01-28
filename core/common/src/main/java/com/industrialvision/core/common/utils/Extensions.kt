package com.industrialvision.core.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Common Extensions and Utilities
 */

// Result wrapper for handling success/error states
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this == Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .catch { emit(Result.Error(it)) }

// Date/Time Extensions
fun Long.toFormattedDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000} min ago"
        diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
        diff < 604_800_000 -> "${diff / 86_400_000} days ago"
        else -> toFormattedDate("MMM dd, yyyy")
    }
}

// Bitmap Extensions
fun Bitmap.toByteArray(quality: Int = 90): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return stream.toByteArray()
}

fun ByteArray.toBitmap(): Bitmap? {
    return try {
        BitmapFactory.decodeByteArray(this, 0, size)
    } catch (e: Exception) {
        null
    }
}

fun Bitmap.scale(maxWidth: Int, maxHeight: Int): Bitmap {
    val ratio = minOf(
        maxWidth.toFloat() / width,
        maxHeight.toFloat() / height
    )

    if (ratio >= 1f) return this

    val newWidth = (width * ratio).toInt()
    val newHeight = (height * ratio).toInt()

    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

// Numeric Extensions
fun Float.format(decimals: Int = 2): String = "%.${decimals}f".format(this)

fun Float.toPercentString(): String = "${(this * 100).format(1)}%"

fun Int.toFileSizeString(): String = when {
    this < 1024 -> "$this B"
    this < 1024 * 1024 -> "${(this / 1024f).format(1)} KB"
    this < 1024 * 1024 * 1024 -> "${(this / (1024f * 1024f)).format(1)} MB"
    else -> "${(this / (1024f * 1024f * 1024f)).format(2)} GB"
}

// String Extensions
fun String.truncate(maxLength: Int, suffix: String = "..."): String =
    if (length <= maxLength) this else take(maxLength - suffix.length) + suffix

fun String.toSafeFileName(): String =
    replace(Regex("[^a-zA-Z0-9._-]"), "_")

// Collection Extensions
fun <T> List<T>.chunkedParallel(size: Int): List<List<T>> =
    chunked(size)

inline fun <T> List<T>.forEachIndexedSafe(action: (index: Int, T) -> Unit) {
    forEachIndexed { index, item ->
        try {
            action(index, item)
        } catch (e: Exception) {
            // Log error but continue
        }
    }
}

// Context Extensions
fun Context.dpToPx(dp: Float): Float = dp * resources.displayMetrics.density

fun Context.pxToDp(px: Float): Float = px / resources.displayMetrics.density
