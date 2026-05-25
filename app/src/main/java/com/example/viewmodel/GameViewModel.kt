package com.example.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class PaintedStroke(
    val points: List<Offset>,
    val pattern: PatternStyle
)

data class GameParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val shape: String // "circle", "star", "confetti"
)

data class GameUiState(
    val selectedChar: Char = 'B', // Start with letter 'B' just like in the video!
    val activePattern: PatternStyle = ALL_PATTERNS[0], // Start with PolkaDots
    val isUppercaseActive: Boolean = true,
    val activeStrokeIndex: Int = 0,
    val reachedPointsCount: Int = 0,
    val paintedStrokesUpper: List<PaintedStroke> = emptyList(),
    val paintedStrokesLower: List<PaintedStroke> = emptyList(),
    val isLetterCompleted: Boolean = false
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _particles = MutableStateFlow<List<GameParticle>>(emptyList())
    val particles: StateFlow<List<GameParticle>> = _particles.asStateFlow()

    private val random = Random()
    private var particleIdCounter = 0L

    init {
        loadLetter('B') // Initialize with B as seen in the video
    }

    fun loadLetter(char: Char) {
        _particles.value = emptyList()
        _uiState.update {
            it.copy(
                selectedChar = char,
                isUppercaseActive = true,
                activeStrokeIndex = 0,
                reachedPointsCount = 0,
                paintedStrokesUpper = emptyList(),
                paintedStrokesLower = emptyList(),
                isLetterCompleted = false
            )
        }
    }

    fun selectPattern(pattern: PatternStyle) {
        _uiState.update {
            it.copy(activePattern = pattern)
        }
    }

    fun nextLetter() {
        val curr = _uiState.value.selectedChar
        val next = if (curr == 'Z') 'A' else (curr + 1)
        loadLetter(next)
    }

    fun prevLetter() {
        val curr = _uiState.value.selectedChar
        val prev = if (curr == 'A') 'Z' else (curr - 1)
        loadLetter(prev)
    }

    fun resetCurrentLetter() {
        loadLetter(_uiState.value.selectedChar)
    }

    /**
     * Handles drag gestures. The canvas calls this with normalized drag coordinates (0.0 to 1.0).
     */
    fun onTraceDrag(normalizedOffset: Offset) {
        val state = _uiState.value
        if (state.isLetterCompleted) return

        val activeStrokes = getActiveStrokes()
        if (state.activeStrokeIndex >= activeStrokes.size) return

        val activeStroke = activeStrokes[state.activeStrokeIndex]
        val points = activeStroke.points
        if (points.isEmpty()) return

        val targetIdx = state.reachedPointsCount
        if (targetIdx >= points.size) return

        // Check distance to next target point
        val targetPoint = points[targetIdx]
        val dx = normalizedOffset.x - targetPoint.x
        val dy = normalizedOffset.y - targetPoint.y
        val distSq = dx * dx + dy * dy

        // If close enough (e.g., within ~0.15 normalized coordinates)
        if (distSq < 0.0225f) { // roughly 0.15 distance
            val nextReachedCount = targetIdx + 1
            triggerSparkParticle(targetPoint.x, targetPoint.y, state.activePattern)

            if (nextReachedCount >= points.size) {
                // Stroke complete! Store painted stroke with its pattern
                val currentPattern = state.activePattern
                val completeStroke = PaintedStroke(points, currentPattern)

                // Trigger huge splash at the end of the stroke
                triggerStrokeCompleteSplash(targetPoint.x, targetPoint.y, currentPattern)

                if (state.isUppercaseActive) {
                    val updatedUpper = state.paintedStrokesUpper + completeStroke
                    val andNextStroke = state.activeStrokeIndex + 1
                    val isUppercaseDone = andNextStroke >= activeStrokes.size

                    _uiState.update {
                        it.copy(
                            paintedStrokesUpper = updatedUpper,
                            reachedPointsCount = 0,
                            activeStrokeIndex = if (isUppercaseDone) 0 else andNextStroke,
                            isUppercaseActive = !isUppercaseDone
                        )
                    }
                } else {
                    val updatedLower = state.paintedStrokesLower + completeStroke
                    val andNextStroke = state.activeStrokeIndex + 1
                    val isLowercaseDone = andNextStroke >= activeStrokes.size

                    if (isLowercaseDone) {
                        // Letter completed!
                        triggerLetterCompleteCelebration()
                        _uiState.update {
                            it.copy(
                                paintedStrokesLower = updatedLower,
                                reachedPointsCount = 0,
                                activeStrokeIndex = andNextStroke,
                                isLetterCompleted = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                paintedStrokesLower = updatedLower,
                                reachedPointsCount = 0,
                                activeStrokeIndex = andNextStroke
                            )
                        }
                    }
                }
            } else {
                // Just advance the point
                _uiState.update {
                    it.copy(reachedPointsCount = nextReachedCount)
                }
            }
        }
    }

    private fun getActiveStrokes(): List<GameStroke> {
        val letter = LetterRepository.getLetter(_uiState.value.selectedChar)
        return if (_uiState.value.isUppercaseActive) {
            letter.uppercaseStrokes
        } else {
            letter.lowercaseStrokes
        }
    }

    // --- Particle System Logic ---

    fun updateParticles() {
        if (_particles.value.isEmpty()) return

        _particles.update { list ->
            list.mapNotNull { p ->
                val nextAlpha = p.alpha - 0.025f
                if (nextAlpha <= 0f) {
                    null
                } else {
                    p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy + 0.001f, // simple gravity
                        alpha = nextAlpha,
                        rotation = (p.rotation + p.rotationSpeed) % 360f
                    )
                }
            }
        }
    }

    private fun triggerSparkParticle(nx: Float, ny: Float, pattern: PatternStyle) {
        val color = when (pattern) {
            is PatternStyle.PolkaDots -> pattern.dotsColor[random.nextInt(pattern.dotsColor.size)]
            is PatternStyle.Confetti -> pattern.shapeColors[random.nextInt(pattern.shapeColors.size)]
            is PatternStyle.StarrySky -> pattern.starColor
            is PatternStyle.RainbowStripes -> pattern.colors[random.nextInt(pattern.colors.size)]
            is PatternStyle.Bubbles -> Color(0xFFE0F7FA)
            is PatternStyle.SolidNeon -> Color(0xFF00FFCC)
        }

        val newParticles = (1..3).map {
            val angle = random.nextDouble() * 2.0 * Math.PI
            val speed = 0.005f + random.nextFloat() * 0.008f
            GameParticle(
                id = ++particleIdCounter,
                x = nx,
                y = ny,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                color = color,
                size = 12f + random.nextFloat() * 15f,
                alpha = 1.0f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (-5..5).filter { it != 0 }.random().toFloat(),
                shape = listOf("circle", "star", "confetti").random()
            )
        }

        _particles.update {
            it + newParticles
        }
    }

    private fun triggerStrokeCompleteSplash(nx: Float, ny: Float, pattern: PatternStyle) {
        val colors = when (pattern) {
            is PatternStyle.PolkaDots -> pattern.dotsColor
            is PatternStyle.Confetti -> pattern.shapeColors
            is PatternStyle.StarrySky -> listOf(pattern.starColor, Color.White, Color(0xFFE1F5FE))
            is PatternStyle.RainbowStripes -> pattern.colors
            is PatternStyle.Bubbles -> listOf(Color(0xFF80DEEA), Color(0xFFB2EBF2), Color.White)
            is PatternStyle.SolidNeon -> listOf(pattern.neonColor, Color.White)
        }

        val newParticles = (1..20).map {
            val angle = random.nextDouble() * 2.0 * Math.PI
            val speed = 0.01f + random.nextFloat() * 0.015f
            val col = colors[random.nextInt(colors.size)]
            GameParticle(
                id = ++particleIdCounter,
                x = nx,
                y = ny,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                color = col,
                size = 15f + random.nextFloat() * 20f,
                alpha = 1.0f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (-10..10).filter { it != 0 }.random().toFloat(),
                shape = listOf("circle", "star", "confetti").random()
            )
        }

        _particles.update {
            it + newParticles
        }
    }

    private fun triggerLetterCompleteCelebration() {
        // Explode fireworks/confetti from multiple points!
        val colors = listOf(
            Color(0xFFFF3D00), Color(0xFFFFEB3B), Color(0xFF00E676),
            Color(0xFF00B0FF), Color(0xFFD500F9), Color(0xFFFF2A6D)
        )
        val celebrationParticles = mutableListOf<GameParticle>()

        // Generate explosions from 3 locations
        val spawnPoints = listOf(
            Offset(0.3f, 0.4f),
            Offset(0.7f, 0.4f),
            Offset(0.5f, 0.7f)
        )

        for (pt in spawnPoints) {
            for (i in 1..25) {
                val angle = random.nextDouble() * 2.0 * Math.PI
                val speed = 0.012f + random.nextFloat() * 0.02f
                val col = colors.random()
                celebrationParticles.add(
                    GameParticle(
                        id = ++particleIdCounter,
                        x = pt.x,
                        y = pt.y,
                        vx = (cos(angle) * speed).toFloat(),
                        vy = (sin(angle) * speed).toFloat(),
                        color = col,
                        size = 18f + random.nextFloat() * 25f,
                        alpha = 1.0f,
                        rotation = random.nextFloat() * 360f,
                        rotationSpeed = (-15..15).filter { it != 0 }.random().toFloat(),
                        shape = listOf("circle", "star", "confetti").random()
                    )
                )
            }
        }

        _particles.update {
            it + celebrationParticles
        }
    }
}
