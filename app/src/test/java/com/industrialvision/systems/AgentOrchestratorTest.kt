package com.industrialvision.systems

import com.industrialvision.ai.agents.core.*
import com.industrialvision.core.domain.models.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for AgentOrchestrator
 */
class AgentOrchestratorTest {

    private lateinit var agentRegistry: AgentRegistry
    private lateinit var messageRouter: MessageRouter
    private lateinit var orchestrator: AgentOrchestrator

    @Before
    fun setup() {
        agentRegistry = mockk(relaxed = true)
        messageRouter = mockk(relaxed = true)
        orchestrator = AgentOrchestrator(agentRegistry, messageRouter)
    }

    @Test
    fun `executeTask returns output from executor`() = runTest {
        // Given
        val agent = createTestAgent()
        val task = createTestTask()
        val expectedOutput = TaskOutput(
            confidence = 0.95f,
            findings = listOf(
                Finding(
                    type = FindingType.DEFECT,
                    severity = FindingSeverity.MINOR,
                    description = "Test finding",
                    confidence = 0.9f
                )
            )
        )

        val executor = mockk<AgentExecutor> {
            coEvery { execute(task) } returns expectedOutput
            every { isAvailable() } returns true
        }

        every { agentRegistry.getAgent(agent.id) } returns agent
        every { agentRegistry.getExecutor(agent.type) } returns executor

        // When
        val result = orchestrator.executeTask(agent.id, task)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.confidence).isEqualTo(0.95f)
        assertThat(result?.findings).hasSize(1)
    }

    @Test
    fun `executeTask returns null when agent not found`() = runTest {
        // Given
        every { agentRegistry.getAgent("unknown-agent") } returns null

        // When
        val result = orchestrator.executeTask("unknown-agent", createTestTask())

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `executeTask returns null when executor not available`() = runTest {
        // Given
        val agent = createTestAgent()
        val executor = mockk<AgentExecutor> {
            every { isAvailable() } returns false
        }

        every { agentRegistry.getAgent(agent.id) } returns agent
        every { agentRegistry.getExecutor(agent.type) } returns executor

        // When
        val result = orchestrator.executeTask(agent.id, createTestTask())

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `getAvailableAgents returns only enabled and ready agents`() {
        // Given
        val activeAgent = createTestAgent().copy(
            status = AgentStatus.READY,
            configuration = AgentConfiguration(enabled = true)
        )
        val disabledAgent = createTestAgent().copy(
            id = "disabled-agent",
            status = AgentStatus.DISABLED,
            configuration = AgentConfiguration(enabled = false)
        )

        every { agentRegistry.getAllAgents() } returns listOf(activeAgent, disabledAgent)

        // When
        val available = orchestrator.getAvailableAgents()

        // Then
        assertThat(available).hasSize(1)
        assertThat(available[0].id).isEqualTo(activeAgent.id)
    }

    @Test
    fun `getAgentsByCapability filters correctly`() {
        // Given
        val defectAgent = createTestAgent().copy(
            capabilities = listOf(AgentCapability.DEFECT_DETECTION, AgentCapability.IMAGE_ANALYSIS)
        )
        val ocrAgent = createTestAgent().copy(
            id = "ocr-agent",
            capabilities = listOf(AgentCapability.OCR, AgentCapability.IMAGE_ANALYSIS)
        )

        every { agentRegistry.getAllAgents() } returns listOf(defectAgent, ocrAgent)

        // When
        val defectAgents = orchestrator.getAgentsByCapability(AgentCapability.DEFECT_DETECTION)
        val analysisAgents = orchestrator.getAgentsByCapability(AgentCapability.IMAGE_ANALYSIS)

        // Then
        assertThat(defectAgents).hasSize(1)
        assertThat(analysisAgents).hasSize(2)
    }

    private fun createTestAgent(): VisionAgent {
        return VisionAgent(
            id = "test-agent",
            name = "Test Agent",
            type = AgentType.DEFECT_DETECTOR,
            description = "Test agent for unit tests",
            capabilities = listOf(AgentCapability.DEFECT_DETECTION),
            status = AgentStatus.READY,
            configuration = AgentConfiguration(enabled = true)
        )
    }

    private fun createTestTask(): AgentTask {
        return AgentTask(
            id = "test-task",
            inspectionId = "test-inspection",
            inputData = TaskInput(
                imageBytes = ByteArray(0)
            )
        )
    }
}
