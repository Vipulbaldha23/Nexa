package com.example

import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testVideoItemFormatting() {
        val video = VideoItem(
            id = 1L,
            uriString = "content://media/external/video/media/1",
            title = "Quantum Horizon",
            displayName = "quantum_horizon.mp4",
            durationMs = 3665000L, // 1h 1m 5s
            sizeBytes = 1073741824L * 2, // 2 GB
            width = 3840,
            height = 2160,
            dateAddedSec = 1700000000L,
            mimeType = "video/mp4",
            folderName = "Cinema",
            path = "/storage/emulated/0/Movies/quantum_horizon.mp4"
        )

        assertEquals("1:01:05", video.formattedDuration)
        assertEquals("2.0 GB", video.formattedSize)
        assertEquals("4K UHD", video.resolutionLabel)
    }
}
