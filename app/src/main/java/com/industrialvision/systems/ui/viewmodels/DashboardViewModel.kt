package com.industrialvision.systems.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.industrialvision.ai.agents.core.AgentRegistry
import com.industrialvision.core.data.repository.InspectionRepository
import com.industrialvision.core.data.repository.InspectionStats
import com.industrialvision.core.domain.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Dashboard ViewModel - Manages the main dashboard state and data
 *
 * Features:
 * - Real-time statistics
 * - Recent inspection history
 * - Agent status monitoring
 * - System health tracking
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val inspectionRepository: InspectionRepository,
    private val agentRegistry: AgentRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DashboardEvent>()
    val events: SharedFlow<DashboardEvent> = _events.asSharedFlow()

    init {
        loadDashboardData()
        observeRecentInspections()
        observeAgentStatus()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load statistics
                val stats = inspectionRepository.getInspectionStats()
                _uiState.update {
                    it.copy(
                        stats = stats,
                        isLoading = false
                    )
                }

                // Log dashboard view
                inspectionRepository.logAnalyticsEvent(
                    "dashboard_viewed",
                    mapOf("timestamp" to System.currentTimeMillis().toString())
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard data")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load dashboard data: ${e.message}"
                    )
                }
            }
        }
    }

    private fun observeRecentInspections() {
        viewModelScope.launch {
            inspectionRepository.getRecentInspections(10)
                .catch { e ->
                    Timber.e(e, "Error observing inspections")
                    _events.emit(DashboardEvent.ShowError("Failed to load inspections"))
                }
                .collect { inspections ->
                    _uiState.update { it.copy(recentInspections = inspections) }
                }
        }
    }

    private fun observeAgentStatus() {
        viewModelScope.launch {
            agentRegistry.registeredAgents
                .collect { agents ->
                    val agentStatuses = agents.map { agent ->
                        AgentStatusInfo(
                            id = agent.id,
                            name = agent.name,
                            type = agent.type,
                            status = agent.status,
                            isEnabled = agent.configuration.enabled
                        )
                    }
                    _uiState.update { it.copy(agentStatuses = agentStatuses) }
                }
        }
    }

    fun refreshData() {
        loadDashboardData()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onInspectionClick(inspectionId: String) {
        viewModelScope.launch {
            _events.emit(DashboardEvent.NavigateToInspection(inspectionId))
        }
    }

    fun checkSystemHealth(): SystemHealth {
        // Check various system components
        val cameraStatus = HealthStatus.OK // Would check actual camera
        val mlModelsStatus = if (agentRegistry.getAllAgents().any {
                it.modelInfo != null && it.status == AgentStatus.READY
            }) HealthStatus.OK else HealthStatus.WARNING
        val storageStatus = HealthStatus.OK // Would check actual storage
        val networkStatus = HealthStatus.OK // Would check network

        return SystemHealth(
            camera = cameraStatus,
            mlModels = mlModelsStatus,
            storage = storageStatus,
            network = networkStatus
        )
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stats: InspectionStats = InspectionStats(0, 0, 0, 0f, 0, 0f),
    val recentInspections: List<Inspection> = emptyList(),
    val agentStatuses: List<AgentStatusInfo> = emptyList(),
    val systemHealth: SystemHealth = SystemHealth()
)

data class AgentStatusInfo(
    val id: String,
    val name: String,
    val type: AgentType,
    val status: AgentStatus,
    val isEnabled: Boolean
)

data class SystemHealth(
    val camera: HealthStatus = HealthStatus.UNKNOWN,
    val mlModels: HealthStatus = HealthStatus.UNKNOWN,
    val storage: HealthStatus = HealthStatus.UNKNOWN,
    val network: HealthStatus = HealthStatus.UNKNOWN
)

enum class HealthStatus {
    OK, WARNING, ERROR, UNKNOWN
}

sealed class DashboardEvent {
    data class NavigateToInspection(val inspectionId: String) : DashboardEvent()
    data class ShowError(val message: String) : DashboardEvent()
    object RefreshComplete : DashboardEvent()
}
