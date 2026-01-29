package com.industrialvision.systems

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.industrialvision.core.data.database.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import java.io.IOException

/**
 * Database instrumentation tests
 */
@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var inspectionDao: InspectionDao
    private lateinit var profileDao: ProfileDao
    private lateinit var db: IndustrialVisionDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, IndustrialVisionDatabase::class.java
        ).build()
        inspectionDao = db.inspectionDao()
        profileDao = db.profileDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveInspection() = runTest {
        // Given
        val inspection = createTestInspectionEntity()

        // When
        inspectionDao.insertInspection(inspection)
        val retrieved = inspectionDao.getInspectionById(inspection.id)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.id).isEqualTo(inspection.id)
        assertThat(retrieved?.profileName).isEqualTo(inspection.profileName)
    }

    @Test
    fun getRecentInspections() = runTest {
        // Given
        val inspections = (1..15).map { i ->
            createTestInspectionEntity().copy(
                id = "inspection-$i",
                timestamp = System.currentTimeMillis() + i
            )
        }
        inspections.forEach { inspectionDao.insertInspection(it) }

        // When
        val recent = inspectionDao.getRecentInspections(10).first()

        // Then
        assertThat(recent).hasSize(10)
    }

    @Test
    fun deleteInspection() = runTest {
        // Given
        val inspection = createTestInspectionEntity()
        inspectionDao.insertInspection(inspection)

        // When
        inspectionDao.deleteInspectionById(inspection.id)
        val retrieved = inspectionDao.getInspectionById(inspection.id)

        // Then
        assertThat(retrieved).isNull()
    }

    @Test
    fun getInspectionStatistics() = runTest {
        // Given
        val passInspection = createTestInspectionEntity().copy(
            id = "pass-1",
            passFailStatus = "PASS"
        )
        val failInspection = createTestInspectionEntity().copy(
            id = "fail-1",
            passFailStatus = "FAIL"
        )
        inspectionDao.insertInspection(passInspection)
        inspectionDao.insertInspection(failInspection)

        // When
        val total = inspectionDao.getTotalInspectionCount()
        val passed = inspectionDao.getPassCount()
        val failed = inspectionDao.getFailCount()

        // Then
        assertThat(total).isEqualTo(2)
        assertThat(passed).isEqualTo(1)
        assertThat(failed).isEqualTo(1)
    }

    @Test
    fun insertAndRetrieveProfile() = runTest {
        // Given
        val profile = createTestProfileEntity()

        // When
        profileDao.insertProfile(profile)
        val retrieved = profileDao.getProfileById(profile.id)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo(profile.name)
    }

    @Test
    fun setDefaultProfile() = runTest {
        // Given
        val profile1 = createTestProfileEntity().copy(id = "profile-1", isDefault = true)
        val profile2 = createTestProfileEntity().copy(id = "profile-2", isDefault = false)
        profileDao.insertProfile(profile1)
        profileDao.insertProfile(profile2)

        // When
        profileDao.clearDefaultProfile()
        profileDao.setDefaultProfile("profile-2")
        val defaultProfile = profileDao.getDefaultProfile()

        // Then
        assertThat(defaultProfile?.id).isEqualTo("profile-2")
    }

    private fun createTestInspectionEntity(): InspectionEntity {
        return InspectionEntity(
            id = "test-inspection",
            profileId = "test-profile",
            profileName = "Test Profile",
            timestamp = System.currentTimeMillis(),
            status = "COMPLETED",
            imageUri = null,
            thumbnailUri = null,
            overallScore = 0.95f,
            passFailStatus = "PASS",
            aiAnalysisSummary = "Test analysis",
            processingTimeMs = 1500,
            metadata = "{}"
        )
    }

    private fun createTestProfileEntity(): ProfileEntity {
        return ProfileEntity(
            id = "test-profile",
            name = "Test Profile",
            description = "A test inspection profile",
            category = "Testing",
            isActive = true,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            configuration = "{}"
        )
    }
}
