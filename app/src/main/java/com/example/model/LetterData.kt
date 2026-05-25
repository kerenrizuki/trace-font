package com.example.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class GameStroke(
    val points: List<Offset>
)

data class GameLetter(
    val char: Char,
    val uppercaseStrokes: List<GameStroke>,
    val lowercaseStrokes: List<GameStroke>
)

object LetterRepository {
    private fun line(x1: Float, y1: Float, x2: Float, y2: Float, steps: Int = 18): GameStroke {
        val pts = mutableListOf<Offset>()
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            pts.add(Offset(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t))
        }
        return GameStroke(pts)
    }

    private fun arc(
        cx: Float, cy: Float, rx: Float, ry: Float,
        startAngleDegrees: Float, sweepAngleDegrees: Float,
        steps: Int = 18
    ): GameStroke {
        val pts = mutableListOf<Offset>()
        val startRad = startAngleDegrees * PI.toFloat() / 180f
        val sweepRad = sweepAngleDegrees * PI.toFloat() / 180f
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val angle = startRad + sweepRad * t
            pts.add(Offset(cx + rx * cos(angle), cy + ry * sin(angle)))
        }
        return GameStroke(pts)
    }

    fun getLetter(char: Char): GameLetter {
        val upper = mutableListOf<GameStroke>()
        val lower = mutableListOf<GameStroke>()

        when (char.uppercaseChar()) {
            'A' -> {
                // Main left diagonal up, then right diagonal down, then crossbar
                upper.add(line(0.25f, 0.85f, 0.5f, 0.15f))
                upper.add(line(0.5f, 0.15f, 0.75f, 0.85f))
                upper.add(line(0.35f, 0.55f, 0.65f, 0.55f))

                // lowercase a: round bowl with touching stem on the right
                lower.add(arc(0.5f, 0.6f, 0.2f, 0.2f, 0f, -360f))
                lower.add(line(0.7f, 0.4f, 0.7f, 0.8f))
            }
            'B' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(arc(0.3f, 0.325f, 0.25f, 0.175f, -90f, 180f))
                upper.add(arc(0.3f, 0.675f, 0.28f, 0.175f, -90f, 180f))

                // lowercase b: long vertical stem, bottom loop to the right
                lower.add(line(0.32f, 0.15f, 0.32f, 0.78f))
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, 180f, 360f))
            }
            'C' -> {
                upper.add(arc(0.55f, 0.5f, 0.25f, 0.35f, 315f, -270f))

                // lowercase c
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, 315f, -270f))
            }
            'D' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(arc(0.3f, 0.5f, 0.35f, 0.35f, -90f, 180f))

                // lowercase d: left round circle, long right stem
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, 0f, 360f))
                lower.add(line(0.68f, 0.15f, 0.68f, 0.78f))
            }
            'E' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(line(0.3f, 0.15f, 0.7f, 0.15f))
                upper.add(line(0.3f, 0.5f, 0.6f, 0.5f))
                upper.add(line(0.3f, 0.85f, 0.7f, 0.85f))

                // lowercase e: center loop starting horizontal, then arching counter-clockwise
                val ePoints = mutableListOf<Offset>()
                for (i in 0..8) {
                    val t = i.toFloat() / 8f
                    ePoints.add(Offset(0.32f + (0.68f - 0.32f) * t, 0.6f))
                }
                val arcStroke = arc(0.5f, 0.6f, 0.18f, 0.18f, 0f, -300f)
                ePoints.addAll(arcStroke.points)
                lower.add(GameStroke(ePoints))
            }
            'F' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(line(0.3f, 0.15f, 0.7f, 0.15f))
                upper.add(line(0.3f, 0.5f, 0.6f, 0.5f))

                // lowercase f: curved hook top to bottom stem, crossbar
                val fStem = mutableListOf<Offset>()
                val fArc = arc(0.55f, 0.3f, 0.1f, 0.15f, 0f, -180f)
                fStem.addAll(fArc.points)
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.3f + (0.78f - 0.3f) * t
                    fStem.add(Offset(0.45f, y))
                }
                lower.add(GameStroke(fStem))
                lower.add(line(0.35f, 0.42f, 0.55f, 0.42f))
            }
            'G' -> {
                upper.add(arc(0.55f, 0.5f, 0.25f, 0.35f, -45f, -315f))
                upper.add(line(0.8f, 0.5f, 0.62f, 0.5f))

                // lowercase g: circle, then stem hanging down curving left
                lower.add(arc(0.5f, 0.6f, 0.15f, 0.15f, 0f, -360f))
                val tailPoints = mutableListOf<Offset>()
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.45f + (0.82f - 0.45f) * t
                    tailPoints.add(Offset(0.65f, y))
                }
                val arcStroke = arc(0.475f, 0.82f, 0.175f, 0.15f, 0f, 130f)
                tailPoints.addAll(arcStroke.points)
                lower.add(GameStroke(tailPoints))
            }
            'H' -> {
                upper.add(line(0.25f, 0.15f, 0.25f, 0.85f))
                upper.add(line(0.75f, 0.15f, 0.75f, 0.85f))
                upper.add(line(0.25f, 0.5f, 0.75f, 0.5f))

                // lowercase h: stem, then hump
                lower.add(line(0.32f, 0.15f, 0.32f, 0.78f))
                val hump = mutableListOf<Offset>()
                val hArc = arc(0.5f, 0.6f, 0.18f, 0.18f, 180f, -180f)
                hump.addAll(hArc.points)
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.6f + (0.78f - 0.6f) * t
                    hump.add(Offset(0.68f, y))
                }
                lower.add(GameStroke(hump))
            }
            'I' -> {
                upper.add(line(0.5f, 0.15f, 0.5f, 0.85f))
                upper.add(line(0.3f, 0.15f, 0.7f, 0.15f))
                upper.add(line(0.3f, 0.85f, 0.7f, 0.85f))

                // lowercase i: stem, dot
                lower.add(line(0.5f, 0.42f, 0.5f, 0.78f))
                lower.add(GameStroke(listOf(Offset(0.5f, 0.28f))))
            }
            'J' -> {
                upper.add(line(0.3f, 0.15f, 0.7f, 0.15f))
                upper.add(line(0.5f, 0.15f, 0.5f, 0.7f))
                upper.add(arc(0.4f, 0.7f, 0.1f, 0.15f, 0f, 180f))

                // lowercase j: hanging stem with hook, dot
                val jStem = mutableListOf<Offset>()
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.42f + (0.78f - 0.42f) * t
                    jStem.add(Offset(0.55f, y))
                }
                val jArc = arc(0.45f, 0.78f, 0.1f, 0.15f, 0f, 180f)
                jStem.addAll(jArc.points)
                lower.add(GameStroke(jStem))
                lower.add(GameStroke(listOf(Offset(0.55f, 0.28f))))
            }
            'K' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(line(0.7f, 0.15f, 0.32f, 0.5f))
                upper.add(line(0.32f, 0.5f, 0.7f, 0.85f))

                // lowercase k: vertical stem, smaller legs
                lower.add(line(0.35f, 0.2f, 0.35f, 0.8f))
                lower.add(line(0.65f, 0.45f, 0.37f, 0.6f))
                lower.add(line(0.37f, 0.6f, 0.65f, 0.8f))
            }
            'L' -> {
                upper.add(line(0.35f, 0.15f, 0.35f, 0.85f))
                upper.add(line(0.35f, 0.85f, 0.7f, 0.85f))

                // lowercase l: vertical stem
                lower.add(line(0.5f, 0.2f, 0.5f, 0.8f))
            }
            'M' -> {
                upper.add(line(0.2f, 0.85f, 0.2f, 0.15f))
                upper.add(line(0.2f, 0.15f, 0.5f, 0.55f))
                upper.add(line(0.5f, 0.55f, 0.8f, 0.15f))
                upper.add(line(0.8f, 0.15f, 0.8f, 0.85f))

                // lowercase m: short vertical stem, first hump, second hump
                lower.add(line(0.24f, 0.42f, 0.24f, 0.78f))
                val hump1 = mutableListOf<Offset>()
                val arc1 = arc(0.365f, 0.6f, 0.125f, 0.18f, 180f, -180f)
                hump1.addAll(arc1.points)
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.6f + (0.78f - 0.6f) * t
                    hump1.add(Offset(0.49f, y))
                }
                val hump2 = mutableListOf<Offset>()
                val arc2 = arc(0.615f, 0.6f, 0.125f, 0.18f, 180f, -180f)
                hump2.addAll(arc2.points)
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.6f + (0.78f - 0.6f) * t
                    hump2.add(Offset(0.74f, y))
                }
                lower.add(GameStroke(hump1))
                lower.add(GameStroke(hump2))
            }
            'N' -> {
                upper.add(line(0.25f, 0.85f, 0.25f, 0.15f))
                upper.add(line(0.25f, 0.15f, 0.75f, 0.85f))
                upper.add(line(0.75f, 0.85f, 0.75f, 0.15f))

                // lowercase n: short stem, hump
                lower.add(line(0.32f, 0.42f, 0.32f, 0.78f))
                val humpN = mutableListOf<Offset>()
                val arcN = arc(0.5f, 0.6f, 0.18f, 0.18f, 180f, -180f)
                humpN.addAll(arcN.points)
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.6f + (0.78f - 0.6f) * t
                    humpN.add(Offset(0.68f, y))
                }
                lower.add(GameStroke(humpN))
            }
            'O' -> {
                upper.add(arc(0.5f, 0.5f, 0.25f, 0.35f, -90f, -360f))

                // lowercase o: smaller circle
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, -90f, -360f))
            }
            'P' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(arc(0.3f, 0.35f, 0.25f, 0.2f, -90f, 180f))

                // lowercase p: long stem going down, right loop
                lower.add(line(0.32f, 0.42f, 0.32f, 0.98f))
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, 180f, 360f))
            }
            'Q' -> {
                upper.add(arc(0.5f, 0.5f, 0.25f, 0.35f, -90f, -360f))
                upper.add(line(0.6f, 0.65f, 0.8f, 0.85f))

                // lowercase q: left loop, long stem going down with tail
                lower.add(arc(0.5f, 0.6f, 0.18f, 0.18f, 0f, 360f))
                val qStem = mutableListOf<Offset>()
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.42f + (0.98f - 0.42f) * t
                    qStem.add(Offset(0.68f, y))
                }
                qStem.add(Offset(0.76f, 0.93f))
                lower.add(GameStroke(qStem))
            }
            'R' -> {
                upper.add(line(0.3f, 0.15f, 0.3f, 0.85f))
                upper.add(arc(0.3f, 0.35f, 0.25f, 0.2f, -90f, 180f))
                upper.add(line(0.32f, 0.55f, 0.7f, 0.85f))

                // lowercase r: stem, small branch right
                lower.add(line(0.35f, 0.42f, 0.35f, 0.78f))
                lower.add(arc(0.48f, 0.52f, 0.13f, 0.13f, 180f, -110f))
            }
            'S' -> {
                // Correctly oriented, beautiful S-curves for uppercase and lowercase
                val pts = mutableListOf<Offset>()
                val stepsS = 32
                for (i in 0..stepsS) {
                    val t = i.toFloat() / stepsS
                    val angle = t * 3.0f * PI.toFloat() - PI.toFloat() / 2f
                    val x = 0.5f - 0.18f * sin(angle)
                    val y = 0.15f + 0.7f * t
                    pts.add(Offset(x, y))
                }
                upper.add(GameStroke(pts))

                // lowercase s
                val ptsL = mutableListOf<Offset>()
                for (i in 0..stepsS) {
                    val t = i.toFloat() / stepsS
                    val angle = t * 3.0f * PI.toFloat() - PI.toFloat() / 2f
                    val x = 0.5f - 0.14f * sin(angle)
                    val y = 0.42f + 0.36f * t
                    ptsL.add(Offset(x, y))
                }
                lower.add(GameStroke(ptsL))
            }
            'T' -> {
                upper.add(line(0.2f, 0.15f, 0.8f, 0.15f))
                upper.add(line(0.5f, 0.15f, 0.5f, 0.85f))

                // lowercase t: cross, then vertical with slight hook at bottom
                val tStem = mutableListOf<Offset>()
                for (i in 0..10) {
                    val t = i.toFloat() / 10f
                    val y = 0.25f + (0.7f - 0.25f) * t
                    tStem.add(Offset(0.5f, y))
                }
                val tArc = arc(0.58f, 0.7f, 0.08f, 0.08f, 180f, -90f)
                tStem.addAll(tArc.points)
                lower.add(GameStroke(tStem))
                lower.add(line(0.38f, 0.42f, 0.62f, 0.42f))
            }
            'U' -> {
                upper.add(line(0.25f, 0.15f, 0.25f, 0.6f))
                upper.add(arc(0.5f, 0.6f, 0.25f, 0.25f, 180f, -180f))
                upper.add(line(0.75f, 0.60f, 0.75f, 0.15f))

                // lowercase u
                val uBowl = mutableListOf<Offset>()
                for (i in 0..5) {
                    val t = i.toFloat() / 5f
                    val y = 0.42f + (0.6f - 0.42f) * t
                    uBowl.add(Offset(0.32f, y))
                }
                val uArc = arc(0.5f, 0.6f, 0.18f, 0.18f, 180f, -180f)
                uBowl.addAll(uArc.points)
                for (i in 0..5) {
                    val t = i.toFloat() / 5f
                    val y = 0.6f - (0.6f - 0.42f) * t
                    uBowl.add(Offset(0.68f, y))
                }
                lower.add(GameStroke(uBowl))
                lower.add(line(0.68f, 0.42f, 0.68f, 0.78f))
            }
            'V' -> {
                upper.add(line(0.25f, 0.15f, 0.5f, 0.85f))
                upper.add(line(0.5f, 0.85f, 0.75f, 0.15f))

                // lowercase v
                lower.add(line(0.32f, 0.42f, 0.5f, 0.78f))
                lower.add(line(0.5f, 0.78f, 0.68f, 0.42f))
            }
            'W' -> {
                upper.add(line(0.2f, 0.15f, 0.35f, 0.85f))
                upper.add(line(0.35f, 0.85f, 0.5f, 0.45f))
                upper.add(line(0.5f, 0.45f, 0.65f, 0.85f))
                upper.add(line(0.65f, 0.85f, 0.8f, 0.15f))

                // lowercase w
                lower.add(line(0.24f, 0.42f, 0.365f, 0.78f))
                lower.add(line(0.365f, 0.78f, 0.49f, 0.55f))
                lower.add(line(0.49f, 0.55f, 0.615f, 0.78f))
                lower.add(line(0.615f, 0.78f, 0.74f, 0.42f))
            }
            'X' -> {
                upper.add(line(0.25f, 0.15f, 0.75f, 0.85f))
                upper.add(line(0.75f, 0.15f, 0.25f, 0.85f))

                // lowercase x
                lower.add(line(0.32f, 0.42f, 0.68f, 0.78f))
                lower.add(line(0.68f, 0.42f, 0.32f, 0.78f))
            }
            'Y' -> {
                upper.add(line(0.25f, 0.15f, 0.5f, 0.45f))
                upper.add(line(0.75f, 0.15f, 0.5f, 0.45f))
                upper.add(line(0.5f, 0.45f, 0.5f, 0.85f))

                // lowercase y: standard simple intersection format
                lower.add(line(0.32f, 0.42f, 0.5f, 0.68f))
                lower.add(line(0.68f, 0.42f, 0.38f, 0.92f))
            }
            'Z' -> {
                upper.add(line(0.25f, 0.2f, 0.75f, 0.2f))
                upper.add(line(0.75f, 0.2f, 0.25f, 0.8f))
                upper.add(line(0.25f, 0.8f, 0.75f, 0.8f))

                // lowercase z
                lower.add(line(0.32f, 0.42f, 0.68f, 0.42f))
                lower.add(line(0.68f, 0.42f, 0.32f, 0.78f))
                lower.add(line(0.32f, 0.78f, 0.68f, 0.78f))
            }
        }

        // Clean up some points of lower elements if they are empty
        if (upper.isEmpty()) {
            upper.add(line(0.3f, 0.3f, 0.7f, 0.7f))
        }
        if (lower.isEmpty()) {
            lower.add(line(0.3f, 0.7f, 0.7f, 0.3f))
        }

        return GameLetter(char, upper, lower)
    }
}
