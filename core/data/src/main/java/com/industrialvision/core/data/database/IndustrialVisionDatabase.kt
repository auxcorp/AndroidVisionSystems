package com.industrialvision.core.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Industrial Vision Database
 *
 * Room database for storing inspection data, profiles, and analytics.
 */
@Database(
    entities = [
        InspectionEntity::class,
        InspectionResultEntity::class,
        FindingEntity::class,
        ProfileEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class IndustrialVisionDatabase : RoomDatabase() {
    abstract fun inspectionDao(): InspectionDao
    abstract fun profileDao(): ProfileDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        const val DATABASE_NAME = "industrial_vision_db"
    }
}

// Entities

@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val profileName: String,
    val timestamp: Long,
    val status: String,
    val imageUri: String?,
    val thumbnailUri: String?,
    val overallScore: Float,
    val passFailStatus: String,
    val aiAnalysisSummary: String?,
    val processingTimeMs: Long,
    val metadata: String // JSON serialized
)

@Entity(
    tableName = "inspection_results",
    foreignKeys = [ForeignKey(
        entity = InspectionEntity::class,
        parentColumns = ["id"],
        childColumns = ["inspectionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class InspectionResultEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val inspectionId: String,
    val moduleType: String,
    val moduleName: String,
    val status: String,
    val confidence: Float,
    val processingTimeMs: Long,
    val rawData: String?
)

@Entity(
    tableName = "findings",
    foreignKeys = [ForeignKey(
        entity = InspectionResultEntity::class,
        parentColumns = ["id"],
        childColumns = ["resultId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class FindingEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(index = true) val resultId: String,
    val type: String,
    val severity: String,
    val description: String,
    val confidence: Float,
    val boundingBox: String?, // JSON serialized
    val value: String?,
    val unit: String?,
    val attributes: String // JSON serialized
)

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val isActive: Boolean,
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val configuration: String // JSON serialized
)

@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventType: String,
    val timestamp: Long,
    val data: String // JSON serialized
)

// DAOs

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :id")
    suspend fun getInspectionById(id: String): InspectionEntity?

    @Query("SELECT * FROM inspections ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInspections(limit: Int): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getInspectionsByDateRange(startTime: Long, endTime: Long): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getInspectionsByProfile(profileId: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE passFailStatus = :status ORDER BY timestamp DESC")
    fun getInspectionsByStatus(status: String): Flow<List<InspectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: InspectionEntity)

    @Update
    suspend fun updateInspection(inspection: InspectionEntity)

    @Delete
    suspend fun deleteInspection(inspection: InspectionEntity)

    @Query("DELETE FROM inspections WHERE id = :id")
    suspend fun deleteInspectionById(id: String)

    // Results
    @Query("SELECT * FROM inspection_results WHERE inspectionId = :inspectionId")
    fun getResultsForInspection(inspectionId: String): Flow<List<InspectionResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: InspectionResultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<InspectionResultEntity>)

    // Findings
    @Query("SELECT * FROM findings WHERE resultId = :resultId")
    fun getFindingsForResult(resultId: String): Flow<List<FindingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinding(finding: FindingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(findings: List<FindingEntity>)

    // Statistics
    @Query("SELECT COUNT(*) FROM inspections")
    suspend fun getTotalInspectionCount(): Int

    @Query("SELECT COUNT(*) FROM inspections WHERE passFailStatus = 'PASS'")
    suspend fun getPassCount(): Int

    @Query("SELECT COUNT(*) FROM inspections WHERE passFailStatus = 'FAIL'")
    suspend fun getFailCount(): Int

    @Query("SELECT COUNT(*) FROM inspections WHERE timestamp >= :since")
    suspend fun getInspectionCountSince(since: Long): Int

    @Query("SELECT AVG(overallScore) FROM inspections WHERE timestamp >= :since")
    suspend fun getAverageScoreSince(since: Long): Float?
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE isActive = 1 ORDER BY name")
    fun getActiveProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY name")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultProfile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultProfile()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)
}

@Dao
interface AnalyticsDao {
    @Query("SELECT * FROM analytics_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<AnalyticsEventEntity>>

    @Query("SELECT * FROM analytics_events WHERE eventType = :type ORDER BY timestamp DESC LIMIT :limit")
    fun getEventsByType(type: String, limit: Int): Flow<List<AnalyticsEventEntity>>

    @Insert
    suspend fun insertEvent(event: AnalyticsEventEntity)

    @Query("DELETE FROM analytics_events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long)
}

// Type Converters
class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",")
}
