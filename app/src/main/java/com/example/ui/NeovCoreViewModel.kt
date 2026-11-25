package com.example.ui

import android.app.Application
import android.hardware.Sensor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BatteryInfo
import com.example.data.model.CpuInfo
import com.example.data.model.LiveSensorReading
import com.example.data.model.MemoryInfo
import com.example.data.model.NeovModule
import com.example.data.model.SensorCategory
import com.example.data.model.SensorItem
import com.example.data.model.SystemInfo
import com.example.data.repository.HardwareRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class NeovCoreUiState(
    val selectedModule: NeovModule = NeovModule.SOC,
    val isLiveEnabled: Boolean = true,
    val isDarkThemeOverride: Boolean? = null,
    val showPrivacySheet: Boolean = false,
    val cpuInfo: CpuInfo? = null,
    val cpuClockHistoryMhz: List<Float> = emptyList(),
    val cpuLoadHistory: List<Float> = emptyList(),
    val systemInfo: SystemInfo? = null,
    val memoryInfo: MemoryInfo? = null,
    val ramUsageHistory: List<Float> = emptyList(),
    val batteryInfo: BatteryInfo? = null,
    val batteryTempHistory: List<Float> = emptyList(),
    val sensors: List<SensorItem> = emptyList(),
    val sensorSearchQuery: String = "",
    val selectedSensorCategory: SensorCategory = SensorCategory.ALL,
    val monitoredSensorIds: Set<Int> = emptySet(),
    val expandedSensorId: Int? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NeovCoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HardwareRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(NeovCoreUiState())
    val uiState: StateFlow<NeovCoreUiState> = _uiState.asStateFlow()

    private val _activeSensorSelection = MutableStateFlow<Set<Int>>(emptySet())
    private val _isLive = MutableStateFlow(true)

    val liveSensorReadings: StateFlow<Map<Int, LiveSensorReading>> =
        combine(_isLive, _activeSensorSelection) { live, ids ->
            if (live) ids else emptySet()
        }.flatMapLatest { activeIds ->
            if (activeIds.isEmpty()) {
                flowOf(emptyMap())
            } else {
                repository.observeSensorReadings(activeIds)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(3_000),
            initialValue = emptyMap()
        )

    init {
        loadInitialData()
        observeBatteryStream()
        startPollingLoop()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val sys = repository.readSystemInfo()
            val sensors = repository.getAvailableSensors()

            val defaultMonitored = sensors
                .filter {
                    it.type in setOf(
                        Sensor.TYPE_ACCELEROMETER,
                        Sensor.TYPE_GYROSCOPE,
                        Sensor.TYPE_MAGNETIC_FIELD,
                        Sensor.TYPE_LIGHT,
                        Sensor.TYPE_PRESSURE,
                        Sensor.TYPE_PROXIMITY
                    )
                }
                .take(6)
                .map { it.id }
                .toSet()

            _activeSensorSelection.value = defaultMonitored
            _uiState.update {
                it.copy(
                    systemInfo = sys,
                    sensors = sensors,
                    monitoredSensorIds = defaultMonitored,
                    expandedSensorId = defaultMonitored.firstOrNull()
                )
            }
        }
    }

    private fun observeBatteryStream() {
        viewModelScope.launch {
            repository.observeBatteryInfo().collect { battery ->
                if (_isLive.value || _uiState.value.batteryInfo == null) {
                    _uiState.update { state ->
                        val nextHistory = (state.batteryTempHistory + battery.temperatureCelsius)
                            .takeLast(24)
                        state.copy(
                            batteryInfo = battery,
                            batteryTempHistory = nextHistory
                        )
                    }
                }
            }
        }
    }

    private fun startPollingLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (_isLive.value || _uiState.value.cpuInfo == null) {
                    val cpu = repository.readCpuInfo()
                    val mem = repository.readMemoryInfo()
                    val sys = repository.readSystemInfo()
                    val batterySnap = repository.readCurrentBatterySnapshot()

                    val avgMhz = ((cpu.averageClockKhz ?: cpu.peakClockKhz ?: 0L) / 1000f)
                    _uiState.update { state ->
                        val nextCpuHist = if (avgMhz >= 10f) {
                            (state.cpuClockHistoryMhz + avgMhz).takeLast(28)
                        } else {
                            state.cpuClockHistoryMhz
                        }
                        val nextLoadHist = if (cpu.cpuLoadPercent != null) {
                            (state.cpuLoadHistory + cpu.cpuLoadPercent).takeLast(28)
                        } else {
                            state.cpuLoadHistory
                        }
                        val nextRamHist = (state.ramUsageHistory + mem.ramUsagePercent).takeLast(28)
                        val updatedBattery = batterySnap ?: state.batteryInfo
                        val nextBattTempHist = if (batterySnap != null) {
                            (state.batteryTempHistory + batterySnap.temperatureCelsius).takeLast(24)
                        } else {
                            state.batteryTempHistory
                        }
                        state.copy(
                            cpuInfo = cpu,
                            cpuClockHistoryMhz = nextCpuHist,
                            cpuLoadHistory = nextLoadHist,
                            memoryInfo = mem,
                            ramUsageHistory = nextRamHist,
                            systemInfo = sys,
                            batteryInfo = updatedBattery,
                            batteryTempHistory = nextBattTempHist
                        )
                    }
                }
                delay(1_000L)
            }
        }
    }

    fun selectModule(module: NeovModule) {
        _uiState.update { it.copy(selectedModule = module) }
    }

    fun toggleLive() {
        val next = !_uiState.value.isLiveEnabled
        _isLive.value = next
        _uiState.update { it.copy(isLiveEnabled = next) }
    }

    fun toggleTheme(currentlyDark: Boolean) {
        _uiState.update { it.copy(isDarkThemeOverride = !currentlyDark) }
    }

    fun setPrivacySheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showPrivacySheet = visible) }
    }

    fun updateSensorSearch(query: String) {
        _uiState.update { it.copy(sensorSearchQuery = query) }
    }

    fun selectSensorCategory(category: SensorCategory) {
        _uiState.update { it.copy(selectedSensorCategory = category) }
    }

    fun toggleSensorExpand(sensorId: Int) {
        _uiState.update { state ->
            val newExpanded = if (state.expandedSensorId == sensorId) null else sensorId
            val updatedMonitored = if (newExpanded != null) {
                state.monitoredSensorIds + newExpanded
            } else {
                state.monitoredSensorIds
            }
            _activeSensorSelection.value = updatedMonitored
            state.copy(
                expandedSensorId = newExpanded,
                monitoredSensorIds = updatedMonitored
            )
        }
    }

    fun toggleSensorMonitor(sensorId: Int) {
        _uiState.update { state ->
            val updated = if (sensorId in state.monitoredSensorIds) {
                state.monitoredSensorIds - sensorId
            } else {
                state.monitoredSensorIds + sensorId
            }
            _activeSensorSelection.value = updated
            state.copy(monitoredSensorIds = updated)
        }
    }
}
