package com.example.data.repository

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.example.data.model.BatteryChargeStatus
import com.example.data.model.BatteryHealthState
import com.example.data.model.BatteryInfo
import com.example.data.model.BatteryPowerSource
import com.example.data.model.BatteryThermalLevel
import com.example.data.model.CpuClusterSummary
import com.example.data.model.CpuCoreInfo
import com.example.data.model.CpuInfo
import com.example.data.model.LiveSensorReading
import com.example.data.model.MemoryInfo
import com.example.data.model.SensorCategory
import com.example.data.model.SensorItem
import com.example.data.model.SensorReportingMode
import com.example.data.model.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class HardwareRepository(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager

    private data class CpuTimeSnapshot(val total: Long, val idle: Long)
    private val prevCpuTimes = mutableMapOf<Int, CpuTimeSnapshot>()

    suspend fun readCpuInfo(): CpuInfo = withContext(Dispatchers.IO) {
        val totalCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val procCpuInfo = parseProcCpuInfo()
        val coreLoads = readProcStatLoads()
        val loadAvg = readStringFile("/proc/loadavg")
            ?.split(Regex("\\s+"))
            ?.take(3)
            ?.joinToString(" / ")

        val cores = (0 until totalCores).map { idx ->
            val baseDir = "/sys/devices/system/cpu/cpu$idx"
            val policyDir = "/sys/devices/system/cpu/cpufreq/policy$idx"
            val isOnline = if (idx == 0) {
                true
            } else {
                readLongFile("$baseDir/online")?.let { it == 1L } ?: true
            }
            val sysfsCur = (readLongFile("$baseDir/cpufreq/scaling_cur_freq")
                ?: readLongFile("$policyDir/scaling_cur_freq"))
                ?.takeIf { it > 0L }
            val procMhz = procCpuInfo.perCoreMhz[idx]?.takeIf { it > 0.0 }?.let { (it * 1000.0).toLong() }
            val currentFreq = sysfsCur ?: procMhz
            val minFreq = (readLongFile("$baseDir/cpufreq/cpuinfo_min_freq")
                ?: readLongFile("$baseDir/cpufreq/scaling_min_freq")
                ?: readLongFile("$policyDir/cpuinfo_min_freq")
                ?: readLongFile("$policyDir/scaling_min_freq"))
                ?.takeIf { it > 0L }
            val maxFreq = (readLongFile("$baseDir/cpufreq/cpuinfo_max_freq")
                ?: readLongFile("$baseDir/cpufreq/scaling_max_freq")
                ?: readLongFile("$policyDir/cpuinfo_max_freq")
                ?: readLongFile("$policyDir/scaling_max_freq"))
                ?.takeIf { it > 0L }
            val governor = readStringFile("$baseDir/cpufreq/scaling_governor")
                ?: readStringFile("$policyDir/scaling_governor")

            CpuCoreInfo(
                coreIndex = idx,
                currentFreqKhz = currentFreq,
                minFreqKhz = minFreq,
                maxFreqKhz = maxFreq,
                isOnline = isOnline,
                governor = governor,
                loadPercent = coreLoads[idx]
            )
        }

        val activeFreqs = cores.filter { it.isOnline }.mapNotNull { it.currentFreqKhz }
        val avgFreq = if (activeFreqs.isNotEmpty()) activeFreqs.average().toLong() else null
        val peakFreq = activeFreqs.maxOrNull() ?: cores.mapNotNull { it.maxFreqKhz }.maxOrNull()
        val freqRestricted = cores.all { it.currentFreqKhz == null }
        val overallLoad = coreLoads[-1] ?: coreLoads.values.takeIf { it.isNotEmpty() }?.average()?.toFloat()

        val clusters = cores
            .mapNotNull { it.maxFreqKhz }
            .groupBy { it }
            .entries
            .sortedByDescending { it.key }
            .mapIndexed { index, entry ->
                val tier = when {
                    entry.key >= 2_500_000L -> "Prime / Performance"
                    entry.key >= 1_900_000L -> "Performance"
                    else -> "Efficiency"
                }
                CpuClusterSummary(
                    label = if (index == 0 && entry.value.size < totalCores) tier else "Cluster ${index + 1}",
                    maxFreqKhz = entry.key,
                    coreCount = entry.value.size
                )
            }

        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else {
            null
        } ?: procCpuInfo.hardware
            ?: procCpuInfo.modelName
            ?: Build.HARDWARE.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
            ?: Build.BOARD

        val socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.takeIf { it.isNotBlank() && it != Build.UNKNOWN }
        } else {
            null
        } ?: Build.MANUFACTURER

        val abis = Build.SUPPORTED_ABIS.toList()
        val is64Bit = abis.any { it.contains("64") } ||
            (System.getProperty("os.arch")?.contains("64") == true)

        val primaryGovernor = cores.firstNotNullOfOrNull { it.governor } ?: "schedutil"

        CpuInfo(
            socModel = socModel,
            socManufacturer = socManufacturer,
            hardware = Build.HARDWARE,
            architecture = System.getProperty("os.arch") ?: abis.firstOrNull().orEmpty(),
            supportedAbis = abis,
            is64Bit = is64Bit,
            totalCores = totalCores,
            onlineCores = cores.count { it.isOnline },
            governor = primaryGovernor,
            cores = cores,
            clusters = clusters,
            averageClockKhz = avgFreq,
            peakClockKhz = peakFreq,
            cpuLoadPercent = overallLoad,
            loadAverage = loadAvg,
            bogoMips = procCpuInfo.bogoMips,
            features = procCpuInfo.features,
            freqRestricted = freqRestricted
        )
    }

    suspend fun readSystemInfo(): SystemInfo = withContext(Dispatchers.IO) {
        val defaultDisplay: Display? = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val displayContext = defaultDisplay?.let { context.createDisplayContext(it) } ?: context
        val displayMetrics = DisplayMetrics().apply {
            setTo(displayContext.resources.displayMetrics)
        }

        val (widthPx, heightPx) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val displayWm = displayContext.getSystemService(WindowManager::class.java)
            val bounds = runCatching { displayWm?.maximumWindowMetrics?.bounds }.getOrNull()
            if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
                bounds.width() to bounds.height()
            } else {
                val mode = defaultDisplay?.mode
                (mode?.physicalWidth ?: displayMetrics.widthPixels) to
                    (mode?.physicalHeight ?: displayMetrics.heightPixels)
            }
        } else {
            @Suppress("DEPRECATION")
            defaultDisplay?.getRealMetrics(displayMetrics)
            displayMetrics.widthPixels to displayMetrics.heightPixels
        }

        val xdpi = displayMetrics.xdpi.takeIf { it > 0f } ?: displayMetrics.densityDpi.toFloat()
        val ydpi = displayMetrics.ydpi.takeIf { it > 0f } ?: displayMetrics.densityDpi.toFloat()
        val screenInches = if (widthPx > 0 && heightPx > 0 && xdpi > 0f && ydpi > 0f) {
            sqrt((widthPx / xdpi).toDouble().pow(2.0) + (heightPx / ydpi).toDouble().pow(2.0))
        } else {
            0.0
        }

        val densityBucket = when {
            displayMetrics.densityDpi <= DisplayMetrics.DENSITY_LOW -> "ldpi"
            displayMetrics.densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> "mdpi"
            displayMetrics.densityDpi <= DisplayMetrics.DENSITY_HIGH -> "hdpi"
            displayMetrics.densityDpi <= DisplayMetrics.DENSITY_XHIGH -> "xhdpi"
            displayMetrics.densityDpi <= DisplayMetrics.DENSITY_XXHIGH -> "xxhdpi"
            else -> "xxxhdpi"
        }

        val refreshRate = defaultDisplay?.refreshRate ?: 60f
        val isHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultDisplay?.isHdr == true
        } else {
            false
        }
        val isWideColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultDisplay?.isWideColorGamut == true
        } else {
            false
        }

        val kernelRaw = readStringFile("/proc/version")
            ?.substringAfter("Linux version ", "")
            ?.substringBefore(" (")
            ?.takeIf { it.isNotBlank() }
            ?: System.getProperty("os.version")
            ?: Build.DISPLAY

        SystemInfo(
            brand = Build.BRAND.replaceFirstChar { it.uppercase() },
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            board = Build.BOARD,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            buildId = Build.DISPLAY,
            kernelVersion = kernelRaw,
            vmVersion = System.getProperty("java.vm.version") ?: "ART",
            bootloader = Build.BOOTLOADER.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: "-",
            radioVersion = Build.getRadioVersion()?.takeIf { it.isNotBlank() } ?: "-",
            widthPx = widthPx,
            heightPx = heightPx,
            densityDpi = displayMetrics.densityDpi,
            densityScale = displayMetrics.density,
            densityBucket = densityBucket,
            screenInches = screenInches,
            refreshRateHz = refreshRate,
            isHdrSupported = isHdr,
            isWideColorGamut = isWideColor,
            elapsedRealtimeMs = SystemClock.elapsedRealtime(),
            uptimeMs = SystemClock.uptimeMillis()
        )
    }

    suspend fun readMemoryInfo(): MemoryInfo = withContext(Dispatchers.IO) {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRam = memInfo.totalMem.coerceAtLeast(1L)
        val availRam = memInfo.availMem.coerceIn(0L, totalRam)
        val usedRam = (totalRam - availRam).coerceAtLeast(0L)
        val ramPercent = ((usedRam.toDouble() / totalRam.toDouble()) * 100.0).toFloat()

        val procMem = parseProcMemInfo()

        val dataStats = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val dataTotal = dataStats?.totalBytes ?: 0L
        val dataFree = dataStats?.availableBytes ?: 0L
        val dataUsed = (dataTotal - dataFree).coerceAtLeast(0L)

        val sysStats = runCatching { StatFs(Environment.getRootDirectory().path) }.getOrNull()
        val sysTotal = sysStats?.totalBytes ?: 0L
        val sysFree = sysStats?.availableBytes ?: 0L
        val sysUsed = (sysTotal - sysFree).coerceAtLeast(0L)

        MemoryInfo(
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            usedRamBytes = usedRam,
            lowMemoryThresholdBytes = memInfo.threshold,
            isLowMemory = memInfo.lowMemory,
            ramUsagePercent = ramPercent,
            cachedBytes = procMem["Cached"],
            swapTotalBytes = procMem["SwapTotal"],
            swapFreeBytes = procMem["SwapFree"],
            dataTotalBytes = dataTotal,
            dataFreeBytes = dataFree,
            dataUsedBytes = dataUsed,
            systemTotalBytes = sysTotal,
            systemFreeBytes = sysFree,
            systemUsedBytes = sysUsed
        )
    }

    fun observeBatteryInfo(): Flow<BatteryInfo> = callbackFlow {
        fun buildBatteryInfo(intent: Intent?): BatteryInfo {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val levelPct = if (level >= 0 && scale > 0) {
                ((level * 100f) / scale).toInt().coerceIn(0, 100)
            } else {
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    ?.coerceIn(0, 100) ?: 0
            }

            val status = when (intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargeStatus.CHARGING
                BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
                BatteryManager.BATTERY_STATUS_FULL -> BatteryChargeStatus.FULL
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
                else -> BatteryChargeStatus.UNKNOWN
            }

            val source = when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> BatteryPowerSource.AC
                BatteryManager.BATTERY_PLUGGED_USB -> BatteryPowerSource.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPowerSource.WIRELESS
                else -> BatteryPowerSource.BATTERY
            }

            val tempRaw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempRaw / 10f
            val thermalLevel = when {
                tempC < 20f -> BatteryThermalLevel.COOL
                tempC < 36f -> BatteryThermalLevel.NORMAL
                tempC < 43f -> BatteryThermalLevel.WARM
                else -> BatteryThermalLevel.HOT
            }

            val voltageRaw = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val voltageMv = if (voltageRaw in 1..99) voltageRaw * 1000 else voltageRaw

            val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthState.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthState.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthState.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthState.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthState.COLD
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthState.FAILURE
                else -> BatteryHealthState.UNKNOWN
            }

            val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
                ?.takeIf { it.isNotBlank() } ?: "Li-ion"

            val rawCurrentUa = batteryManager
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                ?.takeIf { it != Int.MIN_VALUE && it != 0 }

            val currentMa = rawCurrentUa?.let { raw ->
                if (abs(raw) >= 10_000) raw / 1000f else raw.toFloat()
            }

            val powerWatts = if (currentMa != null && voltageMv > 0) {
                (abs(currentMa) / 1000f) * (voltageMv / 1000f)
            } else {
                null
            }

            val chargeCounterUah = batteryManager
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                ?.takeIf { it != Int.MIN_VALUE && it > 0 }
            val chargeCounterMah = chargeCounterUah?.let { it / 1000 }

            return BatteryInfo(
                levelPercent = levelPct,
                status = status,
                powerSource = source,
                temperatureCelsius = tempC,
                thermalLevel = thermalLevel,
                voltageMv = voltageMv,
                health = health,
                technology = tech,
                currentMa = currentMa,
                powerWatts = powerWatts,
                chargeCounterMah = chargeCounterMah
            )
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(buildBatteryInfo(intent))
            }
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        trySend(buildBatteryInfo(stickyIntent))

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fun readCurrentBatterySnapshot(): BatteryInfo? {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return null
        val tempC = (intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)) / 10f
        val voltageRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageMv = if (voltageRaw in 1..99) voltageRaw * 1000 else voltageRaw
        val rawCurrentUa = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Int.MIN_VALUE && it != 0 }
        val currentMa = rawCurrentUa?.let { raw ->
            if (abs(raw) >= 10_000) raw / 1000f else raw.toFloat()
        }
        val powerWatts = if (currentMa != null && voltageMv > 0) {
            (abs(currentMa) / 1000f) * (voltageMv / 1000f)
        } else {
            null
        }
        val chargeCounterMah = batteryManager
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it != Int.MIN_VALUE && it > 0 }
            ?.let { it / 1000 }

        return BatteryInfo(
            levelPercent = ((level * 100f) / scale).toInt().coerceIn(0, 100),
            status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargeStatus.CHARGING
                BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargeStatus.DISCHARGING
                BatteryManager.BATTERY_STATUS_FULL -> BatteryChargeStatus.FULL
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargeStatus.NOT_CHARGING
                else -> BatteryChargeStatus.UNKNOWN
            },
            powerSource = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
                BatteryManager.BATTERY_PLUGGED_AC -> BatteryPowerSource.AC
                BatteryManager.BATTERY_PLUGGED_USB -> BatteryPowerSource.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPowerSource.WIRELESS
                else -> BatteryPowerSource.BATTERY
            },
            temperatureCelsius = tempC,
            thermalLevel = when {
                tempC < 20f -> BatteryThermalLevel.COOL
                tempC < 36f -> BatteryThermalLevel.NORMAL
                tempC < 43f -> BatteryThermalLevel.WARM
                else -> BatteryThermalLevel.HOT
            },
            voltageMv = voltageMv,
            health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthState.GOOD
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthState.OVERHEAT
                BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthState.DEAD
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthState.OVER_VOLTAGE
                BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthState.COLD
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthState.FAILURE
                else -> BatteryHealthState.UNKNOWN
            },
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
                ?.takeIf { it.isNotBlank() } ?: "Li-ion",
            currentMa = currentMa,
            powerWatts = powerWatts,
            chargeCounterMah = chargeCounterMah
        )
    }

    fun getAvailableSensors(): List<SensorItem> {
        val rawSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return rawSensors.mapIndexed { index, sensor ->
            val meta = describeSensor(sensor.type)
            val mode = when (sensor.reportingMode) {
                Sensor.REPORTING_MODE_ON_CHANGE -> SensorReportingMode.ON_CHANGE
                Sensor.REPORTING_MODE_ONE_SHOT -> SensorReportingMode.ONE_SHOT
                Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> SensorReportingMode.SPECIAL_TRIGGER
                else -> SensorReportingMode.CONTINUOUS
            }
            SensorItem(
                id = sensorId(sensor, index),
                name = sensor.name,
                vendor = sensor.vendor,
                type = sensor.type,
                friendlyTypeArabic = meta.arabicName,
                category = meta.category,
                powerMa = sensor.power,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                minDelayUs = sensor.minDelay,
                reportingMode = mode,
                valueLabels = meta.axisLabels,
                unit = meta.unit
            )
        }
    }

    fun observeSensorReadings(activeSensorIds: Set<Int>): Flow<Map<Int, LiveSensorReading>> =
        callbackFlow {
            val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
            val idToSensor = allSensors.mapIndexed { index, sensor ->
                sensorId(sensor, index) to sensor
            }.toMap()

            val targetSensors = activeSensorIds.mapNotNull { id ->
                idToSensor[id]?.let { id to it }
            }

            if (targetSensors.isEmpty()) {
                trySend(emptyMap())
                awaitClose { }
                return@callbackFlow
            }

            val latestMap = mutableMapOf<Int, LiveSensorReading>()
            val sensorToId = targetSensors.associate { (id, sensor) -> sensor to id }

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val id = sensorToId[event.sensor] ?: return
                    synchronized(latestMap) {
                        latestMap[id] = LiveSensorReading(
                            values = event.values.toList(),
                            accuracy = event.accuracy,
                            timestampNs = event.timestamp
                        )
                        trySend(latestMap.toMap())
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
            }

            targetSensors.forEach { (_, sensor) ->
                runCatching {
                    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }

    private fun sensorId(sensor: Sensor, index: Int): Int {
        return index
    }

    private data class SensorMetadata(
        val arabicName: String,
        val category: SensorCategory,
        val axisLabels: List<String>,
        val unit: String
    )

    @Suppress("DEPRECATION")
    private fun describeSensor(type: Int): SensorMetadata {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> SensorMetadata(
                "مقياس التسارع",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z"),
                "m/s²"
            )
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> SensorMetadata(
                "مقياس التسارع (غير معاير)",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z", "ΔX", "ΔY", "ΔZ"),
                "m/s²"
            )
            Sensor.TYPE_GYROSCOPE -> SensorMetadata(
                "الجيروسكوب",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z"),
                "rad/s"
            )
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorMetadata(
                "الجيروسكوب (غير معاير)",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z"),
                "rad/s"
            )
            Sensor.TYPE_GRAVITY -> SensorMetadata(
                "مستشعر الجاذبية",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z"),
                "m/s²"
            )
            Sensor.TYPE_LINEAR_ACCELERATION -> SensorMetadata(
                "التسارع الخطي",
                SensorCategory.MOTION,
                listOf("X", "Y", "Z"),
                "m/s²"
            )
            Sensor.TYPE_ROTATION_VECTOR -> SensorMetadata(
                "متجه الدوران",
                SensorCategory.POSITION,
                listOf("X", "Y", "Z", "Scalar"),
                ""
            )
            Sensor.TYPE_GAME_ROTATION_VECTOR -> SensorMetadata(
                "متجه دوران الألعاب",
                SensorCategory.POSITION,
                listOf("X", "Y", "Z"),
                ""
            )
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorMetadata(
                "متجه الدوران المغناطيسي",
                SensorCategory.POSITION,
                listOf("X", "Y", "Z"),
                ""
            )
            Sensor.TYPE_MAGNETIC_FIELD -> SensorMetadata(
                "المقياس المغناطيسي (البوصلة)",
                SensorCategory.POSITION,
                listOf("X", "Y", "Z"),
                "μT"
            )
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorMetadata(
                "المجال المغناطيسي (غير معاير)",
                SensorCategory.POSITION,
                listOf("X", "Y", "Z"),
                "μT"
            )
            Sensor.TYPE_ORIENTATION -> SensorMetadata(
                "مستشعر الاتجاه",
                SensorCategory.POSITION,
                listOf("Azimuth", "Pitch", "Roll"),
                "°"
            )
            Sensor.TYPE_PROXIMITY -> SensorMetadata(
                "مستشعر التقارب",
                SensorCategory.POSITION,
                listOf("المسافة"),
                "cm"
            )
            Sensor.TYPE_LIGHT -> SensorMetadata(
                "مستشعر الإضاءة المحيطة",
                SensorCategory.ENVIRONMENT,
                listOf("السطوع"),
                "lx"
            )
            Sensor.TYPE_PRESSURE -> SensorMetadata(
                "مقياس الضغط الجوي",
                SensorCategory.ENVIRONMENT,
                listOf("الضغط"),
                "hPa"
            )
            Sensor.TYPE_AMBIENT_TEMPERATURE -> SensorMetadata(
                "درجة الحرارة المحيطة",
                SensorCategory.ENVIRONMENT,
                listOf("الحرارة"),
                "°C"
            )
            Sensor.TYPE_RELATIVE_HUMIDITY -> SensorMetadata(
                "الرطوبة النسبية",
                SensorCategory.ENVIRONMENT,
                listOf("الرطوبة"),
                "%"
            )
            Sensor.TYPE_STEP_COUNTER -> SensorMetadata(
                "عداد الخطوات",
                SensorCategory.MOTION,
                listOf("الخطوات"),
                "steps"
            )
            Sensor.TYPE_STEP_DETECTOR -> SensorMetadata(
                "كاشف الخطوات",
                SensorCategory.MOTION,
                listOf("نبضة"),
                ""
            )
            Sensor.TYPE_SIGNIFICANT_MOTION -> SensorMetadata(
                "الحركة الملحوظة",
                SensorCategory.MOTION,
                listOf("حدث"),
                ""
            )
            Sensor.TYPE_HEART_RATE -> SensorMetadata(
                "معدل نبض القلب",
                SensorCategory.OTHER,
                listOf("النبض"),
                "bpm"
            )
            Sensor.TYPE_HINGE_ANGLE -> SensorMetadata(
                "زاوية المفصلة",
                SensorCategory.POSITION,
                listOf("الزاوية"),
                "°"
            )
            else -> SensorMetadata(
                "مستشعر نظام (#$type)",
                SensorCategory.OTHER,
                listOf("V1", "V2", "V3"),
                ""
            )
        }
    }

    private data class ProcCpuParsed(
        val hardware: String? = null,
        val modelName: String? = null,
        val bogoMips: String? = null,
        val features: List<String> = emptyList(),
        val perCoreMhz: Map<Int, Double> = emptyMap()
    )

    private fun parseProcCpuInfo(): ProcCpuParsed {
        return runCatching {
            val file = File("/proc/cpuinfo")
            if (!file.exists() || !file.canRead()) return ProcCpuParsed()
            var hardware: String? = null
            var modelName: String? = null
            var bogoMips: String? = null
            var features: List<String> = emptyList()
            val perCoreMhz = mutableMapOf<Int, Double>()
            var currentProcessor = -1

            file.forEachLine { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) return@forEachLine
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                when (key.lowercase()) {
                    "processor" -> currentProcessor = value.toIntOrNull() ?: currentProcessor
                    "hardware" -> if (hardware == null && value.isNotBlank()) hardware = value
                    "model name" -> if (modelName == null && value.isNotBlank()) modelName = value
                    "bogomips" -> if (bogoMips == null && value.isNotBlank()) bogoMips = value
                    "features", "flags" -> if (features.isEmpty() && value.isNotBlank()) {
                        features = value.split(Regex("\\s+")).filter { it.isNotBlank() }.take(12)
                    }
                    "cpu mhz" -> {
                        val mhz = value.toDoubleOrNull()
                        if (mhz != null && currentProcessor >= 0) {
                            perCoreMhz[currentProcessor] = mhz
                        }
                    }
                }
            }
            ProcCpuParsed(hardware, modelName, bogoMips, features, perCoreMhz)
        }.getOrDefault(ProcCpuParsed())
    }

    private fun readProcStatLoads(): Map<Int, Float> {
        return runCatching {
            val file = File("/proc/stat")
            if (!file.exists() || !file.canRead()) return emptyMap()
            val loads = mutableMapOf<Int, Float>()
            file.forEachLine { line ->
                if (!line.startsWith("cpu")) return@forEachLine
                val tokens = line.trim().split(Regex("\\s+"))
                if (tokens.size < 5) return@forEachLine
                val keyStr = tokens[0]
                val coreIdx = if (keyStr == "cpu") {
                    -1
                } else {
                    keyStr.removePrefix("cpu").toIntOrNull() ?: return@forEachLine
                }
                val values = tokens.drop(1).mapNotNull { it.toLongOrNull() }
                if (values.size < 4) return@forEachLine
                val idle = values[3] + values.getOrElse(4) { 0L }
                val total = values.sum()
                val prev = prevCpuTimes[coreIdx]
                prevCpuTimes[coreIdx] = CpuTimeSnapshot(total = total, idle = idle)
                if (prev != null) {
                    val totalDelta = total - prev.total
                    val idleDelta = idle - prev.idle
                    if (totalDelta > 0L) {
                        val active = (totalDelta - idleDelta).coerceAtLeast(0L)
                        loads[coreIdx] = ((active.toFloat() / totalDelta.toFloat()) * 100f)
                            .coerceIn(0f, 100f)
                    }
                }
            }
            loads
        }.getOrDefault(emptyMap())
    }

    private fun parseProcMemInfo(): Map<String, Long> {
        return runCatching {
            val file = File("/proc/meminfo")
            if (!file.exists() || !file.canRead()) return emptyMap()
            val result = mutableMapOf<String, Long>()
            file.forEachLine { line ->
                val parts = line.split(':')
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val kbStr = parts[1].trim().substringBefore(" ")
                    val kb = kbStr.toLongOrNull()
                    if (kb != null) {
                        result[key] = kb * 1024L
                    }
                }
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun readLongFile(path: String): Long? {
        return runCatching {
            val f = File(path)
            if (f.exists() && f.canRead()) f.readText().trim().toLongOrNull() else null
        }.getOrNull()
    }

    private fun readStringFile(path: String): String? {
        return runCatching {
            val f = File(path)
            if (f.exists() && f.canRead()) f.readText().trim().takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }
}
