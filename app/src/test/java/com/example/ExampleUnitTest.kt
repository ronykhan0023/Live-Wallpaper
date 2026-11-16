package com.example

import com.example.domain.model.SpeedCategory
import com.example.domain.model.SpeedPresets
import com.example.util.WallpaperHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `speed presets contain all required values`() {
        assertEquals(
            listOf(0.25f, 0.50f, 0.75f, 1.00f, 1.25f, 1.50f, 2.00f, 2.50f, 3.00f),
            SpeedPresets.values
        )
        assertEquals(SpeedCategory.SLOW_MOTION, SpeedPresets.classify(0.25f))
        assertEquals(SpeedCategory.NORMAL, SpeedPresets.classify(1.00f))
        assertEquals(SpeedCategory.FAST_MOTION, SpeedPresets.classify(3.00f))
    }

    @Test
    fun `file size and duration formatting`() {
        assertEquals("0 MB", WallpaperHelper.formatFileSize(0L))
        assertEquals("1.0 MB", WallpaperHelper.formatFileSize(1024L * 1024L))
        assertEquals("1:05", WallpaperHelper.formatDuration(65_000L))
        assertEquals("1:01:05", WallpaperHelper.formatDuration(3_665_000L))
    }
}
