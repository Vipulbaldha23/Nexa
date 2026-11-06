package com.example.data.model

enum class VideoSortOrder(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DURATION_DESC("Longest First"),
    SIZE_DESC("Largest First")
}

enum class VideoResizeMode(val displayName: String) {
    FIT("Fit Screen"),
    ZOOM("Crop & Fill"),
    STRETCH("Stretch"),
    FIXED_16_9("16:9 Cinema")
}

enum class NexaAccent(val displayName: String, val hexColor: Long) {
    CYAN("Neon Cyan", 0xFF00E5FF),
    VIOLET("Electric Violet", 0xFF9D4EDD),
    SAPPHIRE("Hyper Blue", 0xFF2979FF),
    EMERALD("Cyber Emerald", 0xFF00F5D4)
}

data class PlayerSettings(
    val defaultPlaybackSpeed: Float = 1.0f,
    val doubleTapSeekSeconds: Int = 10,
    val autoPlayNext: Boolean = true,
    val loopMode: Boolean = false,
    val resizeMode: VideoResizeMode = VideoResizeMode.FIT,
    val themeAccent: NexaAccent = NexaAccent.CYAN,
    val amoledMode: Boolean = true,
    val pipEnabled: Boolean = true,
    val rememberPlaybackPosition: Boolean = true
)
