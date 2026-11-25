package com.example.data.model

enum class NeovModule(val route: String) {
    SOC("soc"),
    SYSTEM("system"),
    MEMORY("memory"),
    BATTERY("battery"),
    SENSORS("sensors")
}

data class CpuCoreInfo(
    val coreIndex: Int,
    val currentFreqKhz: Long?,
    val minFreqKhz: Long?,
    val maxFreqKhz: Long?,
    val isOnline: Boolean,
    val governor: String?,
    val loadPercent: Float? = null
) {
    val utilizationRatio: Float
        get() {
            if (!isOnline) return 0f
            if (loadPercent != null) {
                return (loadPercent / 100f).coerceIn(0.04f, 1f)
            }
            if (currentFreqKhz == null || maxFreqKhz == null || maxFreqKhz <= 0L) return 0f
            val min = minFreqKhz ?: 0L
            val span = (maxFreqKhz - min).coerceAtLeast(1L)
            return ((currentFreqKhz - min).toFloat() / span.toFloat()).coerceIn(0.05f, 1f)
        }
}

data class CpuClusterSummary(
    val label: String,
    val maxFreqKhz: Long,
    val coreCount: Int
)

data class CpuInfo(
    val socModel: String,
    val socManufacturer: String,
    val hardware: String,
    val architecture: String,
    val supportedAbis: List<String>,
    val is64Bit: Boolean,
    val totalCores: Int,
    val onlineCores: Int,
    val governor: String,
    val cores: List<CpuCoreInfo>,
    val clusters: List<CpuClusterSummary>,
    val averageClockKhz: Long?,
    val peakClockKhz: Long?,
    val cpuLoadPercent: Float? = null,
    val loadAverage: String? = null,
    val bogoMips: String?,
    val features: List<String>,
    val freqRestricted: Boolean
)

data class SystemInfo(
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,
    val kernelVersion: String,
    val vmVersion: String,
    val bootloader: String,
    val radioVersion: String,
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityScale: Float,
    val densityBucket: String,
    val screenInches: Double,
    val refreshRateHz: Float,
    val isHdrSupported: Boolean,
    val isWideColorGamut: Boolean,
    val elapsedRealtimeMs: Long,
    val uptimeMs: Long
)

data class MemoryInfo(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamBytes: Long,
    val lowMemoryThresholdBytes: Long,
    val isLowMemory: Boolean,
    val ramUsagePercent: Float,
    val cachedBytes: Long?,
    val swapTotalBytes: Long?,
    val swapFreeBytes: Long?,
    val dataTotalBytes: Long,
    val dataFreeBytes: Long,
    val dataUsedBytes: Long,
    val systemTotalBytes: Long,
    val systemFreeBytes: Long,
    val systemUsedBytes: Long
)

enum class BatteryChargeStatus {
    CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN
}

enum class BatteryPowerSource {
    AC, USB, WIRELESS, BATTERY
}

enum class BatteryHealthState {
    GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, FAILURE, UNKNOWN
}

enum class BatteryThermalLevel {
    COOL, NORMAL, WARM, HOT
}

data class BatteryInfo(
    val levelPercent: Int,
    val status: BatteryChargeStatus,
    val powerSource: BatteryPowerSource,
    val temperatureCelsius: Float,
    val thermalLevel: BatteryThermalLevel,
    val voltageMv: Int,
    val health: BatteryHealthState,
    val technology: String,
    val currentMa: Float?,
    val powerWatts: Float?,
    val chargeCounterMah: Int?
)

enum class SensorCategory {
    ALL, MOTION, ENVIRONMENT, POSITION, OTHER
}

enum class SensorReportingMode {
    CONTINUOUS, ON_CHANGE, ONE_SHOT, SPECIAL_TRIGGER
}

data class SensorItem(
    val id: Int,
    val name: String,
    val vendor: String,
    val type: Int,
    val friendlyTypeArabic: String,
    val category: SensorCategory,
    val powerMa: Float,
    val maxRange: Float,
    val resolution: Float,
    val minDelayUs: Int,
    val reportingMode: SensorReportingMode,
    val valueLabels: List<String>,
    val unit: String
)

data class LiveSensorReading(
    val values: List<Float>,
    val accuracy: Int,
    val timestampNs: Long
)
