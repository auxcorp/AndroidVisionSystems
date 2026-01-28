package com.industrialvision.core.data.repository

import com.industrialvision.core.data.database.*
import com.industrialvision.core.domain.models.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inspection Repository
 *
 * Provides data access for inspections, profiles, and analytics.
 * Handles conversion between domain models and database entities.
 */
@Singleton
class InspectionRepository @Inject constructor(
    private val inspectionDao: InspectionDao,
    private val profileDao: ProfileDao,
    private val analyticsDao: AnalyticsDao,
    private val gson: Gson
) {
    // Inspections

    fun getAllInspections(): Flow<List<Inspection>> =
        inspectionDao.getAllInspections().map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getRecentInspections(limit: Int = 10): Flow<List<Inspection>> =
        inspectionDao.getRecentInspections(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getInspectionsByDateRange(startTime: Long, endTime: Long): Flow<List<Inspection>> =
        inspectionDao.getInspectionsByDateRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getInspectionsByProfile(profileId: String): Flow<List<Inspection>> =
        inspectionDao.getInspectionsByProfile(profileId).map { entities ->
            entities.map { it.toDomainModel() }
        }

    suspend fun getInspectionById(id: String): Inspection? =
        inspectionDao.getInspectionById(id)?.toDomainModel()

    suspend fun saveInspection(inspection: Inspection) {
        // Save main inspection
        inspectionDao.insertInspection(inspection.toEntity())

        // Save results
        inspection.results.forEach { result ->
            val resultEntity = result.toEntity(inspection.id)
            inspectionDao.insertResult(resultEntity)

            // Save findings
            result.findings.forEach { finding ->
                inspectionDao.insertFinding(finding.toEntity(resultEntity.id))
            }
        }

        // Log analytics event
        logAnalyticsEvent("inspection_saved", mapOf(
            "inspectionId" to inspection.id,
            "profileId" to inspection.profileId,
            "status" to inspection.passFailStatus.name,
            "findingsCount" to inspection.results.sumOf { it.findings.size }.toString()
        ))
    }

    suspend fun deleteInspection(id: String) {
        inspectionDao.deleteInspectionById(id)
    }

    // Statistics

    suspend fun getInspectionStats(): InspectionStats {
        val total = inspectionDao.getTotalInspectionCount()
        val passed = inspectionDao.getPassCount()
        val failed = inspectionDao.getFailCount()

        val dayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val todayCount = inspectionDao.getInspectionCountSince(dayAgo)
        val todayAvgScore = inspectionDao.getAverageScoreSince(dayAgo) ?: 0f

        return InspectionStats(
            totalInspections = total,
            passedInspections = passed,
            failedInspections = failed,
            passRate = if (total > 0) passed.toFloat() / total else 0f,
            todayInspections = todayCount,
            todayAverageScore = todayAvgScore
        )
    }

    // Profiles

    fun getActiveProfiles(): Flow<List<InspectionProfile>> =
        profileDao.getActiveProfiles().map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getAllProfiles(): Flow<List<InspectionProfile>> =
        profileDao.getAllProfiles().map { entities ->
            entities.map { it.toDomainModel() }
        }

    suspend fun getProfileById(id: String): InspectionProfile? =
        profileDao.getProfileById(id)?.toDomainModel()

    suspend fun getDefaultProfile(): InspectionProfile? =
        profileDao.getDefaultProfile()?.toDomainModel()

    suspend fun saveProfile(profile: InspectionProfile) {
        profileDao.insertProfile(profile.toEntity())
    }

    suspend fun deleteProfile(profile: InspectionProfile) {
        profileDao.deleteProfile(profile.toEntity())
    }

    suspend fun setDefaultProfile(profileId: String) {
        profileDao.clearDefaultProfile()
        profileDao.setDefaultProfile(profileId)
    }

    // Analytics

    suspend fun logAnalyticsEvent(eventType: String, data: Map<String, String>) {
        analyticsDao.insertEvent(
            AnalyticsEventEntity(
                eventType = eventType,
                timestamp = System.currentTimeMillis(),
                data = gson.toJson(data)
            )
        )
    }

    suspend fun cleanupOldAnalytics(daysToKeep: Int = 30) {
        val cutoff = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000L
        analyticsDao.deleteOldEvents(cutoff)
    }

    // Mapping Functions

    private fun InspectionEntity.toDomainModel(): Inspection {
        return Inspection(
            id = id,
            profileId = profileId,
            profileName = profileName,
            timestamp = timestamp,
            status = InspectionStatus.valueOf(status),
            imageUri = imageUri,
            thumbnailUri = thumbnailUri,
            overallScore = overallScore,
            passFailStatus = PassFailStatus.valueOf(passFailStatus),
            aiAnalysis = aiAnalysisSummary?.let {
                AIAnalysis(
                    summary = it,
                    insights = emptyList(),
                    recommendations = emptyList(),
                    confidenceScore = 0f
                )
            },
            metadata = try {
                gson.fromJson(metadata, InspectionMetadata::class.java)
            } catch (e: Exception) {
                InspectionMetadata()
            }
        )
    }

    private fun Inspection.toEntity(): InspectionEntity {
        return InspectionEntity(
            id = id,
            profileId = profileId,
            profileName = profileName,
            timestamp = timestamp,
            status = status.name,
            imageUri = imageUri,
            thumbnailUri = thumbnailUri,
            overallScore = overallScore,
            passFailStatus = passFailStatus.name,
            aiAnalysisSummary = aiAnalysis?.summary,
            processingTimeMs = results.sumOf { it.processingTimeMs },
            metadata = gson.toJson(metadata)
        )
    }

    private fun InspectionResult.toEntity(inspectionId: String): InspectionResultEntity {
        return InspectionResultEntity(
            id = id,
            inspectionId = inspectionId,
            moduleType = moduleType.name,
            moduleName = moduleName,
            status = status.name,
            confidence = confidence,
            processingTimeMs = processingTimeMs,
            rawData = rawData
        )
    }

    private fun Finding.toEntity(resultId: String): FindingEntity {
        return FindingEntity(
            id = id,
            resultId = resultId,
            type = type.name,
            severity = severity.name,
            description = description,
            confidence = confidence,
            boundingBox = boundingBox?.let { gson.toJson(it) },
            value = value,
            unit = unit,
            attributes = gson.toJson(attributes)
        )
    }

    private fun ProfileEntity.toDomainModel(): InspectionProfile {
        return try {
            gson.fromJson(configuration, InspectionProfile::class.java).copy(
                id = id,
                name = name,
                description = description,
                category = category,
                isActive = isActive,
                isDefault = isDefault,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            InspectionProfile(
                id = id,
                name = name,
                description = description,
                category = category,
                isActive = isActive,
                isDefault = isDefault,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }

    private fun InspectionProfile.toEntity(): ProfileEntity {
        return ProfileEntity(
            id = id,
            name = name,
            description = description,
            category = category,
            isActive = isActive,
            isDefault = isDefault,
            createdAt = createdAt,
            updatedAt = updatedAt,
            configuration = gson.toJson(this)
        )
    }
}

data class InspectionStats(
    val totalInspections: Int,
    val passedInspections: Int,
    val failedInspections: Int,
    val passRate: Float,
    val todayInspections: Int,
    val todayAverageScore: Float
)
