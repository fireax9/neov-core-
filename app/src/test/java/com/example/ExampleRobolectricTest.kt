package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.HardwareRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Neov Core`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Neov Core", appName)
    }

    @Test
    fun `hardware repository reads system cpu and memory telemetry safely`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = HardwareRepository(context)

        val cpu = repo.readCpuInfo()
        assertTrue(cpu.totalCores >= 1)
        assertTrue(cpu.cores.size == cpu.totalCores)

        val sys = repo.readSystemInfo()
        assertTrue(sys.apiLevel > 0)

        val mem = repo.readMemoryInfo()
        assertTrue(mem.totalRamBytes > 0L)
    }
}
