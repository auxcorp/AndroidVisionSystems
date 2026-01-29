package com.industrialvision.systems

import com.google.gson.Gson
import com.industrialvision.core.data.database.*
import com.industrialvision.core.data.repository.InspectionRepository
import com.industrialvision.core.domain.models.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for InspectionRepository
 */
class InspectionRepositoryTest {

    private lateinit var inspectionDao: InspectionDao
    private lateinit var profileDao: ProfileDao
    private lateinit var analyticsDao: AnalyticsDao
    private lateinit var gson: Gson
    private lateinit var repository: InspectionRepository

    @Before
    fun setup() {
        inspectionDao = mockk(relaxed = true)
        profileDao = mockk(relaxed = true)
        analyticsDao = mockk(relaxed = true)
        gson = Gson()

        repository = InspectionRepository(inspectionDao, profileDao, analyticsDao, gson)
    }

    @Test
    fun `getInspectionStats returns correct statistics`() = runTest {
        // Given
        coEvery { inspectionDao.getTotalInspectionCount() } returns 100
        coEvery { inspectionDao.getPassCount() } returns 85
        coEvery { inspectionDao.getFailCount() } returns 15
        coEvery { inspectionDao.getInspectionCountSince(any()) } returns 10
        coEvery { inspectionDao.getAverageScoreSince(any()) } returns 0.92f

        // When
        val stats = repository.getInspectionStats()

        // Then
        assertThat(stats.totalInspections).isEqualTo(100)
        assertThat(stats.passedInspections).isEqualTo(85)
        assertThat(stats.failedInspections).isEqualTo(15)
        assertThat(stats.passRate).isEqualTo(0.85f)
        assertThat(stats.todayInspections).isEqualTo(10)
        assertThat(stats.todayAverageScore).isEqualTo(0.92f)
    }

    @Test
    fun `getInspectionStats returns zero pass rate when no inspections`() = runTest {
        // Given
        coEvery { inspectionDao.getTotalInspectionCount() } returns 0
        coEvery { inspectionDao.getPassCount() } returns 0
        coEvery { inspectionDao.getFailCount() } returns 0
        coEvery { inspectionDao.getInspectionCountSince(any()) } returns 0
        coEvery { inspectionDao.getAverageScoreSince(any()) } returns null

        // When
        val stats = repository.getInspectionStats()

        // Then
        assertThat(stats.passRate).isEqualTo(0f)
        assertThat(stats.todayAverageScore).isEqualTo(0f)
    }

    @Test
    fun `saveInspection inserts inspection and results`() = runTest {
        // Given
        val inspection = createTestInspection()
        coEvery { inspectionDao.insertInspection(any()) } just Runs
        coEvery { inspectionDao.insertResult(any()) } just Runs
        coEvery { inspectionDao.insertFinding(any()) } just Runs
        coEvery { analyticsDao.insertEvent(any()) } just Runs

        // When
        repository.saveInspection(inspection)

        // Then
        coVerify { inspectionDao.insertInspection(any()) }
        coVerify(exactly = inspection.results.size) { inspectionDao.insertResult(any()) }
    }

    @Test
    fun `deleteInspection calls dao`() = runTest {
        // Given
        val inspectionId = "test-id"
        coEvery { inspectionDao.deleteInspectionById(inspectionId) } just Runs

        // When
        repository.deleteInspection(inspectionId)

        // Then
        coVerify { inspectionDao.deleteInspectionById(inspectionId) }
    }

    @Test
    fun `setDefaultProfile clears previous and sets new`() = runTest {
        // Given
        val profileId = "profile-123"
        coEvery { profileDao.clearDefaultProfile() } just Runs
        coEvery { profileDao.setDefaultProfile(profileId) } just Runs

        // When
        repository.setDefaultProfile(profileId)

        // Then
        coVerifyOrder {
            profileDao.clearDefaultProfile()
            profileDao.setDefaultProfile(profileId)
        }
    }

    @Test
    fun `cleanupOldAnalytics deletes events older than threshold`() = runTest {
        // Given
        coEvery { analyticsDao.deleteOldEvents(any()) } just Runs

        // When
        repository.cleanupOldAnalytics(30)

        // Then
        coVerify { analyticsDao.deleteOldEvents(any()) }
    }

    private fun createTestInspection(): Inspection {
        return Inspection(
            id = "test-inspection-id",
            profileId = "test-profile-id",
            profileName = "Test Profile",
            status = InspectionStatus.COMPLETED,
            overallScore = 0.95f,
            passFailStatus = PassFailStatus.PASS,
            results = listOf(
                InspectionResult(
                    moduleType = InspectionModuleType.DEFECT_DETECTION,
                    moduleName = "Defect Detection",
                    status = ResultStatus.SUCCESS,
                    confidence = 0.95f,
                    findings = listOf(
                        Finding(
                            type = FindingType.DEFECT,
                            severity = FindingSeverity.MINOR,
                            description = "Small scratch detected",
                            confidence = 0.87f
                        )
                    )
                )
            )
        )
    }
}
