package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.JournalEntry
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricPink
import com.example.ui.theme.RadiantPurple
import kotlin.math.sin

@Composable
fun AuraPulseChart(
    entries: List<JournalEntry>,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        // Safe animated Tranquil breathing wave
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        val phaseShift by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "breathing_phase"
        )
        val pulseYScale by infiniteTransition.animateFloat(
            initialValue = 15f,
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_scale"
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "✨ Mind Stream Serenity State ✨",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanGlow,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                val path = Path()
                val fillPath = Path()

                path.moveTo(0f, midY)
                fillPath.moveTo(0f, height)
                fillPath.lineTo(0f, midY)

                val points = 100
                for (i in 0..points) {
                    val x = (i / points.toFloat()) * width
                    // Generate pretty sine wave representing breathing cycle
                    val angle = (i / points.toFloat()) * 3 * Math.PI.toFloat() + phaseShift
                    val y = midY + sin(angle) * pulseYScale

                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                val lineBrush = Brush.horizontalGradient(
                    colors = listOf(ElectricPink, RadiantPurple, CyanGlow)
                )
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(CyanGlow.copy(alpha = 0.25f), Color.Transparent)
                )

                drawPath(path = fillPath, brush = fillBrush)
                drawPath(path = path, brush = lineBrush, style = Stroke(width = 3.dp.toPx()))

                // Add aesthetic glowing center point
                drawCircle(
                    brush = lineBrush,
                    radius = 8.dp.toPx(),
                    center = Offset(width / 2f, midY + sin((points / 2f / points.toFloat()) * 3 * Math.PI.toFloat() + phaseShift) * pulseYScale)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Breathe in... Breathe out... Write your first entry to activate your personalized Aura Mind Stats.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    } else {
        // Draw real user session timeline
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📊 Historical Aura Activity Pulse",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricPink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height

                // Take up to last 7 entries for charting
                val chartEntries = entries.take(7).reversed()
                val dataPointsCount = chartEntries.size

                val path = Path()
                val fillPath = Path()

                val lineBrush = Brush.horizontalGradient(
                    colors = listOf(RadiantPurple, ElectricPink, CyanGlow)
                )
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(ElectricPink.copy(alpha = 0.2f), Color.Transparent)
                )

                if (dataPointsCount == 1) {
                    // Draw simple centered pulse
                    val midX = width / 2f
                    val midY = height / 2f
                    drawCircle(brush = lineBrush, radius = 12.dp.toPx(), center = Offset(midX, midY))
                    drawCircle(color = RadiantPurple.copy(alpha = 0.4f), radius = 24.dp.toPx(), center = Offset(midX, midY))
                } else {
                    val stepX = width / (dataPointsCount - 1)

                    // Map mood vibes to Y levels
                    val mapYValue = { mood: String ->
                        when {
                            mood.contains("Spirited", ignoreCase = true) -> height * 0.2f
                            mood.contains("Dreamy", ignoreCase = true) -> height * 0.4f
                            mood.contains("Reflective", ignoreCase = true) -> height * 0.6f
                            mood.contains("Calm", ignoreCase = true) -> height * 0.8f
                            else -> height * 0.5f
                        }
                    }

                    val firstX = 0f
                    val firstY = mapYValue(chartEntries[0].moodVibe)

                    path.moveTo(firstX, firstY)
                    fillPath.moveTo(0f, height)
                    fillPath.lineTo(firstX, firstY)

                    val coords = mutableListOf<Offset>()
                    coords.add(Offset(firstX, firstY))

                    for (i in 1 until dataPointsCount) {
                        val currentX = i * stepX
                        val currentY = mapYValue(chartEntries[i].moodVibe)
                        coords.add(Offset(currentX, currentY))
                    }

                    // Draw smooth natural curves
                    for (i in 0 until coords.size - 1) {
                        val p0 = coords[i]
                        val p1 = coords[i + 1]
                        val controlX = (p0.x + p1.x) / 2
                        path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    }

                    fillPath.lineTo(width, height)
                    fillPath.close()

                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = path, brush = lineBrush, style = Stroke(width = 3.dp.toPx()))

                    // Draw nodes
                    coords.forEachIndexed { index, point ->
                        drawCircle(
                            color = CardBackground,
                            radius = 6.dp.toPx(),
                            center = point
                        )
                        drawCircle(
                            brush = lineBrush,
                            radius = 4.dp.toPx(),
                            center = point
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "🧘 Calm", fontSize = 11.sp, color = SoftText)
                Text(text = "🔮 Reflective", fontSize = 11.sp, color = SoftText)
                Text(text = "⚡ Spirited", fontSize = 11.sp, color = SoftText)
            }
        }
    }
}
