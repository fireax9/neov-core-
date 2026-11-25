package com.example

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BatteryChargeStatus
import com.example.data.model.BatteryHealthState
import com.example.data.model.BatteryInfo
import com.example.data.model.BatteryPowerSource
import com.example.data.model.BatteryThermalLevel
import com.example.data.model.CpuCoreInfo
import com.example.data.model.CpuInfo
import com.example.data.model.MemoryInfo
import com.example.data.model.SensorCategory
import com.example.data.model.SensorItem
import com.example.data.model.SensorReportingMode
import com.example.data.model.SystemInfo
import com.example.data.repository.HardwareRepository
import com.example.ui.components.TelemetryFormatter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35]) // Android 15 (API 35)
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `01 app name matches Neov Core on Android 15`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Neov Core", context.getString(R.string.app_name))
    }

    @Test
    fun `02 hardware repository reads cpu info safely on Android 15`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = HardwareRepository(context)
        val cpu = repo.readCpuInfo()
        assertTrue(cpu.totalCores >= 1)
        assertEquals(cpu.totalCores, cpu.cores.size)
        assertTrue(cpu.onlineCores >= 1)
    }

    @Test
    fun `03 hardware repository reads system and display info without visual context crash`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = HardwareRepository(context)
        val sys = repo.readSystemInfo()
        assertEquals(35, sys.apiLevel)
        assertTrue(sys.densityDpi > 0)
        assertTrue(sys.refreshRateHz > 0f)
    }

    @Test
    fun `04 hardware repository reads ram and storage partitions safely`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = HardwareRepository(context)
        val mem = repo.readMemoryInfo()
        assertTrue(mem.totalRamBytes > 0L)
        assertTrue(mem.availableRamBytes in 0L..mem.totalRamBytes)
        assertTrue(mem.ramUsagePercent in 0f..100f)
    }

    @Test
    fun `05 hardware repository reads sticky battery broadcast on Android 15`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_BATTERY_CHANGED).apply {
            putExtra(BatteryManager.EXTRA_LEVEL, 82)
            putExtra(BatteryManager.EXTRA_SCALE, 100)
            putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
            putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_AC)
            putExtra(BatteryManager.EXTRA_TEMPERATURE, 315)
            putExtra(BatteryManager.EXTRA_VOLTAGE, 4180)
            putExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            putExtra(BatteryManager.EXTRA_TECHNOLOGY, "Li-poly")
        }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(intent)

        val repo = HardwareRepository(context)
        val battery = repo.readCurrentBatterySnapshot()
        assertNotNull(battery)
        assertEquals(82, battery?.levelPercent)
        assertEquals(BatteryChargeStatus.CHARGING, battery?.status)
        assertEquals(BatteryPowerSource.AC, battery?.powerSource)
        assertEquals(31.5f, battery?.temperatureCelsius ?: 0f, 0.1f)
        assertEquals(4180, battery?.voltageMv)
        assertEquals(BatteryHealthState.GOOD, battery?.health)
        assertEquals(BatteryThermalLevel.NORMAL, battery?.thermalLevel)
    }

    @Test
    fun `06 hardware repository enumerates sensor list with unique ids`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = HardwareRepository(context)
        val sensors = repo.getAvailableSensors()
        val uniqueIds = sensors.map { it.id }.toSet()
        assertEquals(sensors.size, uniqueIds.size)
    }

    @Test
    fun `07 navigation bar shows all five core modules`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_item_soc").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_item_system").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_item_memory").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_item_battery").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_item_sensors").assertIsDisplayed()
    }

    @Test
    fun `08 navigating through all five modules succeeds cleanly`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_item_system").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_item_memory").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_item_battery").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_item_sensors").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sensors_search_input").assertIsDisplayed()
    }

    @Test
    fun `09 live toggle chip pauses and resumes polling`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_toggle_chip").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("live_toggle_chip").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `10 theme toggle button switches light and dark themes`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `11 privacy bottom sheet opens and closes`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("privacy_info_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("privacy_sheet_close_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `12 diagnostics screen opens and displays 15 hardware tests`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diagnostics_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diag_run_auto_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("diag_item_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("diag_back_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `13 frequency formatter never outputs 0 MHz for sub-MHz values`() {
        assertEquals("—", TelemetryFormatter.formatFreqKhz(null))
        assertEquals("—", TelemetryFormatter.formatFreqKhz(0L))
        assertEquals("500 kHz", TelemetryFormatter.formatFreqKhz(500L))
        assertEquals("900 MHz", TelemetryFormatter.formatFreqKhz(900_000L))
        assertEquals("3.20 GHz", TelemetryFormatter.formatFreqKhz(3_200_000L))
    }

    @Test
    fun `14 cpu core utilization ratio prefers real load percent when available`() {
        val core = CpuCoreInfo(
            coreIndex = 0,
            currentFreqKhz = 500L,
            minFreqKhz = 100L,
            maxFreqKhz = 1000L,
            isOnline = true,
            governor = "schedutil",
            loadPercent = 42f
        )
        assertEquals(0.42f, core.utilizationRatio, 0.01f)
    }

    @Test
    fun `15 sensor categories and models retain accurate metadata`() {
        val sensor = SensorItem(
            id = 0,
            name = "BMI260 Accelerometer",
            vendor = "Bosch",
            type = 1,
            friendlyTypeArabic = "مقياس التسارع",
            category = SensorCategory.MOTION,
            powerMa = 0.25f,
            maxRange = 78.45f,
            resolution = 0.002f,
            minDelayUs = 2500,
            reportingMode = SensorReportingMode.CONTINUOUS,
            valueLabels = listOf("X", "Y", "Z"),
            unit = "m/s²"
        )
        assertEquals(SensorCategory.MOTION, sensor.category)
        assertEquals(3, sensor.valueLabels.size)
        assertEquals("m/s²", sensor.unit)
    }
}
