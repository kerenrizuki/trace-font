package com.example.model

import androidx.compose.ui.graphics.Color

sealed class PatternStyle {
    abstract val thumbnailBgColor: Color
    abstract val displayName: String

    // Bubble/Starry/Confetti/PolkaDots styles etc.
    data class PolkaDots(
        override val thumbnailBgColor: Color = Color(0xFF673AB7), // Deep purple
        override val displayName: String = "Polka Dots",
        val dotsColor: List<Color> = listOf(Color(0xFFFFEB3B), Color(0xFF4CAF50), Color(0xFFFF5722), Color(0xFFE91E63))
    ) : PatternStyle()

    data class Confetti(
        override val thumbnailBgColor: Color = Color(0xFFFF5722), // Red Orange
        override val displayName: String = "Confetti",
        val shapeColors: List<Color> = listOf(Color(0xFF8BC34A), Color(0xFFFFC107), Color(0xFF00BCD4), Color(0xFFE91E63))
    ) : PatternStyle()

    data class StarrySky(
        override val thumbnailBgColor: Color = Color(0xFF3F51B5), // Indigo blue
        override val displayName: String = "Bintang",
        val starColor: Color = Color(0xFFFFEB3B)
    ) : PatternStyle()

    data class RainbowStripes(
        override val thumbnailBgColor: Color = Color(0xFFE91E63), // Dark Pink / Rainbow representation
        override val displayName: String = "Pelangi",
        val colors: List<Color> = listOf(
            Color(0xFFFF3D00), // Red
            Color(0xFFFFB300), // Orange
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF00E676), // Green
            Color(0xFF00B0FF), // Blue
            Color(0xFFD500F9)  // Violet
        )
    ) : PatternStyle()

    data class Bubbles(
        override val thumbnailBgColor: Color = Color(0xFF009688), // Teal
        override val displayName: String = "Gelembung",
        val bubbleColor: Color = Color(0x66FFFFFF) // Translucent white bubbles
    ) : PatternStyle()

    data class SolidNeon(
        override val thumbnailBgColor: Color = Color(0xFFFFEB3B), // Neon Yellow
        override val displayName: String = "Neon",
        val neonColor: Color = Color(0xFF22FF22)
    ) : PatternStyle()
}

val ALL_PATTERNS = listOf(
    PatternStyle.PolkaDots(),
    PatternStyle.Confetti(),
    PatternStyle.StarrySky(),
    PatternStyle.RainbowStripes(),
    PatternStyle.Bubbles(),
    PatternStyle.SolidNeon()
)
