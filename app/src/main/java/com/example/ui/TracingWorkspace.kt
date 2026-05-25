package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.GameParticle
import com.example.viewmodel.GameViewModel
import com.example.viewmodel.PaintedStroke
import kotlinx.coroutines.isActive
import java.util.Random
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TracingWorkspace(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current

    // Infinite transition for floating cloud animation
    val infiniteTransition = rememberInfiniteTransition(label = "clouds")
    val cloudOffset1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "cloud1"
    )
    val cloudOffset2 by infiniteTransition.animateFloat(
        initialValue = 700f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "cloud2"
    )

    // Pulse animation for guides / target triangle
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000 / 60, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    // Local tone generator helper for short playful beep
    val toneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                // Defensive catch
            }
        }
    }

    // Text to Speech for Indonesian feedback
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        try {
            ttsInstance = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val localeId = Locale("id", "ID")
                        val result = ttsInstance?.setLanguage(localeId)
                        if (result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.LANG_MISSING_DATA) {
                            ttsInstance?.setLanguage(Locale.getDefault())
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("TracingWorkspace", "Failed to initialize TextToSpeech", e)
        }
        tts = ttsInstance
        onDispose {
            try {
                ttsInstance?.shutdown()
            } catch (e: Throwable) {
                // ignore
            }
        }
    }

    // Tick the particle system frame logic
    LaunchedEffect(viewModel) {
        while (isActive) {
            if (viewModel.particles.value.isNotEmpty()) {
                withFrameNanos {
                    viewModel.updateParticles()
                }
            } else {
                kotlinx.coroutines.delay(16)
            }
        }
    }

    // Play chime on completing strokes or level
    var lastCompletedStrokeCount by remember { mutableStateOf(0) }
    val totalCompletedCount = uiState.paintedStrokesUpper.size + uiState.paintedStrokesLower.size
    LaunchedEffect(totalCompletedCount) {
        if (totalCompletedCount > lastCompletedStrokeCount) {
            try {
                if (uiState.isLetterCompleted) {
                    // Celebration fanfare beep sequence!
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_1, 120)
                    kotlinx.coroutines.delay(140)
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_5, 120)
                    kotlinx.coroutines.delay(140)
                    toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 120)
                    kotlinx.coroutines.delay(140)
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 450)

                    // Indonesian TTS feedback
                    val alphabetName = uiState.selectedChar.toString()
                    val textToSpeak = "Bagus sekali! Kamu berhasil melukis huruf $alphabetName!"
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "letter_completed_tts")
                } else {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                }
            } catch (e: Throwable) {
                // Defensive catch if not supported
            }
            lastCompletedStrokeCount = totalCompletedCount
        } else if (totalCompletedCount == 0) {
            lastCompletedStrokeCount = 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF81D4FA), // Playful Sky Blue
                            Color(0xFFB3E5FC),
                            Color(0xFFE1F5FE)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            // Background Vector Clouds
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Cloud 1
                drawCloud(Offset(cloudOffset1.dp.toPx(), 80.dp.toPx()), 0.8f)
                // Cloud 2
                drawCloud(Offset(cloudOffset2.dp.toPx(), 220.dp.toPx()), 0.5f)
                // Static decorative clouds
                drawCloud(Offset(120.dp.toPx(), 450.dp.toPx()), 0.6f)
                drawCloud(Offset(500.dp.toPx(), 130.dp.toPx()), 0.7f)
            }

            // Foreground Layout containing: Header, Workspace Card, Bottom Selection
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top Section - Custom Styled Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Floating Restart / Reset button
                    Card(
                        modifier = Modifier
                            .testTag("reset_button")
                            .clickable { viewModel.resetCurrentLetter() },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF7043)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Restart",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ulangi",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Large Fun Title Banner
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tris & Lukis",
                            color = Color(0xFF0D47A1),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ikuti garis dan warnai!",
                            color = Color(0xFF1565C0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Display Current Letter Tracker
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.selectedChar.toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // 2. Center Section - Drawing Slate Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .widthIn(max = 600.dp)
                        .shadow(16.dp, RoundedCornerShape(32.dp))
                        .background(Color.White, RoundedCornerShape(32.dp))
                        .border(6.dp, Color(0xFF64B5F6).copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                ) {
                    // Dual canvases separated vertically inside Card (B at top, b at bottom)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        // UPPERCASE HALF
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            TracingCanvas(
                                strokes = LetterRepository.getLetter(uiState.selectedChar).uppercaseStrokes,
                                activeStrokeIndex = uiState.activeStrokeIndex,
                                reachedPointsCount = uiState.reachedPointsCount,
                                activePattern = uiState.activePattern,
                                isSectionActive = uiState.isUppercaseActive && !uiState.isLetterCompleted,
                                paintedStrokes = uiState.paintedStrokesUpper,
                                pulseScale = pulseScale,
                                onDragNormalized = { viewModel.onTraceDrag(it) }
                            )

                            // Title helper inside canvas background
                            Text(
                                text = "HURUF BESAR",
                                color = if (uiState.isUppercaseActive && !uiState.isLetterCompleted) Color(0xFF1565C0) else Color(0x33000000),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }

                        // Divider Line inside Card
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(
                                    Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )

                        // LOWERCASE HALF
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            TracingCanvas(
                                strokes = LetterRepository.getLetter(uiState.selectedChar).lowercaseStrokes,
                                activeStrokeIndex = uiState.activeStrokeIndex,
                                reachedPointsCount = uiState.reachedPointsCount,
                                activePattern = uiState.activePattern,
                                isSectionActive = !uiState.isUppercaseActive && !uiState.isLetterCompleted,
                                paintedStrokes = uiState.paintedStrokesLower,
                                pulseScale = pulseScale,
                                onDragNormalized = { viewModel.onTraceDrag(it) }
                            )

                            // Title helper inside canvas background
                            Text(
                                text = "HURUF KECIL",
                                color = if (!uiState.isUppercaseActive && !uiState.isLetterCompleted) Color(0xFF1565C0) else Color(0x33000000),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                            )
                        }
                    }

                    // Render Sparkle Particles directly over Slate using an isolated flow collector to avoid general layout recomposition
                    SparklesOverlay(
                        particlesFlow = viewModel.particles,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Left navigation chevron on top of Canvas natively
                    FloatingChevronButton(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Sebelumnya",
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp),
                        onClick = { viewModel.prevLetter() }
                    )

                    // Right navigation chevron on top of Canvas natively
                    FloatingChevronButton(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Berikutnya",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp),
                        onClick = { viewModel.nextLetter() }
                    )

                    // 4. Celebrations Completion Overlay
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.isLetterCompleted,
                        enter = fadeIn(tween(400)) + scaleIn(tween(500, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(300)),
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Large Animated Star
                                val breathingScale by animateFloatAsState(
                                    targetValue = pulseScale,
                                    label = "br"
                                )
                                Box(
                                    modifier = Modifier
                                        .scale(breathingScale * 1.2f)
                                        .size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawStarParticle(
                                            center = Offset(size.width / 2, size.height / 2),
                                            size = 90.dp.toPx(),
                                            color = Color(0xFFFFD54F)
                                        )
                                    }
                                    Text(
                                        text = "🏆",
                                        fontSize = 44.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Bagus Sekali!",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Kamu berhasil melukis huruf '${uiState.selectedChar}'!",
                                    color = Color(0xFF1B5E20),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Button Restart
                                    Button(
                                        onClick = { viewModel.resetCurrentLetter() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, "Retry", tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ulang", color = Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    // Button Next
                                    Button(
                                        onClick = { viewModel.nextLetter() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Text("Teruskan", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Bottom Section - Palette selections for colors and patterns (Vibrant circles shelf)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = "Pilih Pola Lukisan:",
                        color = Color(0xFF0D47A1).copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ALL_PATTERNS.forEachIndexed { index, pattern ->
                            val isSelected = uiState.activePattern.displayName == pattern.displayName
                            val selectScale by animateFloatAsState(if (isSelected) 1.2f else 1.0f, label = "sel")

                            Box(
                                modifier = Modifier
                                    .scale(selectScale)
                                    .size(52.dp)
                                    .shadow(if (isSelected) 8.dp else 2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 4.dp else 2.dp,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.selectPattern(pattern) }
                            ) {
                                // Procedural drawing for circular palette preview
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val scopePath = Path().apply {
                                        addOval(Rect(Offset.Zero, size))
                                    }
                                    drawPatternPath(
                                        path = scopePath,
                                        pattern = pattern,
                                        strokeWidth = 0f,
                                        strokeIndex = index + 10
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TracingCanvas(
    strokes: List<GameStroke>,
    activeStrokeIndex: Int,
    reachedPointsCount: Int,
    activePattern: PatternStyle,
    isSectionActive: Boolean,
    paintedStrokes: List<PaintedStroke>,
    pulseScale: Float,
    onDragNormalized: (Offset) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isSectionActive) {
                if (isSectionActive) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val normalized = Offset(
                            x = change.position.x / size.width,
                            y = change.position.y / size.height
                        )
                        // Constrain coordinates safely (0.0f .. 1.0f)
                        val constrained = Offset(
                            x = normalized.x.coerceIn(0f, 1f),
                            y = normalized.y.coerceIn(0f, 1f)
                        )
                        onDragNormalized(constrained)
                    }
                }
            }
    ) {
        val trackWidth = 44.dp.toPx()

        // 1. Draw PREVIOUSLY finished strokes in this canvas section
        paintedStrokes.forEachIndexed { strokeIdx, painted ->
            if (painted.points.size > 1) {
                val path = constructStrokePath(painted.points, size)
                drawPatternPath(
                    points = painted.points,
                    path = path,
                    pattern = painted.pattern,
                    strokeWidth = trackWidth,
                    strokeIndex = strokeIdx
                )
            }
        }

        // Avoid rendering beyond range bounds
        if (activeStrokeIndex < strokes.size) {
            val stroke = strokes[activeStrokeIndex]
            val points = stroke.points

            // 2. Draw ALL FUTURE guidelines tracks first
            strokes.forEachIndexed { strokeIdx, s ->
                if (strokeIdx > activeStrokeIndex || (strokeIdx == activeStrokeIndex && !isSectionActive)) {
                    // Draw light grey uncompleted track bounds
                    if (s.points.size > 1) {
                        val trackPath = constructStrokePath(s.points, size)

                        // Outer thin borderline
                        drawPath(
                            path = trackPath,
                            color = Color(0xFFE3F2FD),
                            style = Stroke(width = trackWidth + 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Main track fill
                        drawPath(
                            path = trackPath,
                            color = Color(0xFFF5F5F5),
                            style = Stroke(width = trackWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Dotted center guide line inside upcoming strokes
                        s.points.forEach { pt ->
                            drawCircle(
                                color = Color(0xFFBDBDBD),
                                radius = 2.5f.dp.toPx(),
                                center = Offset(pt.x * size.width, pt.y * size.height)
                            )
                        }
                    }
                }
            }

            // 3. Draw ACTIVE stroke (with guides, dots and active painted prefix)
            if (isSectionActive && points.isNotEmpty()) {
                val fullTrackPath = constructStrokePath(points, size)

                // Fill guideline tracks
                drawPath(
                    path = fullTrackPath,
                    color = Color(0xFFFFECB3), // Golden amber highlight for current track
                    style = Stroke(width = trackWidth + 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = fullTrackPath,
                    color = Color(0xFFFFFDE7),
                    style = Stroke(width = trackWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw completed painted prefix of current active stroke
                if (reachedPointsCount > 1) {
                    val completedPoints = points.take(reachedPointsCount)
                    val completedPath = constructStrokePath(completedPoints, size)
                    drawPatternPath(
                        points = completedPoints,
                        path = completedPath,
                        pattern = activePattern,
                        strokeWidth = trackWidth,
                        strokeIndex = activeStrokeIndex
                    )
                }

                // Draw guide dots along remaining unreached section of current stroke
                for (i in reachedPointsCount until points.size) {
                    val pt = points[i]
                    drawCircle(
                        color = Color(0xFFFFB300), // Rich amber guide circle
                        radius = 4.dp.toPx(),
                        center = Offset(pt.x * size.width, pt.y * size.height)
                    )
                }

                // 4. Draw guiding arrow and hand helper at target point
                val targetIdx = reachedPointsCount
                if (targetIdx < points.size) {
                    val targetPt = points[targetIdx]
                    val px = targetPt.x * size.width
                    val py = targetPt.y * size.height

                    // Determine tangent orientation direction arrow
                    val angleRad = if (targetIdx > 0) {
                        val prevPt = points[targetIdx - 1]
                        atan2(targetPt.y - prevPt.y, targetPt.x - prevPt.x)
                    } else if (points.size > 1) {
                        val nextPt = points[1]
                        atan2(nextPt.y - targetPt.y, nextPt.x - targetPt.x)
                    } else {
                        0f
                    }

                    // Draw orange triangle arrow showing writing direction
                    withTransform({
                        translate(px, py)
                        rotate(angleRad * 180f / PI.toFloat(), pivot = Offset.Zero)
                        scale(pulseScale, pulseScale, pivot = Offset.Zero)
                    }) {
                        val trianglePath = Path().apply {
                            moveTo(10.dp.toPx(), 0f)
                            lineTo(-8.dp.toPx(), -8.dp.toPx())
                            lineTo(-8.dp.toPx(), 8.dp.toPx())
                            close()
                        }
                        drawPath(
                            path = trianglePath,
                            color = Color(0xFFFF3D00)
                        )
                    }

                    // Visual pointer hand floating near active guide
                    if (targetIdx == 0) {
                        // Drawing child finger tap-guide indicator if idle at start point
                        drawCircle(
                            color = Color(0xFFFF3D00).copy(alpha = 0.3f),
                            radius = (16f * pulseScale).dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}

// Helper to construct graphics line path from coordinate list
private fun constructStrokePath(points: List<Offset>, canvasSize: Size): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x * canvasSize.width, points[0].y * canvasSize.height)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x * canvasSize.width, points[i].y * canvasSize.height)
    }
    return path
}

// Procedural pattern filler
fun DrawScope.drawPatternPath(
    points: List<Offset>? = null,
    path: Path,
    pattern: PatternStyle,
    strokeWidth: Float,
    strokeIndex: Int
) {
    if (points != null && strokeWidth > 0f) {
        // High-performance procedural drawing directly along the stroke points
        val pixelPoints = points.map { Offset(it.x * size.width, it.y * size.height) }
        val spacing = 16.dp.toPx().coerceAtLeast(1f)
        val interpolated = mutableListOf<Offset>()
        
        for (i in 0 until pixelPoints.size - 1) {
            val p1 = pixelPoints[i]
            val p2 = pixelPoints[i + 1]
            val dx = p2.x - p1.x
            val dy = p2.y - p1.y
            val len = kotlin.math.sqrt(dx * dx + dy * dy)
            val steps = (len / spacing).toInt().coerceAtLeast(1).coerceAtMost(25)
            for (s in 0 until steps) {
                val t = s.toFloat() / steps
                interpolated.add(Offset(p1.x + dx * t, p1.y + dy * t))
            }
        }
        if (pixelPoints.isNotEmpty()) {
            interpolated.add(pixelPoints.last())
        }

        // 1. Draw solid background thick stroke
        drawPath(
            path = path,
            color = pattern.thumbnailBgColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val rand = java.util.Random((strokeIndex * 37 + pattern.displayName.hashCode()).toLong())
        val maxPointsToDraw = 40
        val drawStep = (interpolated.size / maxPointsToDraw).coerceAtLeast(1)

        // 2. Draw pattern texture over the background stroke
        when (pattern) {
            is PatternStyle.PolkaDots -> {
                for (idx in 0 until interpolated.size step drawStep) {
                    val pt = interpolated[idx]
                    val dotColor = pattern.dotsColor[rand.nextInt(pattern.dotsColor.size)]
                    val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
                    val dist = rand.nextFloat() * (strokeWidth * 0.22f)
                    val offsetPt = Offset(
                        pt.x + kotlin.math.cos(angle) * dist,
                        pt.y + kotlin.math.sin(angle) * dist
                    )
                    drawCircle(
                        color = dotColor,
                        radius = 4.dp.toPx() + rand.nextFloat() * 3.dp.toPx(),
                        center = offsetPt
                    )
                }
            }
            is PatternStyle.Confetti -> {
                for (idx in 0 until interpolated.size step drawStep) {
                    val pt = interpolated[idx]
                    val col = pattern.shapeColors[rand.nextInt(pattern.shapeColors.size)]
                    val shapeSize = 6.dp.toPx() + rand.nextFloat() * 8.dp.toPx()
                    val angle = rand.nextFloat() * 360f
                    val dist = rand.nextFloat() * (strokeWidth * 0.22f)
                    val offsetAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
                    val cx = pt.x + kotlin.math.cos(offsetAngle) * dist
                    val cy = pt.y + kotlin.math.sin(offsetAngle) * dist

                    rotate(degrees = angle, pivot = Offset(cx, cy)) {
                        drawRect(
                            color = col,
                            topLeft = Offset(cx - shapeSize / 2, cy - shapeSize / 2),
                            size = Size(shapeSize, shapeSize)
                        )
                    }
                }
            }
            is PatternStyle.StarrySky -> {
                for (idx in 0 until interpolated.size step drawStep) {
                    val pt = interpolated[idx]
                    if (rand.nextFloat() > 0.35f) {
                        val r = 4.dp.toPx() + rand.nextFloat() * 4.dp.toPx()
                        val dist = rand.nextFloat() * (strokeWidth * 0.2f)
                        val offsetAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
                        val cx = pt.x + kotlin.math.cos(offsetAngle) * dist
                        val cy = pt.y + kotlin.math.sin(offsetAngle) * dist

                        drawLine(
                            color = pattern.starColor,
                            start = Offset(cx - r, cy),
                            end = Offset(cx + r, cy),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = pattern.starColor,
                            start = Offset(cx, cy - r),
                            end = Offset(cx, cy + r),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
            is PatternStyle.RainbowStripes -> {
                // To display perfect rainbow segment sequence along the curves
                for (i in 0 until pixelPoints.size - 1) {
                    val p1 = pixelPoints[i]
                    val p2 = pixelPoints[i + 1]
                    val color = pattern.colors[i % pattern.colors.size]
                    drawLine(
                        color = color,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            is PatternStyle.Bubbles -> {
                for (idx in 0 until interpolated.size step drawStep) {
                    val pt = interpolated[idx]
                    if (rand.nextFloat() > 0.35f) {
                        val radius = 8.dp.toPx() + rand.nextFloat() * 8.dp.toPx()
                        val dist = rand.nextFloat() * (strokeWidth * 0.2f)
                        val offsetAngle = rand.nextFloat() * 2f * Math.PI.toFloat()
                        val cx = pt.x + kotlin.math.cos(offsetAngle) * dist
                        val cy = pt.y + kotlin.math.sin(offsetAngle) * dist

                        drawCircle(
                            color = pattern.bubbleColor,
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0x44FFFFFF),
                            radius = radius * 0.6f,
                            center = Offset(cx - radius * 0.3f, cy - radius * 0.3f)
                        )
                    }
                }
            }
            is PatternStyle.SolidNeon -> {
                // Glow effect to center
                drawPath(
                    path = path,
                    color = pattern.neonColor,
                    style = Stroke(width = strokeWidth * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = strokeWidth * 0.32f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    } else {
        // Fallback or when strokeWidth is 0f (like circular pattern preview in shelf)
        clipPath(path) {
            // Flat background base
            drawRect(color = pattern.thumbnailBgColor)

            val rand = java.util.Random((strokeIndex * 37 + pattern.displayName.hashCode()).toLong())

            when (pattern) {
                is PatternStyle.PolkaDots -> {
                    val step = 28f
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        for (y in 0..size.height.toInt() step step.toInt()) {
                            if (rand.nextFloat() > 0.45f) {
                                val dotColor = pattern.dotsColor[rand.nextInt(pattern.dotsColor.size)]
                                val jitterX = (rand.nextFloat() - 0.5f) * step * 0.4f
                                val jitterY = (rand.nextFloat() - 0.5f) * step * 0.4f
                                drawCircle(
                                    color = dotColor,
                                    radius = 4f + rand.nextFloat() * 7f,
                                    center = Offset(x.toFloat() + jitterX, y.toFloat() + jitterY)
                                )
                            }
                        }
                    }
                }
                is PatternStyle.Confetti -> {
                    val step = 32f
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        for (y in 0..size.height.toInt() step step.toInt()) {
                            if (rand.nextFloat() > 0.5f) {
                                val color = pattern.shapeColors[rand.nextInt(pattern.shapeColors.size)]
                                val jitterX = (rand.nextFloat() - 0.5f) * step * 0.5f
                                val jitterY = (rand.nextFloat() - 0.5f) * step * 0.5f
                                val shapeSize = 5f + rand.nextFloat() * 10f
                                val angle = rand.nextFloat() * 360f

                                val cx = x.toFloat() + jitterX
                                val cy = y.toFloat() + jitterY

                                rotate(degrees = angle, pivot = Offset(cx, cy)) {
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(cx - shapeSize / 2, cy - shapeSize / 2),
                                        size = Size(shapeSize, shapeSize)
                                    )
                                }
                            }
                        }
                    }
                }
                is PatternStyle.StarrySky -> {
                    val count = 25
                    for (i in 0..count) {
                        val sx = rand.nextFloat() * size.width
                        val sy = rand.nextFloat() * size.height
                        val r = 2.5f + rand.nextFloat() * 5f
                        // Draw cross star shape
                        drawLine(
                            color = pattern.starColor,
                            start = Offset(sx - r, sy),
                            end = Offset(sx + r, sy),
                            strokeWidth = 2.5f
                        )
                        drawLine(
                            color = pattern.starColor,
                            start = Offset(sx, sy - r),
                            end = Offset(sx, sy + r),
                            strokeWidth = 2.5f
                        )
                    }
                }
                is PatternStyle.RainbowStripes -> {
                    val stripeWidth = 24f
                    val colors = pattern.colors
                    val totalWidth = size.width + size.height
                    var currentX = -totalWidth
                    var colorIdx = 0
                    while (currentX < totalWidth) {
                        val col = colors[colorIdx % colors.size]
                        val stripePath = Path().apply {
                            moveTo(currentX, 0f)
                            lineTo(currentX + stripeWidth, 0f)
                            lineTo(currentX + stripeWidth - size.height, size.height)
                            lineTo(currentX - size.height, size.height)
                            close()
                        }
                        drawPath(stripePath, color = col)
                        currentX += stripeWidth
                        colorIdx++
                    }
                }
                is PatternStyle.Bubbles -> {
                    val bubbleCount = 18
                    for (i in 0..bubbleCount) {
                        val bx = rand.nextFloat() * size.width
                        val by = rand.nextFloat() * size.height
                        val radius = 10f + rand.nextFloat() * 18f
                        drawCircle(
                            color = pattern.bubbleColor,
                            radius = radius,
                            center = Offset(bx, by),
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = radius * 0.6f,
                            center = Offset(bx - radius * 0.3f, by - radius * 0.3f)
                        )
                    }
                }
                is PatternStyle.SolidNeon -> {
                    val gradient = Brush.linearGradient(
                        colors = listOf(pattern.thumbnailBgColor, Color(0xFF00E676), pattern.thumbnailBgColor)
                    )
                    drawRect(brush = gradient)
                }
            }
        }
    }
}

// Vector cloud drawing helper
private fun DrawScope.drawCloud(center: Offset, scale: Float) {
    val baseRadius = 30.dp.toPx() * scale
    val color = Color.White.copy(alpha = 0.85f)

    drawCircle(color, baseRadius * 0.8f, center - Offset(baseRadius * 0.9f, 0f))
    drawCircle(color, baseRadius * 1.1f, center)
    drawCircle(color, baseRadius * 0.8f, center + Offset(baseRadius * 0.9f, 0f))
    drawCircle(color, baseRadius * 0.7f, center - Offset(baseRadius * 0.3f, -baseRadius * 0.3f))
    drawCircle(color, baseRadius * 0.7f, center + Offset(baseRadius * 0.3f, -baseRadius * 0.3f))
}

// 4 pointed star particle helper
private fun DrawScope.drawStarParticle(center: Offset, size: Float, color: Color) {
    val r = size / 2
    val path = Path().apply {
        moveTo(center.x, center.y - r)
        quadraticBezierTo(center.x, center.y, center.x + r, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + r)
        quadraticBezierTo(center.x, center.y, center.x - r, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(path = path, color = color)
}

@Composable
fun FloatingChevronButton(
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .border(2.dp, Color(0xFFBBDEFB), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun SparklesOverlay(
    particlesFlow: kotlinx.coroutines.flow.StateFlow<List<com.example.viewmodel.GameParticle>>,
    modifier: Modifier = Modifier
) {
    val particles by particlesFlow.collectAsState()
    Canvas(modifier = modifier) {
        for (p in particles) {
            val px = p.x * size.width
            val py = p.y * size.height

            rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                when (p.shape) {
                    "star" -> drawStarParticle(Offset(px, py), p.size, p.color.copy(alpha = p.alpha))
                    "confetti" -> {
                        drawRect(
                            color = p.color.copy(alpha = p.alpha),
                            topLeft = Offset(px - p.size / 2, py - p.size / 2),
                            size = Size(p.size, p.size / 2)
                        )
                    }
                    else -> {
                        drawCircle(
                            color = p.color.copy(alpha = p.alpha),
                            radius = p.size / 2,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
