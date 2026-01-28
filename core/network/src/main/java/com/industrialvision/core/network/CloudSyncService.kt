package com.industrialvision.core.network

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud Sync Service
 *
 * Handles synchronization of inspection data with cloud services:
 * - Upload inspection results
 * - Download configuration updates
 * - Sync profiles across devices
 * - Backup and restore functionality
 */
@Singleton
class CloudSyncService @Inject constructor(
    private val gson: Gson
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private var baseUrl: String = ""
    private var apiKey: String = ""

    /**
     * Configure the cloud service
     */
    fun configure(baseUrl: String, apiKey: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.apiKey = apiKey
    }

    /**
     * Upload inspection data
     */
    suspend fun uploadInspection(
        inspectionData: InspectionUploadData
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(inspectionData)
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/v1/inspections")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val result = gson.fromJson(responseBody, UploadResponse::class.java)
                    SyncResult.Success(result.id)
                } else {
                    SyncResult.Error("Upload failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Upload inspection image
     */
    suspend fun uploadImage(
        inspectionId: String,
        imageFile: File
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("inspection_id", inspectionId)
                .addFormDataPart(
                    "image",
                    imageFile.name,
                    imageFile.asRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$baseUrl/api/v1/images")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SyncResult.Success("Image uploaded")
                } else {
                    SyncResult.Error("Image upload failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Download profile configurations
     */
    suspend fun downloadProfiles(): ProfilesDownloadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/profiles")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val profiles = gson.fromJson(responseBody, ProfilesResponse::class.java)
                    ProfilesDownloadResult.Success(profiles.profiles)
                } else {
                    ProfilesDownloadResult.Error("Download failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            ProfilesDownloadResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Sync inspection statistics
     */
    suspend fun syncStatistics(
        deviceId: String,
        stats: DeviceStatistics
    ): SyncResult = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(mapOf("device_id" to deviceId, "stats" to stats))
            val body = json.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/api/v1/statistics")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SyncResult.Success("Statistics synced")
                } else {
                    SyncResult.Error("Sync failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Check for model updates
     */
    suspend fun checkModelUpdates(): ModelUpdateResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/models/updates")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val updates = gson.fromJson(responseBody, ModelUpdatesResponse::class.java)
                    ModelUpdateResult.Available(updates.models)
                } else {
                    ModelUpdateResult.NoUpdates
                }
            }
        } catch (e: Exception) {
            ModelUpdateResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Download ML model
     */
    suspend fun downloadModel(
        modelId: String,
        targetPath: String
    ): ModelDownloadResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/api/v1/models/$modelId/download")
                .addHeader("Authorization", "Bearer $apiKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        File(targetPath).outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    ModelDownloadResult.Success(targetPath)
                } else {
                    ModelDownloadResult.Error("Download failed: ${response.code}")
                }
            }
        } catch (e: Exception) {
            ModelDownloadResult.Error(e.message ?: "Unknown error")
        }
    }
}

// Data Classes

data class InspectionUploadData(
    val id: String,
    val profileId: String,
    val timestamp: Long,
    val status: String,
    val score: Float,
    val findingsCount: Int,
    val metadata: Map<String, String>
)

data class UploadResponse(
    val id: String,
    val status: String
)

data class ProfilesResponse(
    val profiles: List<ProfileData>
)

data class ProfileData(
    val id: String,
    val name: String,
    val configuration: String,
    val version: Int
)

data class DeviceStatistics(
    val totalInspections: Int,
    val passRate: Float,
    val avgProcessingTime: Long,
    val lastSyncTimestamp: Long
)

data class ModelUpdatesResponse(
    val models: List<ModelInfo>
)

data class ModelInfo(
    val id: String,
    val name: String,
    val version: String,
    val size: Long,
    val checksum: String
)

// Result Types

sealed class SyncResult {
    data class Success(val message: String) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class ProfilesDownloadResult {
    data class Success(val profiles: List<ProfileData>) : ProfilesDownloadResult()
    data class Error(val message: String) : ProfilesDownloadResult()
}

sealed class ModelUpdateResult {
    data class Available(val models: List<ModelInfo>) : ModelUpdateResult()
    object NoUpdates : ModelUpdateResult()
    data class Error(val message: String) : ModelUpdateResult()
}

sealed class ModelDownloadResult {
    data class Success(val path: String) : ModelDownloadResult()
    data class Error(val message: String) : ModelDownloadResult()
}
