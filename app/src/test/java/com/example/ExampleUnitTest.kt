package com.example

import com.example.data.model.CpuCoreInfo
import com.example.ui.components.TelemetryFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `formatFreqKhz formats kHz MHz and GHz accurately`() {
        assertEquals("—", TelemetryFormatter.formatFreqKhz(null))
        assertEquals("—", TelemetryFormatter.formatFreqKhz(0L))
        assertEquals("500 kHz", TelemetryFormatter.formatFreqKhz(500L))
        assertEquals("800 MHz", TelemetryFormatter.formatFreqKhz(800_000L))
        assertEquals("2.40 GHz", TelemetryFormatter.formatFreqKhz(2_400_000L))
    }

    @Test
    fun `formatBytes formats KB MB and GB accurately`() {
        assertEquals("512 B", TelemetryFormatter.formatBytes(512L))
        assertEquals("2.0 MB", TelemetryFormatter.formatBytes(2L * 1024L * 1024L))
        assertEquals("4.00 GB", TelemetryFormatter.formatBytes(4L * 1024L * 1024L * 1024L))
    }

    @Test
    fun `formatUptime formats days and hours cleanly`() {
        assertEquals("01:02:03", TelemetryFormatter.formatUptime((3600L + 120L + 3L) * 1000L))
        assertEquals("1d 02:00:00", TelemetryFormatter.formatUptime((26L * 3600L) * 1000L))
    }

    @Test
    fun `cpu core utilization ratio clamps within expected bounds`() {
        val core = CpuCoreInfo(
            coreIndex = 0,
            currentFreqKhz = 1_500_000L,
            minFreqKhz = 500_000L,
            maxFreqKhz = 2_500_000L,
            isOnline = true,
            governor = "schedutil"
        )
        assertEquals(0.5f, core.utilizationRatio, 0.01f)

        val offlineCore = core.copy(isOnline = false)
        assertEquals(0f, offlineCore.utilizationRatio, 0.001f)
    }
}
